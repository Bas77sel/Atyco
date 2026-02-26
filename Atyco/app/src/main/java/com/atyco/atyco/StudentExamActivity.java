package com.atyco.atyco;

import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class StudentExamActivity extends AppCompatActivity {

    private List<QuestionModel> questionsList = new ArrayList<>();
    private int currentIndex = 0;

    private TextView tvQuestionNumber, tvQuestionText;
    private RadioGroup rgOptions;
    private RadioButton rbA, rbB, rbC, rbD;
    private Button btnPrev, btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_exam);


        String rawData = getIntent().getStringExtra("EXAM_DATA");
        parseExamData(rawData);


        initViews();


        displayQuestion(currentIndex);


        btnNext.setOnClickListener(v -> {
            saveCurrentAnswer();
            if (currentIndex < questionsList.size() - 1) {
                currentIndex++;
                displayQuestion(currentIndex);
            } else {

                submitExam();
            }
        });

        btnPrev.setOnClickListener(v -> {
            saveCurrentAnswer();
            if (currentIndex > 0) {
                currentIndex--;
                displayQuestion(currentIndex);
            }
        });
    }

    private void initViews() {
        tvQuestionNumber = findViewById(R.id.tvQuestionNumber);
        tvQuestionText = findViewById(R.id.tvStudentQuestion);
        rgOptions = findViewById(R.id.rgStudentOptions);
        rbA = findViewById(R.id.rbOpA);
        rbB = findViewById(R.id.rbOpB);
        rbC = findViewById(R.id.rbOpC);
        rbD = findViewById(R.id.rbOpD);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
    }


    private void parseExamData(String data) {
        String[] qBlocks = data.split("###");
        for (String block : qBlocks) {
            String[] parts = block.split(":::");
            if (parts.length >= 5) {
                questionsList.add(new QuestionModel(parts[0], parts[1], parts[2], parts[3], parts[4]));
            }
        }
    }


    private void displayQuestion(int index) {
        QuestionModel q = questionsList.get(index);
        tvQuestionNumber.setText("السؤال " + (index + 1) + " من " + questionsList.size());
        tvQuestionText.setText(q.getQuestion());
        rbA.setText(q.getOpA());
        rbB.setText(q.getOpB());
        rbC.setText(q.getOpC());
        rbD.setText(q.getOpD());


        rgOptions.clearCheck();
        String savedAns = q.getSelectedAnswer();
        if (savedAns.equals("A")) rbA.setChecked(true);
        else if (savedAns.equals("B")) rbB.setChecked(true);
        else if (savedAns.equals("C")) rbC.setChecked(true);
        else if (savedAns.equals("D")) rbD.setChecked(true);


        btnNext.setText(index == questionsList.size() - 1 ? "إرسال الحل" : "التالي");
        btnPrev.setEnabled(index > 0);
    }


    private void saveCurrentAnswer() {
        int id = rgOptions.getCheckedRadioButtonId();
        String ans = "";
        if (id == R.id.rbOpA) ans = "A";
        else if (id == R.id.rbOpB) ans = "B";
        else if (id == R.id.rbOpC) ans = "C";
        else if (id == R.id.rbOpD) ans = "D";

        questionsList.get(currentIndex).setSelectedAnswer(ans);
    }

    private void submitExam() {

        Toast.makeText(this, "جاري إرسال الإجابات للتصحيح...", Toast.LENGTH_SHORT).show();
    }
}