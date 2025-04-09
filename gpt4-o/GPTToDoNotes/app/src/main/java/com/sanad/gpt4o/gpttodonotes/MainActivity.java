package com.sanad.gpt4o.gpttodonotes;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.sanad.gpt4o.gpttodonotes.MarkdownHelperKt;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_CAMERA = 1;
    private static final int REQUEST_CODE_PERMISSIONS = 2;

    private EditText editText;
    private ImageView imageView;
    private Bitmap picture;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = findViewById(R.id.editText);
        imageView = findViewById(R.id.imageView);

        Button takePictureButton = findViewById(R.id.takePictureButton);
        Button saveNoteButton = findViewById(R.id.saveNoteButton);

        takePictureButton.setOnClickListener(v -> {
            if (checkPermissions()) {
                openCamera();
            } else {
                requestPermissions();
            }
        });
        Button viewNotesButton = findViewById(R.id.viewNotesButton);
        viewNotesButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NoteListActivity.class);
            startActivity(intent);
        });

        saveNoteButton.setOnClickListener(v -> saveNote());
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, REQUEST_CODE_PERMISSIONS);
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, REQUEST_CODE_CAMERA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CAMERA && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            picture = (Bitmap) extras.get("data");
            imageView.setImageBitmap(picture);
            imageView.setVisibility(View.VISIBLE);
        }
    }

    private void saveNote() {
        String text = editText.getText().toString();
        if (text.isEmpty()) {
            Toast.makeText(this, "Please enter a note.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert markdown to plain text for saving
        String textToSave = MarkdownHelperKt.getHtmlAsText(MarkdownHelperKt.convertMarkdownToHtml(text));

        // Create a folder to save notes
        File folder = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Notes");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // Save note as a text file
        String fileName = "Note_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt";
        File file = new File(folder, fileName);

        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(textToSave.getBytes());

            // Save image if taken
            if (picture != null) {
                String imageName = fileName.replace(".txt", ".jpg");
                File imageFile = new File(folder, imageName);
                FileOutputStream imageFos = new FileOutputStream(imageFile);
                picture.compress(Bitmap.CompressFormat.JPEG, 100, imageFos);
                imageFos.close();
            }

            fos.close();
            Toast.makeText(this, "Note saved!", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save note.", Toast.LENGTH_SHORT).show();
        }
    }
}