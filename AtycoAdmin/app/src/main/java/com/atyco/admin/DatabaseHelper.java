package com.atyco.admin;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "AtycoAdmin.db";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Students (DEVICE_ID TEXT PRIMARY KEY, STUDENT_NAME TEXT)");
        db.execSQL("CREATE TABLE Attendance (DEVICE_ID TEXT, SESSION_NAME TEXT, TIME_STAMP TEXT)");

        db.execSQL("CREATE TABLE Fraud_Log (DEVICE_ID TEXT, ORIGINAL_NAME TEXT, FAKE_NAME TEXT, TIME_STAMP TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS Students");
        db.execSQL("DROP TABLE IF EXISTS Attendance");
        db.execSQL("DROP TABLE IF EXISTS Fraud_Log");
        onCreate(db);
    }


    public String checkOrRegisterStudent(String devId, String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT STUDENT_NAME FROM Students WHERE DEVICE_ID = ?", new String[]{devId});
        if (cursor.moveToFirst()) {
            String registeredName = cursor.getString(0);
            cursor.close();
            return registeredName;
        } else {
            ContentValues cv = new ContentValues();
            cv.put("DEVICE_ID", devId);
            cv.put("STUDENT_NAME", name);
            db.insert("Students", null, cv);
            cursor.close();
            return name;
        }
    }

    public void markAttendance(String devId, String session, String time) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM Attendance WHERE DEVICE_ID = ? AND SESSION_NAME = ?", new String[]{devId, session});
        if (cursor.getCount() == 0) {
            ContentValues cv = new ContentValues();
            cv.put("DEVICE_ID", devId);
            cv.put("SESSION_NAME", session);
            cv.put("TIME_STAMP", time);
            db.insert("Attendance", null, cv);
        }
        cursor.close();
    }

    public void logFraud(String devId, String originalName, String fakeName, String time) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("DEVICE_ID", devId);
        cv.put("ORIGINAL_NAME", originalName);
        cv.put("FAKE_NAME", fakeName);
        cv.put("TIME_STAMP", time);
        db.insert("Fraud_Log", null, cv);
    }


    public Cursor getUniqueSessions() {
        return getWritableDatabase().rawQuery("SELECT DISTINCT SESSION_NAME FROM Attendance", null);
    }

    public Cursor getStudentsBySession(String sessionName) {
        return getWritableDatabase().rawQuery("SELECT Students.STUDENT_NAME, Attendance.TIME_STAMP " +
                "FROM Attendance INNER JOIN Students ON Attendance.DEVICE_ID = Students.DEVICE_ID " +
                "WHERE Attendance.SESSION_NAME = ?", new String[]{sessionName});
    }

    public Cursor getFraudLog() {
        return getWritableDatabase().rawQuery("SELECT * FROM Fraud_Log", null);
    }
}