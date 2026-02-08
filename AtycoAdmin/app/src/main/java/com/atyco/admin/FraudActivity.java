package com.atyco.admin;

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

public class FraudActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fraud);

        try {
            ListView lv = findViewById(R.id.lvFraud);
            DatabaseHelper db = new DatabaseHelper(this);
            ArrayList<String> fraudList = new ArrayList<>();

            Cursor res = db.getFraudLog();
            if (res != null && res.getCount() > 0) {
                while (res.moveToNext()) {
                    String record = "⚠️ أصلي: " + res.getString(1) +
                            "\n❌ مزور: " + res.getString(2);
                    fraudList.add(record);
                }
                res.close();
            } else {
                fraudList.add("لا توجد محاولات تلاعب حالياً");
            }


            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, fraudList) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    TextView text = (TextView) view.findViewById(android.R.id.text1);


                    text.setTextColor(Color.BLACK);
                    text.setTextSize(16);
                    return view;
                }
            };

            lv.setAdapter(adapter);

        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("FRAUD_ERROR", "Crash inside FraudActivity: " + e.getMessage());
        }
    }
}