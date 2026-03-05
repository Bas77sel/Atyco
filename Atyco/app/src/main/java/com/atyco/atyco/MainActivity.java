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
                etStudentName.setError("من فضلك اكتب اسمك أولاً");
            } else if (!isArabic(name)) {

                etStudentName.setError("يجب كتابة الاسم باللغة العربية فقط");
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
        // حماية: التأكد أن البيانات ليست null قبل أي معالجة
        if (qrData == null || qrData.isEmpty()) {
            runOnUiThread(() -> Toast.makeText(this, "فشل قراءة بيانات QR", Toast.LENGTH_SHORT).show());
            return;
        }

        // الحل: إنشاء متغير جديد final لاستخدامه داخل الـ Thread
        final String finalQrData = qrData.trim();

        new Thread(() -> {
            try {
                // نستخدم المتغير الجديد finalQrData هنا
                if (!finalQrData.contains(":")) {
                    runOnUiThread(() -> Toast.makeText(this, "كود QR غير صالح", Toast.LENGTH_SHORT).show());
                    return;
                }

                String[] parts = finalQrData.split(":");
                String ip = parts[0].trim();

                String portClean = parts[1].replaceAll("[^0-9]", "");
                int port = Integer.parseInt(portClean);

                String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                String studentName = etStudentName.getText().toString().trim();

                String payload = deviceId + "|" + studentName;

                Socket socket = new Socket(ip, port);
                socket.setSoTimeout(5000);

                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));

                out.println(payload);

                String response = in.readLine();

                if (response != null) {
                    if (response.startsWith("EXAM|")) {
                        String examData = response.substring(5);
                        runOnUiThread(() -> {
                            Intent intent = new Intent(MainActivity.this, StudentExamActivity.class);
                            intent.putExtra("EXAM_DATA", examData);
                            intent.putExtra("IP", ip);
                            intent.putExtra("PORT", port);
                            startActivity(intent);
                        });
                    } else if (response.equals("SUCCESS")) {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "✅ تم تسجيل الحضور بنجاح", Toast.LENGTH_LONG).show());
                    } else if (response.equals("FRAUD_DETECTED")) {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "⚠️ تحذير: تم رصد تلاعب!", Toast.LENGTH_LONG).show());
                    }
                }

                out.close();
                in.close();
                socket.close();

            } catch (Exception e) {
                e.printStackTrace();
                final String error = e.getMessage();
                runOnUiThread(() -> {
                    Toast.makeText(this, "فشل الاتصال: " + error, Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    public boolean isArabic(String text) {

        String arabicPattern = "^[\\u0621-\\u064A\\s]+$";
        return text.matches(arabicPattern);
    }
}