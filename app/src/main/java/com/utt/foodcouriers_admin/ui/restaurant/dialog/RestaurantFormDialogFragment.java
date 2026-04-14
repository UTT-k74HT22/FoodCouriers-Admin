package com.utt.foodcouriers_admin.ui.restaurant.dialog;

import android.app.TimePickerDialog;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.data.common.BaseResponse;
import com.utt.foodcouriers_admin.data.common.RepositoryCallback;
import com.utt.foodcouriers_admin.data.model.Restaurant;
import com.utt.foodcouriers_admin.data.repository.StorageRepository;
import com.utt.foodcouriers_admin.data.request.RestaurantUpsertRequest;
import com.utt.foodcouriers_admin.utils.ToastBanner;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RestaurantFormDialogFragment extends DialogFragment {

    public interface RestaurantFormListener {
        void onSubmit(@Nullable String restaurantId, RestaurantUpsertRequest request, RestaurantFormDialogFragment dialog);
    }

    private static final String ARG_RESTAURANT = "arg_restaurant";

    public static RestaurantFormDialogFragment newInstance(@Nullable Restaurant restaurant) {
        RestaurantFormDialogFragment fragment = new RestaurantFormDialogFragment();
        Bundle bundle = new Bundle();
        if (restaurant != null) {
            bundle.putSerializable(ARG_RESTAURANT, restaurant);
        }
        fragment.setArguments(bundle);
        return fragment;
    }

    private Restaurant restaurant;
    private RestaurantFormListener listener;
    private StorageRepository storageRepository;
    private Uri selectedImageUri;
    private boolean isUploading = false;
    private double currentLatitude = 21.0285;
    private double currentLongitude = 105.8542;
    private Marker currentMarker;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private AutoCompleteTextView etAddressAutocomplete;
    private org.osmdroid.views.MapView mapView;
    private TextView tvCoordinates;
    private ProgressBar progressLocation;
    private AddressSuggestionAdapter suggestionAdapter;
    private final android.os.Handler searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable searchRunnable;

    private EditText etName;
    private EditText etDescription;
    private EditText etImage;
    private EditText etPhone;
    private TextView tvOpenTime;
    private TextView tvCloseTime;
    private EditText etDeliveryFee;
    private EditText etMinOrder;
    private EditText etLatitude;
    private EditText etLongitude;
    private MaterialSwitch switchActive;
    private MaterialSwitch switchOpen;
    private TextView btnSave;
    private TextView btnCancel;
    private ProgressBar progressSave;
    private ImageView ivPreview;
    private TextView tvFormTitle;
    private TextView tvFormSubtitle;
    private TextView tvStatusHelper;
    private TextView tvOpenHelper;
    private View inputName;
    private View inputAddress;
    private View inputOpenTime;
    private View inputCloseTime;

    private int openHour = 8;
    private int openMinute = 0;
    private int closeHour = 22;
    private int closeMinute = 0;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    uploadSelectedImage();
                }
            }
    );

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, com.google.android.material.R.style.ThemeOverlay_Material3_Dialog_Alert);
        if (getArguments() != null) {
            restaurant = (Restaurant) getArguments().getSerializable(ARG_RESTAURANT);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_restaurant_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        
        storageRepository = StorageRepository.getInstance();
        initViews(view);
        setupMap(view);
        bindRestaurant();
        setupListeners();
    }

    public void setRestaurantFormListener(RestaurantFormListener listener) {
        this.listener = listener;
    }

    public void setLoading(boolean loading) {
        if (btnSave != null) {
            btnSave.setEnabled(!loading && !isUploading);
            btnSave.setText(loading ? getString(R.string.restaurant_saving) : getString(R.string.action_save));
        }
        if (progressSave != null) {
            progressSave.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (btnCancel != null) {
            btnCancel.setEnabled(!loading);
        }
    }

    public void showError(String message) {
        if (etAddressAutocomplete != null) {
            etAddressAutocomplete.setError(message);
        }
    }

    private void initViews(View view) {
        inputName = view.findViewById(R.id.input_name);
        inputAddress = null;
        inputOpenTime = view.findViewById(R.id.input_open_time);
        inputCloseTime = view.findViewById(R.id.input_close_time);
        
        etName = inputName.findViewById(R.id.et_input);
        etDescription = view.findViewById(R.id.input_description).findViewById(R.id.et_input);
        etImage = view.findViewById(R.id.input_image).findViewById(R.id.et_input);
        etAddressAutocomplete = view.findViewById(R.id.et_address_autocomplete);
        etPhone = view.findViewById(R.id.input_phone).findViewById(R.id.et_input);
        tvOpenTime = inputOpenTime.findViewById(R.id.tv_hint);
        tvCloseTime = inputCloseTime.findViewById(R.id.tv_hint);
        etDeliveryFee = view.findViewById(R.id.input_delivery_fee).findViewById(R.id.et_input);
        etMinOrder = view.findViewById(R.id.input_min_order).findViewById(R.id.et_input);
        etLatitude = view.findViewById(R.id.input_latitude).findViewById(R.id.et_input);
        etLongitude = view.findViewById(R.id.input_longitude).findViewById(R.id.et_input);
        
        switchActive = view.findViewById(R.id.switch_active);
        switchOpen = view.findViewById(R.id.switch_open);
        btnSave = view.findViewById(R.id.btn_save);
        btnCancel = view.findViewById(R.id.btn_cancel);
        progressSave = view.findViewById(R.id.progress_save);
        ivPreview = view.findViewById(R.id.iv_form_image);
        tvFormTitle = view.findViewById(R.id.tv_form_title);
        tvFormSubtitle = view.findViewById(R.id.tv_form_subtitle);
        tvStatusHelper = view.findViewById(R.id.tv_status_helper);
        tvOpenHelper = view.findViewById(R.id.tv_open_helper);

        View inputNameRoot = view.findViewById(R.id.input_name);
        View inputDescRoot = view.findViewById(R.id.input_description);
        View inputImageRoot = view.findViewById(R.id.input_image);
        View inputPhoneRoot = view.findViewById(R.id.input_phone);
        View inputDeliveryFeeRoot = view.findViewById(R.id.input_delivery_fee);
        View inputMinOrderRoot = view.findViewById(R.id.input_min_order);
        View inputLatitudeRoot = view.findViewById(R.id.input_latitude);
        View inputLongitudeRoot = view.findViewById(R.id.input_longitude);
        
        ImageView ivNameIcon = inputNameRoot.findViewById(R.id.iv_icon);
        ImageView ivDescIcon = inputDescRoot.findViewById(R.id.iv_icon);
        ImageView ivImageIcon = inputImageRoot.findViewById(R.id.iv_icon);
        ImageView ivPhoneIcon = inputPhoneRoot.findViewById(R.id.iv_icon);
        ImageView ivDeliveryFeeIcon = inputDeliveryFeeRoot.findViewById(R.id.iv_icon);
        ImageView ivMinOrderIcon = inputMinOrderRoot.findViewById(R.id.iv_icon);
        ImageView ivLatitudeIcon = inputLatitudeRoot.findViewById(R.id.iv_icon);
        ImageView ivLongitudeIcon = inputLongitudeRoot.findViewById(R.id.iv_icon);
        
        ivNameIcon.setImageResource(R.drawable.ic_restaurant);
        ivDescIcon.setImageResource(R.drawable.ic_menu);
        ivImageIcon.setImageResource(R.drawable.ic_menu_item);
        ivPhoneIcon.setImageResource(R.drawable.ic_orders);
        ivDeliveryFeeIcon.setImageResource(R.drawable.ic_orders);
        ivMinOrderIcon.setImageResource(R.drawable.ic_orders);
        ivLatitudeIcon.setImageResource(R.drawable.ic_orders);
        ivLongitudeIcon.setImageResource(R.drawable.ic_orders);
        
        ImageView ivOpenTimeIcon = inputOpenTime.findViewById(R.id.iv_icon);
        ImageView ivCloseTimeIcon = inputCloseTime.findViewById(R.id.iv_icon);
        ImageView ivOpenTimePickerIcon = inputOpenTime.findViewById(R.id.iv_time_icon);
        ImageView ivCloseTimePickerIcon = inputCloseTime.findViewById(R.id.iv_time_icon);
        
        ivOpenTimeIcon.setImageResource(R.drawable.ic_clock);
        ivCloseTimeIcon.setImageResource(R.drawable.ic_clock);
        ivOpenTimePickerIcon.setImageResource(R.drawable.ic_clock);
        ivCloseTimePickerIcon.setImageResource(R.drawable.ic_clock);
        
        etName.setHint(R.string.label_name_vi_en);
        etDescription.setHint(R.string.label_description_vi_en);
        etImage.setHint(R.string.label_image_url);
        etImage.setEnabled(false);
        etAddressAutocomplete.setHint(R.string.label_address_vi_en);
        etPhone.setHint(R.string.label_phone_vi_en);
        tvOpenTime.setHint(R.string.label_open_time);
        tvCloseTime.setHint(R.string.label_close_time);
        etDeliveryFee.setHint(R.string.label_delivery_fee);
        etMinOrder.setHint(R.string.label_min_order);
        etLatitude.setHint("Vĩ độ (Latitude)");
        etLongitude.setHint("Kinh độ (Longitude)");
        
        etDescription.setMinLines(2);
    }

    private void bindRestaurant() {
        boolean isEdit = restaurant != null && !TextUtils.isEmpty(restaurant.getId());
        tvFormTitle.setText(isEdit ? R.string.dialog_restaurant_title_edit : R.string.dialog_restaurant_title_create);
        tvFormSubtitle.setText(isEdit ? restaurant.getName() : getString(R.string.restaurant_label_info));
        switchActive.setChecked(isEdit ? restaurant.isActive() : true);
        switchOpen.setChecked(isEdit ? restaurant.isOpen() : false);
        
        if (isEdit) {
            etName.setText(restaurant.getName());
            etDescription.setText(restaurant.getDescription());
            etImage.setText(restaurant.getImageUrl());
            etAddressAutocomplete.setText(restaurant.getAddress());
            etPhone.setText(restaurant.getPhone());
            
            if (!TextUtils.isEmpty(restaurant.getOpenTime())) {
                parseTime(restaurant.getOpenTime());
            }
            if (!TextUtils.isEmpty(restaurant.getCloseTime())) {
                parseTimeClose(restaurant.getCloseTime());
            }
            
            etDeliveryFee.setText(String.valueOf(restaurant.getDeliveryFee()));
            etMinOrder.setText(String.valueOf(restaurant.getMinOrder()));
            
            if (restaurant.getLatitude() != null) {
                etLatitude.setText(String.valueOf(restaurant.getLatitude()));
            }
            if (restaurant.getLongitude() != null) {
                etLongitude.setText(String.valueOf(restaurant.getLongitude()));
            }
            
            loadPreviewImage(restaurant.getImageUrl());
        }
        
        updateTimeDisplay();
        tvStatusHelper.setText(switchActive.isChecked() ? R.string.label_active_vi_en : R.string.label_inactive_vi_en);
        tvOpenHelper.setText(switchOpen.isChecked() ? "Mở cửa" : "Đóng cửa");
    }

    private void parseTime(String time) {
        try {
            String[] parts = time.split(":");
            openHour = Integer.parseInt(parts[0]);
            openMinute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        } catch (Exception e) {
            openHour = 8;
            openMinute = 0;
        }
    }
    
    private void parseTimeClose(String time) {
        try {
            String[] parts = time.split(":");
            closeHour = Integer.parseInt(parts[0]);
            closeMinute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        } catch (Exception e) {
            closeHour = 22;
            closeMinute = 0;
        }
    }

    private void updateTimeDisplay() {
        tvOpenTime.setText(formatTime(openHour, openMinute));
        tvCloseTime.setText(formatTime(closeHour, closeMinute));
    }

    private String formatTime(int hour, int minute) {
        return String.format("%02d:%02d", hour, minute);
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> handleSubmit());
        btnCancel.setOnClickListener(v -> dismiss());

        inputOpenTime.setOnClickListener(v -> showTimePicker(true));
        inputCloseTime.setOnClickListener(v -> showTimePicker(false));

        ivPreview.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        etImage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadPreviewImage(s != null ? s.toString() : null);
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        switchActive.setOnCheckedChangeListener((buttonView, isChecked) ->
                tvStatusHelper.setText(isChecked ? R.string.label_active_vi_en : R.string.label_inactive_vi_en));

        switchOpen.setOnCheckedChangeListener((buttonView, isChecked) ->
                tvOpenHelper.setText(isChecked ? "Mở cửa" : "Đóng cửa"));
    }

    private void showTimePicker(boolean isOpenTime) {
        int currentHour = isOpenTime ? openHour : closeHour;
        int currentMinute = isOpenTime ? openMinute : closeMinute;
        
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute) -> {
                    if (isOpenTime) {
                        openHour = hourOfDay;
                        openMinute = minute;
                    } else {
                        closeHour = hourOfDay;
                        closeMinute = minute;
                    }
                    updateTimeDisplay();
                },
                currentHour,
                currentMinute,
                true
        );
        timePickerDialog.show();
    }

    private void handleSubmit() {
        if (!validate()) {
            return;
        }
        String name = etName.getText() != null ? etName.getText().toString().trim() : null;
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : null;
        String imageUrl = etImage.getText() != null ? etImage.getText().toString().trim() : null;
        String address = etAddressAutocomplete.getText() != null ? etAddressAutocomplete.getText().toString().trim() : null;
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : null;
        String openTime = formatTime(openHour, openMinute);
        String closeTime = formatTime(closeHour, closeMinute);
        Integer deliveryFee = parseInteger(etDeliveryFee);
        Integer minOrder = parseInteger(etMinOrder);
        Double latitude = parseDouble(etLatitude);
        Double longitude = parseDouble(etLongitude);
        
        android.util.Log.d("DEBUG_SUBMIT", "name=" + name + ", address=" + address + ", lat=" + latitude + ", lon=" + longitude);

        RestaurantUpsertRequest request = new RestaurantUpsertRequest(
                name, description, address, phone, imageUrl,
                switchActive.isChecked(), switchOpen.isChecked(),
                openTime, closeTime, deliveryFee, minOrder,
                latitude, longitude
        );
        if (listener != null) {
            listener.onSubmit(restaurant != null ? restaurant.getId() : null, request, this);
        }
    }

    private boolean validate() {
        boolean isValid = true;
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        if (TextUtils.isEmpty(name)) {
            inputName.setBackgroundResource(R.drawable.admin_input_error);
            isValid = false;
        } else {
            inputName.setBackgroundResource(R.drawable.admin_input);
        }
        String address = etAddressAutocomplete.getText() != null ? etAddressAutocomplete.getText().toString().trim() : "";
        if (TextUtils.isEmpty(address)) {
            etAddressAutocomplete.setBackgroundResource(R.drawable.admin_input_error);
            isValid = false;
        } else {
            etAddressAutocomplete.setBackgroundResource(R.drawable.admin_input);
        }
        
        int openTimeMinutes = openHour * 60 + openMinute;
        int closeTimeMinutes = closeHour * 60 + closeMinute;
        
        if (openTimeMinutes == closeTimeMinutes) {
            inputCloseTime.setBackgroundResource(R.drawable.admin_input_error);
            inputOpenTime.setBackgroundResource(R.drawable.admin_input_error);
            isValid = false;
        } else {
            inputCloseTime.setBackgroundResource(R.drawable.admin_input);
            inputOpenTime.setBackgroundResource(R.drawable.admin_input);
        }
        
        return isValid;
    }

    private Integer parseInteger(EditText editText) {
        if (editText.getText() == null) {
            return null;
        }
        String value = editText.getText().toString().trim();
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseDouble(EditText editText) {
        if (editText == null || editText.getText() == null) {
            return null;
        }
        String value = editText.getText().toString().trim();
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void loadPreviewImage(@Nullable String url) {
        if (TextUtils.isEmpty(url)) {
            ivPreview.setImageResource(R.drawable.ic_restaurant);
            return;
        }
        Glide.with(ivPreview.getContext())
                .load(url)
                .placeholder(R.drawable.ic_restaurant)
                .error(R.drawable.ic_restaurant)
                .centerCrop()
                .into(ivPreview);
    }

    private void uploadSelectedImage() {
        if (selectedImageUri == null) return;

        isUploading = true;
        setLoading(true);
        ToastBanner.showWarning(getString(R.string.toast_uploading));

        storageRepository.uploadImage(requireContext(), selectedImageUri, "restaurants", new RepositoryCallback<String>() {
            @Override
            public void onComplete(BaseResponse<String> response) {
                isUploading = false;
                if (response.isSuccess()) {
                    String imageUrl = response.getData();
                    if (restaurant != null) {
                        restaurant.setImageUrl(imageUrl);
                    }
                    if (etImage != null) {
                        etImage.setText(imageUrl);
                    }
                    loadPreviewImage(imageUrl);
                    ToastBanner.showSuccess(getString(R.string.toast_upload_success));
                } else {
                    ToastBanner.showError(getString(R.string.toast_upload_failed, response.getMessage()));
                }
                setLoading(false);
            }
        });
    }

    private void setupMap(View view) {
        mapView = view.findViewById(R.id.map_view);
        tvCoordinates = view.findViewById(R.id.tv_coordinates);
        progressLocation = view.findViewById(R.id.progress_location);
        
        if (mapView != null) {
            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapView.setMultiTouchControls(true);
            mapView.getController().setZoom(15.0);
            
            GeoPoint startPoint = new GeoPoint(currentLatitude, currentLongitude);
            mapView.getController().setCenter(startPoint);
            addMarker(startPoint);
        }
        
        etAddressAutocomplete = view.findViewById(R.id.et_address_autocomplete);
        etAddressAutocomplete.setThreshold(2);
        
        suggestionAdapter = new AddressSuggestionAdapter(requireContext(), new ArrayList<>());
        etAddressAutocomplete.setAdapter(suggestionAdapter);
        
        etAddressAutocomplete.setOnItemClickListener((parent, view1, position, id) -> {
            AddressSuggestionAdapter.SuggestionItem item = suggestionAdapter.getItem(position);
            if (item != null) {
                currentLatitude = item.lat;
                currentLongitude = item.lon;
                GeoPoint point = new GeoPoint(item.lat, item.lon);
                addMarker(point);
                if (mapView != null) {
                    mapView.getController().animateTo(point);
                    mapView.getController().setZoom(16.0);
                }
                updateCoordinatesText();
                if (etAddressAutocomplete != null) {
                    etAddressAutocomplete.setText(item.displayName);
                }
            }
        });
        
        etAddressAutocomplete.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (suggestionAdapter == null) return;
                String text = s.toString().trim();
                if (text.length() >= 3) {
                    if (searchRunnable != null) {
                        searchHandler.removeCallbacks(searchRunnable);
                    }
                    searchRunnable = () -> searchSuggestions(text);
                    searchHandler.postDelayed(searchRunnable, 500);
                } else {
                    suggestionAdapter.clearItems();
                }
            }
        });
        
        view.findViewById(R.id.btn_search_address).setOnClickListener(v -> searchAddress());
    }
    
    private void searchSuggestions(String query) {
        executor.execute(() -> {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            try {
                @SuppressWarnings("deprecation")
                List<Address> addresses = geocoder.getFromLocationName(query + ", Việt Nam", 5);
                
                List<AddressSuggestionAdapter.SuggestionItem> items = new ArrayList<>();
                
                if (addresses != null && !addresses.isEmpty()) {
                    for (Address addr : addresses) {
                        String fullAddress = addr.getAddressLine(0);
                        if (fullAddress != null && !fullAddress.isEmpty()) {
                            items.add(new AddressSuggestionAdapter.SuggestionItem(
                                fullAddress, addr.getLatitude(), addr.getLongitude()
                            ));
                        }
                    }
                }
                
                final List<AddressSuggestionAdapter.SuggestionItem> finalItems = items;
                requireActivity().runOnUiThread(() -> suggestionAdapter.updateItems(finalItems));
                
            } catch (IOException e) {
                // Ignore
            }
        });
    }

    private void addMarker(GeoPoint point) {
        if (mapView == null || point == null) return;
        
        if (currentMarker != null) {
            mapView.getOverlays().remove(currentMarker);
        }
        
        currentMarker = new Marker(mapView);
        currentMarker.setPosition(point);
        currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        currentMarker.setDraggable(true);
        
        currentMarker.setOnMarkerDragListener(new Marker.OnMarkerDragListener() {
            @Override
            public void onMarkerDrag(Marker marker) {}

            @Override
            public void onMarkerDragEnd(Marker marker) {
                currentLatitude = marker.getPosition().getLatitude();
                currentLongitude = marker.getPosition().getLongitude();
                updateCoordinatesText();
            }

            @Override
            public void onMarkerDragStart(Marker marker) {}
        });
        
        mapView.getOverlays().add(currentMarker);
        mapView.invalidate();
    }

    private void updateCoordinatesText() {
        if (tvCoordinates != null) {
            tvCoordinates.setText(String.format(Locale.getDefault(), 
                "Vĩ độ: %.6f, Kinh độ: %.6f", currentLatitude, currentLongitude));
        }
        if (etLatitude != null) {
            etLatitude.setText(String.valueOf(currentLatitude));
        }
        if (etLongitude != null) {
            etLongitude.setText(String.valueOf(currentLongitude));
        }
    }

    private void searchAddress() {
        if (etAddressAutocomplete == null || etAddressAutocomplete == null) return;
        
        String address = etAddressAutocomplete.getText().toString().trim();
        if (TextUtils.isEmpty(address)) {
            etAddressAutocomplete.setHint(R.string.error_field_required);
            return;
        }
        
        if (progressLocation != null) {
            progressLocation.setVisibility(View.VISIBLE);
        }
        
        final String searchQuery = address + ", Hà Nội, Việt Nam";
        
        executor.execute(() -> {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            try {
                @SuppressWarnings("deprecation")
                List<Address> addresses = geocoder.getFromLocationName(searchQuery, 5);
                
                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    currentLatitude = addr.getLatitude();
                    currentLongitude = addr.getLongitude();
                    
                    requireActivity().runOnUiThread(() -> {
                        if (progressLocation != null) {
                            progressLocation.setVisibility(View.GONE);
                        }
                        
                        GeoPoint point = new GeoPoint(currentLatitude, currentLongitude);
                        addMarker(point);
                        if (mapView != null) {
                            mapView.getController().animateTo(point);
                            mapView.getController().setZoom(16.0);
                        }
                        updateCoordinatesText();
                        
                        if (etAddressAutocomplete != null && addr.getAddressLine(0) != null) {
                            etAddressAutocomplete.setText(addr.getAddressLine(0));
                        }
                        
                        ToastBanner.showSuccess("Đã tìm thấy địa chỉ");
                    });
                } else {
                    final String searchQuery2 = address + ", Việt Nam";
                    @SuppressWarnings("deprecation")
                    List<Address> addresses2 = geocoder.getFromLocationName(searchQuery2, 3);
                    
                    if (addresses2 != null && !addresses2.isEmpty()) {
                        Address addr = addresses2.get(0);
                        currentLatitude = addr.getLatitude();
                        currentLongitude = addr.getLongitude();
                        
                        requireActivity().runOnUiThread(() -> {
                            if (progressLocation != null) {
                                progressLocation.setVisibility(View.GONE);
                            }
                            
                            GeoPoint point = new GeoPoint(currentLatitude, currentLongitude);
                            addMarker(point);
                            if (mapView != null) {
                                mapView.getController().animateTo(point);
                                mapView.getController().setZoom(16.0);
                            }
                            updateCoordinatesText();
                            
                            if (etAddressAutocomplete != null && addr.getAddressLine(0) != null) {
                                etAddressAutocomplete.setText(addr.getAddressLine(0));
                            }
                            
                            ToastBanner.showSuccess("Đã tìm thấy địa chỉ");
                        });
                    } else {
                        requireActivity().runOnUiThread(() -> {
                            if (progressLocation != null) {
                                progressLocation.setVisibility(View.GONE);
                            }
                            ToastBanner.showError("Không tìm thấy. Thử nhập: số nhà + đường + quận (VD: 123 Đường Láng, Đống Đa, Hà Nội)");
                        });
                    }
                }
            } catch (IOException e) {
                requireActivity().runOnUiThread(() -> {
                    if (progressLocation != null) {
                        progressLocation.setVisibility(View.GONE);
                    }
                    ToastBanner.showError("Lỗi: " + e.getMessage());
                });
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdown();
        if (searchHandler != null) {
            searchHandler.removeCallbacksAndMessages(null);
        }
    }

    private static class AddressSuggestionAdapter extends android.widget.ArrayAdapter<AddressSuggestionAdapter.SuggestionItem> implements android.widget.Filterable {
        
        static class SuggestionItem {
            String displayName;
            double lat;
            double lon;
            
            SuggestionItem(String displayName, double lat, double lon) {
                this.displayName = displayName;
                this.lat = lat;
                this.lon = lon;
            }
            
            @NonNull
            @Override
            public String toString() {
                return displayName;
            }
        }
        
        private List<SuggestionItem> items = new ArrayList<>();
        
        AddressSuggestionAdapter(@NonNull Context context, List<SuggestionItem> items) {
            super(context, android.R.layout.simple_dropdown_item_1line, (List<SuggestionItem>) items);
            this.items = new ArrayList<>(items);
        }
        
        void updateItems(List<SuggestionItem> newItems) {
            this.items = new ArrayList<>(newItems);
            clear();
            addAll(newItems);
            notifyDataSetChanged();
        }
        
        void clearItems() {
            items.clear();
            clear();
            notifyDataSetChanged();
        }
        
        @NonNull
        @Override
        public Filter getFilter() {
            return null;
        }
    }
}
