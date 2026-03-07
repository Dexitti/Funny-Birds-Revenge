package com.example.funnybirds;

import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.ActivityInfo;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Принудительно устанавливаем горизонтальную ориентацию для старых Android
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        GameSurfaceView surfaceView = new GameSurfaceView(this, new GameView(this));
        setContentView(surfaceView);
    }

}