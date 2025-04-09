package com.sanad.gemini_2_dot_5_pro_preview.game2048;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import androidx.core.content.ContextCompat;
import androidx.core.view.GestureDetectorCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GameView extends View {

    private static final int GRID_SIZE = 4;
    private int[][] grid = new int[GRID_SIZE][GRID_SIZE];
    private int score = 0;
    private boolean isGameOver = false;
    private boolean isMoving = false; // To prevent multiple moves at once

    private Paint gridBackgroundPaint;
    private Paint cellEmptyPaint;
    private Paint tilePaint;
    private Paint textPaint;
    private Map<Integer, Integer> tileColorMap = new HashMap<>();
    private Map<Integer, Integer> textColorMap = new HashMap<>();

    private float cellSize;
    private float cellPadding;
    private float cornerRadius;

    private GestureDetectorCompat gestureDetector;
    private GameListener gameListener; // Interface for callbacks

    // Interface to communicate score changes and game over state
    public interface GameListener {
        void onScoreChanged(int score);
        void onGameOver();
        void onGameWon(); // Optional: If you want to detect winning
    }

    public void setGameListener(GameListener listener) {
        this.gameListener = listener;
    }

    // --- Constructors ---
    public GameView(Context context) {
        super(context);
        init(context);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public GameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    // --- Initialization ---
    private void init(Context context) {
        gridBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridBackgroundPaint.setColor(ContextCompat.getColor(context, R.color.grid_background));
        gridBackgroundPaint.setStyle(Paint.Style.FILL);

        cellEmptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cellEmptyPaint.setColor(ContextCompat.getColor(context, R.color.cell_empty_background));
        cellEmptyPaint.setStyle(Paint.Style.FILL);

        tilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tilePaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(48f); // Start with a base size, adjust in onDraw

        loadColors(context);

        gestureDetector = new GestureDetectorCompat(context, new GestureListener());

        restartGame(); // Start the game when view is created
    }

    private void loadColors(Context context) {
        // Tile Background Colors
        tileColorMap.put(0, ContextCompat.getColor(context, R.color.cell_empty_background)); // Should not be drawn
        tileColorMap.put(2, ContextCompat.getColor(context, R.color.tile_2));
        tileColorMap.put(4, ContextCompat.getColor(context, R.color.tile_4));
        tileColorMap.put(8, ContextCompat.getColor(context, R.color.tile_8));
        tileColorMap.put(16, ContextCompat.getColor(context, R.color.tile_16));
        tileColorMap.put(32, ContextCompat.getColor(context, R.color.tile_32));
        tileColorMap.put(64, ContextCompat.getColor(context, R.color.tile_64));
        tileColorMap.put(128, ContextCompat.getColor(context, R.color.tile_128));
        tileColorMap.put(256, ContextCompat.getColor(context, R.color.tile_256));
        tileColorMap.put(512, ContextCompat.getColor(context, R.color.tile_512));
        tileColorMap.put(1024, ContextCompat.getColor(context, R.color.tile_1024));
        tileColorMap.put(2048, ContextCompat.getColor(context, R.color.tile_2048));
        // Add more if needed

        // Text Colors (Light for darker tiles, Dark for lighter tiles)
        int lightText = ContextCompat.getColor(context, R.color.text_light);
        int darkText = ContextCompat.getColor(context, R.color.text_dark);
        textColorMap.put(2, darkText);
        textColorMap.put(4, darkText);
        textColorMap.put(8, lightText);
        textColorMap.put(16, lightText);
        textColorMap.put(32, lightText);
        textColorMap.put(64, lightText);
        textColorMap.put(128, lightText);
        textColorMap.put(256, lightText);
        textColorMap.put(512, lightText);
        textColorMap.put(1024, lightText);
        textColorMap.put(2048, lightText);
    }

    // --- Game State Management ---
    public void restartGame() {
        grid = new int[GRID_SIZE][GRID_SIZE]; // Reset grid
        score = 0;
        isGameOver = false;
        isMoving = false;
        addNewTile();
        addNewTile(); // Start with two tiles
        if (gameListener != null) {
            gameListener.onScoreChanged(score);
        }
        invalidate(); // Request redraw
    }

    private void addNewTile() {
        List<int[]> emptyCells = new ArrayList<>();
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                if (grid[r][c] == 0) {
                    emptyCells.add(new int[]{r, c});
                }
            }
        }

        if (!emptyCells.isEmpty()) {
            Random random = new Random();
            int[] cell = emptyCells.get(random.nextInt(emptyCells.size()));
            grid[cell[0]][cell[1]] = random.nextInt(10) < 9 ? 2 : 4; // 90% chance of 2
        }
    }

    // --- Drawing ---
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Ensure square grid based on smaller dimension
        int size = Math.min(w, h);
        cellPadding = size * 0.02f; // Padding between cells
        cellSize = (size - (cellPadding * (GRID_SIZE + 1))) / GRID_SIZE;
        cornerRadius = cellSize * 0.1f; // Rounded corners for tiles
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw grid background
        float totalGridWidth = (cellSize * GRID_SIZE) + (cellPadding * (GRID_SIZE + 1));
        float startX = (getWidth() - totalGridWidth) / 2f;
        float startY = (getHeight() - totalGridWidth) / 2f; // Use totalGridWidth for Y too for square aspect
        RectF gridRect = new RectF(startX, startY, startX + totalGridWidth, startY + totalGridWidth);
        canvas.drawRoundRect(gridRect, cornerRadius, cornerRadius, gridBackgroundPaint);


        // Draw cells (empty background and tiles)
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                float cellX = startX + cellPadding + c * (cellSize + cellPadding);
                float cellY = startY + cellPadding + r * (cellSize + cellPadding);
                RectF cellRect = new RectF(cellX, cellY, cellX + cellSize, cellY + cellSize);

                // Draw empty cell background first
                canvas.drawRoundRect(cellRect, cornerRadius, cornerRadius, cellEmptyPaint);

                int value = grid[r][c];
                if (value != 0) {
                    drawTile(canvas, cellRect, value);
                }
            }
        }

        // Optional: Draw Game Over overlay
        if (isGameOver) {
            drawGameOverOverlay(canvas);
        }
    }

    private void drawTile(Canvas canvas, RectF rect, int value) {
        // Set tile background color
        tilePaint.setColor(tileColorMap.getOrDefault(value, ContextCompat.getColor(getContext(), R.color.tile_other)));
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, tilePaint);

        // Set text color and size
        textPaint.setColor(textColorMap.getOrDefault(value, ContextCompat.getColor(getContext(), R.color.text_light)));
        // Adjust text size based on number of digits
        int numDigits = String.valueOf(value).length();
        if (numDigits <= 2) textPaint.setTextSize(cellSize * 0.45f);
        else if (numDigits == 3) textPaint.setTextSize(cellSize * 0.35f);
        else textPaint.setTextSize(cellSize * 0.3f); // For 1024, 2048 etc.

        // Draw text centered
        float centerX = rect.centerX();
        float centerY = rect.centerY() - ((textPaint.descent() + textPaint.ascent()) / 2); // Center vertically
        canvas.drawText(String.valueOf(value), centerX, centerY, textPaint);
    }

    private void drawGameOverOverlay(Canvas canvas) {
        Paint overlayPaint = new Paint();
        overlayPaint.setColor(Color.argb(180, 100, 100, 100)); // Semi-transparent grey
        canvas.drawRect(0, 0, getWidth(), getHeight(), overlayPaint);

        Paint textPaintGameOver = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaintGameOver.setColor(Color.WHITE);
        textPaintGameOver.setTextSize(80f);
        textPaintGameOver.setTextAlign(Paint.Align.CENTER);
        textPaintGameOver.setFakeBoldText(true);

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;

        canvas.drawText("Game Over!", centerX, centerY - 40, textPaintGameOver);

        // Optional: Show final score on overlay
        Paint scorePaint = new Paint(textPaintGameOver);
        scorePaint.setTextSize(40f);
        scorePaint.setFakeBoldText(false);
        canvas.drawText("Score: " + score, centerX, centerY + 40, scorePaint);
    }


    // --- Touch Handling ---
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isGameOver || isMoving) {
            return true; // Ignore input if game over or already processing a move
        }
        // Let the GestureDetector handle the event
        boolean result = gestureDetector.onTouchEvent(event);
        // If it wasn't a gesture we care about, let the system handle it
        return result || super.onTouchEvent(event);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onDown(MotionEvent e) {
            // Necessary for GestureDetector to work
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (isGameOver || isMoving) return false; // Already handled in onTouchEvent, but belt and suspenders

            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();

            boolean moved = false;
            isMoving = true; // Start processing move

            if (Math.abs(diffX) > Math.abs(diffY)) {
                // Horizontal swipe
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        Log.d("GameView", "Swipe Right");
                        moved = moveRight();
                    } else {
                        Log.d("GameView", "Swipe Left");
                        moved = moveLeft();
                    }
                }
            } else {
                // Vertical swipe
                if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        Log.d("GameView", "Swipe Down");
                        moved = moveDown();
                    } else {
                        Log.d("GameView", "Swipe Up");
                        moved = moveUp();
                    }
                }
            }

            if (moved) {
                addNewTile();
                if (gameListener != null) {
                    gameListener.onScoreChanged(score);
                    // Optional: Check for win condition (e.g., reaching 2048)
                    if (checkWinCondition()) {
                        gameListener.onGameWon();
                        // You might want to set a 'won' flag and stop further moves or allow playing on
                    }
                }
                if (!canMove()) { // Check game over AFTER adding new tile
                    isGameOver = true;
                    if (gameListener != null) {
                        gameListener.onGameOver();
                    }
                    Log.d("GameView", "Game Over!");
                }
                invalidate(); // Redraw the board
            }
            isMoving = false; // Finished processing move
            return true; // Indicate gesture was handled
        }
    }

    // --- Game Logic: Movement and Merging ---

    // Helper: Creates a copy of the grid
    private int[][] copyGrid() {
        int[][] newGrid = new int[GRID_SIZE][GRID_SIZE];
        for (int r = 0; r < GRID_SIZE; r++) {
            System.arraycopy(grid[r], 0, newGrid[r], 0, GRID_SIZE);
        }
        return newGrid;
    }

    // Helper: Checks if two grids are identical
    private boolean gridsEqual(int[][] grid1, int[][] grid2) {
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                if (grid1[r][c] != grid2[r][c]) {
                    return false;
                }
            }
        }
        return true;
    }

    // --- Core Move Logic ---
    // Returns true if any tile moved or merged
    private boolean moveLeft() {
        int[][] originalGrid = copyGrid();
        for (int r = 0; r < GRID_SIZE; r++) {
            grid[r] = processLine(grid[r]);
        }
        return !gridsEqual(originalGrid, grid);
    }

    private boolean moveRight() {
        int[][] originalGrid = copyGrid();
        rotateGridClockwise();
        rotateGridClockwise(); // Rotate 180 degrees
        moveLeft();            // Move "left" (which is now right)
        rotateGridClockwise();
        rotateGridClockwise(); // Rotate back
        return !gridsEqual(originalGrid, grid);
    }

    private boolean moveUp() {
        int[][] originalGrid = copyGrid();
        rotateGridClockwise();
        rotateGridClockwise();
        rotateGridClockwise(); // Rotate 270 degrees (or -90)
        moveLeft();            // Move "left" (which is now up)
        rotateGridClockwise(); // Rotate back
        return !gridsEqual(originalGrid, grid);
    }

    private boolean moveDown() {
        int[][] originalGrid = copyGrid();
        rotateGridClockwise(); // Rotate 90 degrees
        moveLeft();            // Move "left" (which is now down)
        rotateGridClockwise();
        rotateGridClockwise();
        rotateGridClockwise(); // Rotate back (270 degrees)
        return !gridsEqual(originalGrid, grid);
    }

    // Helper: Process a single line (row/column) for moving left
    private int[] processLine(int[] line) {
        // 1. Slide non-zero tiles to the left
        int[] slidLine = slide(line);
        // 2. Merge adjacent identical tiles
        int[] mergedLine = merge(slidLine);
        // 3. Slide again after merging
        return slide(mergedLine);
    }

    // Helper: Slides non-zero numbers to the start of the array
    private int[] slide(int[] line) {
        int[] newLine = new int[GRID_SIZE];
        int insertPos = 0;
        for (int val : line) {
            if (val != 0) {
                newLine[insertPos++] = val;
            }
        }
        return newLine;
    }

    // Helper: Merges adjacent identical numbers, adds to score
    private int[] merge(int[] line) {
        for (int i = 0; i < GRID_SIZE - 1; i++) {
            if (line[i] != 0 && line[i] == line[i + 1]) {
                line[i] *= 2;       // Merge
                score += line[i];  // Add merged value to score
                line[i + 1] = 0;   // Clear the merged tile
                // Optional: Check for win on merge
                if (line[i] == 2048 && gameListener != null) {
                    // Handle win condition immediately or flag it
                    // gameListener.onGameWon();
                }
                // Don't merge the newly created tile again in this pass (e.g., 2 2 4 -> 4 0 4, not 8 0 0)
                // i++; // Skip the next tile if merge occurred (or just let slide handle it)
            }
        }
        return line;
    }

    // Helper: Rotates the grid 90 degrees clockwise
    private void rotateGridClockwise() {
        int[][] newGrid = new int[GRID_SIZE][GRID_SIZE];
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                newGrid[c][GRID_SIZE - 1 - r] = grid[r][c];
            }
        }
        grid = newGrid;
    }

    // --- Game Over / Win Condition Checks ---

    private boolean canMove() {
        // Check for empty cells
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                if (grid[r][c] == 0) {
                    return true; // Found an empty cell
                }
            }
        }

        // Check for possible merges horizontally
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE - 1; c++) {
                if (grid[r][c] == grid[r][c + 1]) {
                    return true; // Found adjacent identical tiles horizontally
                }
            }
        }

        // Check for possible merges vertically
        for (int c = 0; c < GRID_SIZE; c++) {
            for (int r = 0; r < GRID_SIZE - 1; r++) {
                if (grid[r][c] == grid[r + 1][c]) {
                    return true; // Found adjacent identical tiles vertically
                }
            }
        }

        return false; // No empty cells and no possible merges
    }

    private boolean checkWinCondition() {
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                if (grid[r][c] == 2048) { // Or whatever your target is
                    return true;
                }
            }
        }
        return false;
    }

}