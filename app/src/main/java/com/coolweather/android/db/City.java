package com.coolweather.android.db;

import org.litepal.crud.DataSupport;

public class City extends DataSupport {
    private int id;
    private String cityName;//城市名字
    private int cityCode;//城市的代码
    private int provinceId;//当前城市所属的省id

    public int getCityCode() {
        return cityCode;
    }

    public int getId() {
        return id;
    }

    public String getCityName() {
        return cityName;
    }

    public int getProvinceId() {
        return provinceId;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public void setCityCode(int cityCode) {
        this.cityCode = cityCode;
    }

    public void setProvinceId(int provinceId) {
        this.provinceId = provinceId;
    }
}
