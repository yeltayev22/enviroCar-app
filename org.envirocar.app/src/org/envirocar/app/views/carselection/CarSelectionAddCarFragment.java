/*
  Copyright (C) 2013 - 2019 the enviroCar community
  <p>
  This file is part of the enviroCar app.
  <p>
  The enviroCar app is free software: you can redistribute it and/or
  modify it under the terms of the GNU General Public License as published
  by the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.
  <p>
  The enviroCar app is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
  Public License for more details.
  <p>
  You should have received a copy of the GNU General Public License along
  with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.views.carselection;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
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
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.retrofit.RetrofitClient;
import org.envirocar.app.views.utils.ECAnimationUtils;
import org.envirocar.app.views.utils.RoundedBottomPanelLayout;
import org.envirocar.app.views.utils.WheelPickerView;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.CarImpl;
import org.envirocar.core.entity.Manufacturer;
import org.envirocar.core.entity.Vehicle;
import org.envirocar.core.logging.Logger;
import org.envirocar.remote.service.VehicleService;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    @BindView(R.id.envirocar_toolbar)
    protected Toolbar toolbar;
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
    @BindView(R.id.power_source_layout)
    protected LinearLayout powerSourceLayout;
    @BindView(R.id.power_source_text)
    protected TextView powerSourceText;
    @BindView(R.id.engine_displacement_layout)
    protected LinearLayout engineLayout;
    @BindView(R.id.engine_displacement_text)
    protected TextView engineText;

    @BindView(R.id.warning_layout)
    protected LinearLayout warningLayout;

    @BindView(R.id.sliding_up_panel)
    protected SlidingUpPanelLayout slidingUpPanel;
    @BindView(R.id.recycler_view_layout)
    protected LinearLayout recyclerViewLayout;
    @BindView(R.id.wheel_picker_layout)
    protected LinearLayout wheelPickerLayout;
    @BindView(R.id.wheel_picker_view)
    protected WheelPickerView wheelPickerView;
    @BindView(R.id.select_button)
    protected Button selectButton;

    @BindView(R.id.back_icon)
    protected ImageView backIcon;
    @BindView(R.id.search_title)
    protected TextView searchTitle;
    @BindView(R.id.search_view)
    protected SearchView searchView;
    @BindView(R.id.brand_list)
    protected RecyclerView brandsRecyclerView;
    @BindView(R.id.panel)
    protected RoundedBottomPanelLayout roundedBottomPanel;

    private List<Brand> manufacturersCache = new ArrayList<>();
    private Manufacturer selectedManufacturer;
    private Vehicle selectedVehicle;
    private BrandAdapter brandAdapter;

    private Step currentStep;
    private List<String> years = new ArrayList<>();
    private List<String> powerSources = new ArrayList<>();
    private List<String> engineTypes = new ArrayList<>();
    private String selectedYear = "";
    private String selectedPowerSource = "";
    private String selectedEngineCapacity = "";

    private CompositeDisposable disposables = new CompositeDisposable();
    private Scheduler.Worker mainThreadWorker = AndroidSchedulers.mainThread().createWorker();
    final Handler handler = new Handler();

    @Nullable
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.activity_car_selection_newcar_fragment, container, false);
        ButterKnife.bind(this, view);

        initViews();
        fetchManufacturers();

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
        slidingUpPanel.setVisibility(View.GONE);
        loadingView.setVisibility(View.INVISIBLE);

        // Initialize recycler view
        brandsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        brandAdapter = new BrandAdapter();
        brandsRecyclerView.setAdapter(brandAdapter);
        brandsRecyclerView.addItemDecoration(new DividerItemDecoration(brandsRecyclerView.getContext(), DividerItemDecoration.VERTICAL));

        // Initialize sliding up panel layout
        slidingUpPanel.addPanelSlideListener(this);

        // Initialize brand layout views
        brandLayout.setOnClickListener(v -> {
            if (isNotClickable()) {
                return;
            }

            currentStep = Step.BRAND_AND_MODEL;

            showRecyclerViewLayout();
            setManufacturersSelectView();
            setManufacturerAdapter(manufacturersCache);
        });

        searchView.setOnSearchClickListener(v -> slidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED));
        searchView.setOnClickListener(v -> {
            slidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
            searchView.setIconified(false);
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                brandAdapter.getFilter().filter(query);
                return false;
            }
        });
        backIcon.setOnClickListener(v -> {
            setManufacturersSelectView();
            setManufacturerAdapter(manufacturersCache);
        });

        // Initialize construction year layout views
        constructionYearLayout.setOnClickListener(v -> {
            if (isNotClickable()) {
                return;
            }

            searchTitle.setText(getString(R.string.label_select_year));
            currentStep = Step.CONSTRUCTION_YEAR;
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
        powerSourceLayout.setOnClickListener(v -> {
            if (isNotClickable()) {
                return;
            }

            searchTitle.setText(getString(R.string.label_select_fuel));
            currentStep = Step.POWER_SOURCE;
            showWheelPickerLayout();

            powerSources.clear();
            powerSources.add("Gasoline");
            powerSources.add("Diesel");
            powerSources.add("Gas");
            powerSources.add("Hybrid");
            powerSources.add("Electric");
            wheelPickerView.setData(powerSources);
        });

        // Initialize engine capacity layout views
        engineLayout.setOnClickListener(v -> {
            if (isNotClickable()) {
                return;
            }

            searchTitle.setText(getString(R.string.label_select_engine_capacity));
            currentStep = Step.ENGINE_DISPLACEMENT;
            showWheelPickerLayout();

            engineTypes.clear();
            engineTypes.add("1300");
            engineTypes.add("1400");
            engineTypes.add("1533");
            engineTypes.add("1700");
            wheelPickerView.setData(engineTypes);
        });

        // Initialize select button
        selectButton.setOnClickListener(v -> {
            int selectedPosition = wheelPickerView.getCurrentItemPosition();
            switch (currentStep) {
                case CONSTRUCTION_YEAR:
                    selectedYear = years.get(selectedPosition);
                    constructionYearText.setText(selectedYear);
                    break;
                case POWER_SOURCE:
                    selectedPowerSource = powerSources.get(selectedPosition);
                    powerSourceText.setText(selectedPowerSource);
                    break;
                case ENGINE_DISPLACEMENT:
                    selectedEngineCapacity = engineTypes.get(selectedPosition);
                    engineText.setText(selectedEngineCapacity);
                    break;
            }

            slidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        });

        roundedBottomPanel.setOnClickListener(v -> {
            if (currentStep == Step.CONSTRUCTION_YEAR || currentStep == Step.POWER_SOURCE || currentStep == Step.ENGINE_DISPLACEMENT) {
                slidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        });

        // Handle toolbar done action
        RxToolbar.itemClicks(toolbar)
                .filter(continueWhenFormIsCorrect())
                .map(createCarFromForm())
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
                        LOG.info("car succesfully added");
                        ((CarSelectionUiListener) getActivity()).onCarAdded(car);
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
        ECAnimationUtils.animateShowView(getContext(), slidingUpPanel,
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
                        mainThreadWorker.schedule(() -> loadingView.setVisibility(View.INVISIBLE));
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
        searchView.setIconified(true);
        searchTitle.setText(getString(R.string.label_select_manufacturer));
        backIcon.setVisibility(View.GONE);
    }

    private void setModelsSelectView() {
        searchView.setIconified(true);
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
                brandText.setText(selectedManufacturer.getName() + " " + selectedVehicle.getCommercialName());

                slidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
            }
        });
        brandsRecyclerView.scrollToPosition(0);
    }

    private void showRecyclerViewLayout() {
        slidingUpPanel.setAnchorPoint(0.7f);
        slidingUpPanel.setTouchEnabled(true);

        handler.postDelayed(() -> {
            slidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);
            recyclerViewLayout.setVisibility(View.VISIBLE);
            wheelPickerLayout.setVisibility(View.GONE);
        }, 50);
    }

    private void showWheelPickerLayout() {
        slidingUpPanel.setAnchorPoint(0.45f);
        slidingUpPanel.setTouchEnabled(false);

        handler.postDelayed(() -> {
            slidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);
            recyclerViewLayout.setVisibility(View.GONE);
            wheelPickerLayout.setVisibility(View.VISIBLE);
        }, 50);
    }

    private boolean isNotClickable() {
        SlidingUpPanelLayout.PanelState currentPanelState = slidingUpPanel.getPanelState();
        return currentPanelState == SlidingUpPanelLayout.PanelState.ANCHORED || currentPanelState == SlidingUpPanelLayout.PanelState.EXPANDED;
    }

    private Predicate<MenuItem> continueWhenFormIsCorrect() {
        return menuItem -> {
            if (selectedManufacturer != null && selectedVehicle != null
                    && !selectedYear.isEmpty() && !selectedPowerSource.isEmpty()
                    && !selectedEngineCapacity.isEmpty()) {
                warningLayout.setVisibility(View.GONE);
                return true;
            }

            warningLayout.setVisibility(View.VISIBLE);
            return false;
        };
    }

    private <T> Function<T, Car> createCarFromForm() {
        return t -> {
            // Get the values
            String manufacturer = selectedManufacturer.getName();
            String model = selectedVehicle.getCommercialName();
            String yearString = selectedYear;
            String engineString = selectedEngineCapacity;

            Car.FuelType fueltype = Car.FuelType.getFuelTybeByTranslatedString(getContext(),
                    selectedPowerSource);

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

    void closeThisFragment() {
        slidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);

        ECAnimationUtils.animateHideView(getContext(),
                ((CarSelectionActivity) getActivity()).overlayView, R.anim.fade_out);
        ECAnimationUtils.animateHideView(getContext(), slidingUpPanel, R.anim
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

    @Override
    public void onPanelSlide(View panel, float slideOffset) {
        // nothing
    }

    @Override
    public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
        if (newState == SlidingUpPanelLayout.PanelState.HIDDEN || newState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
            hideKeyboard(getView());
        }
    }
}
