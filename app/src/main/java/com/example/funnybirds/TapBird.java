package com.example.funnybirds;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class TapBird {
    private Sprite sprite;
    private boolean isActive = false;
    private boolean isAlive = true;
    private float targetY;

    // Поля из GameView
    private int viewWidth;
    private int viewHeight;
    private float timerInterval;

    public boolean isActive() { return isActive; }
    public double getX() { return sprite.getX(); }
    public double getY() { return sprite.getY(); }

    public TapBird(Context context, int viewWidth, int viewHeight, float timerInterval) {
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
        this.timerInterval = timerInterval;

        // Инициализация Тап-птицы
        Bitmap b = BitmapFactory.decodeResource(context.getResources(), R.drawable.enemy);
        Bitmap pinkBitmap = tintBitmap(b, Color.argb(255, 255,50,255)); // розовый оттенок
        int w = pinkBitmap.getWidth()/5;
        int h = pinkBitmap.getHeight()/3;
        sprite = new Sprite(-1000, 0, -350, 0, new Rect(0, 0, w, h), pinkBitmap);

            for (int i = 0; i < 3; i++) {
                for (int j = 4; j >= 0; j--) {
                    if (i ==0 && j == 4) continue;
                    if (i ==2 && j == 0) continue;
                    sprite.addFrame(new Rect(j*w, i*h, j*w+w, i*h+h));
                }
            }
    }

    private Bitmap tintBitmap(Bitmap src, int color) {
        Bitmap result = src.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.MULTIPLY));
        canvas.drawBitmap(result, 0, 0, paint);
        return result;
    }

    public void updateViewSize(int width, int height) {
        this.viewWidth = width;
        this.viewHeight = height;
    }

    public void activate(float playerX, float playerY) {
        isActive = true;
        isAlive = true;
        targetY = playerY;
        sprite.setX(viewWidth + 50); // справа
        sprite.setY(playerY);
        sprite.setVx(-350);
    }

    public void update(float playerX, float playerY, float gameSpeed) {
        if (!isActive || !isAlive) return;

        double currentY = sprite.getY();

        float targetVx = -350 * gameSpeed;
        sprite.setVx(targetVx);
        sprite.update((int)timerInterval);
        sprite.setY(playerY);
    }

    public boolean checkClick(float touchX, float touchY) {
        return isActive && isAlive && sprite.contains((int)touchX, (int)touchY);
    }

    public void kill() {
        isAlive = false;
        isActive = false;
        sprite.setX(-1000); // убираем
    }

    public void draw(Canvas canvas) {
        if (isActive && isAlive) {
            sprite.drawSprite(canvas);
        }
    }

    public boolean checkCollision(Sprite player) {
        return isActive && isAlive && sprite.intersect(player);
    }
}
