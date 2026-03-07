package com.example.funnybirds;

import static android.graphics.Color.rgb;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import java.util.ArrayList;
import java.util.List;

public class Sprite {
    private Bitmap bitmap;

    private List<Rect> frames;
    private int frameWidth;
    private int frameHeight;
    private int currentFrame;
    private double frameTime;
    private double timeForCurrentFrame;

    private double x;
    private double y;

    private double velocityX;
    private double velocityY;

    private int padding;

    /**
     * Создает новый спрайт с заданными параметрами
     *
     * @param x начальная позиция по X
     * @param y начальная позиция по Y
     * @param velocityX скорость по X (пикселей/секунду)
     * @param velocityY скорость по Y (пикселей/секунду)
     * @param initialFrame начальный кадр анимации
     * @param bitmap изображение спрайта
     */
    public Sprite(double x, double y,
                  double velocityX, double velocityY,
                  Rect initialFrame, Bitmap bitmap) {
        this.x = x;
        this.y = y;
        this.velocityX = velocityX;
        this.velocityY = velocityY;

        this.bitmap = bitmap;

        this.frames = new ArrayList<Rect>();
        this.frames.add(initialFrame);

        this.timeForCurrentFrame = 0.0;
        this.frameTime = 30;
        this.currentFrame = 0;

        this.frameWidth = initialFrame.width();
        this.frameHeight = initialFrame.height();

        this.padding = 50;
    }

    public void setX(double x) { this.x = x; }
    public double getX() { return x; }
    public void setY(double y) { this.y = y; }
    public double getY() { return y; }
    public int getFrameWidth() {
        return frameWidth;
    }
    public int getFrameHeight() {
        return frameHeight;
    }
    public void setVx(double velocityX) {
        this.velocityX = velocityX;
    }
    public double getVx() {
        return velocityX;
    }
    public void setVy(double velocityY) { this.velocityY = velocityY; }
    public double getVy() {
        return velocityY;
    }
    public void setCurrentFrame(int currentFrame) { this.currentFrame = currentFrame % frames.size(); }
    public int getCurrentFrame() {
        return currentFrame;
    }
    public void setFrameTime(double frameTime) { this.frameTime = Math.abs(frameTime); }
    public double getFrameTime() {
        return frameTime;
    }
    public void setTimeForCurrentFrame(double timeForCurrentFrame) {
        this.timeForCurrentFrame = Math.abs(timeForCurrentFrame);
    }
    public double getTimeForCurrentFrame() {
        return timeForCurrentFrame;
    }

    public void addFrame (Rect frame) {
        frames.add(frame);
    }

    public int getFramesCount () {
        return frames.size();
    }
    public void setPadding(int padding) { this.padding = padding; }


    public void update (int ms) {
        timeForCurrentFrame += ms;

        if (timeForCurrentFrame >= frameTime) {
            currentFrame = (currentFrame + 1) % frames.size();
            timeForCurrentFrame = timeForCurrentFrame - frameTime;
        }

        x += velocityX * ms/1000.0;
        y += velocityY * ms/1000.0;
    }

    public void drawSprite(Canvas canvas) {
        Paint p = new Paint();
        Rect destination = new Rect((int)x, (int)y, (int)(x + frameWidth), (int)(y + frameHeight));
        canvas.drawBitmap(bitmap, frames.get(currentFrame), destination,  p);
//        drawHitboxGizmos(canvas, p);
    }

    public Rect getHitbox() {
        return new Rect((int)x + 2 * padding, (int)y + 2 * padding,
                (int)(x + frameWidth - padding), (int)(y + frameHeight - padding));
    }

    public void drawHitboxGizmos(Canvas canvas, Paint p) {
        p.setAntiAlias(true);
        p.setStyle(Paint.Style.FILL_AND_STROKE);
        p.setColor(rgb(220, 40, 40));
        Rect r = getHitbox();
        canvas.drawRect(r, p);
    }

    public boolean intersect (Sprite s) {
        return getHitbox().intersect(s.getHitbox());
    }

    public boolean contains(int x, int y) {
        return x >= this.x && x <= this.x + frameWidth &&
                y >= this.y && y <= this.y + frameHeight;
    }
}
