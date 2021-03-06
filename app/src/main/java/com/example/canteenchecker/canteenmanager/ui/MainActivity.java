package com.example.canteenchecker.canteenmanager.ui;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.canteenchecker.canteenmanager.CanteenManagerApplication;
import com.example.canteenchecker.canteenmanager.R;
import com.example.canteenchecker.canteenmanager.domain.Canteen;
import com.example.canteenchecker.canteenmanager.proxy.ServiceProxy;
import com.example.canteenchecker.canteenmanager.service.CanteenFirebaseMessagingService;
import com.example.canteenchecker.canteenmanager.ui.helper.SectionsPageAdapter;

import java.io.IOException;

/**
 * handles tablayout fragments
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.toString();

    private SectionsPageAdapter sectionsPageAdapter = new SectionsPageAdapter(getSupportFragmentManager());
    private ViewPager viewPager;

    private CanteenFragment canteenFragment = new CanteenFragment();
    private RatingsFragment ratingsFragment = new RatingsFragment();
    private Toolbar toolbar;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            GetCanteenData(true);
        }
    };

    private void showCanteenStats() {
        Intent intent = new Intent(MainActivity.this, StatsActivity.class);
        startActivity(intent);
    }

    private void logout() {
        CanteenManagerApplication.getInstance().setAuthenticationToken(null);
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Important -> otherwise memory leaks!!
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        viewPager = findViewById(R.id.container);
        setupViewPager(viewPager);

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setSubtitle(CanteenManagerApplication.getInstance().getUsername());
        toolbar.inflateMenu(R.menu.main_activity_menu);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // Handle item selection
                switch (item.getItemId()) {
                    case R.id.menu_item_logout:
                        logout();
                        return true;
                    case R.id.menu_item_show_stats:
                        showCanteenStats();
                        return true;
                    default:
                        return false;
                }
            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(
                broadcastReceiver,
                CanteenFirebaseMessagingService.createCanteenChangedIntentFilter());



        GetCanteenData(false);
    }

    private void setupViewPager(ViewPager viewPager) {
        SectionsPageAdapter adapter =
                new SectionsPageAdapter(getSupportFragmentManager());

        adapter.addFragment(canteenFragment, getString(R.string.canteen_fragment_title));
        adapter.addFragment(ratingsFragment, getString(R.string.ratings_fragment_title));

        viewPager.setAdapter(adapter);
    }

    @SuppressLint("StaticFieldLeak")
    private void GetCanteenData(final boolean doInBackground) {
        new AsyncTask<Void, Void, Canteen>() {
            private ProgressDialog progressDialog;

            @Override
            protected void onPreExecute() {
                if (!doInBackground) {
                    progressDialog = new ProgressDialog(MainActivity.this);
                    progressDialog.setTitle("Fetching data for you...");
                    progressDialog.show();
                }
            }

            @Override
            protected Canteen doInBackground(Void... Voids) {
                try {
                    return new ServiceProxy().getCanteen();
                } catch (IOException e) {
                    Log.e(TAG, getString(R.string.fetch_canteenData_failed), e);
                    Toast.makeText(MainActivity.this, R.string.fetch_canteenData_failed, Toast.LENGTH_SHORT).show();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Canteen canteen) {
                if (!doInBackground) {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                }

                if (canteen != null) {
                    CanteenManagerApplication.getInstance().setCanteenId(canteen.getId());
                    canteenFragment.setCanteenData(canteen);
                    if (canteen.getRatings() != null) {
                        ratingsFragment.setRatings(canteen.getRatings());
                    }
                }

            }
        }.execute();
    }


}
