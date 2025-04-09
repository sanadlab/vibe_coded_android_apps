package com.sanad.gemini_2_dot_5_pro_preview.flappybird;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View; // Import View
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends AppCompatActivity {

    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make activity fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Hide navigation bar (optional, for more immersion)
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);


        setContentView(R.layout.activity_main);

        gameView = findViewById(R.id.gameView);

        // No explicit call to gameView.resume() needed here,
        // lifecycle methods (surfaceCreated) handle starting the game loop.
    }

    // Handle Activity lifecycle events to pause/resume the game thread

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("MainActivity", "onPause called");
        if (gameView != null) {
            gameView.pause(); // Pause the game loop thread
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MainActivity", "onResume called");
        // The game loop will be started/resumed in GameView's surfaceCreated method
        // if (gameView != null) {
        //    gameView.resume(); // Not typically needed here due to SurfaceView lifecycle
        // }

        // Re-hide navigation bar on resume if it reappeared
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("MainActivity", "onDestroy called");
        // Ensure thread is stopped cleanly, although pause() called in onPause should handle it.
        // if (gameView != null) {
        // gameView.pause(); // Already called in onPause usually
        // }
    }
}