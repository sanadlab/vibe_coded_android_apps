package com.sanad.gemini_2_dot_5_pro_preview.flappybird;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private static final String TAG = "GameView";

    // Game Thread
    private Thread gameThread = null;
    private volatile boolean isPlaying; // volatile for thread safety
    private boolean isGameOver = false;

    // Drawing
    private SurfaceHolder surfaceHolder;
    private Canvas canvas;
    private Paint paint;
    private Paint scorePaint; // Separate paint for score text

    // Screen dimensions
    private int screenWidth;
    private int screenHeight;

    // Game Objects
    private Bitmap backgroundBitmap;
    private Bitmap birdBitmap;
    private Bitmap topPipeBitmap;
    private Bitmap bottomPipeBitmap;

    // Bird properties
    private int birdX;
    private int birdY;
    private int birdVelocityY;
    private final int GRAVITY = 1; // Adjust gravity strength
    private final int FLAP_STRENGTH = -20; // Negative for upward velocity, adjust flap power

    // Pipe properties
    private List<PipePair> pipes;
    private final int PIPE_SPEED = 7; // Adjust pipe scrolling speed
    private final int PIPE_GAP = 500; // Adjust gap between top/bottom pipes
    private final int PIPE_WIDTH = 150; // Adjust pipe width visually
    private final int PIPE_SPAWN_DISTANCE = 450; // Distance between pipe pairs
    private int lastPipeX; // Track the X position of the last spawned pipe

    // Scoring
    private int score = 0;
    private Random random;

    // Need Handler to post Runnable updates back to UI thread if needed, though not strictly required for drawing here
    // private Handler handler;

    // Timing for consistent updates (Optional but good practice)
    private long lastFrameTime;
    private final long TARGET_FRAME_TIME = 16; // Approx 60 FPS (1000ms / 60fps)

    // Scaling factors (in case assets are not designed for screen density)
    private float scaleFactorX = 1.0f;
    private float scaleFactorY = 1.0f;
    private int scaledBirdWidth;
    private int scaledBirdHeight;
    private int scaledPipeWidth;
    // Pipe height will be determined dynamically

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
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        setFocusable(true); // To receive touch events

        // Get screen dimensions
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size); // Use getSize for modern approach
        screenWidth = size.x;
        screenHeight = size.y;

        paint = new Paint();
        scorePaint = new Paint();
        scorePaint.setColor(Color.WHITE);
        scorePaint.setTextSize(80); // Adjust score text size
        scorePaint.setTextAlign(Paint.Align.LEFT);
        scorePaint.setFakeBoldText(true); // Make score stand out
        // Add shadow for better visibility
        scorePaint.setShadowLayer(5.0f, 3.0f, 3.0f, Color.BLACK);


        // Load and scale Bitmaps (handle potential nulls)
        try {
            backgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.background);
            if (backgroundBitmap != null) {
                backgroundBitmap = Bitmap.createScaledBitmap(backgroundBitmap, screenWidth, screenHeight, false);
            } else {
                Log.e(TAG, "Failed to load background bitmap");
                // Maybe create a default colored background if loading fails
                backgroundBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
                backgroundBitmap.eraseColor(Color.CYAN); // Simple blue sky fallback
            }

            Bitmap originalBird = BitmapFactory.decodeResource(getResources(), R.drawable.bird);
            if (originalBird != null) {
                // Calculate desired bird size (e.g., a fraction of screen height)
                scaledBirdHeight = screenHeight / 15; // Example size
                // Calculate width based on aspect ratio
                scaledBirdWidth = (int) (((float)scaledBirdHeight / originalBird.getHeight()) * originalBird.getWidth());
                birdBitmap = Bitmap.createScaledBitmap(originalBird, scaledBirdWidth, scaledBirdHeight, false);
            } else {
                Log.e(TAG, "Failed to load bird bitmap");
                // Handle error - maybe draw a simple shape?
            }


            Bitmap originalTopPipe = BitmapFactory.decodeResource(getResources(), R.drawable.pipe_top);
            Bitmap originalBottomPipe = BitmapFactory.decodeResource(getResources(), R.drawable.pipe_bottom);
            if (originalTopPipe != null && originalBottomPipe != null) {
                scaledPipeWidth = screenWidth / 8; // Example width
                // Heights will vary, so we keep originals for scaling later or scale to a large max height
                // For simplicity, let's keep original pipes here and scale in draw/update logic if needed,
                // or create scaled versions on the fly in PipePair.
                // Let's just store the originals for now.
                topPipeBitmap = originalTopPipe;
                bottomPipeBitmap = originalBottomPipe;

                // Alternative: Scale to a maximum possible height and clip later
                // int maxPipeHeight = screenHeight;
                // topPipeBitmap = Bitmap.createScaledBitmap(originalTopPipe, scaledPipeWidth, maxPipeHeight, false);
                // bottomPipeBitmap = Bitmap.createScaledBitmap(originalBottomPipe, scaledPipeWidth, maxPipeHeight, false);

            } else {
                Log.e(TAG, "Failed to load pipe bitmaps");
                // Handle error
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading bitmaps: " + e.getMessage());
            // Fallback drawing can be implemented here
        }


        random = new Random();
        pipes = new ArrayList<>();

        // handler = new Handler(Looper.getMainLooper()); // If needed for UI thread tasks

        resetGame();
    }

    private void resetGame() {
        birdX = screenWidth / 4; // Starting position
        birdY = screenHeight / 2;
        birdVelocityY = 0;
        score = 0;
        pipes.clear();
        isGameOver = false;
        lastPipeX = screenWidth; // Start spawning pipes immediately
        spawnInitialPipes();
        lastFrameTime = System.currentTimeMillis(); // Reset frame timer
    }

    private void spawnInitialPipes() {
        // Spawn a couple of pipes off-screen initially to start the flow
        int currentX = screenWidth + PIPE_SPAWN_DISTANCE; // Start well off-screen
        for (int i = 0; i < 2; i++) {
            addPipePair(currentX);
            currentX += PIPE_SPAWN_DISTANCE;
        }
        lastPipeX = currentX - PIPE_SPAWN_DISTANCE; // Correct lastPipeX
    }


    // --- Game Loop ---
    @Override
    public void run() {
        while (isPlaying) {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - lastFrameTime;

            // Optional: Frame rate limiting / delta time (can be simpler without initially)
            if (elapsedTime < TARGET_FRAME_TIME) {
                try {
                    Thread.sleep(TARGET_FRAME_TIME - elapsedTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Restore interrupt status
                    Log.e(TAG, "Game thread interrupted", e);
                }
                // Recalculate elapsed time after sleep for more accuracy if needed
                // currentTime = System.currentTimeMillis();
                // elapsedTime = currentTime - lastFrameTime;
            }
            lastFrameTime = currentTime; // Use current time for next frame's calculation


            if (!isGameOver) {
                update();
            }
            draw();
        }
    }

    // --- Update Game State ---
    private void update() {
        // 1. Apply Gravity to Bird
        birdVelocityY += GRAVITY;
        birdY += birdVelocityY;

        // 2. Bird Bounds Check (Ground and Ceiling)
        if (birdY < 0) { // Hit ceiling
            birdY = 0;
            birdVelocityY = 0; // Stop upward movement
            // Optionally trigger game over on ceiling hit too
            // gameOver();
        }
        if (birdY + scaledBirdHeight > screenHeight) { // Hit ground
            birdY = screenHeight - scaledBirdHeight;
            birdVelocityY = 0;
            gameOver();
            return; // Stop further updates if game over
        }

        // 3. Move Pipes
        List<PipePair> pipesToRemove = new ArrayList<>();
        boolean passedPipe = false;
        for (PipePair pipe : pipes) {
            pipe.x -= PIPE_SPEED;

            // 4. Collision Detection
            if (pipe.intersects(birdX, birdY, scaledBirdWidth, scaledBirdHeight)) {
                gameOver();
                return; // Stop updates
            }

            // 5. Scoring
            if (!pipe.isScored && birdX > pipe.x + scaledPipeWidth) {
                score++;
                pipe.isScored = true;
                passedPipe = true; // Flag that we passed a pipe this frame (optional)
                Log.d(TAG,"Score: " + score);
            }

            // 6. Remove Off-screen Pipes
            if (pipe.x + scaledPipeWidth < 0) {
                pipesToRemove.add(pipe);
            }
        }
        pipes.removeAll(pipesToRemove);

        // 7. Spawn New Pipes
        // Check if the last spawned pipe has moved enough to spawn a new one
        if (!pipes.isEmpty()) {
            lastPipeX = pipes.get(pipes.size() - 1).x; // Get the x of the rightmost pipe
        } else {
            lastPipeX = 0; // Reset if no pipes exist, ensure spawning starts
        }

        if (screenWidth - lastPipeX >= PIPE_SPAWN_DISTANCE) {
            addPipePair(screenWidth); // Spawn just off the right edge
        }

        // Optional: Add sound effects here if passedPipe is true
    }

    private void addPipePair(int startX) {
        // Calculate height for the top pipe's bottom edge
        int minTopPipeHeight = screenHeight / 8; // Ensure minimum space
        int maxTopPipeHeight = screenHeight - PIPE_GAP - minTopPipeHeight; // Max height for top pipe bottom edge
        int topPipeBottomY = random.nextInt(maxTopPipeHeight - minTopPipeHeight + 1) + minTopPipeHeight;

        pipes.add(new PipePair(startX, topPipeBottomY));
        Log.d(TAG, "Added pipe pair at x=" + startX + ", topPipeBottomY=" + topPipeBottomY);
    }

    private void gameOver() {
        if (!isGameOver) {
            Log.d(TAG, "Game Over!");
            isGameOver = true;
            // Optionally stop the bird's horizontal movement if it had any
            // Play sound effect, show Game Over screen etc.
        }
    }


    // --- Draw Frame ---
    private void draw() {
        if (surfaceHolder.getSurface().isValid()) {
            try {
                canvas = surfaceHolder.lockCanvas();
                if (canvas != null) {
                    synchronized (surfaceHolder) {
                        // 1. Draw Background
                        if (backgroundBitmap != null) {
                            canvas.drawBitmap(backgroundBitmap, 0, 0, paint);
                        } else {
                            canvas.drawColor(Color.CYAN); // Fallback color
                        }


                        // 2. Draw Pipes
                        for (PipePair pipe : pipes) {
                            pipe.draw(canvas);
                        }

                        // 3. Draw Bird
                        if (birdBitmap != null) {
                            canvas.drawBitmap(birdBitmap, birdX, birdY, paint);
                        } else {
                            // Fallback: draw a rectangle if bird bitmap failed
                            paint.setColor(Color.YELLOW);
                            canvas.drawRect(birdX, birdY, birdX + scaledBirdWidth, birdY + scaledBirdHeight, paint);
                        }

                        // 4. Draw Score
                        canvas.drawText("Score: " + score, 50, 100, scorePaint);

                        // 5. Draw Game Over Text (if applicable)
                        if (isGameOver) {
                            drawGameOverScreen(canvas);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during drawing: " + e.getMessage());
            }
            finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    } catch (Exception e) {
                        Log.e(TAG, "Error unlocking canvas: " + e.getMessage());
                    }
                }
            }
        }
    }

    private void drawGameOverScreen(Canvas canvas) {
        Paint gameOverPaint = new Paint();
        gameOverPaint.setColor(Color.RED);
        gameOverPaint.setTextSize(120);
        gameOverPaint.setTextAlign(Paint.Align.CENTER);
        gameOverPaint.setFakeBoldText(true);
        gameOverPaint.setShadowLayer(10.0f, 5.0f, 5.0f, Color.BLACK);

        Paint instructionPaint = new Paint(gameOverPaint); // Copy properties
        instructionPaint.setTextSize(60);
        instructionPaint.setFakeBoldText(false);
        instructionPaint.setColor(Color.WHITE);


        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        canvas.drawText("Game Over!", centerX, centerY - 80, gameOverPaint);
        canvas.drawText("Score: " + score, centerX, centerY, instructionPaint);
        canvas.drawText("Tap to Restart", centerX, centerY + 80, instructionPaint);
    }


    // --- Input Handling ---
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (isGameOver) {
                // Restart the game on tap if game over
                resetGame();
            } else {
                // Flap the bird
                birdVelocityY = FLAP_STRENGTH;
                // Play flap sound effect here
            }
            return true; // Indicate event was handled
        }
        return super.onTouchEvent(event);
    }


    // --- SurfaceHolder.Callback Methods ---
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "Surface Created");
        // Surface is ready, safe to start the game loop
        if (gameThread == null || !gameThread.isAlive()) {
            isPlaying = true;
            gameThread = new Thread(this);
            gameThread.start();
            lastFrameTime = System.currentTimeMillis(); // Initialize frame timer when thread starts
        } else {
            // If thread exists but was paused, just set isPlaying
            isPlaying = true;
            lastFrameTime = System.currentTimeMillis(); // Re-initialize timer on resume
        }

        // Ensure game state is appropriate (might need reset if resuming from pause)
        if (isGameOver) {
            // Keep game over state if surface recreated after game over
        } else if (pipes.isEmpty()) { // If surface created first time or after full stop
            resetGame();
        }


    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "Surface Changed: " + width + "x" + height);
        // Handle screen size changes if necessary (e.g., orientation change, though we lock it)
        // We get dimensions in init, but could update here if needed.
        screenWidth = width;
        screenHeight = height;
        // Re-scale assets if necessary based on new dimensions
        // Re-calculate bird/pipe sizes, etc.
        // backgroundBitmap = Bitmap.createScaledBitmap(backgroundBitmap, screenWidth, screenHeight, false); // Example
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "Surface Destroyed");
        // Surface is gone, must stop the game loop thread
        pause();
    }

    // --- Control Methods ---
    public void pause() {
        Log.d(TAG, "Pausing game thread");
        isPlaying = false;
        try {
            if (gameThread != null) {
                gameThread.join(); // Wait for thread to finish
                gameThread = null; // Clean up thread reference
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Error joining game thread", e);
            Thread.currentThread().interrupt();
        }
    }

    public void resume() {
        Log.d(TAG, "Resuming game thread");
        // Game thread will be restarted in surfaceCreated
        // If the surface wasn't destroyed, we might need to manually restart here,
        // but standard lifecycle handles this via surfaceCreated.
        // isPlaying = true; // Let surfaceCreated handle thread start
    }

    // --- Helper PipePair Class ---
    private class PipePair {
        int x;
        int topPipeBottomY; // Y-coordinate of the bottom edge of the top pipe
        boolean isScored = false;

        Rect topPipeRect = new Rect();
        Rect bottomPipeRect = new Rect();

        PipePair(int x, int topPipeBottomY) {
            this.x = x;
            this.topPipeBottomY = topPipeBottomY;
        }

        void draw(Canvas canvas) {
            if (topPipeBitmap != null && bottomPipeBitmap != null) {
                // Draw top pipe (anchored to top)
                int topPipeHeight = topPipeBottomY; // Height is simply its bottom Y
                // Source rect: Use whole bitmap height
                Rect srcTop = new Rect(0, 0, topPipeBitmap.getWidth(), topPipeBitmap.getHeight());
                // Destination rect: Scale height correctly
                topPipeRect.set(x, 0, x + scaledPipeWidth, topPipeHeight); // Draw from top edge down
                canvas.drawBitmap(topPipeBitmap, srcTop, topPipeRect, paint);


                // Draw bottom pipe (anchored to bottom)
                int bottomPipeTopY = topPipeBottomY + PIPE_GAP;
                int bottomPipeHeight = screenHeight - bottomPipeTopY;
                // Source rect: Use whole bitmap height
                Rect srcBottom = new Rect(0, 0, bottomPipeBitmap.getWidth(), bottomPipeBitmap.getHeight());
                // Destination rect: Scale height correctly
                bottomPipeRect.set(x, bottomPipeTopY, x + scaledPipeWidth, screenHeight); // Draw from gap bottom down to screen bottom
                canvas.drawBitmap(bottomPipeBitmap, srcBottom, bottomPipeRect, paint);

            } else {
                // Fallback drawing if bitmaps failed
                paint.setColor(Color.GREEN);
                // Top Pipe
                topPipeRect.set(x, 0, x + scaledPipeWidth, topPipeBottomY);
                canvas.drawRect(topPipeRect, paint);
                // Bottom Pipe
                int bottomPipeTopY = topPipeBottomY + PIPE_GAP;
                bottomPipeRect.set(x, bottomPipeTopY, x + scaledPipeWidth, screenHeight);
                canvas.drawRect(bottomPipeRect, paint);
            }
        }

        boolean intersects(int birdX, int birdY, int birdWidth, int birdHeight) {
            Rect birdRect = new Rect(birdX, birdY, birdX + birdWidth, birdY + birdHeight);
            // Update pipe rects just before check (needed if not updated in draw every frame)
            topPipeRect.set(x, 0, x + scaledPipeWidth, topPipeBottomY);
            int bottomPipeTopY = topPipeBottomY + PIPE_GAP;
            bottomPipeRect.set(x, bottomPipeTopY, x + scaledPipeWidth, screenHeight);

            return Rect.intersects(birdRect, topPipeRect) || Rect.intersects(birdRect, bottomPipeRect);
        }
    }
}