package com.sanad.gemini_2_dot_5_pro_preview.weatherapp;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.TimeZone;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    // Open-Meteo API Endpoint
    private final String BASE_URL = "https://api.open-meteo.com/v1/forecast?";
    // Parameters: latitude, longitude, current_weather, daily forecast data
    private final String CURRENT_PARAM = "current_weather=true";
    private final String DAILY_PARAMS = "&daily=weathercode,temperature_2m_max,temperature_2m_min";
    private final String UNITS_PARAM = "&temperature_unit=celsius&windspeed_unit=ms";
    private final String TIMEZONE_PARAM = "&timezone=auto"; // Auto-detect timezone

    // --- UI Elements ---
    private EditText editTextCity;
    private Button buttonGetWeather;
    private Button buttonCurrentLocation;
    private TextView textViewCity;
    private TextView textViewTemperature;
    private TextView textViewDescription;
    private TextView textViewWind;
    private ProgressBar progressBar;
    private LinearLayout currentWeatherInfoLayout;
    private LinearLayout forecastLayout;
    private TextView textViewForecastLabel;

    // --- Forecast Views (assuming 3 days) ---
    private TextView forecastDate1, forecastDesc1, forecastTemp1;
    private TextView forecastDate2, forecastDesc2, forecastTemp2;
    private TextView forecastDate3, forecastDesc3, forecastTemp3;
    private ImageView forecastIcon1, forecastIcon2, forecastIcon3;


    // --- Services & Utilities ---
    private RequestQueue requestQueue;
    private Geocoder geocoder;
    private FusedLocationProviderClient fusedLocationClient;
    private ExecutorService executorService;
    private DecimalFormat decimalFormat = new DecimalFormat("#.#");
    private SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private SimpleDateFormat outputDateFormat = new SimpleDateFormat("EEE, MMM d", Locale.US);


    // --- Permission Handling ---
    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                boolean fineLocationGranted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                boolean coarseLocationGranted = permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                if (fineLocationGranted || coarseLocationGranted) {
                    Log.d(TAG, "Location Permission Granted");
                    // Try getting current location again if the button was just pressed
                    getCurrentLocationWeather();
                } else {
                    Log.d(TAG, "Location Permission Denied");
                    Toast.makeText(this, "Location permission needed for current location feature.", Toast.LENGTH_SHORT).show();
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI
        initializeUI();

        // Initialize Services
        requestQueue = Volley.newRequestQueue(this);
        geocoder = new Geocoder(this, Locale.getDefault());
        executorService = Executors.newSingleThreadExecutor();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Set Button Click Listeners
        buttonGetWeather.setOnClickListener(view -> handleCitySearch());
        buttonCurrentLocation.setOnClickListener(view -> handleCurrentLocationRequest());

        // Set default timezone for date formatting if needed (optional)
        // inputDateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // API provides UTC dates
    }

    private void initializeUI() {
        editTextCity = findViewById(R.id.editTextCity);
        buttonGetWeather = findViewById(R.id.buttonGetWeather);
        buttonCurrentLocation = findViewById(R.id.buttonCurrentLocation);
        textViewCity = findViewById(R.id.textViewCity);
        textViewTemperature = findViewById(R.id.textViewTemperature);
        textViewDescription = findViewById(R.id.textViewDescription);
        textViewWind = findViewById(R.id.textViewWind);
        progressBar = findViewById(R.id.progressBar);
        currentWeatherInfoLayout = findViewById(R.id.currentWeatherInfoLayout);
        forecastLayout = findViewById(R.id.forecastLayout);
        textViewForecastLabel = findViewById(R.id.textViewForecastLabel);

        // Forecast Day 1 Views
        forecastDate1 = findViewById(R.id.forecast_day1_date);
        forecastDesc1 = findViewById(R.id.forecast_day1_desc);
        forecastTemp1 = findViewById(R.id.forecast_day1_temp);
        forecastIcon1 = findViewById(R.id.forecast_day1_icon);
        // Forecast Day 2 Views
        forecastDate2 = findViewById(R.id.forecast_day2_date);
        forecastDesc2 = findViewById(R.id.forecast_day2_desc);
        forecastTemp2 = findViewById(R.id.forecast_day2_temp);
        forecastIcon2 = findViewById(R.id.forecast_day2_icon);
        // Forecast Day 3 Views
        forecastDate3 = findViewById(R.id.forecast_day3_date);
        forecastDesc3 = findViewById(R.id.forecast_day3_desc);
        forecastTemp3 = findViewById(R.id.forecast_day3_temp);
        forecastIcon3 = findViewById(R.id.forecast_day3_icon);
    }

    // --- Event Handlers ---

    private void handleCitySearch() {
        String city = editTextCity.getText().toString().trim();
        if (city.isEmpty()) {
            Toast.makeText(MainActivity.this, "Please enter a city name", Toast.LENGTH_SHORT).show();
        } else {
            hideKeyboard();
            getCoordinatesAndFetchWeather(city);
        }
    }

    private void handleCurrentLocationRequest() {
        hideKeyboard();
        if (checkLocationPermissions()) {
            getCurrentLocationWeather();
        } else {
            requestLocationPermissions();
        }
    }

    // --- Location Handling ---

    private boolean checkLocationPermissions() {
        boolean fineLocationGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarseLocationGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        return fineLocationGranted || coarseLocationGranted;
    }

    private void requestLocationPermissions() {
        requestPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    @SuppressLint("MissingPermission") // We check permissions before calling this
    private void getCurrentLocationWeather() {
        showLoading(true);
        // Request current location (more accurate than last known)
        // Requires Google Play Services
        fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY, new CancellationTokenSource().getToken())
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        Log.d(TAG, "Current Location: Lat=" + location.getLatitude() + ", Lon=" + location.getLongitude());
                        // Reverse geocode to get a display name (optional, can show "Current Location")
                        reverseGeocodeAndFetch(location.getLatitude(), location.getLongitude());
                    } else {
                        Log.w(TAG, "FusedLocationClient returned null location.");
                        // Fallback: Try Last Known Location (might be null or stale)
                        tryGetLastKnownLocation();
                    }
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Error getting current location", e);
                    tryGetLastKnownLocation(); // Try fallback on failure
                });
    }

    @SuppressLint("MissingPermission") // We check permissions before calling this
    private void tryGetLastKnownLocation() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        Log.d(TAG, "Last Known Location: Lat=" + location.getLatitude() + ", Lon=" + location.getLongitude());
                        reverseGeocodeAndFetch(location.getLatitude(), location.getLongitude());
                    } else {
                        Log.w(TAG, "Last known location is also null.");
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(MainActivity.this, "Could not retrieve location.", Toast.LENGTH_SHORT).show();
                        });
                    }
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Error getting last known location", e);
                    runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(MainActivity.this, "Failed to get location.", Toast.LENGTH_SHORT).show();
                    });
                });
    }

    // Reverse geocode coordinates to get a city name (runs in background)
    private void reverseGeocodeAndFetch(double latitude, double longitude) {
        executorService.execute(() -> {
            String cityName = "Current Location"; // Default display name
            try {
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    // Construct a name from address details
                    String city = address.getLocality();
                    String adminArea = address.getSubAdminArea() != null ? address.getSubAdminArea() : address.getAdminArea(); // Try sub-admin first
                    if (city != null) {
                        cityName = city;
                    } else if (adminArea != null) {
                        cityName = adminArea; // Use admin area if city is null
                    }
                    Log.d(TAG, "Reverse Geocoded: " + cityName);
                }
            } catch (IOException e) {
                Log.w(TAG, "Reverse geocoding failed", e);
            }

            // Fetch weather on main thread
            final String finalCityName = cityName; // Need final variable for lambda
            runOnUiThread(() -> fetchWeatherData(latitude, longitude, finalCityName));
        });
    }


    // --- Geocoding for City Search ---
    private void getCoordinatesAndFetchWeather(String cityName) {
        showLoading(true);
        executorService.execute(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocationName(cityName, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    double latitude = address.getLatitude();
                    double longitude = address.getLongitude();
                    String resolvedCityName = address.getLocality() != null ? address.getLocality() :
                            (address.getSubAdminArea() != null ? address.getSubAdminArea() :
                                    (address.getAdminArea() != null ? address.getAdminArea() : cityName));
                    Log.d(TAG, "Geocoded " + cityName + " to: Lat=" + latitude + ", Lon=" + longitude);
                    runOnUiThread(() -> fetchWeatherData(latitude, longitude, resolvedCityName));
                } else {
                    Log.w(TAG, "Geocoder returned no results for: " + cityName);
                    handleFetchError("Could not find coordinates for city: " + cityName);
                }
            } catch (IOException e) {
                Log.e(TAG, "Geocoder IOException for city: " + cityName, e);
                handleFetchError("Geocoding failed. Check network or city name.");
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Geocoder IllegalArgumentException for city: " + cityName, e);
                handleFetchError("Invalid city name entered.");
            }
        });
    }

    // --- Weather Data Fetching & Parsing ---

    private void fetchWeatherData(double latitude, double longitude, String displayName) {
        String apiUrl = BASE_URL
                + "latitude=" + latitude
                + "&longitude=" + longitude
                + "&" + CURRENT_PARAM // Explicitly add current weather param
                + DAILY_PARAMS
                + UNITS_PARAM
                + TIMEZONE_PARAM; // Add timezone parameter
        Log.d(TAG, "API URL: " + apiUrl);

        // Loading indicator should already be visible

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET, apiUrl, null,
                response -> {
                    Log.d(TAG, "API Response: " + response.toString());
                    showLoading(false);
                    parseWeatherData(response, displayName);
                },
                error -> {
                    Log.e(TAG, "Volley Error: " + error.toString());
                    String message = "Could not fetch weather data.";
                    if (error.networkResponse != null) message = "API Error: " + error.networkResponse.statusCode;
                    else if (!isNetworkAvailable()) message = "No internet connection.";
                    handleFetchError(message);
                });
        requestQueue.add(jsonObjectRequest);
    }


    private void parseWeatherData(JSONObject response, String displayName) { // displayName is the parameter causing the issue
        try {
            // -- Parse Current Weather --
            double currentTemp = Double.NaN;
            double currentWindSpeed = Double.NaN;
            int currentWeatherCode = -1;

            if (response.has("current_weather")) {
                JSONObject current_weather = response.getJSONObject("current_weather");
                currentTemp = current_weather.optDouble("temperature", Double.NaN);
                currentWindSpeed = current_weather.optDouble("windspeed", Double.NaN);
                currentWeatherCode = current_weather.optInt("weathercode", -1);
            } else {
                Log.w(TAG, "API Response missing 'current_weather' object.");
            }

            // -- Parse Daily Forecast --
            JSONObject daily = null;
            if(response.has("daily")){
                daily = response.getJSONObject("daily");
            } else {
                Log.w(TAG, "API Response missing 'daily' forecast object.");
                // Make final copies even if forecast is missing, for the current weather update below
                final String finalDisplayName = displayName;
                final double finalCurrentTemp = currentTemp;
                final int finalCurrentWeatherCode = currentWeatherCode;
                final double finalCurrentWindSpeed = currentWindSpeed;
                runOnUiThread(() -> {
                    forecastLayout.setVisibility(View.GONE);
                    textViewForecastLabel.setVisibility(View.GONE);
                    updateCurrentUI(finalDisplayName, finalCurrentTemp, finalCurrentWeatherCode, finalCurrentWindSpeed);
                });
                return;
            }

            JSONArray timeArray = daily.getJSONArray("time");
            JSONArray weathercodeArray = daily.getJSONArray("weathercode");
            JSONArray tempMaxArray = daily.getJSONArray("temperature_2m_max");
            JSONArray tempMinArray = daily.getJSONArray("temperature_2m_min");

            int forecastDaysToShow = Math.min(timeArray.length(), 3);

            // Create lists/arrays for forecast data
            final String[] dates = new String[forecastDaysToShow]; // Make arrays effectively final by assignment
            final String[] descs = new String[forecastDaysToShow];
            final String[] temps = new String[forecastDaysToShow];
            final int[] icons = new int[forecastDaysToShow];

            for (int i = 0; i < forecastDaysToShow; i++) {
                String dateStr = timeArray.getString(i);
                int code = weathercodeArray.getInt(i);
                double maxTemp = tempMaxArray.optDouble(i, Double.NaN);
                double minTemp = tempMinArray.optDouble(i, Double.NaN);

                dates[i] = formatDate(dateStr);
                descs[i] = getWeatherDescription(code);
                temps[i] = formatTemperatureRange(minTemp, maxTemp);
                icons[i] = getWeatherIconResource(code);
            }

            // *** FIX STARTS HERE ***
            // Create final copies of variables needed inside the lambda
            final String finalDisplayName = displayName;
            final double finalCurrentTemp = currentTemp;
            final int finalCurrentWeatherCode = currentWeatherCode;
            final double finalCurrentWindSpeed = currentWindSpeed;
            // The forecast arrays (dates, icons, descs, temps) are already effectively final above

            // -- Update UI (on main thread) --
            runOnUiThread(() -> {
                // Use the final copies inside the lambda
                updateCurrentUI(finalDisplayName, finalCurrentTemp, finalCurrentWeatherCode, finalCurrentWindSpeed);
                updateForecastUI(dates, icons, descs, temps); // Arrays are okay
            });
            // *** FIX ENDS HERE ***

        } catch (JSONException e) {
            Log.e(TAG, "JSON Parsing Error: " + e.getMessage());
            handleFetchError("Error parsing weather data");
        }
    }


    // --- UI Update Methods ---

    private void updateCurrentUI(String city, double temp, int weatherCode, double windSpeed) {
        textViewCity.setText(city);

        if (!Double.isNaN(temp)) {
            textViewTemperature.setText(decimalFormat.format(temp) + "°C");
        } else {
            textViewTemperature.setText("N/A");
        }

        textViewDescription.setText(getWeatherDescription(weatherCode).toUpperCase());

        if (!Double.isNaN(windSpeed)) {
            textViewWind.setText("Wind: " + decimalFormat.format(windSpeed) + " m/s");
        } else {
            textViewWind.setText("Wind: N/A");
        }

        // Only show current weather if data was available
        currentWeatherInfoLayout.setVisibility(Double.isNaN(temp) ? View.GONE : View.VISIBLE);

    }

    private void updateForecastUI(String[] dates, int[] iconResIds, String[] descs, String[] temps) {
        if (dates.length < 1) { // Hide if no forecast data
            forecastLayout.setVisibility(View.GONE);
            textViewForecastLabel.setVisibility(View.GONE);
            return;
        }

        // Day 1
        forecastDate1.setText(dates[0]);
        forecastIcon1.setImageResource(iconResIds[0]);
        forecastDesc1.setText(descs[0]);
        forecastTemp1.setText(temps[0]);

        // Day 2 (Check if data exists)
        if (dates.length > 1) {
            forecastDate2.setText(dates[1]);
            forecastIcon2.setImageResource(iconResIds[1]);
            forecastDesc2.setText(descs[1]);
            forecastTemp2.setText(temps[1]);
            forecastDate2.getRootView().setVisibility(View.VISIBLE); // Show layout for day 2
        } else {
            forecastDate2.getRootView().setVisibility(View.GONE); // Hide layout for day 2
        }

        // Day 3 (Check if data exists)
        if (dates.length > 2) {
            forecastDate3.setText(dates[2]);
            forecastIcon3.setImageResource(iconResIds[2]);
            forecastDesc3.setText(descs[2]);
            forecastTemp3.setText(temps[2]);
            forecastDate3.getRootView().setVisibility(View.VISIBLE);
        } else {
            forecastDate3.getRootView().setVisibility(View.GONE);
        }

        // Show the forecast section
        textViewForecastLabel.setVisibility(View.VISIBLE);
        forecastLayout.setVisibility(View.VISIBLE);
    }


    // --- Helper Methods ---

    // Improved error handling
    private void handleFetchError(String message) {
        runOnUiThread(() -> {
            showLoading(false);
            currentWeatherInfoLayout.setVisibility(View.GONE);
            forecastLayout.setVisibility(View.GONE);
            textViewForecastLabel.setVisibility(View.GONE);
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
        });
    }

    private String formatDate(String dateString) {
        try {
            Date date = inputDateFormat.parse(dateString);
            return outputDateFormat.format(date);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + dateString, e);
            return dateString; // Return original string if parsing fails
        }
    }

    private String formatTemperatureRange(double min, double max) {
        String minStr = Double.isNaN(min) ? "--" : decimalFormat.format(min);
        String maxStr = Double.isNaN(max) ? "--" : decimalFormat.format(max);
        return maxStr + "° / " + minStr + "°";
    }


    private String getWeatherDescription(int code) {
        // (Same implementation as before)
        switch (code) {
            case 0: return "Clear sky";
            case 1: return "Mainly clear";
            case 2: return "Partly cloudy";
            case 3: return "Overcast";
            case 45: return "Fog";
            case 48: return "Depositing rime fog";
            case 51: return "Light drizzle";
            case 53: return "Moderate drizzle";
            case 55: return "Dense drizzle";
            case 56: return "Light freezing drizzle";
            case 57: return "Dense freezing drizzle";
            case 61: return "Slight rain";
            case 63: return "Moderate rain";
            case 65: return "Heavy rain";
            case 66: return "Light freezing rain";
            case 67: return "Heavy freezing rain";
            case 71: return "Slight snow fall";
            case 73: return "Moderate snow fall";
            case 75: return "Heavy snow fall";
            case 77: return "Snow grains";
            case 80: return "Slight rain showers";
            case 81: return "Moderate rain showers";
            case 82: return "Violent rain showers";
            case 85: return "Slight snow showers";
            case 86: return "Heavy snow showers";
            case 95: return "Thunderstorm";
            case 96: return "Thunderstorm + slight hail";
            case 99: return "Thunderstorm + heavy hail";
            case -1: return "N/A";
            default: return "Unknown";
        }
    }

    // Maps weather code to a drawable resource ID (NEEDS ICONS!)
    private int getWeatherIconResource(int code) {
        // ** IMPORTANT: You need to add actual drawable icons to your project **
        // These are just examples - replace with your actual icon resource IDs
        switch (code) {
            case 0: return R.drawable.ic_clear_sky; // Example: create ic_clear_sky.xml
            case 1: return R.drawable.ic_mainly_clear;
            case 2: return R.drawable.ic_partly_cloudy;
            case 3: return R.drawable.ic_overcast;
            case 45: case 48: return R.drawable.ic_clear_sky;
            case 51: case 53: case 55: case 56: case 57: return R.drawable.ic_drizzle;
            case 61: case 63: case 65: case 66: case 67: return R.drawable.ic_rain;
            case 71: case 73: case 75: case 77: return R.drawable.ic_clear_sky;
            case 80: case 81: case 82: return R.drawable.ic_rain_showers;
            case 85: case 86: return R.drawable.ic_clear_sky;
            case 95: case 96: case 99: return R.drawable.ic_clear_sky;
            default: return android.R.drawable.ic_menu_help; // Default placeholder
        }
    }

    private void showLoading(boolean isLoading) {
        runOnUiThread(() -> { // Ensure UI updates are on the main thread
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (isLoading) { // Hide content when loading starts
                currentWeatherInfoLayout.setVisibility(View.GONE);
                forecastLayout.setVisibility(View.GONE);
                textViewForecastLabel.setVisibility(View.GONE);
            }
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view == null) view = new View(this);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}