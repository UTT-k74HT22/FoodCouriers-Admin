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
    private final DeliveryListFragment[] pages = new DeliveryListFragment[3];

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
                if (pages[position] == null) {
                    switch (position) {
                        case 0:
                            pages[position] = DeliveryListFragment.newInstance(DeliveryListFragment.TYPE_AVAILABLE);
                            break;
                        case 1:
                            pages[position] = DeliveryListFragment.newInstance(DeliveryListFragment.TYPE_ONGOING);
                            break;
                        default:
                            pages[position] = DeliveryListFragment.newInstance(DeliveryListFragment.TYPE_HISTORY);
                            break;
                    }
                }
                return pages[position];
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

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                refreshPage(position);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewPager != null) {
            refreshPage(viewPager.getCurrentItem());
        }
    }

    private void refreshPage(int position) {
        if (position >= 0 && position < pages.length && pages[position] != null) {
            pages[position].refreshData();
        }
    }
}
