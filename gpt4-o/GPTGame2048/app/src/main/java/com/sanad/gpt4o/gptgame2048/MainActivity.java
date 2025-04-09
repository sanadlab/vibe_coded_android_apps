package com.sanad.gpt4o.gptgame2048;

import android.os.Bundle;
import android.widget.GridLayout;
import android.widget.TextView;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private int[][] grid = new int[4][4];
    private TextView[][] cellViews = new TextView[4][4];
    private GridLayout gridLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gridLayout = findViewById(R.id.grid_layout);
        initializeGrid();
        spawnRandomTile();
        spawnRandomTile();

        gridLayout.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeRight() {
                if (moveRight()) spawnRandomTile();
                checkGameOver();
            }

            @Override
            public void onSwipeLeft() {
                if (moveLeft()) spawnRandomTile();
                checkGameOver();
            }

            @Override
            public void onSwipeUp() {
                if (moveUp()) spawnRandomTile();
                checkGameOver();
            }

            @Override
            public void onSwipeDown() {
                if (moveDown()) spawnRandomTile();
                checkGameOver();
            }
        });

        updateGrid();
    }

    private void initializeGrid() {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                TextView cell = new TextView(this);
                cell.setTextSize(32);
                cell.setGravity(View.TEXT_ALIGNMENT_CENTER);
                cell.setBackgroundResource(R.drawable.cell_background);
                gridLayout.addView(cell, 150, 150);
                cellViews[i][j] = cell;
            }
        }
    }

    private void updateGrid() {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                int value = grid[i][j];
                cellViews[i][j].setText(value == 0 ? "" : String.valueOf(value));
            }
        }
    }

    private void spawnRandomTile() {
        Random rand = new Random();
        int i, j;
        do {
            i = rand.nextInt(4);
            j = rand.nextInt(4);
        } while (grid[i][j] != 0);
        grid[i][j] = rand.nextDouble() < 0.9 ? 2 : 4;
        updateGrid();
    }

    private boolean moveLeft() {
        boolean moved = false;
        for (int i = 0; i < 4; i++) {
            int[] row = grid[i];
            int[] newRow = new int[4];
            int position = 0;
            for (int j = 0; j < 4; j++) {
                if (row[j] != 0) {
                    if (newRow[position] == row[j]) {
                        newRow[position++] *= 2;
                        moved = true;
                    } else if (newRow[position] == 0) {
                        newRow[position] = row[j];
                    } else {
                        newRow[++position] = row[j];
                        if (position != j) moved = true;
                    }
                }
            }
            grid[i] = newRow;
        }
        updateGrid();
        return moved;
    }

    private boolean moveRight() {
        boolean moved = false;
        for (int i = 0; i < 4; i++) {
            int[] row = grid[i];
            int[] newRow = new int[4];
            int position = 3;
            for (int j = 3; j >= 0; j--) {
                if (row[j] != 0) {
                    if (newRow[position] == row[j]) {
                        newRow[position--] *= 2;
                        moved = true;
                    } else if (newRow[position] == 0) {
                        newRow[position] = row[j];
                    } else {
                        newRow[--position] = row[j];
                        if (position != j) moved = true;
                    }
                }
            }
            grid[i] = newRow;
        }
        updateGrid();
        return moved;
    }

    private boolean moveUp() {
        boolean moved = false;
        for (int j = 0; j < 4; j++) {
            int[] column = new int[4];
            for (int i = 0; i < 4; i++) {
                column[i] = grid[i][j];
            }
            int[] newColumn = new int[4];
            int position = 0;
            for (int i = 0; i < 4; i++) {
                if (column[i] != 0) {
                    if (newColumn[position] == column[i]) {
                        newColumn[position++] *= 2;
                        moved = true;
                    } else if (newColumn[position] == 0) {
                        newColumn[position] = column[i];
                    } else {
                        newColumn[++position] = column[i];
                        if (position != i) moved = true;
                    }
                }
            }
            for (int i = 0; i < 4; i++) {
                grid[i][j] = newColumn[i];
            }
        }
        updateGrid();
        return moved;
    }

    private boolean moveDown() {
        boolean moved = false;
        for (int j = 0; j < 4; j++) {
            int[] column = new int[4];
            for (int i = 0; i < 4; i++) {
                column[i] = grid[i][j];
            }
            int[] newColumn = new int[4];
            int position = 3;
            for (int i = 3; i >= 0; i--) {
                if (column[i] != 0) {
                    if (newColumn[position] == column[i]) {
                        newColumn[position--] *= 2;
                        moved = true;
                    } else if (newColumn[position] == 0) {
                        newColumn[position] = column[i];
                    } else {
                        newColumn[--position] = column[i];
                        if (position != i) moved = true;
                    }
                }
            }
            for (int i = 0; i < 4; i++) {
                grid[i][j] = newColumn[i];
            }
        }
        updateGrid();
        return moved;
    }

    private void checkGameOver() {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (grid[i][j] == 0) return;
                if (j < 3 && grid[i][j] == grid[i][j + 1]) return;
                if (i < 3 && grid[i][j] == grid[i + 1][j]) return;
            }
        }
        Toast.makeText(this, "Game Over!", Toast.LENGTH_LONG).show();
    }
}