package com.utt.foodcouriers_admin;

import android.app.Application;

import com.utt.foodcouriers_admin.utils.ToastBanner;

public class FoodCouriersApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ToastBanner.init(this);
    }
}
