package com.example.shopping;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ManageAddressActivity extends AppCompatActivity {

    private TextInputEditText etAddress, etCity;
    private FirebaseFirestore db;
    private String userEmail;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_address);

        db = FirebaseFirestore.getInstance();
        userEmail = getSharedPreferences("UserSession", MODE_PRIVATE).getString("userEmail", "");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initToolbar();
        initViews();
        loadCurrentAddress();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarAddress);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        etAddress = findViewById(R.id.etSavedAddress);
        etCity = findViewById(R.id.etSavedCity);
        Button btnSave = findViewById(R.id.btnSaveAddress);
        Button btnLocate = findViewById(R.id.btnLocateMe);

        btnLocate.setOnClickListener(v -> requestLocation());

        btnSave.setOnClickListener(v -> {
            String address = etAddress.getText().toString().trim();
            String city = etCity.getText().toString().trim();

            if (TextUtils.isEmpty(address)) {
                etAddress.setError("Address required");
                return;
            }
            if (TextUtils.isEmpty(city)) {
                etCity.setError("City required");
                return;
            }

            saveAddress(address, city);
        });
    }

    private void requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                getAddressFromLocation(location);
            } else {
                Toast.makeText(this, "Could not find location. Make sure GPS is ON.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getAddressFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address adr = addresses.get(0);
                String fullAddress = adr.getAddressLine(0);
                String city = adr.getLocality();

                etAddress.setText(fullAddress);
                etCity.setText(city != null ? city : adr.getAdminArea());
            }
        } catch (Exception e) {
            Toast.makeText(this, "Geocoder error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestLocation();
        }
    }

    private void loadCurrentAddress() {
        if (userEmail.isEmpty()) return;

        db.collection("users").document(userEmail).get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        if (user.getSavedAddress() != null) etAddress.setText(user.getSavedAddress());
                        if (user.getSavedCity() != null) etCity.setText(user.getSavedCity());
                    }
                });
    }

    private void saveAddress(String address, String city) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("savedAddress", address);
        updates.put("savedCity", city);

        db.collection("users").document(userEmail)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Address Saved!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}