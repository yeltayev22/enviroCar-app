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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jakewharton.rxbinding3.appcompat.RxToolbar;
import com.jakewharton.rxbinding3.widget.RxTextView;
import com.jakewharton.rxbinding3.widget.TextViewAfterTextChangeEvent;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.retrofit.RetrofitClient;
import org.envirocar.app.views.utils.ECAnimationUtils;
import org.envirocar.app.views.utils.WheelPickerView;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.CarImpl;
import org.envirocar.core.entity.Manufacturer;
import org.envirocar.core.entity.Vehicle;
import org.envirocar.core.logging.Logger;
import org.envirocar.remote.service.VehicleService;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
public class CarSelectionAddCarFragment extends BaseInjectorFragment implements SlidingUpPanelLayout.PanelSlideListener {
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
    @BindView(R.id.loading_layout)
    protected View loadingView;

    @BindView(R.id.brand_layout)
    protected LinearLayout brandLayout;
    @BindView(R.id.brand_text)
    protected TextView brandText;
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
    @BindView(R.id.recycler_view_layout)
    protected LinearLayout recyclerViewLayout;
    @BindView(R.id.wheel_picker_layout)
    protected LinearLayout wheelPickerLayout;
    @BindView(R.id.wheel_picker_view)
    protected WheelPickerView wheelPickerView;

    @BindView(R.id.back_icon)
    protected ImageView backIcon;
    @BindView(R.id.search_title)
    protected TextView searchTitle;
    @BindView(R.id.search_view)
    protected SearchView searchView;
    @BindView(R.id.brand_list)
    protected RecyclerView brandsRecyclerView;

    private List<Brand> manufacturersCache = new ArrayList<>();
    private Manufacturer selectedManufacturer;
    private Vehicle selectedVehicle;
    private BrandAdapter brandAdapter;

    private List<String> years = new ArrayList<>();
    private List<String> fuelTypes = new ArrayList<>();
    private List<String> engineTypes = new ArrayList<>();

    private CompositeDisposable disposables = new CompositeDisposable();
    private Scheduler.Worker mainThreadWorker = AndroidSchedulers.mainThread().createWorker();

    @Nullable
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle
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
        loadingView.setVisibility(View.INVISIBLE);

        // Initialize recycler view
        brandsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        brandAdapter = new BrandAdapter();
        brandsRecyclerView.setAdapter(brandAdapter);
        brandsRecyclerView.addItemDecoration(new DividerItemDecoration(brandsRecyclerView.getContext(), DividerItemDecoration.VERTICAL));

        // Initialize brand layout views
        brandLayout.setOnClickListener(v -> {
            showRecyclerViewLayout();
            setManufacturersSelectView();
            setManufacturerAdapter(manufacturersCache);
        });
        searchView.setOnClickListener(v -> {
            searchView.setIconified(false);
            slidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
        });
        backIcon.setOnClickListener(v -> {
            setManufacturersSelectView();
            setManufacturerAdapter(manufacturersCache);
        });

        // Initialize construction year layout views
        constructionYearLayout.setOnClickListener(v -> {

            showWheelPickerLayout();

            years.clear();
            years.add("2007");
            years.add("2009");
            years.add("2010");
            years.add("2012");
            years.add("2013");
            years.add("2016");
            years.add("2018");
            wheelPickerView.setData(years);
        });

        // Initialize fuel type layout views
        fuelTypeLayout.setOnClickListener(v -> {
            showWheelPickerLayout();

            fuelTypes.clear();
            fuelTypes.add("Electric");
            fuelTypes.add("Benzine");
            fuelTypes.add("Gas");
            fuelTypes.add("Hybrid");
            fuelTypes.add("Diesel");
            fuelTypes.add("Gas-Hybrid");
            wheelPickerView.setData(fuelTypes);
        });

        // Initialize engine capacity layout views
        engineLayout.setOnClickListener(v -> {
            showWheelPickerLayout();

            engineTypes.clear();
            engineTypes.add("1300");
            engineTypes.add("1400");
            engineTypes.add("1533");
            engineTypes.add("1700");
            wheelPickerView.setData(engineTypes);
        });

        // Handle toolbar done action
        RxToolbar.itemClicks(toolbar)
//                .filter(continueWhenFormIsCorrect())
                .map(createCarFromForm())
                .filter(continueWhenCarHasCorrectValues())
//                .map(checkCarAlreadyExist())
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
                        LOG.info("onStart() download manufacturers");
                        loadingView.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onComplete() {
                        LOG.info("onCompleted(): manufacturers successfully downloaded.");

                        mainThreadWorker.schedule(() -> {
                            dispose();
                            loadingView.setVisibility(View.INVISIBLE);
                        });
                    }

                    @Override
                    public void onError(Throwable e) {
                        LOG.error(e.getMessage(), e);
                        mainThreadWorker.schedule(() -> loadingView.setVisibility(View.INVISIBLE));
                    }

                    @Override
                    public void onNext(List<Manufacturer> manufacturers) {
                        Collections.sort(manufacturers, (a, b) -> a.getName().compareTo(b.getName()));
                        ArrayList<Brand> brands = new ArrayList<>();
                        for (Manufacturer manufacturer : manufacturers) {
                            brands.add(new Brand(manufacturer, null));
                        }

                        manufacturersCache.addAll(brands);

                        mainThreadWorker.schedule(() -> setManufacturerAdapter(brands));
                    }
                }));
    }

    private void fetchVehiclesByManufacturer(String hsn) {
        VehicleService vehicleService = RetrofitClient.getRetrofit(getContext()).create(VehicleService.class);

        disposables.add(vehicleService.fetchVehiclesByManufacturer(hsn)
                .toFlowable(BackpressureStrategy.BUFFER)
                .onBackpressureBuffer(10000)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .toObservable()
                .subscribeWith(new DisposableObserver<List<Vehicle>>() {

                    @Override
                    protected void onStart() {
                        LOG.info("onStart() download vehicles by manufacturer");
                        loadingView.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onComplete() {
                        LOG.info("onCompleted(): vehicles successfully downloaded.");

                        mainThreadWorker.schedule(() -> {
                            dispose();
                            loadingView.setVisibility(View.INVISIBLE);
                        });
                    }

                    @Override
                    public void onError(Throwable e) {
                        LOG.error(e.getMessage(), e);
                        mainThreadWorker.schedule(() -> {
                            loadingView.setVisibility(View.INVISIBLE);
                        });
                    }

                    @Override
                    public void onNext(List<Vehicle> vehicles) {
                        Collections.sort(vehicles, (a, b) -> a.getCommercialName().compareTo(b.getCommercialName()));
                        ArrayList<Brand> brands = new ArrayList<>();
                        for (Vehicle vehicle : vehicles) {
                            brands.add(new Brand(null, vehicle));
                        }

                        mainThreadWorker.schedule(() -> {
                            setModelsSelectView();
                            setModelsAdapter(brands);
                        });
                    }
                }));
    }

    private void setManufacturersSelectView() {
        searchTitle.setText(getString(R.string.label_select_manufacturer));
        backIcon.setVisibility(View.GONE);
    }

    private void setModelsSelectView() {
        searchTitle.setText(getString(R.string.label_select_model));
        backIcon.setVisibility(View.VISIBLE);
    }

    private void setManufacturerAdapter(List<Brand> brands) {
        brandAdapter.clear();
        brandAdapter.set(brands, brand -> {
            Manufacturer manufacturer = brand.getManufacturer();
            if (manufacturer != null) {
                selectedManufacturer = manufacturer;
                fetchVehiclesByManufacturer(brand.getManufacturer().getHsn());
                brandText.setText(selectedManufacturer.getName());
            }
        });
        brandsRecyclerView.scrollToPosition(0);
    }

    private void setModelsAdapter(List<Brand> brands) {
        brandAdapter.clear();
        brandAdapter.set(brands, brand -> {
            Vehicle vehicle = brand.getVehicle();
            if (vehicle != null) {
                selectedVehicle = vehicle;
                slidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);

                // TODO: Fix keyboard
                hideKeyboard(getView());

                brandText.setText(selectedManufacturer.getName() + " " + selectedVehicle.getCommercialName());
            }
        });
        brandsRecyclerView.scrollToPosition(0);
    }

    private void showRecyclerViewLayout() {
        slidingUpPanel.setAnchorPoint(0.7f);
        slidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);

        recyclerViewLayout.setVisibility(View.VISIBLE);
        wheelPickerLayout.setVisibility(View.GONE);
    }

    private void showWheelPickerLayout() {
        slidingUpPanel.setAnchorPoint(0.4f);
        slidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);

        recyclerViewLayout.setVisibility(View.GONE);
        wheelPickerLayout.setVisibility(View.VISIBLE);
    }

    private <T> Function<T, Car> createCarFromForm() {
        return t -> {
            // Get the values
            String[] make = brandText.getText().toString().split("\\s+");
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

    void closeThisFragment() {
        slidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);

        ECAnimationUtils.animateHideView(getContext(),
                ((CarSelectionActivity) getActivity()).overlayView, R.anim.fade_out);
        ECAnimationUtils.animateHideView(getContext(), contentView, R.anim
                .translate_slide_out_bottom, () -> ((CarSelectionUiListener) getActivity()).onHideAddCarFragment());
    }

    private void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    protected void injectDependencies(BaseApplicationComponent appComponent) {
        appComponent.inject(this);
    }

    private void initWatcher() {
        disposables.add(RxTextView.afterTextChangeEvents(brandText)
                .debounce(ERROR_DEBOUNCE_TIME, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .map(TextViewAfterTextChangeEvent::toString)
                .subscribe(model -> {
                    if (model.trim().isEmpty()) {
                        brandText.setError(getString(R.string.car_selection_error_empty_input));
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

    @Override
    public void onPanelSlide(View panel, float slideOffset) {
        // nothing
    }

    @Override
    public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
        if (newState == SlidingUpPanelLayout.PanelState.HIDDEN) {
            hideKeyboard(getView());
        }
    }
}
