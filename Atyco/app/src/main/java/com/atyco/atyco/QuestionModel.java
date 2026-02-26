package com.atyco.atyco;

public class QuestionModel {
    private String question;
    private String opA, opB, opC, opD;
    private String selectedAnswer = "";


    public QuestionModel(String question, String opA, String opB, String opC, String opD) {
        this.question = question;
        this.opA = opA;
        this.opB = opB;
        this.opC = opC;
        this.opD = opD;
    }


    public String getQuestion() { return question; }
    public String getOpA() { return opA; }
    public String getOpB() { return opB; }
    public String getOpC() { return opC; }
    public String getOpD() { return opD; }


    public void setSelectedAnswer(String selectedAnswer) { this.selectedAnswer = selectedAnswer; }
    public String getSelectedAnswer() { return selectedAnswer; }
}