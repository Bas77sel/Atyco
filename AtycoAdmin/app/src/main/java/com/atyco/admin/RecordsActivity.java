package com.atyco.admin;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class RecordsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_records);


        ListView listView = findViewById(R.id.listViewRecords);
        DatabaseHelper myDb = new DatabaseHelper(this);
        ArrayList<String> recordsList = new ArrayList<>();


        String sessionName = getIntent().getStringExtra("SESSION_NAME");


        Cursor res = myDb.getStudentsBySession(sessionName);

        if (res.getCount() == 0) {
            recordsList.add("لا يوجد طلاب حاضرين في هذه الحصة حالياً");
        } else {
            while (res.moveToNext()) {
                String record = "الطالب: " + res.getString(0) +
                        "\nوقت الحضور: " + res.getString(1);
                recordsList.add(record);
            }
        }
        res.close();


        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, recordsList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);


                text.setTextColor(Color.BLACK);
                text.setTextSize(16);
                return view;
            }
        };

        listView.setAdapter(adapter);

        Button btnExport = findViewById(R.id.btnExport);
        btnExport.setOnClickListener(v -> {

            exportToExcel(sessionName);
        });
    }
    private void exportToExcel(String sessionName) {
        DatabaseHelper myDb = new DatabaseHelper(this);
        Cursor res = myDb.getStudentsBySession(sessionName);

        if (res.getCount() == 0) {
            Toast.makeText(this, "لا يوجد طلاب لتصديرهم", Toast.LENGTH_SHORT).show();
            return;
        }


        StringBuilder htmlContent = new StringBuilder();
        htmlContent.append("<html><head><meta charset=\"UTF-8\"></head><body><table border='1'>");


        htmlContent.append("<tr style='background-color:#2196F3; color:white;'>")
                .append("<th>Student Name</th>")
                .append("<th>Attendance Time</th>")
                .append("</tr>");

        while (res.moveToNext()) {
            htmlContent.append("<tr>");
            htmlContent.append("<td>").append(res.getString(0)).append("</td>");
            htmlContent.append("<td>").append(res.getString(1)).append("</td>");
            htmlContent.append("</tr>");
        }
        htmlContent.append("</table></body></html>");
        res.close();

        try {

            String fileName = "Attendance_" + sessionName + ".xls";
            java.io.File fileLocation = new java.io.File(getExternalFilesDir(null), fileName);

            java.io.FileOutputStream out = new java.io.FileOutputStream(fileLocation);
            out.write(htmlContent.toString().getBytes("UTF-8"));
            out.close();


            android.net.Uri path = androidx.core.content.FileProvider.getUriForFile(this,
                    "com.atyco.admin.fileprovider", fileLocation);

            android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("application/vnd.ms-excel");
            shareIntent.putExtra(android.content.Intent.EXTRA_STREAM, path);
            shareIntent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(android.content.Intent.createChooser(shareIntent, "إرسال كشف الحضور (Excel)"));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "حدث خطأ في استخراج الملف: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}