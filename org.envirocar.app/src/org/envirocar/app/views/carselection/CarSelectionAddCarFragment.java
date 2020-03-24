/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 * <p>
 * This file is part of the enviroCar app.
 * <p>
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.views.carselection;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jakewharton.rxbinding3.appcompat.RxToolbar;
import com.jakewharton.rxbinding3.widget.RxTextView;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.handler.DAOProvider;
import org.envirocar.app.handler.preferences.CarPreferenceHandler;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.retrofit.RetrofitClient;
import org.envirocar.app.views.utils.ECAnimationUtils;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.CarImpl;
import org.envirocar.core.entity.Manufacturer;
import org.envirocar.core.logging.Logger;
import org.envirocar.remote.service.VehicleService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class CarSelectionAddCarFragment extends BaseInjectorFragment {
    private static final Logger LOG = Logger.getLogger(CarSelectionAddCarFragment.class);

    private static final int ERROR_DEBOUNCE_TIME = 750;
    private static final int CONSTRUCTION_YEAR_MIN = 1990;
    private static final int CONSTRUCTION_YEAR_MAX = Calendar.getInstance().get(Calendar.YEAR);
    private static final int ENGINE_DISPLACEMENT_MIN = 500;
    private static final int ENGINE_DISPLACEMENT_MAX = 5000;

    @BindView(R.id.envirocar_toolbar)
    protected Toolbar toolbar;
    @BindView(R.id.activity_car_selection_newcar_content_view)
    protected View contentView;
    @BindView(R.id.activity_car_selection_newcar_download_layout)
    protected View downloadView;

    @BindView(R.id.manufacturer_layout)
    protected LinearLayout manufacturerLayout;
    @BindView(R.id.manufacturer_text)
    protected TextView manufacturerText;
    @BindView(R.id.construction_year_layout)
    protected LinearLayout constructionYearLayout;
    @BindView(R.id.construction_year_text)
    protected TextView constructionYearText;
    @BindView(R.id.fuel_type_layout)
    protected LinearLayout fuelTypeLayout;
    @BindView(R.id.fuel_type_text)
    protected TextView fuelTypeText;
    @BindView(R.id.engine_displacement_layout)
    protected LinearLayout engineLayout;
    @BindView(R.id.engine_displacement_text)
    protected TextView engineText;

    @BindView(R.id.sliding_up_panel)
    protected SlidingUpPanelLayout slidingUpPanel;
    @BindView(R.id.brand_list)
    protected RecyclerView brandsRecyclerView;

    @Inject
    protected DAOProvider daoProvider;
    @Inject
    protected CarPreferenceHandler carManager;

    private BrandAdapter brandAdapter;

    private CompositeDisposable disposables = new CompositeDisposable();
    private Scheduler.Worker mainThreadWorker = AndroidSchedulers.mainThread().createWorker();

    private Set<Car> mCars = new HashSet<>();
    private Set<String> mManufacturerNames = new HashSet<>();
    private Map<String, Set<String>> mCarToModelMap = new ConcurrentHashMap<>();
    private Map<String, Set<String>> mModelToYear = new ConcurrentHashMap<>();
    private Map<Pair<String, String>, Set<String>> mModelToCCM = new ConcurrentHashMap<>();


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.activity_car_selection_newcar_fragment, container, false);
        ButterKnife.bind(this, view);

        initViews();
        fetchManufacturers();
        initWatcher();

        return view;
    }

    @SuppressLint("CheckResult")
    private void initViews() {
        // Initialize toolbar menu
        toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
        toolbar.inflateMenu(R.menu.menu_logbook_add_fueling);
        toolbar.setNavigationOnClickListener(v -> {
            hideKeyboard(v);
            closeThisFragment();
        });

        // Initialize views' visibility
        toolbar.setVisibility(View.GONE);
        contentView.setVisibility(View.GONE);
        downloadView.setVisibility(View.INVISIBLE);

        // Initialize recycler views
        brandsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        brandAdapter = new BrandAdapter();
        brandsRecyclerView.setAdapter(brandAdapter);
        manufacturerLayout.setOnClickListener(v -> {

            slidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);
        });

        RxToolbar.itemClicks(toolbar)
//                .filter(continueWhenFormIsCorrect())
                .map(createCarFromForm())
                .filter(continueWhenCarHasCorrectValues())
                .map(checkCarAlreadyExist())
                .subscribeWith(new DisposableObserver<Car>() {
                    @Override
                    public void onComplete() {
                        LOG.info("onCompleted car");
                    }

                    @Override
                    public void onError(Throwable e) {
                        LOG.warn(e.getMessage(), e);
                    }

                    @Override
                    public void onNext(Car car) {
                        LOG.info("car added");
                        ((CarSelectionUiListener) getActivity()).onCarAdded(car);
                        hideKeyboard(getView());
                        closeThisFragment();
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        LOG.info("onResume()");

        ECAnimationUtils.animateShowView(getContext(), toolbar,
                R.anim.translate_slide_in_top_fragment);
        ECAnimationUtils.animateShowView(getContext(), contentView,
                R.anim.translate_slide_in_bottom_fragment);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LOG.info("onDestroy()");

        disposables.clear();
    }

    private void fetchManufacturers() {

        VehicleService vehicleService = RetrofitClient.getRetrofit(getContext()).create(VehicleService.class);

        disposables.add(vehicleService.fetchManufacturers()
                .toFlowable(BackpressureStrategy.BUFFER)
                .onBackpressureBuffer(10000)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .toObservable()
                .subscribeWith(new DisposableObserver<List<Manufacturer>>() {

                    @Override
                    protected void onStart() {
                        LOG.info("onStart() download sensors");
                        downloadView.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onComplete() {
                        LOG.info("onCompleted(): cars successfully downloaded.");

                        mainThreadWorker.schedule(() -> {
                            dispose();
                            downloadView.setVisibility(View.INVISIBLE);
                        });
                    }

                    @Override
                    public void onError(Throwable e) {
                        LOG.error(e.getMessage(), e);
                        mainThreadWorker.schedule(() -> {
                            downloadView.setVisibility(View.INVISIBLE);
                        });
                    }

                    @Override
                    public void onNext(List<Manufacturer> manufacturers) {
                        List<String> manufacturersList = new ArrayList<>();
                        for (Manufacturer manufacturer : manufacturers) {
                            manufacturersList.add(manufacturer.getName());
                        }

                        Collections.sort(manufacturersList, String::compareTo);
                        getActivity().runOnUiThread(() -> brandAdapter.setItems(manufacturersList));
                    }
                }));
    }

    private <T> Function<T, Car> createCarFromForm() {
        return t -> {
            // Get the values
            String[] make = manufacturerText.getText().toString().split("\\s+");
            String manufacturer = make[0];
            String model = make[1];
            String yearString = constructionYearText.getText().toString();
            String engineString = engineText.getText().toString();

            Car.FuelType fueltype = Car.FuelType.getFuelTybeByTranslatedString(getContext(),
                    fuelTypeText.getText().toString());

            // create the car
            int year = Integer.parseInt(yearString);
            if (fueltype != Car.FuelType.ELECTRIC) {
                try {
                    int engine = Integer.parseInt(engineString);
                    return new CarImpl(manufacturer, model, fueltype, year, engine);
                } catch (Exception e) {
                    LOG.error(String.format("Unable to parse engine [%s]", engineString), e);
                }
            }
            return new CarImpl(manufacturer, model, fueltype, year);
        };
    }

    private Predicate<Car> continueWhenCarHasCorrectValues() {
        return car -> {
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            View focusView = null;

            // Check the values of engine and year for validity.
            if (car.getFuelType() != Car.FuelType.ELECTRIC &&
                    (car.getEngineDisplacement() < 500 || car.getEngineDisplacement() > 5000)) {
                engineText.setError(getString(R.string.car_selection_error_invalid_input));
                focusView = engineText;
            }
            if (car.getConstructionYear() < 1990 || car.getConstructionYear() > currentYear) {
                constructionYearText.setError(getString(R.string.car_selection_error_invalid_input));
                focusView = constructionYearText;
            }

            // if tengine or year have invalid values, then request the focus.
            if (focusView != null) {
                focusView.requestFocus();
                return false;
            }

            return true;
        };
    }

    private Function<Car, Car> checkCarAlreadyExist() {
        return car -> {
            String manu = car.getManufacturer();
            String model = car.getModel();
            String year = "" + car.getConstructionYear();
            String engine = "" + car.getEngineDisplacement();
            Pair<String, String> modelYear = new Pair<>(model, year);

            Car selectedCar = null;
            if (mManufacturerNames.contains(manu)
                    && mCarToModelMap.get(manu) != null
                    && mCarToModelMap.get(manu).contains(model)
                    && mModelToYear.get(model) != null
                    && mModelToYear.get(model).contains(year)
                    && mModelToCCM.get(modelYear) != null
                    && mModelToCCM.get(modelYear).contains(engine)) {
                for (Car other : mCars) {
                    if (other.getManufacturer().equals(manu)
                            && other.getModel().equals(model)
                            && other.getConstructionYear() == car.getConstructionYear()
                            && other.getEngineDisplacement() == car.getEngineDisplacement()
                            && other.getFuelType() == car.getFuelType()) {
                        selectedCar = other;
                        break;
                    }
                }
            }

            if (selectedCar == null) {
                LOG.info("New Car type. Register car at server.");
                carManager.registerCarAtServer(car);
                return car;
            } else {
                LOG.info(String.format("Car already existed -> [%s]", selectedCar.getId()));
                return selectedCar;
            }
        };
    }

    private void checkFuelingType() {
        String[] make = manufacturerText.getText().toString().split("\\s+");
        String manufacturer = make[0];
        String model = make[1];
        String engineString = engineText.getText().toString();
        Pair<String, String> modelYear = new Pair<>(model, constructionYearText.toString());

        Car selectedCar = null;
        if (mManufacturerNames.contains(manufacturer)
                && mCarToModelMap.get(manufacturer) != null
                && mCarToModelMap.get(manufacturer).contains(model)
                && mModelToYear.get(model) != null
                && mModelToYear.get(model).contains(constructionYearText.toString())
                && mModelToCCM.get(modelYear) != null
                && mModelToCCM.get(modelYear).contains(engineString)) {
            for (Car other : mCars) {
                if (other.getManufacturer() == null ||
                        other.getModel() == null ||
                        other.getConstructionYear() == 0 ||
                        other.getEngineDisplacement() == 0 ||
                        other.getFuelType() == null) {
                    continue;
                }
                if (other.getManufacturer().equals(manufacturer)
                        && other.getModel().equals(model)
                        && other.getConstructionYear() == Integer.parseInt(constructionYearText.toString())
                        && other.getEngineDisplacement() == Integer.parseInt(engineString)) {
                    selectedCar = other;
                    break;
                }
            }
        }

//        if (selectedCar != null && selectedCar.getFuelType() != null) {
//            fueltypeText.setText(selectedCar.getFuelType().toString());
//        }
    }

    private void addCarToAutocompleteList(Car car) {

        mCars.add(car);
        String manufacturer = car.getManufacturer().trim();
        String model = car.getModel().trim();
        String year = Integer.toString(car.getConstructionYear());

        if (manufacturer.isEmpty() || model.isEmpty() || year.isEmpty())
            return;

        mManufacturerNames.add(manufacturer);

        if (!mCarToModelMap.containsKey(manufacturer))
            mCarToModelMap.put(manufacturer, new HashSet<>());
        mCarToModelMap.get(manufacturer).add(model);

        if (!mModelToYear.containsKey(model))
            mModelToYear.put(model, new HashSet<>());
        mModelToYear.get(model).add(Integer.toString(car.getConstructionYear()));

        Pair<String, String> modelYearPair = new Pair<>(model, year);
        if (!mModelToCCM.containsKey(modelYearPair))
            mModelToCCM.put(modelYearPair, new HashSet<>());
        mModelToCCM.get(modelYearPair).add(Integer.toString(car.getEngineDisplacement()));
    }

    public void closeThisFragment() {
        ECAnimationUtils.animateHideView(getContext(),
                ((CarSelectionActivity) getActivity()).overlayView, R.anim.fade_out);
        ECAnimationUtils.animateHideView(getContext(), contentView, R.anim
                .translate_slide_out_bottom, () -> ((CarSelectionUiListener) getActivity()).onHideAddCarFragment());
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    protected void injectDependencies(BaseApplicationComponent appComponent) {
        appComponent.inject(this);
    }

    private void initWatcher() {
        disposables.add(RxTextView.afterTextChangeEvents(manufacturerText)
                .debounce(ERROR_DEBOUNCE_TIME, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .map(t -> t.toString())
                .subscribe(model -> {
                    if (model.trim().isEmpty()) {
                        manufacturerText.setError(getString(R.string.car_selection_error_empty_input));
                    }
                }, LOG::error));

        // Year input validity check.
        disposables.add(RxTextView.textChanges(constructionYearText)
                .skipInitialValue()
                .debounce(ERROR_DEBOUNCE_TIME, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .map(CharSequence::toString)
                .filter(s -> !s.isEmpty())
                .subscribe(yearString -> {
                    try {
                        int year = Integer.parseInt(yearString);
                        if (year < CONSTRUCTION_YEAR_MIN || year > CONSTRUCTION_YEAR_MAX) {
                            constructionYearText.setError(getString(R.string.car_selection_error_invalid_input));
                            constructionYearText.requestFocus();
                        }
                    } catch (Exception e) {
                        LOG.error(String.format("Unable to parse year [%s]", yearString), e);
                        constructionYearText.setError(getString(R.string.car_selection_error_invalid_input));
                        constructionYearText.requestFocus();
                    }
                }, LOG::error));

        // Engine input validity check.
        disposables.add(RxTextView.textChanges(engineText)
                .skipInitialValue()
                .debounce(ERROR_DEBOUNCE_TIME, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .map(CharSequence::toString)
                .filter(s -> !s.isEmpty())
                .subscribe(engineString -> {
                    if (engineString.isEmpty())
                        return;

                    try {
                        int engine = Integer.parseInt(engineString);
                        if (engine < ENGINE_DISPLACEMENT_MIN || engine > ENGINE_DISPLACEMENT_MAX) {
                            engineText.setError(getString(R.string.car_selection_error_invalid_input));
                            engineText.requestFocus();
                        }
                    } catch (Exception e) {
                        LOG.error(String.format("Unable to parse engine [%s]", engineString), e);
                        engineText.setError(getString(R.string.car_selection_error_invalid_input));
                        engineText.requestFocus();
                    }
                }, LOG::error));
    }
}
