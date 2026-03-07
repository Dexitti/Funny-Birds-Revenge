package com.example.funnybirds;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GameSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private GameView gameView; // делегируем логику существующему GameView
    private SurfaceHolder holder;
    private RenderThread renderThread;
    private boolean isSurfaceReady = false;

    public GameSurfaceView(Context context, GameView gameView) {
        super(context);
        this.gameView = gameView;
        holder = getHolder();
        holder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        renderThread = new RenderThread();
        renderThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        gameView.onSizeChanged(width, height, 0, 0);
        isSurfaceReady = true;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        renderThread.running = false;
        try {
            renderThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gameView.onTouchEvent(event);
    }

    private class RenderThread extends Thread {
        boolean running = true;
        private final int TARGET_FPS = 30; // 30 FPS
        private final long FRAME_TIME = 1000 / TARGET_FPS;

        @Override
        public void run() {
            while (running) {
                long startTime = System.currentTimeMillis();
                gameView.update();

                Canvas canvas = holder.lockCanvas();
                if (canvas != null) {
                    synchronized (holder) {
                        gameView.draw(canvas); // делегируем отрисовку
                    }
                    holder.unlockCanvasAndPost(canvas);
                }

                // Небольшая задержка для FPS
                long frameTime = System.currentTimeMillis() - startTime;
                long sleepTime = FRAME_TIME - frameTime;

                if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}