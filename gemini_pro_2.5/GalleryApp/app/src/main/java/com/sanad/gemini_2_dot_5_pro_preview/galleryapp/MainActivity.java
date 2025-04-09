package com.sanad.gemini_2_dot_5_pro_preview.galleryapp;

import android.Manifest;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int GRID_SPAN_COUNT = 3; // Number of columns in grid view

    private RecyclerView recyclerViewPhotos;
    private PhotoAdapter photoAdapter;
    private ProgressBar progressBar;
    private TextView textViewEmpty;

    private LinearLayoutManager linearLayoutManager;
    private GridLayoutManager gridLayoutManager;
    private boolean isGridLayout = true; // Start with grid layout

    private final List<Uri> photoUris = new ArrayList<>();

    // --- Permission Handling ---
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "Storage Permission Granted");
                    loadPhotosAsync();
                } else {
                    Log.d(TAG, "Storage Permission Denied");
                    Toast.makeText(this, "Permission needed to display photos", Toast.LENGTH_SHORT).show();
                    showEmptyView();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerViewPhotos = findViewById(R.id.recyclerViewPhotos);
        progressBar = findViewById(R.id.progressBar);
        textViewEmpty = findViewById(R.id.textViewEmpty);

        setupRecyclerView();
        checkPermissionAndLoadPhotos();
    }

    private void setupRecyclerView() {
        // Initialize Layout Managers
        linearLayoutManager = new LinearLayoutManager(this);
        gridLayoutManager = new GridLayoutManager(this, GRID_SPAN_COUNT);

        // Initialize Adapter (add item click listener)
        photoAdapter = new PhotoAdapter(this, photoUri -> {
            // Handle photo click - e.g., open in a viewer Activity
            Toast.makeText(MainActivity.this, "Clicked: " + photoUri.toString(), Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(this, PhotoViewerActivity.class);
            // intent.setData(photoUri);
            // startActivity(intent);
        });

        recyclerViewPhotos.setAdapter(photoAdapter);

        // Set initial layout manager
        setLayoutManager(isGridLayout);

        // Optional: Improve performance
        recyclerViewPhotos.setHasFixedSize(true);
        recyclerViewPhotos.setItemViewCacheSize(20); // Cache more items
    }

    private void checkPermissionAndLoadPhotos() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadPhotosAsync();
        } else if (shouldShowRequestPermissionRationale(permission)) {
            // Show explanation to user (e.g., in a dialog) - Optional
            Log.w(TAG, "Showing permission rationale is recommended.");
            requestPermissionLauncher.launch(permission); // Still request it
        }
        else {
            requestPermissionLauncher.launch(permission);
        }
    }

    private void loadPhotosAsync() {
        showLoading();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<Uri> loadedUris = queryPhotos();
            // Update UI on the main thread
            runOnUiThread(() -> {
                hideLoading();
                if (loadedUris.isEmpty()) {
                    showEmptyView();
                } else {
                    showPhotos();
                    photoUris.clear();
                    photoUris.addAll(loadedUris);
                    photoAdapter.setPhotos(photoUris); // Update adapter data
                    Log.d(TAG, "Loaded " + loadedUris.size() + " photos.");
                }
            });
        });
        executor.shutdown(); // Shut down executor when done
    }


    private List<Uri> queryPhotos() {
        List<Uri> uris = new ArrayList<>();
        Uri collectionUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Scoped storage - standard URI for images
            collectionUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            // Older versions - direct external storage URI
            collectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }

        String[] projection = new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                // Add other columns if needed, like DATE_TAKEN
        };
        // Sort by date added or taken for typical gallery order
        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";

        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(
                    collectionUri,
                    projection,
                    null, // No selection (get all images)
                    null, // No selection args
                    sortOrder
            );

            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                // int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    // String name = cursor.getString(nameColumn);
                    Uri contentUri = ContentUris.withAppendedId(collectionUri, id);
                    uris.add(contentUri);
                    // Log.d(TAG, "Found photo: " + name + " Uri: " + contentUri);
                }
            } else {
                Log.e(TAG, "MediaStore query returned null cursor.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying MediaStore", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return uris;
    }

    private void setLayoutManager(boolean useGrid) {
        isGridLayout = useGrid;
        if (isGridLayout) {
            recyclerViewPhotos.setLayoutManager(gridLayoutManager);
        } else {
            recyclerViewPhotos.setLayoutManager(linearLayoutManager);
        }
        // Optional: Force redraw/remeasure if needed, though usually handled automatically
        // recyclerViewPhotos.requestLayout();
    }

    // --- Menu Handling ---

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem switchItem = menu.findItem(R.id.action_switch_layout);
        if (isGridLayout) {
            switchItem.setTitle("Switch to List");
            switchItem.setIcon(R.drawable.ic_view_list); // Set list icon
        } else {
            switchItem.setTitle("Switch to Grid");
            switchItem.setIcon(R.drawable.ic_view_grid); // Set grid icon
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_switch_layout) {
            setLayoutManager(!isGridLayout); // Toggle the layout
            invalidateOptionsMenu(); // Update the menu item icon/title
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // --- UI State Helpers ---

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerViewPhotos.setVisibility(View.GONE);
        textViewEmpty.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
    }

    private void showPhotos() {
        recyclerViewPhotos.setVisibility(View.VISIBLE);
        textViewEmpty.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE); // Ensure progressbar is hidden too
    }

    private void showEmptyView() {
        recyclerViewPhotos.setVisibility(View.GONE);
        textViewEmpty.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }
}