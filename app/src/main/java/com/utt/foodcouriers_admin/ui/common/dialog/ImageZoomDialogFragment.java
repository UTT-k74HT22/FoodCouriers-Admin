package com.utt.foodcouriers_admin.ui.common.dialog;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.utt.foodcouriers_admin.R;

public class ImageZoomDialogFragment extends DialogFragment {

    private static final String ARG_IMAGE_URL = "arg_image_url";

    public static ImageZoomDialogFragment newInstance(String imageUrl) {
        ImageZoomDialogFragment fragment = new ImageZoomDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(ARG_IMAGE_URL, imageUrl);
        fragment.setArguments(bundle);
        return fragment;
    }

    private String imageUrl;
    private ImageView ivZoomedImage;
    private ProgressBar progressLoading;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        if (getArguments() != null) {
            imageUrl = getArguments().getString(ARG_IMAGE_URL);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_image_zoom, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ivZoomedImage = view.findViewById(R.id.iv_zoomed_image);
        progressLoading = view.findViewById(R.id.progress_loading);

        ivZoomedImage.setOnClickListener(v -> dismiss());

        loadImage();
    }

    private void loadImage() {
        if (TextUtils.isEmpty(imageUrl)) {
            progressLoading.setVisibility(View.GONE);
            dismiss();
            return;
        }

        progressLoading.setVisibility(View.VISIBLE);

        Glide.with(requireContext())
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_error)
                .fitCenter()
                .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                        progressLoading.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                        progressLoading.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(ivZoomedImage);
    }
}
