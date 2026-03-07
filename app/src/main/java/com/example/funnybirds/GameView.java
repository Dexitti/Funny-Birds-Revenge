package com.example.funnybirds;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.CountDownTimer;
import android.util.SparseBooleanArray;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class GameView extends View {
    private Sprite playerBird;
    private ArrayList<Sprite> enemies = new ArrayList<>();
    private ArrayList<Sprite> bonuses = new ArrayList<>();
    private Sprite bigBonus;
    private TapBird tapBird;
    private BossBeastfly boss;
    private Sprite pauseButton;
    private Sprite againButton;
    private Sprite continueButton;

    private int difficultyLevel = 0;
    private final int[][] LEVEL_CONFIG = {
            {1, 0, 0, 0}, // уровень 0: 1 enemy, 1 bonus, 0 bigBonus
            {3, 0, 0, 0},
            {4, 1, 0, 0},
            {5, 3, 1, 0},
            {6, 3, 1, 1}
    };
    private int enemyCount = 1;
    private int bonusCount = 0;
    private int bigBonusCount = 0;
    private int tapBirdCount = 0;
    private int enemySpawnCounter = 0;
    private int bonusSpawnCounter = 0;
    private int bigBonusSpawnCounter = 0;
    private boolean isBigBonusActive = false;
    private boolean isBossActive = false;
    private int tapBirdSpawnCounter = 0;
    private boolean isTapBirdActive = false;
    private float backgroundTransition = 0f;

    private final int ENEMY_SPAWN_RATE = 45; // кадров между спавном врагов (меньше = чаще)
    private final int BONUS_SPAWN_RATE = 60;
    private final int BIG_BONUS_SPAWN_RATE = 250;
    private final int TAP_BIRD_SPAWN_RATE = 280;

    private int viewWidth;
    private int viewHeight;
    private int points = 0;
    private final int timerInterval = 30;

    // Управление пальцем
    private boolean isTouching = false;
    private float targetX = 0;
    private float targetY = 0;
    private float smoothSpeed = 0.3f;

    // Пауза
    private boolean isPaused = false;
    private long totalPausedTime = 0; // общее время паузы
    private long pauseStartTime = 0; // когда началась пауза
    private long gameStartTime; // время старта игры

    private String hintText = "Избегайте птиц!";
    private boolean showFlyHint = true;
    private boolean showBerryHint = true;
    private boolean showBossHint = true;
    private boolean showTapBirdHint = true;
    private long hintShowTime = 0;

    // Состояние игры
    private float gameSpeed = 1.0f;
    private boolean isGameOver = false;
    private boolean isVictory = false;
    private int victoryScore = 5000;

    // Обработчик касаний
    private SparseBooleanArray activePointers = new SparseBooleanArray();
    private int controlPointerId = -1; // ID пальца для управления

    /** Конструктор.
     * Загружает спрайты и таймер */
    public GameView(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setLongClickable(false);
        gameStartTime = System.currentTimeMillis();

        // Инициализация игрока
        Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.player);
        int w = b.getWidth()/5; // ширина атласа
        int h = b.getHeight()/3; // высота атласа
        Rect firstFrame = new Rect(0, 0, w, h);
        playerBird = new Sprite(10, 0, 0, 0, firstFrame, b);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 4; j++) {
                if (i ==0 && j == 0) continue;
                if (i ==2 && j == 3) continue;
                playerBird.addFrame(new Rect(j*w, i*h, j*w+w, i*h+h));
            }
        }

        // Инициализация крутого бонуса
        b = BitmapFactory.decodeResource(getResources(), R.drawable.strawberry);
        w = b.getWidth()/4;
        h = b.getHeight();
        firstFrame = new Rect(0, 0, w, h);
        bigBonus = new Sprite(-1000, 250, -350, 0, firstFrame, b);

        for (int j = 1; j < 4; j++) {
            bigBonus.addFrame(new Rect(j * w, 0, (j + 1) * w, h));
        }
        bigBonus.setFrameTime(130);
        bigBonus.setPadding(60);
        isBigBonusActive = false;

        // Инициализация босса
        boss = new BossBeastfly(context, viewWidth, viewHeight, timerInterval);

        // Инициализация "умной" птицы
        tapBird = new TapBird(context, viewWidth, viewHeight, timerInterval);

        // Инициализация кнопки паузы
        b = BitmapFactory.decodeResource(getResources(), R.drawable.pause);
        int desiredWidth = 128;
        int desiredHeight = (b.getHeight() * desiredWidth) / b.getWidth();
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(b, desiredWidth, desiredHeight, true);
        pauseButton =  new Sprite(0, 110, 0, 0, new Rect(0, 0, desiredWidth, desiredHeight), scaledBitmap);
        pauseButton.setPadding(100);

        // Инициализация меню
        b = BitmapFactory.decodeResource(getResources(), R.drawable.again);
        desiredWidth = 192;
        desiredHeight = (b.getHeight() * desiredWidth) / b.getWidth();
        scaledBitmap = Bitmap.createScaledBitmap(b, desiredWidth, desiredHeight, true);
        againButton =  new Sprite(0, 110, 0, 0, new Rect(0, 0, desiredWidth, desiredHeight), scaledBitmap);
        againButton.setPadding(100);

        b = BitmapFactory.decodeResource(getResources(), R.drawable.continue_endless);
        desiredWidth = 192;
        desiredHeight = (b.getHeight() * desiredWidth) / b.getWidth();
        scaledBitmap = Bitmap.createScaledBitmap(b, desiredWidth, desiredHeight, true);
        continueButton =  new Sprite(0, 110, 0, 0, new Rect(0, 0, desiredWidth, desiredHeight), scaledBitmap);
        continueButton.setPadding(100);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;

        playerBird.setY(viewHeight / 2f - playerBird.getFrameHeight() / 2f);
        playerBird.setX(15);

        boss.updateViewSize(w, h);
        tapBird.updateViewSize(w, h);

        pauseButton.setX(viewWidth - 150);
        pauseButton.setY(20);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // Принудительно запрашиваем размеры
        requestLayout();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        draw(canvas); // делегируем
    }

    @SuppressLint("MissingSuperCall")
    public void draw(Canvas canvas) {
        // Фон
        drawSmoothColorTransition(canvas);

        // Рисуем спрайты
        playerBird.drawSprite(canvas);
        for (Sprite enemy : enemies) {
            enemy.drawSprite(canvas);
        }
        for (Sprite bonusSprite : bonuses) {
            bonusSprite.drawSprite(canvas);
        }
        bigBonus.drawSprite(canvas);

        boss.draw(canvas);
        tapBird.draw(canvas);

        // Рисуем счет
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextSize(55.0f);
        paint.setColor(Color.WHITE);
        canvas.drawText(points + "", viewWidth / 2f, 110, paint);

        // Рисуем подсказки
        paint.setTextSize(65.0f);
        paint.setTextAlign(Paint.Align.CENTER);
        String[] lines = hintText.split("\n");
        float y = 200;
        for (String line : lines) {
            canvas.drawText(line, viewWidth / 2f, y, paint);
            y += 70; // отступ между строками
        }

        if (!isPaused) pauseButton.drawSprite(canvas);
        if (isPaused) {
            // Полупрозрачный затемняющий слой
            paint.setColor(Color.argb(150, 0, 0, 0));
            canvas.drawRect(0, 0, viewWidth, viewHeight, paint);

            pauseButton.drawSprite(canvas);

            // Надпись ПАУЗА
            paint.setAntiAlias(true);
            paint.setTextSize(120);
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("ПАУЗА", viewWidth / 2, viewHeight / 2, paint);
        }

        if (isGameOver) {
            // Полупрозрачный затемняющий слой
            paint.setColor(Color.argb(150, 0, 0, 0));
            canvas.drawRect(0, 0, viewWidth, viewHeight, paint);

            // Надпись ПОРАЖЕНИЕ
            paint.setAntiAlias(true);
            paint.setTextSize(120);
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("ПОРАЖЕНИЕ", viewWidth / 2f, viewHeight / 2f - 50, paint);

            againButton.setX(viewWidth / 2f - againButton.getFrameWidth() / 2f);
            againButton.setY(viewHeight / 2f + 50);
            againButton.drawSprite(canvas);
        }

        if (isVictory) {
            // Надпись ПОБЕДА
            paint.setAntiAlias(true);
            paint.setTextSize(120);
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("ПОБЕДА", viewWidth / 2f, viewHeight / 2f - 50, paint);

            paint.setTextSize(80);
            paint.setColor(Color.WHITE);
            canvas.drawText("Счет: " + points, viewWidth / 2f, viewHeight / 2f - 250, paint);

            againButton.setX(viewWidth / 2f  - againButton.getFrameWidth() / 2f - 300);
            againButton.setY(viewHeight / 2f + 30);
            againButton.drawSprite(canvas);
            continueButton.setX(viewWidth / 2f - continueButton.getFrameWidth() / 2f  + 300);
            continueButton.setY(viewHeight / 2f + 30);
            continueButton.drawSprite(canvas);
        }
    }

    private void drawSmoothColorTransition(Canvas canvas) {
        int color1, color2;
        float t;

        if (backgroundTransition <= 1f) {
            color1 = Color.argb(250, 127, 199, 255);
            color2 = Color.argb(250, 30, 80, 50);
            t = backgroundTransition;
        } else {
            color1 = Color.argb(250, 30, 80, 50);
            color2 = Color.argb(250, 140, 140, 210);
            t = backgroundTransition - 1f;
        }

        int r = (int)(Color.red(color1) * (1 - t) + Color.red(color2) * t);
        int g = (int)(Color.green(color1) * (1 - t) + Color.green(color2) * t);
        int b = (int)(Color.blue(color1) * (1 - t) + Color.blue(color2) * t);
        int a = 250;
        canvas.drawARGB(a, r, g, b);
    }

    public void update () {
        if (viewWidth == 0 || viewHeight == 0) {
            return;
        }

        if (isPaused || isGameOver || isVictory) {
            return;
        }

        // Проверка победы/поражения
        if (points <= -500) { isGameOver = true; invalidate(); return; }
        if (points >= victoryScore) { isVictory = true; invalidate(); return; }

        updateHints();

        playerBird.update(timerInterval);

        // Ускорение игры со временем
        gameSpeed = 1.0f + totalActiveTime() / 60000f * 0.2f;
        float baseSpeed = -400 * gameSpeed;

        updateDifficulty();
        updateSpawning();

        // Обновление всех врагов
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Sprite enemy = enemies.get(i);
            enemy.setVx(baseSpeed);
            enemy.update(timerInterval);

            if (enemy.getX() < -enemy.getFrameWidth()) {
                enemies.remove(i);
                if (difficultyLevel == 0 && points == -200)
                    points += 210;
                else points += 10;
//                android.util.Log.d("GAME_DEBUG", "excessive points!");
            } else if (enemy.intersect(playerBird)) {
                enemies.remove(i);
                points -= 200;
            }
        }

        // Обновление всех бонусов (fly)
        for (int i = bonuses.size() - 1; i >= 0; i--) {
            Sprite bonus = bonuses.get(i);
            bonus.setVx(baseSpeed);
            bonus.update(timerInterval);

            if (bonus.getX() < -bonus.getFrameWidth()) {
                bonuses.remove(i);
            } else if (bonus.intersect(playerBird)) {
                bonuses.remove(i);
                points += 100;
            }
        }

        if (!isBigBonusActive) {
            bigBonusSpawnCounter++;
            if (bigBonusSpawnCounter >= BIG_BONUS_SPAWN_RATE && bigBonusCount == 1) {
                bigBonus.setX(viewWidth + 300); // спавним
                isBigBonusActive = true;
                bigBonusSpawnCounter = 0;
            }
        }
        else{
            bigBonus.setVx(baseSpeed * 0.875f);
            bigBonus.update(timerInterval);

            // Волнообразное движение
            long activeTime = totalActiveTime();
            double waveOffset = (Math.sin(activeTime / 300.0 / gameSpeed) + 1) / 2; // период
            double minY = 0;
            double maxY = viewHeight - bigBonus.getFrameHeight();
            double newstrawberryY = minY + waveOffset * (maxY - minY);
            bigBonus.setY(newstrawberryY);

            // Проверка - улетела или собрали
            if (bigBonus.getX() < -bigBonus.getFrameWidth()) {
                isBigBonusActive = false;
            }
            else if (bigBonus.intersect(playerBird)) {
                points += 300;
                bigBonus.setX(-1000);
                isBigBonusActive = false;
            }
        }

        // Обработка движения игрока
        if (isTouching) {
            double currentX = playerBird.getX();
            double newX = currentX + (targetX - currentX);
            double currentY = playerBird.getY();
            double newY = currentY + (targetY - currentY) * smoothSpeed;

            playerBird.setX(newX);
            playerBird.setY(newY);
        }

        // Проверка границ экрана
        Rect hitbox = playerBird.getHitbox();
        if (hitbox.bottom > viewHeight) {
            playerBird.setY(viewHeight - playerBird.getFrameHeight());
        } else if (hitbox.top < 0) {
            playerBird.setY(0);
        }

        // Условие появления босса
        boolean timeCondition = totalActiveTime() > 7 * 60 * 1000; // 7 минут в мс
        boolean scoreCondition = (points >= 4000 && points < 6000) || points >= 9000;

        if (!boss.isActive() && (timeCondition || scoreCondition)) {
            boss.activate();
            enemies.clear();
            bonuses.clear();
            isBigBonusActive = false;
        }

        if (boss.isActive() && points >= 6000 && points < 9000) {
            boss.deactivate();
        }

        // Логика смены фона
        float targetTransition = 0f;
        if (points >= 8000) {
            targetTransition = 2f;
        } else if (points >= 4000) {
            targetTransition = 1f;
        }
        if (backgroundTransition < targetTransition) {
            backgroundTransition += 0.02f;
            if (backgroundTransition > targetTransition) backgroundTransition = targetTransition;
        } else if (backgroundTransition > targetTransition) {
            backgroundTransition -= 0.04f;
            if (backgroundTransition < targetTransition) backgroundTransition = targetTransition;
        }

        boss.update();

        if (boss.checkHit(playerBird)) {
            points -= 500;
        }

        if (tapBirdCount > 0) {
            tapBirdSpawnCounter++;
        }

        if (tapBirdCount > 0 && !isTapBirdActive && tapBirdSpawnCounter >= TAP_BIRD_SPAWN_RATE) {
            tapBird.activate((float)playerBird.getX(), (float)playerBird.getY());
            isTapBirdActive = true;
            tapBirdSpawnCounter = 0;
        }

        // Обновление тап-птицы
        if (isTapBirdActive) {
            tapBird.update((float)playerBird.getX(), (float)playerBird.getY(), gameSpeed);

            if (tapBird.checkCollision(playerBird)) {
                points -= 300;
                tapBird.kill();
                isTapBirdActive = false;
            }
        }

        invalidate();
    }

    private void updateDifficulty() {
        difficultyLevel = 0;
        if (points >= 10) difficultyLevel = 1;
        if (points >= 50) difficultyLevel = 2;
        if (points >= 300) difficultyLevel = 3;
        if (points >= 5800) difficultyLevel = 4;
//        android.util.Log.d("GAME_DEBUG", "earlier tap-bird");

        enemyCount = LEVEL_CONFIG[difficultyLevel][0];
        bonusCount = LEVEL_CONFIG[difficultyLevel][1];
        bigBonusCount = LEVEL_CONFIG[difficultyLevel][2];
        tapBirdCount = LEVEL_CONFIG[difficultyLevel][3];
    }

    private void updateHints() {
        long currentTime = System.currentTimeMillis();
        if (!hintText.isEmpty() && currentTime - hintShowTime < 3000) {
            return; // продолжаем показывать ту же подсказку
        }

        String newHint = "";
        if (points < 20) newHint = "Избегайте птиц!";
        else if (showFlyHint && points > 40){
            newHint = "Птица любит есть мух";
            showFlyHint = false;
        }
        else if (showBerryHint && points >= 300) {
            newHint = "А также не против клубники";
            showBerryHint = false;
        }
        else if (showBossHint && boss.isActive()) {
            newHint = "Вы разозлили зверомуху!";
            showBossHint = false;
        }
        else if (showTapBirdHint && points >= 6000){
            newHint = "От умных злых птиц не увернуться! \nКликни по ним, чтобы прогнать";
            showTapBirdHint = false;
        }

        if (!newHint.equals(hintText)) {
            hintText = newHint;
            hintShowTime = System.currentTimeMillis();
        }
    }

    private void updateSpawning() {
        // Спавн врагов
        enemySpawnCounter++;
        if (enemySpawnCounter >= ENEMY_SPAWN_RATE && enemies.size() < enemyCount) {
            spawnEnemy();
            enemySpawnCounter = 0;
        }

        // Спавн бонусов
        bonusSpawnCounter++;
        if (bonusSpawnCounter >= BONUS_SPAWN_RATE && bonuses.size() < bonusCount) {
            spawnBonus();
            bonusSpawnCounter = 0;
        }
    }

    private void spawnEnemy() {
        Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.enemy);
        int w = b.getWidth()/5;
        int h = b.getHeight()/3;
        Rect firstFrame = new Rect(4*w, 0, 5*w, h);
        Sprite newEnemy = new Sprite(viewWidth + 50, Math.random() * (viewHeight - h), -400, 0, firstFrame, b);

        for (int i = 0; i < 3; i++) {
            for (int j = 4; j >= 0; j--) {
                if (i ==0 && j == 4) continue;
                if (i ==2 && j == 0) continue;
                newEnemy.addFrame(new Rect(j*w, i*h, j*w+w, i*h+h));
            }
        }

        enemies.add(newEnemy);
    }

    private void spawnBonus() {
        Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.fly);
        int w = b.getWidth() / 2;
        int h = b.getHeight();
        int sizeX = 128;
        int sizeY = (h * sizeX) / w;
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(b, sizeX * 2, sizeY, true);

        Rect firstFrame = new Rect(0, 0, sizeX, sizeY);
        float bonusSpeed = -280 - (float)(Math.random() * 170);
        Sprite newBonus = new Sprite(viewWidth + 50,Math.random() * (viewHeight - sizeY), bonusSpeed, 0, firstFrame, scaledBitmap);
        newBonus.addFrame(new Rect(sizeX, 0, sizeX * 2, sizeY));
        newBonus.setFrameTime(10);
        newBonus.setPadding(10);

        bonuses.add(newBonus);
    }

    private long totalActiveTime() {
        if (isPaused) {
            return (pauseStartTime - gameStartTime - totalPausedTime);
        } else {
            return (System.currentTimeMillis() - gameStartTime - totalPausedTime);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);
        int pointerCount = event.getPointerCount();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                float touchX = event.getX(pointerIndex);
                float touchY = event.getY(pointerIndex);

                // Проверяем, нажата ли кнопка паузы
                if (pauseButton != null && pauseButton.contains((int)touchX, (int)touchY)) {
                    if (!isPaused) {
                        pauseStartTime = System.currentTimeMillis();
                    } else {
                        totalPausedTime += System.currentTimeMillis() - pauseStartTime;
                    }
                    isPaused = !isPaused; // Переключаем паузу
                    invalidate();
                    break;
                }

                // Обработка кнопок на экране поражения
                if (isGameOver) {
                    if (againButton.contains((int)touchX, (int)touchY)) {
                        restartGame();
                        invalidate();
                    }
                    break;
                }

                // Обработка кнопок на экране победы
                if (isVictory) {
                    if (continueButton.contains((int)touchX, (int)touchY)) {
                        isVictory = false;
                        // Увеличиваем цель
                        victoryScore += 5000;
                        invalidate();
                    }
                    if (againButton.contains((int)touchX, (int)touchY)) {
                        restartGame();
                        invalidate();
                    }
                    break;
                }

                // Начало касания
                // Обработка клика по тап-птице
                if (isTapBirdActive && tapBird.checkClick(touchX, touchY)) {
                    tapBird.kill();
                    isTapBirdActive = false;
                    invalidate();
                    break;
                }

                // Управление
                if (controlPointerId == -1) {
                    controlPointerId = pointerId;
                    isTouching = true;
                    updateTargetPosition(touchX, touchY);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                // Обрабатываем движение всех пальцев
                if (controlPointerId != -1) {
                    int idx = event.findPointerIndex(controlPointerId);
                    if (idx != -1) {
                        float x = event.getX(idx);
                        float y = event.getY(idx);
                        updateTargetPosition(x, y);
                    }
                }
                break;

            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
                // Если отпустили управляющий палец
                if (pointerId == controlPointerId) {
                    controlPointerId = -1;
                    isTouching = false;

                    // Ищем другой палец для управления
                    for (int i = 0; i < pointerCount; i++) {
                        int id = event.getPointerId(i);
                        if (id != pointerId) {
                            controlPointerId = id;
                            isTouching = true;
                            break;
                        }
                    }
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                // Отпускание пальцев
                controlPointerId = -1;
                isTouching = false;
                break;
        }
        return true;
    }

    private void updateTargetPosition(float touchX, float touchY) {
        targetX = Math.max(0, Math.min(touchX, viewWidth / 4f - playerBird.getFrameWidth()));

        targetY = Math.max(-100, Math.min(touchY - playerBird.getFrameHeight() / 2f,
                viewHeight - playerBird.getFrameHeight()));
    }

    private void restartGame() {
        points = 0;
        gameSpeed = 1.0f;
        isGameOver = false;
        isVictory = false;
        isPaused = false;
        totalPausedTime = 0;
        gameStartTime = System.currentTimeMillis();

        // Очищаем списки
        enemies.clear();
        bonuses.clear();
        boss.deactivate();

        showFlyHint = true;
        showBerryHint = true;
        showBossHint = true;
        hintText = "Избегайте птиц!";
        hintShowTime = System.currentTimeMillis();
    }
}
