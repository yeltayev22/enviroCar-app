package org.envirocar.core.entity;

public class Vehicle {
    private int tsn;
    private String commercialName;
    private String allotmentDate;
    private String category;
    private String bodywork;
    private int power;
    private int engineCapacity;

    private int axles;
    private int poweredAxles;
    private int seats;
    private int maximumMass;

    public int getTsn() {
        return tsn;
    }

    public void setTsn(int tsn) {
        this.tsn = tsn;
    }

    public String getCommercialName() {
        return commercialName;
    }

    public void setCommercialName(String commercialName) {
        this.commercialName = commercialName;
    }

    public String getAllotmentDate() {
        return allotmentDate;
    }

    public void setAllotmentDate(String allotmentDate) {
        this.allotmentDate = allotmentDate;
    }
}
