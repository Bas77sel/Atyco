package com.atyco.admin;

import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
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
import java.util.ArrayList;
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

    class NetworkInfoModel {
        String name, ip;
        NetworkInfoModel(String name, String ip) { this.name = name; this.ip = ip; }
        @Override public String toString() { return name; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_records);


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            }
        }

        myDb = new DatabaseHelper(this);
        sessionName = getIntent().getStringExtra("SESSION_NAME");

        TextView tvTitle = findViewById(R.id.tvSessionTitle);
        tvTitle.setText(sessionName);
        ivQrCode = findViewById(R.id.ivQrCode);
        tvIpAddress = findViewById(R.id.tvIpAddress);
        listView = findViewById(R.id.listViewAttendance);

        setupAttendanceList();

        findViewById(R.id.btnStartServer).setOnClickListener(v -> showNetworkSelectionDialog());
        findViewById(R.id.btnStopServer).setOnClickListener(v -> stopServer());
        findViewById(R.id.btnExportExcel).setOnClickListener(v -> exportToExcel());

        ivQrCode.setOnClickListener(v -> {
            if (!isServerRunning) return;
            Dialog builder = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            ImageView imageView = new ImageView(this);
            imageView.setBackgroundColor(Color.WHITE);


            String currentIp = tvIpAddress.getTag() != null ? tvIpAddress.getTag().toString() : "";

            generateLargeQRCode(currentIp + ":8080", imageView);
            builder.addContentView(imageView, new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            builder.show();
            imageView.setOnClickListener(v1 -> builder.dismiss());
        });
    }

    private void showNetworkSelectionDialog() {
        List<NetworkInfoModel> networks = getAvailableNetworks();
        if (networks.isEmpty()) {
            Toast.makeText(this, "لا يوجد اتصال بالشبكة!", Toast.LENGTH_LONG).show();
            return;
        }


        String[] items = new String[networks.size()];
        for (int i = 0; i < networks.size(); i++) items[i] = networks.get(i).name;

        new AlertDialog.Builder(this)
                .setTitle("اختر شبكة الحضور")
                .setItems(items, (dialog, which) -> {
                    String selectedIp = networks.get(which).ip;
                    String selectedName = networks.get(which).name;
                    startServer(selectedIp, selectedName);
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private List<NetworkInfoModel> getAvailableNetworks() {
        List<NetworkInfoModel> networks = new ArrayList<>();
        android.net.wifi.WifiManager wifiManager = (android.net.wifi.WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        String currentSsid = "شبكة غير معروفة";

        if (wifiManager != null) {
            android.net.wifi.WifiInfo info = wifiManager.getConnectionInfo();
            if (info != null && info.getSSID() != null && !info.getSSID().equals("<unknown ssid>")) {
                currentSsid = info.getSSID().replace("\"", "");
            }
        }

        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (intf.isLoopback() || !intf.isUp()) continue;

                for (InetAddress addr : Collections.list(intf.getInetAddresses())) {
                    if (!addr.isLoopbackAddress() && addr instanceof java.net.Inet4Address) {
                        String ip = addr.getHostAddress();
                        String interfaceName = intf.getName().toLowerCase();
                        String finalLabel;

                        if (interfaceName.contains("wlan")) {

                            finalLabel = "Wi-Fi: " + currentSsid;
                        } else if (interfaceName.contains("p2p")) {
                            finalLabel = "اتصال شاشة (Casting)";
                        } else if (interfaceName.contains("ap") || interfaceName.contains("softap")) {
                            finalLabel = "نقطة اتصال (Hotspot)";
                        } else {
                            finalLabel = "واجهة: " + intf.getName();
                        }

                        if (ip.startsWith("192.168.")) finalLabel += " ⭐";
                        networks.add(new NetworkInfoModel(finalLabel, ip));
                    }
                }
            }
        } catch (Exception ignored) {}
        return networks;
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
                if (oldCursor != null) oldCursor.close();
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void startServer(String selectedIp, String networkName) {
        if (isServerRunning) stopServer();
        isServerRunning = true;


        tvIpAddress.setText("متصل عبر: " + networkName.replace(" ⭐", ""));


        generateQRCode(selectedIp + ":8080");

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(8080);
                serverSocket.setReuseAddress(true);
                while (isServerRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        new Thread(() -> handleClient(clientSocket)).start();
                    } catch (Exception e) {
                        android.util.Log.d("SERVER", "Stopped");
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();

        Toast.makeText(this, "تم بدء الاستقبال بنجاح ✅", Toast.LENGTH_SHORT).show();
    }

    private void handleClient(Socket clientSocket) {
        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true);
            String payload = input.readLine();
            if (payload != null && payload.contains("|")) {
                String[] data = payload.split("\\|");
                String receivedId = data[0];
                String receivedName = data[1];
                String registeredName = myDb.checkOrRegisterStudent(receivedId, receivedName);
                if (registeredName.equals(receivedName)) {
                    String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
                    myDb.markAttendance(receivedId, sessionName, time);
                    output.println("SUCCESS");
                    runOnUiThread(() -> {
                        Toast.makeText(this, "✅ تم تسجيل: " + registeredName, Toast.LENGTH_SHORT).show();
                        refreshList();
                    });
                } else {
                    output.println("FRAUD_DETECTED");
                    myDb.logFraud(receivedId, registeredName, receivedName, sessionName);
                    showSecurityWarning(receivedId, registeredName, receivedName);
                }
            }
            clientSocket.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void stopServer() {
        isServerRunning = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (Exception e) { e.printStackTrace(); }
        runOnUiThread(() -> {
            ivQrCode.setImageBitmap(null);
            tvIpAddress.setText("IP Address: Stopped");
        });
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

    private void generateLargeQRCode(String text, ImageView targetView) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            com.google.zxing.common.BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 1024, 1024);
            Bitmap bmp = Bitmap.createBitmap(1024, 1024, Bitmap.Config.RGB_565);
            for (int x = 0; x < 1024; x++) {
                for (int y = 0; y < 1024; y++) bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
            runOnUiThread(() -> targetView.setImageBitmap(bmp));
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
            if (v != null) v.vibrate(500);
            Toast.makeText(this, "⚠️ محاولة تلاعب: " + originalName + " دخل باسم " + fakeName, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopServer();
    }
    private String getCurrentSsid(Context context) {
        android.net.wifi.WifiManager wifiManager = (android.net.wifi.WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        android.net.wifi.WifiInfo info = wifiManager.getConnectionInfo();
        if (info != null) {
            String ssid = info.getSSID();
            if (ssid.equals("<unknown ssid>")) {
                return null;
            }
            return ssid.replace("\"", "");
        }
        return null;
    }
}