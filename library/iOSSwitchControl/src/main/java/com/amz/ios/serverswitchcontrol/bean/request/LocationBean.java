package com.amz.ios.serverswitchcontrol.bean.request;

public class LocationBean {

    /**
     * "province":"广东省",
     * "city"    :"深圳市"
     */

    private String province;
    private String city;

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public static LocationBean newCommonBean(String province, String city) {
        LocationBean locationBean = new LocationBean();
        locationBean.setProvince(province);
        locationBean.setCity(city);
        return locationBean;
    }
}
