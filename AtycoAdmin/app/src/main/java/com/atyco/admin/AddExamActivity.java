package com.atyco.admin;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AddExamActivity extends AppCompatActivity {

    private EditText etQuestion, etOpA, etOpB, etOpC, etOpD;
    private RadioGroup rgOptions;
    private DatabaseHelper myDb;
    private String sessionName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_exam);

        myDb = new DatabaseHelper(this);

        sessionName = getIntent().getStringExtra("SESSION_NAME");

        
        etQuestion = findViewById(R.id.etQuestion);
        etOpA = findViewById(R.id.etOpA);
        etOpB = findViewById(R.id.etOpB);
        etOpC = findViewById(R.id.etOpC);
        etOpD = findViewById(R.id.etOpD);
        rgOptions = findViewById(R.id.rgOptions);

        Button btnAddNext = findViewById(R.id.btnAddNextQuestion);
        Button btnFinish = findViewById(R.id.btnFinishExam);

        btnAddNext.setOnClickListener(v -> saveQuestionAndClear());

        btnFinish.setOnClickListener(v -> {

            if (!etQuestion.getText().toString().trim().isEmpty()) {
                saveQuestionAndClear();
            }
            finish();
        });
    }

    private void saveQuestionAndClear() {
        String qText = etQuestion.getText().toString().trim();
        String a = etOpA.getText().toString().trim();
        String b = etOpB.getText().toString().trim();
        String c = etOpC.getText().toString().trim();
        String d = etOpD.getText().toString().trim();


        int selectedId = rgOptions.getCheckedRadioButtonId();
        if (qText.isEmpty() || a.isEmpty() || b.isEmpty() || c.isEmpty() || d.isEmpty() || selectedId == -1) {
            Toast.makeText(this, "يرجى إكمال بيانات السؤال واختيار الإجابة الصحيحة", Toast.LENGTH_SHORT).show();
            return;
        }

        String correctAnswer = "";
        if (selectedId == R.id.rbA) correctAnswer = "A";
        else if (selectedId == R.id.rbB) correctAnswer = "B";
        else if (selectedId == R.id.rbC) correctAnswer = "C";
        else if (selectedId == R.id.rbD) correctAnswer = "D";


        myDb.addQuestion(sessionName, qText, a, b, c, d, correctAnswer);


        etQuestion.setText("");
        etOpA.setText("");
        etOpB.setText("");
        etOpC.setText("");
        etOpD.setText("");
        rgOptions.clearCheck();

        Toast.makeText(this, "تم حفظ السؤال بنجاح", Toast.LENGTH_SHORT).show();
    }
}