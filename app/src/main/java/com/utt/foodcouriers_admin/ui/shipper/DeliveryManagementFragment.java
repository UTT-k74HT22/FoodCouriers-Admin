package com.utt.foodcouriers_admin.ui.shipper;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.utt.foodcouriers_admin.R;

public class DeliveryManagementFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_delivery_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        tabLayout = view.findViewById(R.id.tab_layout);
        viewPager = view.findViewById(R.id.view_pager);

        setupViewPager();
    }

    private void setupViewPager() {
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0: return DeliveryListFragment.newInstance(DeliveryListFragment.TYPE_AVAILABLE);
                    case 1: return DeliveryListFragment.newInstance(DeliveryListFragment.TYPE_ONGOING);
                    default: return DeliveryListFragment.newInstance(DeliveryListFragment.TYPE_HISTORY);
                }
            }

            @Override
            public int getItemCount() {
                return 3;
            }
        });

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("Đơn mới"); break;
                case 1: tab.setText("Đang giao"); break;
                case 2: tab.setText("Lịch sử"); break;
            }
        }).attach();
    }
}
