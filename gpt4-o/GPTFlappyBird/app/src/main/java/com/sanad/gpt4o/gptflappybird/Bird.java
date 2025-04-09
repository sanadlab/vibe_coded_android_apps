package com.sanad.gpt4o.gptflappybird;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class Bird {
    private final int x;
    private int y;
    private final int size = 100;
    private final int gravity = 3;
    private final int flapPower = -30;
    private int velocity = 0;
    private final Rect rect;
    private final Paint paint;
    private final int screenHeight;

    public Bird(int screenWidth, int screenHeight) {
        this.x = screenWidth / 2;
        this.y = screenHeight / 2;
        this.screenHeight = screenHeight;
        this.rect = new Rect(x, y, x + size, y + size);
        this.paint = new Paint();
        paint.setColor(Color.YELLOW);
    }

    public void update() {
        velocity += gravity;
        y += velocity;
        if (y < 0) y = 0;
        if (y + size > screenHeight) y = screenHeight - size;
        rect.set(x, y, x + size, y + size);
    }

    public void flap() {
        velocity = flapPower;
    }

    public void draw(Canvas canvas) {
        canvas.drawRect(rect, paint);
    }

    public Rect getRect() {
        return rect;
    }
}