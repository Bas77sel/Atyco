package com.atyco.atyco;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.PrintWriter;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private Button btnScan;
    private EditText etStudentName;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etStudentName = findViewById(R.id.etStudentName);
        btnScan = findViewById(R.id.btnScan);


        sharedPreferences = getSharedPreferences("StudentPrefs", MODE_PRIVATE);
        String savedName = sharedPreferences.getString("student_name", "");
        etStudentName.setText(savedName);

        btnScan.setOnClickListener(v -> {
            String name = etStudentName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "من فضلك اكتب اسمك أولاً", Toast.LENGTH_SHORT).show();
            } else {

                sharedPreferences.edit().putString("student_name", name).apply();


                startScanner();
            }
        });
    }

    private void startScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("امسح كود المعلم لتسجيل الحضور");
        integrator.setOrientationLocked(false);
        integrator.setBeepEnabled(true);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "تم إلغاء المسح", Toast.LENGTH_LONG).show();
            } else {
                String qrData = result.getContents();
                sendAttendance(qrData);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void sendAttendance(String qrData) {
        new Thread(() -> {
            try {
                String[] parts = qrData.split(":");
                String ip = parts[0];
                int port = Integer.parseInt(parts[1]);

                String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                String studentName = etStudentName.getText().toString().trim();


                String payload = deviceId + "|" + studentName;

                Socket socket = new Socket(ip, port);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);


                out.println(payload);

                out.close();
                socket.close();

                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "تم إرسال بيانات الحضور بنجاح ✅", Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "فشل الاتصال: تأكد من الاتصال بشبكة المعلم ❌", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}