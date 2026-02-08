package com.atyco.admin;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Vibrator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView tvIpAddress;
    private ImageView ivQrCode;
    private Button btnGenerate, btnStop;
    private EditText etSessionName;

    private ServerSocket serverSocket;
    private boolean isServerRunning = false;
    private DatabaseHelper myDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);


        tvIpAddress = findViewById(R.id.tvIpAddress);
        ivQrCode = findViewById(R.id.ivQrCode);
        btnGenerate = findViewById(R.id.btnGenerate);
        btnStop = findViewById(R.id.btnStop);
        etSessionName = findViewById(R.id.etSessionName);


        myDb = new DatabaseHelper(this);

        btnStop.setOnClickListener(v -> stopServer());

        btnGenerate.setOnClickListener(v -> {
            String session = etSessionName.getText().toString().trim();
            if (session.isEmpty()) {
                Toast.makeText(this, "يرجى إدخال اسم الحصة أولاً", Toast.LENGTH_SHORT).show();
                return;
            }

            String ip = getIPAddress();
            if (!ip.equals("0.0.0.0")) {
                tvIpAddress.setText("IP Address: " + ip);
                generateQRCode(ip + ":8080");
                startServer();
                Toast.makeText(this, "بدأت الجلسة: " + session, Toast.LENGTH_SHORT).show();
            } else {
                tvIpAddress.setText("خطأ: تأكد من فتح الـ Hotspot");
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btnCurrentAttendance = findViewById(R.id.btnCurrentAttendance);
        btnCurrentAttendance.setOnClickListener(v -> {
            String currentSession = etSessionName.getText().toString().trim();
            if (currentSession.isEmpty()) {
                Toast.makeText(this, "اكتب اسم الحصة الحالية أولاً", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(this, RecordsActivity.class);
                intent.putExtra("SESSION_NAME", currentSession);
                startActivity(intent);
            }
        });

        findViewById(R.id.btnViewSessions).setOnClickListener(v -> {
            startActivity(new Intent(this, SessionsActivity.class));
        });


        findViewById(R.id.btnViewFraud).setOnClickListener(v -> {
            startActivity(new Intent(this, FraudActivity.class));
        });
    }

    public String getIPAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        if (sAddr.indexOf(':') < 0) return sAddr;
                    }
                }
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        return "0.0.0.0";
    }

    private void generateQRCode(String data) {
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(data, BarcodeFormat.QR_CODE, 500, 500);
            ivQrCode.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopServer() {
        isServerRunning = false;
        new Thread(() -> {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
                runOnUiThread(() -> {
                    ivQrCode.setImageBitmap(null);
                    tvIpAddress.setText("IP Address: Stopped");
                    Toast.makeText(this, "تم إيقاف الاستقبال", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void showSecurityWarning(String devId, String originalName, String fakeName) {
        runOnUiThread(() -> {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) v.vibrate(1000);

            new AlertDialog.Builder(this)
                    .setTitle("⚠️ تحذير: محاولة تلاعب!")
                    .setMessage("هذا الجهاز مسجل مسبقاً باسم: " + originalName + "\n\n" +
                            "يحاول الآن الدخول باسم: " + fakeName)
                    .setPositiveButton("إغلاق", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        });
    }

    private void startServer() {
        isServerRunning = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(8080);
                while (isServerRunning) {
                    Socket clientSocket = serverSocket.accept();
                    BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    String payload = input.readLine();

                    if (payload != null && payload.contains("|")) {
                        String[] data = payload.split("\\|");
                        String receivedId = data[0];
                        String receivedName = data[1];
                        String currentSession = etSessionName.getText().toString().trim();


                        String registeredName = myDb.checkOrRegisterStudent(receivedId, receivedName);

                        if (registeredName.equals(receivedName)) {

                            String time = java.text.DateFormat.getTimeInstance().format(new java.util.Date());
                            myDb.markAttendance(receivedId, currentSession, time);

                            runOnUiThread(() -> Toast.makeText(this, "تم تسجيل: " + registeredName, Toast.LENGTH_SHORT).show());
                        } else {

                                String time = java.text.DateFormat.getDateTimeInstance().format(new java.util.Date());
                                myDb.logFraud(receivedId, registeredName, receivedName, time);
                                showSecurityWarning(receivedId, registeredName, receivedName);

                        }
                    }
                    clientSocket.close();
                }
            } catch (Exception e) {
                android.util.Log.d("SERVER", "Server Stopped");
            }
        }).start();
    }
}