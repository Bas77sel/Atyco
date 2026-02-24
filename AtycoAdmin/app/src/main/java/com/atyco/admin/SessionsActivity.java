package com.atyco.admin;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class SessionsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sessions);

        ListView lv = findViewById(R.id.lvSessions);
        DatabaseHelper db = new DatabaseHelper(this);
        ArrayList<String> sessions = new ArrayList<>();

        Cursor cursor = db.getUniqueSessions();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                sessions.add(cursor.getString(0));
            }
            cursor.close();
        }


        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, sessions) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);


                text.setTextColor(Color.BLACK);
                text.setTextSize(18);
                return view;
            }
        };

        lv.setAdapter(adapter);

        lv.setOnItemClickListener((parent, view, position, id) -> {
            String selectedSession = sessions.get(position);
            Intent intent = new Intent(this, RecordsActivity.class);
            intent.putExtra("SESSION_NAME", selectedSession);
            startActivity(intent);
        });


        FloatingActionButton fabAddSession = findViewById(R.id.fabAddSession);
        ListView lvSessions = findViewById(R.id.lvSessions);


        fabAddSession.setOnClickListener(v -> {
            EditText etSession = new EditText(this);
            etSession.setHint("اسم الحصة الجديدة");

            new AlertDialog.Builder(this)
                    .setTitle("إنشاء حصة جديدة")
                    .setView(etSession)
                    .setPositiveButton("بدء الحضور", (dialog, which) -> {
                        String name = etSession.getText().toString().trim();
                        if (!name.isEmpty()) {
                            Intent intent = new Intent(this, MainActivity.class);
                            intent.putExtra("NEW_SESSION_NAME", name);
                            startActivity(intent);
                            finish();
                        }
                    })
                    .setNegativeButton("إلغاء", null)
                    .show();
        });


        lvSessions.setOnItemLongClickListener((parent, view, position, id) -> {
            String selectedSession = (String) parent.getItemAtPosition(position);

            new AlertDialog.Builder(this)
                    .setTitle("خيارات: " + selectedSession)
                    .setItems(new String[]{"إكمال الغياب (توليد QR)", "إلغاء"}, (dialog, which) -> {
                        if (which == 0) {
                            Intent intent = new Intent(this, MainActivity.class);
                            intent.putExtra("REGENERATE_SESSION", selectedSession);
                            startActivity(intent);
                            finish();
                        }
                    }).show();
            return true;
        });
    }
}