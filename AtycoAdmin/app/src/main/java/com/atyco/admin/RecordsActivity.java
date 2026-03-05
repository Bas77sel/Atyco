package com.atyco.admin;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecordsActivity extends AppCompatActivity {

    private String sessionName;
    private DatabaseHelper myDb;
    private SimpleCursorAdapter adapter;
    private ListView listView;
    private ServerSocket serverSocket;
    private boolean isServerRunning = false;
    private ImageView ivQrCode;
    private TextView tvIpAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_records);

        myDb = new DatabaseHelper(this);
        sessionName = getIntent().getStringExtra("SESSION_NAME");

        TextView tvTitle = findViewById(R.id.tvSessionTitle);
        tvTitle.setText(sessionName);
        ivQrCode = findViewById(R.id.ivQrCode);
        tvIpAddress = findViewById(R.id.tvIpAddress);
        listView = findViewById(R.id.listViewAttendance);

        setupAttendanceList();

        findViewById(R.id.btnStartServer).setOnClickListener(v -> startServer());
        findViewById(R.id.btnStopServer).setOnClickListener(v -> stopServer());
        findViewById(R.id.btnExportExcel).setOnClickListener(v -> exportToExcel());
        Button btnAddExam = findViewById(R.id.btnAddExam);

        btnAddExam.setOnClickListener(v -> {
            Intent intent = new Intent(RecordsActivity.this, AddExamActivity.class);

            intent.putExtra("SESSION_NAME", sessionName);
            startActivity(intent);
        });

    }
    private void checkExamStatus() {
        TextView tvExamStatus = findViewById(R.id.tvExamStatus);
        Button btnAddExam = findViewById(R.id.btnAddExam);

        if (myDb.hasExam(sessionName)) {
            tvExamStatus.setVisibility(View.VISIBLE);
            btnAddExam.setText("Modify or Add Questions");
        } else {
            tvExamStatus.setVisibility(View.GONE);
            btnAddExam.setText("Add Exam");
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        checkExamStatus();
    }

    private void setupAttendanceList() {

        Cursor cursor = myDb.getStudentsBySession(sessionName);

        String[] from = {"STUDENT_NAME", "TIME_STAMP"};
        int[] to = {android.R.id.text1, android.R.id.text2};


        adapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_2, cursor, from, to, 0) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(android.R.id.text1);
                text1.setTextColor(Color.BLACK);
                TextView text2 = view.findViewById(android.R.id.text2);
                text2.setTextColor(Color.GRAY);
                return view;
            }
        };
        listView.setAdapter(adapter);
    }

    private void refreshList() {
        runOnUiThread(() -> {

            Cursor newCursor = myDb.getStudentsBySession(sessionName);

            if (adapter != null) {

                Cursor oldCursor = adapter.swapCursor(newCursor);
                if (oldCursor != null) {
                    oldCursor.close();
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void startServer() {
        if (isServerRunning) {
            stopServer();
        }

        isServerRunning = true;

        String ip = getIPAddress();
        tvIpAddress.setText("IP: " + ip);
        generateQRCode(ip + ":" + 8080);

        final String currentSessionName = this.sessionName;

        new Thread(() -> {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }

                serverSocket = new ServerSocket(8080);
                serverSocket.setReuseAddress(true);

                while (isServerRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();

                        new Thread(() -> {
                            try {
                                BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                                PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true);

                                String payload = input.readLine();

                                if (payload != null) {

                                    if (payload.startsWith("SUBMIT_EXAM|")) {
                                        String[] parts = payload.split("\\|");
                                        if (parts.length >= 3) {
                                            String studentId = parts[1];
                                            String studentAnswers = parts[2];


                                            String scoreResult = calculateAndSaveScore(studentId, studentAnswers, currentSessionName);


                                            output.println("YOUR_SCORE|" + scoreResult);
                                        }
                                    }

                                    else if (payload.contains("|")) {
                                        String[] data = payload.split("\\|");
                                        String receivedId = data[0];
                                        String receivedName = data[1];


                                        String registeredName = myDb.checkOrRegisterStudent(receivedId, receivedName);

                                        if (registeredName.equals(receivedName)) {

                                            String time = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date());
                                            myDb.markAttendance(receivedId, currentSessionName, time);


                                            String examData = myDb.getExamJson(currentSessionName);
                                            if (examData != null && !examData.isEmpty()) {
                                                output.println("EXAM|" + examData);
                                            } else {
                                                output.println("SUCCESS");
                                            }

                                            runOnUiThread(() -> {
                                                Toast.makeText(this, "✅ تم تسجيل: " + registeredName, Toast.LENGTH_SHORT).show();
                                                refreshList();
                                            });
                                        } else {

                                            output.println("FRAUD_DETECTED");
                                            myDb.logFraud(receivedId, registeredName, receivedName, currentSessionName);
                                            showSecurityWarning(receivedId, registeredName, receivedName);
                                        }
                                    }
                                }
                                clientSocket.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();
                    } catch (Exception e) {
                        android.util.Log.d("SERVER", "Socket accept stopped");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        Toast.makeText(this, "Attendance Start For: " + currentSessionName, Toast.LENGTH_SHORT).show();
    }

    public void stopServer() {
        if (isServerRunning) {
            isServerRunning = false;
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
                runOnUiThread(() -> {
                    tvIpAddress.setText("IP: Offline");
                    ivQrCode.setImageBitmap(null);
                    Toast.makeText(this, "Attendance Stopped", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String getIPAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                for (InetAddress addr : Collections.list(intf.getInetAddresses())) {
                    if (!addr.isLoopbackAddress() && addr.getHostAddress().indexOf(':') < 0) return addr.getHostAddress();
                }
            }
        } catch (Exception e) {}
        return "0.0.0.0";
    }

    private void generateQRCode(String text) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            com.google.zxing.common.BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512);
            Bitmap bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565);
            for (int x = 0; x < 512; x++) {
                for (int y = 0; y < 512; y++) bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
            runOnUiThread(() -> ivQrCode.setImageBitmap(bmp));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void exportToExcel() {
        Cursor res = myDb.getStudentsBySession(sessionName);
        if (res.getCount() == 0) {
            Toast.makeText(this, "لا يوجد حضور لتصديره", Toast.LENGTH_SHORT).show();
            return;
        }
        StringBuilder html = new StringBuilder("<html><head><meta charset=\"UTF-8\"></head><body><table border='1'>");
        html.append("<tr style='background:#2196F3;color:white;'><th>Student Name</th><th>Time</th></tr>");
        while (res.moveToNext()) {
            html.append("<tr><td>").append(res.getString(res.getColumnIndexOrThrow("STUDENT_NAME"))).append("</td>");
            html.append("<td>").append(res.getString(res.getColumnIndexOrThrow("TIME_STAMP"))).append("</td></tr>");
        }
        html.append("</table></body></html>");

        try {
            String date = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
            String fileName = "Attendance_" + sessionName + "_" + date + ".xls";
            File file = new File(getExternalFilesDir(null), fileName);
            FileOutputStream out = new FileOutputStream(file);
            out.write(html.toString().getBytes("UTF-8"));
            out.close();

            Uri path = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            intent.setType("application/vnd.ms-excel");
            intent.putExtra(android.content.Intent.EXTRA_STREAM, path);
            intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(android.content.Intent.createChooser(intent, "إرسال الملف"));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showSecurityWarning(String devId, String originalName, String fakeName) {
        runOnUiThread(() -> {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) {
                v.vibrate(500);
            }
            String message = "⚠️ محاولة تلاعب: " + originalName + " دخل باسم " + fakeName;
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopServer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopServer();
    }

    private String calculateAndSaveScore(String studentId, String studentAnswers, String sessionName) {
        String[] answersArray = studentAnswers.split(",");


        Cursor cursor = myDb.getQuestionsForSession(sessionName);

        int score = 0;
        int total = 0;

        if (cursor != null) {
            total = cursor.getCount();
            int i = 0;
            while (cursor.moveToNext() && i < answersArray.length) {
                String correctAnswer = cursor.getString(cursor.getColumnIndexOrThrow("CORRECT_ANSWER"));
                if (correctAnswer.trim().equalsIgnoreCase(answersArray[i].trim())) {
                    score++;
                }
                i++;
            }
            cursor.close();
        }

        String studentName = myDb.getStudentNameById(studentId);
        myDb.saveExamResult(studentId, studentName, sessionName, score, total);

        return score + "/" + total;
    }
}