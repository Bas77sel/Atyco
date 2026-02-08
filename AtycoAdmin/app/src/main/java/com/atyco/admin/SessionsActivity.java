package com.atyco.admin;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
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
    }
}