package org.envirocar.remote.service;

import org.envirocar.core.entity.Manufacturer;
import org.envirocar.core.entity.PowerSource;
import org.envirocar.core.entity.Vehicle;

import java.util.List;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface VehicleService {

    @GET("manufacturers")
    Observable<List<Manufacturer>> fetchManufacturers();

    @GET("manufacturers/{hsn}/vehicles")
    Observable<List<Vehicle>> fetchVehiclesByManufacturer(@Path("hsn") String hsn);

    @GET("powerSources")
    Observable<List<PowerSource>> fetchPowerSources();

    @GET("manufacturers/{hsn}/vehicles/{tsn}")
    Observable<Vehicle> fetchVehicle(@Path("hsn") String hsn, @Path("tsn") String tsn);
}
