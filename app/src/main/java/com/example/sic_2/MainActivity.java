package com.example.sic_2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    FloatingActionButton fab;
    DatabaseReference databaseReference;
    ValueEventListener eventListener;
    RecyclerView recyclerView;
    List<DataClass> dataList;
//    MyAdapter adapter;
    SearchView searchView;

    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNavigationView;
    private Map<Integer, Fragment> fragmentMap;

    HomeFragment homeFragment;
    NotificationFragment notificationFragment;
    SettingsFragment settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase and RecyclerView
//        recyclerView = findViewById(R.id.recyclerView);
        fab = findViewById(R.id.fab);
//        searchView = findViewById(R.id.search);
//        searchView.clearFocus();
//
//        GridLayoutManager gridLayoutManager = new GridLayoutManager(MainActivity.this, 1);
//        recyclerView.setLayoutManager(gridLayoutManager);
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//        builder.setCancelable(false);
//        builder.setView(R.layout.progress_layout);
//        AlertDialog dialog = builder.create();
//        dialog.show();
//
//        dataList = new ArrayList<>();
//        adapter = new MyAdapter(MainActivity.this, dataList);
//        recyclerView.setAdapter(adapter);
//
//        databaseReference = FirebaseDatabase.getInstance().getReference("Android Tutorials");
//        dialog.show();
//        eventListener = databaseReference.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                dataList.clear();
//                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
//                    DataClass dataClass = itemSnapshot.getValue(DataClass.class);
//                    dataClass.setKey(itemSnapshot.getKey());
//                    dataList.add(dataClass);
//                }
//                adapter.notifyDataSetChanged();
//                dialog.dismiss();
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                dialog.dismiss();
//            }
//        });
//
//        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//            @Override
//            public boolean onQueryTextSubmit(String query) {
//                return false;
//            }
//
//            @Override
//            public boolean onQueryTextChange(String newText) {
//                searchList(newText);
//                return true;
//            }
//        });
//

        fab.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, UploadActivity.class);
            startActivity(intent);
        });

        // Initialize Drawer and Bottom Navigation
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);
        ImageView btnOpenDrawer = findViewById(R.id.btnOpenDrawer);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        setupFragmentMap();
        setupDrawerNavigation(navigationView);
        setupBottomNavigationView();

        // Open Drawer Button Listener
        btnOpenDrawer.setOnClickListener(view -> {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // Load the default fragment
        loadFragment(new HomeFragment());
    }

//    public void searchList(String text) {
//        ArrayList<DataClass> searchList = new ArrayList<>();
//        for (DataClass dataClass : dataList) {
//            if (dataClass.getDataTitle().toLowerCase().contains(text.toLowerCase())) {
//                searchList.add(dataClass);
//            }
//        }
//        adapter.searchDataList(searchList);
//    }

    private void setupFragmentMap() {
        fragmentMap = new HashMap<>();
        fragmentMap.put(R.id.home, new HomeFragment());
        fragmentMap.put(R.id.notification, new NotificationFragment());
        fragmentMap.put(R.id.settings, new SettingsFragment());
    }

    private void setupBottomNavigationView() {
        BadgeDrawable badgeDrawable = bottomNavigationView.getOrCreateBadge(R.id.notification);
        badgeDrawable.setVisible(true);
        badgeDrawable.setNumber(4);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.add_button) {
                showInputDialog(); // Show dialog when add button is clicked
                return true; // Return true to indicate the event was handled
            }

            Fragment selectedFragment = fragmentMap.get(item.getItemId());
            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true; // Return true to indicate the event was handled
            }
            return false; // Return false if no action was taken
        });
    }

    private void loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
        }
    }

    private void showInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Write a Message");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String message = input.getText().toString();
            // Pass the message to HomeFragment
            if (homeFragment != null) {
                homeFragment.addMessage(message); // Call the method to add the message
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show(); // Show the dialog
    }

    private void setupDrawerNavigation(@NonNull NavigationView navigationView) {
        // Implement navigation drawer setup logic here
    }
}
