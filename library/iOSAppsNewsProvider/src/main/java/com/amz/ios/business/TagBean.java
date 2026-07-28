package com.amz.ios.business;

import android.content.Context;

import java.util.Random;

/**
 * Created by liaozhongjun on 2017/2/25.
 */

public class TagBean {

    /**
     * channel : ”koobee”
     * customer : osdxx9
     * brand : ”koobee”
     * project : osdxx9
     * country : ”uk”
     */

    private String channel;
    private String customer;
    private String brand;
    private String project;
    private String country;

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }


    public static final TagBean newTagBean(Context context) {
        TagBean tagBean = new TagBean();
        tagBean.setBrand("Droi");
        tagBean.setChannel("hwdroi");
        tagBean.setCountry(lanuageMapToCountry(context.getResources().getConfiguration().locale.getLanguage()));
        tagBean.setCustomer("hwdroi001");
        tagBean.setProject("IOSLite");
        return tagBean;
    }

    private static final String lanuageMapToCountry(String lanuage) {
        switch (lanuage) {
            case "hi":
            case "en":
                return "in";//india
            case "vi":
                return "vn";//
            case "th":
            case "th_TH":
                return lanuage;
            case "ms":
                return "my";
            case "in":
                return "id";
            case "fa":
                return "ir";
            case "ar_YE":
                return "ye";
            case " ar_SA":
                return "sa";
            case "ar_IQ":
                return "iq";
            case "ar":
                Random random = new Random();
                int i = random.nextInt(1);
                switch (i) {
                    case 0:
                        return "sa";
                    case 1:
                        return "ye";
                    case 2:
                        return "iq";
                }
            default:
                return "th";

        }


    }


}