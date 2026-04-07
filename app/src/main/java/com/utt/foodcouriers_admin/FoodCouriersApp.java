package com.utt.foodcouriers_admin;

import android.app.Application;

import com.utt.foodcouriers_admin.utils.Banner;

public class FoodCouriersApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Banner.init(this);
    }
}
