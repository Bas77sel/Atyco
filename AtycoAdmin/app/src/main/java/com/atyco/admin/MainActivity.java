package com.atyco.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Button btnViewSessions = findViewById(R.id.btnViewSessions);
        btnViewSessions.setOnClickListener(v -> {

            Intent intent = new Intent(MainActivity.this, SessionsActivity.class);
            startActivity(intent);
        });


        Button btnViewFraud = findViewById(R.id.btnViewFraud);
        btnViewFraud.setOnClickListener(v -> {

            Intent intent = new Intent(MainActivity.this, FraudActivity.class);
            startActivity(intent);
        });
    }
}