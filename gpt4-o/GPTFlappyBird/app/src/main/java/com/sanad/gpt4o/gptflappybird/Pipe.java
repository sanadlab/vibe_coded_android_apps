package com.sanad.gpt4o.gptflappybird;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import java.util.Random;

public class Pipe {
    private int x;
    private final int width;
    private final int screenHeight;
    private final int gap = 300;
    private final int speed = 10;
    private final Rect topRect;
    private final Rect bottomRect;
    private final Paint paint;
    private final int topHeight, bottomHeight;

    public Pipe(int startX, int screenHeight, int width, int height) {
        this.screenHeight = screenHeight;
        this.x = startX;
        this.width = width;
        Random random = new Random();
        topHeight = random.nextInt(screenHeight - gap);
        bottomHeight = screenHeight - topHeight - gap;
        this.topRect = new Rect(x, 0, x + width, topHeight);
        this.bottomRect = new Rect(x, screenHeight - bottomHeight, x + width, screenHeight);
        this.paint = new Paint();
        paint.setColor(Color.GREEN);
    }

    public void update() {
        x -= speed;
        topRect.set(x, 0, x + width, topHeight);
        bottomRect.set(x, screenHeight - bottomHeight, x + width, screenHeight);
    }

    public void draw(Canvas canvas) {
        canvas.drawRect(topRect, paint);
        canvas.drawRect(bottomRect, paint);
    }

    public boolean collidesWith(Bird bird) {
        return Rect.intersects(topRect, bird.getRect()) || Rect.intersects(bottomRect, bird.getRect());
    }

    public int getX() {
        return x;
    }
}