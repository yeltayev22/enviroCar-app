package org.envirocar.remote.service;

import org.envirocar.core.entity.Manufacturer;
import org.envirocar.core.entity.PowerSource;
import org.envirocar.core.entity.Vehicle;

import java.util.List;

import io.reactivex.Observable;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface VehicleService {

    @GET("manufacturers")
    Observable<List<Manufacturer>> fetchManufacturers();

    @GET("manufacturers/{hsn}/vehicles")
    Call<List<Vehicle>> fetchVehiclesByManufacturer(@Path("hsn") int hsn);

    @GET("powerSources")
    Call<List<PowerSource>> fetchPowerSources();

    @GET("manufacturers/{hsn}/vehicles/{tsn}")
    Call<List<Vehicle>> fetchVehicle(@Path("hsn") int hsn, @Path("tsn") int tsn);
}
