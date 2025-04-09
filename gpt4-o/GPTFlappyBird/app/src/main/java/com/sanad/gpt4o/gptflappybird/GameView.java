package com.sanad.gpt4o.gptflappybird;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private final Handler handler;
    private final Runnable runnable;
    private int screenWidth, screenHeight;
    private boolean playing = false;
    private Bird bird;
    private ArrayList<Pipe> pipes;
    private final int PIPE_INTERVAL = 2000;
    private final int PIPE_WIDTH = 200;
    private final int PIPE_HEIGHT = 1000;
    private long lastPipeTime;

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                if (playing) {
                    update();
                    drawCanvas(getHolder().lockCanvas());
                    handler.postDelayed(this, 17);
                }
            }
        };
    }

    private void drawCanvas(Canvas canvas) {
        if (canvas != null) {
            canvas.drawColor(Color.CYAN);
            bird.draw(canvas);
            for (Pipe pipe : pipes) {
                pipe.draw(canvas);
            }
            getHolder().unlockCanvasAndPost(canvas);
        }
    }

    private void update() {
        bird.update();
        if (System.currentTimeMillis() - lastPipeTime > PIPE_INTERVAL) {
            lastPipeTime = System.currentTimeMillis();
            pipes.add(new Pipe(screenWidth, screenHeight, PIPE_WIDTH, PIPE_HEIGHT));
        }
        for (Pipe pipe : pipes) {
            pipe.update();
            if (pipe.collidesWith(bird)) {
                playing = false;
            }
        }
        pipes.removeIf(pipe -> pipe.getX() + PIPE_WIDTH < 0);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                bird.flap();
                break;
        }
        return true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        screenWidth = getWidth();
        screenHeight = getHeight();

        bird = new Bird(screenWidth, screenHeight);
        pipes = new ArrayList<>();

        playing = true;
        handler.post(runnable);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        handler.removeCallbacks(runnable);
    }
}