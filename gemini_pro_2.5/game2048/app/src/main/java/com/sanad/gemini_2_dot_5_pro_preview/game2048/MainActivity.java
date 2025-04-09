package com.sanad.gemini_2_dot_5_pro_preview.game2048;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements GameView.GameListener {

    private GameView gameView;
    private TextView scoreTextView;
    private Button restartButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gameView = findViewById(R.id.gameView);
        scoreTextView = findViewById(R.id.scoreTextView);
        restartButton = findViewById(R.id.restartButton);

        // Set the listener in GameView to this Activity
        gameView.setGameListener(this);

        restartButton.setOnClickListener(v -> showRestartConfirmationDialog());

        // Initialize score display
        onScoreChanged(0); // Show initial score (which should be 0)
    }

    @Override
    public void onScoreChanged(int score) {
        // Update the score TextView on the UI thread
        runOnUiThread(() -> scoreTextView.setText(String.valueOf(score)));
    }

    @Override
    public void onGameOver() {
        // Show a game over dialog on the UI thread
        runOnUiThread(() -> showGameOverDialog());
    }

    @Override
    public void onGameWon() {
        // Show a game won dialog on the UI thread (Optional)
        runOnUiThread(() -> showGameWonDialog());
    }


    private void showRestartConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Restart Game?")
                .setMessage("Are you sure you want to restart the current game?")
                .setPositiveButton("Restart", (dialog, which) -> gameView.restartGame())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showGameOverDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Game Over!")
                .setMessage("You couldn't make any more moves. Your final score is: " + scoreTextView.getText())
                .setPositiveButton("Restart", (dialog, which) -> gameView.restartGame())
                .setNegativeButton("Close", null)
                .setCancelable(false) // Prevent dismissing by tapping outside
                .show();
    }

    private void showGameWonDialog() {
        new AlertDialog.Builder(this)
                .setTitle("You Win!")
                .setMessage("Congratulations! You reached 2048! Your score is: " + scoreTextView.getText() + "\nKeep playing?")
                .setPositiveButton("Keep Playing", null) // Just dismiss dialog
                .setNegativeButton("Restart", (dialog, which) -> gameView.restartGame())
                .setCancelable(false)
                .show();
    }

}