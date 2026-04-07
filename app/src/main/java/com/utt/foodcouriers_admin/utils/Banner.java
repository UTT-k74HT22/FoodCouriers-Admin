package com.utt.foodcouriers_admin.utils;

import android.app.Activity;
import android.app.Application;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.utt.foodcouriers_admin.R;

public class Banner implements Application.ActivityLifecycleCallbacks {

    private static Activity currentActivity;
    private static Banner instance;
    private static final int DURATION_MS = 3000;

    public enum BannerType {
        SUCCESS,
        ERROR,
        WARNING
    }

    private Banner() {
    }

    public static void init(Application app) {
        if (instance == null) {
            instance = new Banner();
            app.registerActivityLifecycleCallbacks(instance);
        }
    }

    public static void showSuccess(String message) {
        show(message, BannerType.SUCCESS);
    }

    public static void showError(String message) {
        show(message, BannerType.ERROR);
    }

    public static void showWarning(String message) {
        show(message, BannerType.WARNING);
    }

    private static void show(String message, BannerType type) {
        if (currentActivity == null) {
            return;
        }

        new Handler(Looper.getMainLooper()).post(() -> {
            int backgroundColor;
            int textColor = Color.WHITE;

            switch (type) {
                case SUCCESS:
                    backgroundColor = currentActivity.getColor(R.color.success);
                    break;
                case ERROR:
                    backgroundColor = currentActivity.getColor(R.color.error);
                    break;
                case WARNING:
                    backgroundColor = currentActivity.getColor(R.color.warning);
                    break;
                default:
                    backgroundColor = currentActivity.getColor(R.color.primary);
            }

            showBannerView(currentActivity, message, backgroundColor, textColor);
        });
    }

    private static void showBannerView(Activity activity, String message, int backgroundColor, int textColor) {
        if (activity == null || activity.isFinishing()) {
            return;
        }

        FrameLayout rootView = activity.findViewById(android.R.id.content);
        if (rootView == null) {
            return;
        }

        int bannerId = activity.getResources().getIdentifier("banner_container", "id", activity.getPackageName());
        if (bannerId == 0) {
            bannerId = 12345;
        }

        for (int i = 0; i < rootView.getChildCount(); i++) {
            View child = rootView.getChildAt(i);
            if (child.getId() == bannerId) {
                rootView.removeView(child);
                break;
            }
        }

        TextView banner = new TextView(activity);
        banner.setId(bannerId);
        banner.setText(message);
        banner.setTextColor(textColor);
        banner.setTextSize(14);
        banner.setGravity(Gravity.CENTER);
        banner.setPadding(48, 32, 48, 32);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(16);
        drawable.setColor(backgroundColor);
        banner.setBackground(drawable);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.topMargin = getStatusBarHeight(activity) + 16;

        rootView.addView(banner, params);

        banner.setAlpha(0f);
        banner.setTranslationY(-100f);
        banner.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .start();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (banner.getParent() != null) {
                banner.animate()
                        .alpha(0f)
                        .translationY(-100f)
                        .setDuration(300)
                        .withEndAction(() -> rootView.removeView(banner))
                        .start();
            }
        }, DURATION_MS);
    }

    private static int getStatusBarHeight(Activity activity) {
        int result = 0;
        int resourceId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = activity.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        if (currentActivity == activity) {
            currentActivity = null;
        }
    }
}
