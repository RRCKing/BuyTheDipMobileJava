package com.kingchan.buythedip;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
    }

    Handler handler = new Handler();

    private Runnable runnableName= new Runnable() {
        @Override
        public void run() {
            startActivity(new Intent(SplashActivity.this , MainActivity.class));
            finish();
            //call function, do something
            handler.postDelayed(runnableName, 2000);//this is the line that makes a runnable repeat itself
        }
    };
}