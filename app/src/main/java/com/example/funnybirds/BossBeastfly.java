package com.example.funnybirds;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;

public class BossBeastfly {
    private Sprite sprite;
    private boolean active = false;
    private int health = 10;
    private int attackPattern = 0; // 0-верх, 1-середина, 2-низ

    // Состояния босса
    private enum BossState { SPAWNING, WAITING, ATTACKING }
    private BossState state = BossState.SPAWNING;

    private float normalSpeed = -350f;
    private float attackSpeed = -1500f;

    private long waitStartTime = 0;
    private final long WAIT_DURATION = 1000; // 1 секунда ожидания
    private boolean damagedThisPass = false; // флаг для урона за пролет

    // Поля из GameView
    private int viewWidth;
    private int viewHeight;
    private float timerInterval;

    public boolean isActive() { return active; }

    public BossBeastfly(Context context, int viewWidth, int viewHeight, float timerInterval) {
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
        this.timerInterval = timerInterval;

        // Инициализация Зверомухи (босса)
        Bitmap b = BitmapFactory.decodeResource(context.getResources(), R.drawable.savage_beastfly);
        int desiredWidth = 512;
        int desiredHeight = (b.getHeight() * desiredWidth) / b.getWidth();
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(b, desiredWidth, desiredHeight, true);
        sprite =  new Sprite(-1000, 0, normalSpeed, 0, new Rect(0, 0, desiredWidth, desiredHeight), scaledBitmap);
    }

    public void updateViewSize(int width, int height) {
        this.viewWidth = width;
        this.viewHeight = height;
    }

    public void activate() {
        active = true;
        health = 10;
        state = BossState.SPAWNING;
        chooseAttackLine();
        sprite.setVx(normalSpeed);
    }

    private void chooseAttackLine() {
        attackPattern = (int)(Math.random() * 3);
        float y = 0;
        int bossHeight = sprite.getFrameHeight();
        switch(attackPattern) {
            case 0: y = -20; break;
            case 1: y = viewHeight / 2f - bossHeight / 2f - 20; break;
            case 2: y = viewHeight - bossHeight + 50; break;
        }
        sprite.setY(y);
        sprite.setX(viewWidth + 100);
        damagedThisPass = false; // сбрасываем флаг урона
    }

    public void update() {
        if (!active) return;

        switch(state) {
            case SPAWNING:
                sprite.setX(sprite.getX() + sprite.getVx() * timerInterval/1000f);

                if (sprite.getX() < viewWidth - 300) {
                    state = BossState.WAITING;
                    waitStartTime = System.currentTimeMillis();
                    sprite.setVx(0);
                }
                break;

            case WAITING:
                if (System.currentTimeMillis() - waitStartTime > WAIT_DURATION) {
                    state = BossState.ATTACKING;
                    sprite.setVx(attackSpeed);
                }
                break;

            case ATTACKING:
                sprite.setX(sprite.getX() + sprite.getVx() * timerInterval/1000f);

                // Если улетел за левый край - ТЕЛЕПОРТ В НАЧАЛО
                if (sprite.getX() < -sprite.getFrameWidth()) {
                    state = BossState.SPAWNING; // начинаем заново
                    sprite.setVx(normalSpeed);
                    chooseAttackLine(); // новая линия
                }
                break;
        }

        sprite.update((int)timerInterval);
    }

    public boolean checkHit(Sprite player) {
        if (!active) return false;

        if (!damagedThisPass && sprite.intersect(player)) {
            damagedThisPass = true;
            return true;
        }
        return false;
    }

    public void draw(Canvas canvas) {
        if (active) {
            sprite.drawSprite(canvas);
        }
    }

    public void deactivate() {
        active = false;
        sprite.setX(-1000);
        damagedThisPass = false;
        health = 10;
    }
}