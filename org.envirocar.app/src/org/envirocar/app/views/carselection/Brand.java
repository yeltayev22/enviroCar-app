package org.envirocar.app.views.carselection;

import org.envirocar.core.entity.Manufacturer;
import org.envirocar.core.entity.Vehicle;

import javax.annotation.Nullable;

public class Brand {
    private Manufacturer manufacturer;
    private Vehicle vehicle;

    Brand(@Nullable Manufacturer manufacturer, @Nullable Vehicle vehicle) {
        this.manufacturer = manufacturer;
        this.vehicle = vehicle;
    }

    public Manufacturer getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(Manufacturer manufacturer) {
        this.manufacturer = manufacturer;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }
}
