package com.atyco.admin;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class FraudActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private SimpleCursorAdapter adapter;
    private ListView lv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fraud);

        db = new DatabaseHelper(this);
        lv = findViewById(R.id.lvFraud);
        Button btnClear = findViewById(R.id.btnClearFraud);



        String[] from = {"ORIGINAL_NAME", "FAKE_NAME", "SESSION_NAME"};
        int[] to = {R.id.tvOldName, R.id.tvNewName, R.id.tvSessionLabel};


        adapter = new SimpleCursorAdapter(this, R.layout.item_fraud, null, from, to, 0) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                Button btnApprove = view.findViewById(R.id.btnApprove);

                Cursor cursor = (Cursor) getItem(position);
                if (cursor != null) {
                    final String deviceId = cursor.getString(cursor.getColumnIndexOrThrow("DEVICE_ID"));
                    final String newName = cursor.getString(cursor.getColumnIndexOrThrow("FAKE_NAME"));
                    final String sessionName = cursor.getString(cursor.getColumnIndexOrThrow("SESSION_NAME"));

                    btnApprove.setOnClickListener(v -> {
                        db.approveStudentName(deviceId, newName, sessionName);
                        Toast.makeText(FraudActivity.this, "تم اعتماد الاسم ✅", Toast.LENGTH_SHORT).show();
                        refreshFraudList();
                    });
                }
                return view;
            }
        };

        lv.setAdapter(adapter);
        refreshFraudList();


        btnClear.setOnClickListener(v -> {
            db.clearFraudLog();
            refreshFraudList();
            Toast.makeText(this, "تم مسح سجل التلاعب بنجاح", Toast.LENGTH_SHORT).show();
        });
    }


    private void refreshFraudList() {
        new Thread(() -> {

            final Cursor newCursor = db.getFraudLog();

            runOnUiThread(() -> {

                Cursor oldCursor = adapter.swapCursor(newCursor);


                if (oldCursor != null && oldCursor != newCursor) {
                    oldCursor.close();
                }
            });
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (adapter != null && adapter.getCursor() != null) {
            adapter.getCursor().close();
        }
    }
}