package com.atyco.admin;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SessionsActivity extends AppCompatActivity {

    private DatabaseHelper myDb;
    private ListView lvSessions;
    private SimpleCursorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sessions);

        myDb = new DatabaseHelper(this);
        lvSessions = findViewById(R.id.lvSessions);
        FloatingActionButton fabAdd = findViewById(R.id.fabAddSession);

        displaySessions();


        lvSessions.setOnItemClickListener((parent, view, position, id) -> {
            Cursor cursor = (Cursor) parent.getItemAtPosition(position);
            String sessionName = cursor.getString(cursor.getColumnIndexOrThrow("SESSION_NAME"));

            Intent intent = new Intent(this, RecordsActivity.class);
            intent.putExtra("SESSION_NAME", sessionName);
            startActivity(intent);
        });


        lvSessions.setOnItemLongClickListener((parent, view, position, id) -> {
            Cursor cursor = (Cursor) parent.getItemAtPosition(position);
            String sessionName = cursor.getString(cursor.getColumnIndexOrThrow("SESSION_NAME"));
            showDeleteDialog(sessionName);
            return true;
        });


        fabAdd.setOnClickListener(v -> showAddSessionDialog());
    }

    private void displaySessions() {
        Cursor cursor = myDb.getAllSessions();
        String[] from = {"SESSION_NAME", "CREATED_AT"};
        int[] to = {android.R.id.text1, android.R.id.text2};


        adapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_2, cursor, from, to, 0) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View view = super.getView(position, convertView, parent);


                TextView text1 = view.findViewById(android.R.id.text1);
                text1.setTextColor(android.graphics.Color.BLACK);
                text1.setTypeface(null, android.graphics.Typeface.BOLD);


                TextView text2 = view.findViewById(android.R.id.text2);
                text2.setTextColor(android.graphics.Color.parseColor("#333333"));

                return view;
            }
        };

        lvSessions.setAdapter(adapter);
    }

    private void showAddSessionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("إضافة حصة جديدة");
        final EditText input = new EditText(this);
        input.setHint("اكتب اسم الحصة هنا...");
        builder.setView(input);

        builder.setPositiveButton("إنشاء", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                String date = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
                myDb.createNewSession(name, date);
                displaySessions();
            }
        });
        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }

    private void showDeleteDialog(String sessionName) {
        new AlertDialog.Builder(this)
                .setTitle("حذف حصة")
                .setMessage("هل أنت متأكد من حذف '" + sessionName + "' وكل بيانات الحضور فيها؟")
                .setPositiveButton("حذف", (dialog, which) -> {
                    myDb.deleteSession(sessionName);
                    displaySessions();
                    Toast.makeText(this, "تم الحذف بنجاح", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }
}