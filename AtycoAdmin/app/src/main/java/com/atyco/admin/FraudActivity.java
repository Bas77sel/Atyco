package com.atyco.admin;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class FraudActivity extends AppCompatActivity {


    private DatabaseHelper db;
    private ArrayList<String> fraudList;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fraud);

        db = new DatabaseHelper(this);
        fraudList = new ArrayList<>();
        ListView lv = findViewById(R.id.lvFraud);
        Button btnClear = findViewById(R.id.btnClearFraud);

        loadFraudData();

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, fraudList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                String item = getItem(position);
                if (item != null) {
                    text.setText(Html.fromHtml(item, Html.FROM_HTML_MODE_COMPACT));
                }
                text.setTextColor(Color.BLACK);
                text.setLineSpacing(1.2f, 1.2f);
                return view;
            }
        };
        lv.setAdapter(adapter);


        btnClear.setOnClickListener(v -> {
            db.clearFraudLog();
            fraudList.clear();
            fraudList.add("لا توجد محاولات تلاعب حالياً");
            adapter.notifyDataSetChanged();
            Toast.makeText(this, "The Cheating History Deleted Successfully", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadFraudData() {
        try {
            Cursor res = db.getFraudLog();
            fraudList.clear();
            if (res != null && res.getCount() > 0) {
                int originalIdx = res.getColumnIndex("ORIGINAL_NAME");
                int fakeIdx = res.getColumnIndex("FAKE_NAME");
                int sessionIdx = res.getColumnIndex("SESSION_NAME");

                while (res.moveToNext()) {
                    String original = res.getString(originalIdx);
                    String fake = res.getString(fakeIdx);
                    String sessionName = res.getString(sessionIdx);

                    String record = "<b>⚠️ أصلي:</b> " + original + "<br/>" +
                            "<font color='#F44336'><b>❌ مزور:</b> " + fake + "</font><br/>" +
                            "<font color='#000000'><small>📚 الحصة: " + (sessionName != null ? sessionName : "غير معروفة") + "</small></font>";
                    fraudList.add(record);
                }
                res.close();
            } else {
                fraudList.add("لا توجد محاولات تلاعب حالياً");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}