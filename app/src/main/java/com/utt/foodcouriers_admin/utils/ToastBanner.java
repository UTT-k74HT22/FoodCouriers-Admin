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
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.utt.foodcouriers_admin.R;

public class ToastBanner implements Application.ActivityLifecycleCallbacks {

    private static Activity currentActivity;
    private static ToastBanner instance;
    private static final int DURATION_MS = 3000;

    public enum ToastBannerType {
        SUCCESS,
        ERROR,
        WARNING
    }

    private ToastBanner() {
    }

    public static void init(Application app) {
        if (instance == null) {
            instance = new ToastBanner();
            app.registerActivityLifecycleCallbacks(instance);
        }
    }

    public static void showSuccess(String message) {
        show(message, ToastBannerType.SUCCESS);
    }

    public static void showError(String message) {
        show(message, ToastBannerType.ERROR);
    }

    public static void showWarning(String message) {
        show(message, ToastBannerType.WARNING);
    }

    private static void show(String message, ToastBannerType type) {
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

            showToastBannerView(currentActivity, message, backgroundColor, textColor);
        });
    }

    private static void showToastBannerView(Activity activity, String message, int backgroundColor, int textColor) {
        if (activity == null || activity.isFinishing()) {
            return;
        }

        View rootView = activity.findViewById(android.R.id.content);
        if (rootView == null) {
            return;
        }

        ViewGroup viewGroup = (rootView instanceof ViewGroup) ? (ViewGroup) rootView : null;
        if (viewGroup == null) {
            return;
        }

        int bannerId = activity.getResources().getIdentifier("banner_container", "id", activity.getPackageName());
        if (bannerId == 0) {
            bannerId = 12345;
        }

        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            if (child.getId() == bannerId) {
                viewGroup.removeView(child);
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

        ViewGroup.LayoutParams params;
        if (rootView instanceof CoordinatorLayout) {
            CoordinatorLayout.LayoutParams layoutParams = new CoordinatorLayout.LayoutParams(
                    CoordinatorLayout.LayoutParams.WRAP_CONTENT,
                    CoordinatorLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            layoutParams.topMargin = getStatusBarHeight(activity) + 16;
            params = layoutParams;
        } else {
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            layoutParams.topMargin = getStatusBarHeight(activity) + 16;
            params = layoutParams;
        }

        viewGroup.addView(banner, params);

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
                        .withEndAction(() -> viewGroup.removeView(banner))
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
