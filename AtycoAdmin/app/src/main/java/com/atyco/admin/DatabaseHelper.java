package com.atyco.admin;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "AtycoAdmin.db";
    private static final int DATABASE_VERSION = 4;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Students (DEVICE_ID TEXT PRIMARY KEY, STUDENT_NAME TEXT)");

        db.execSQL("CREATE TABLE Attendance (" +
                "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "DEVICE_ID TEXT, " +
                "STUDENT_NAME TEXT, " +
                "SESSION_NAME TEXT, " +
                "TIME_STAMP TEXT)");

        db.execSQL("CREATE TABLE Fraud_Log (DEVICE_ID TEXT, ORIGINAL_NAME TEXT, FAKE_NAME TEXT, SESSION_NAME TEXT)");

        db.execSQL("CREATE TABLE Sessions (SESSION_NAME TEXT PRIMARY KEY, CREATED_AT TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS Students");
        db.execSQL("DROP TABLE IF EXISTS Attendance");
        db.execSQL("DROP TABLE IF EXISTS Fraud_Log");
        db.execSQL("DROP TABLE IF EXISTS Sessions");
        onCreate(db);
    }


    public void markAttendance(String devId, String session, String time) {
        SQLiteDatabase db = this.getWritableDatabase();


        String studentName = "Unknown";
        Cursor nameCursor = db.rawQuery("SELECT STUDENT_NAME FROM Students WHERE DEVICE_ID = ?", new String[]{devId});
        if (nameCursor.moveToFirst()) {
            studentName = nameCursor.getString(0);
        }
        nameCursor.close();


        Cursor cursor = db.rawQuery("SELECT * FROM Attendance WHERE DEVICE_ID = ? AND SESSION_NAME = ?", new String[]{devId, session});

        if (cursor.getCount() == 0) {
            ContentValues cv = new ContentValues();
            cv.put("DEVICE_ID", devId);
            cv.put("STUDENT_NAME", studentName);
            cv.put("SESSION_NAME", session);
            cv.put("TIME_STAMP", time);
            db.insert("Attendance", null, cv);
        }
        cursor.close();
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

    public Cursor getStudentsBySession(String session) {
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT ID AS _id, STUDENT_NAME, TIME_STAMP FROM Attendance WHERE SESSION_NAME = ? ORDER BY ID DESC", new String[]{session});
    }


    public void createNewSession(String sessionName, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("SESSION_NAME", sessionName);
        values.put("CREATED_AT", date);
        db.insertWithOnConflict("Sessions", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public Cursor getAllSessions() {
        return getReadableDatabase().rawQuery("SELECT rowid AS _id, SESSION_NAME, CREATED_AT FROM Sessions ORDER BY rowid DESC", null);
    }

    public void deleteSession(String sessionName) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("Attendance", "SESSION_NAME = ?", new String[]{sessionName});
        db.delete("Sessions", "SESSION_NAME = ?", new String[]{sessionName});
    }

    public void logFraud(String devId, String originalName, String fakeName, String sessionName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("DEVICE_ID", devId);
        cv.put("ORIGINAL_NAME", originalName);
        cv.put("FAKE_NAME", fakeName);
        cv.put("SESSION_NAME", sessionName);
        db.insert("Fraud_Log", null, cv);
    }

    public Cursor getFraudLog() {
        return getReadableDatabase().rawQuery("SELECT rowid AS _id, * FROM Fraud_Log ORDER BY rowid DESC", null);
    }

    public void clearFraudLog() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM Fraud_Log");
    }

    public void approveStudentName(String deviceId, String newName, String sessionId) {
        SQLiteDatabase db = this.getWritableDatabase();


        ContentValues studentValues = new ContentValues();
        studentValues.put("STUDENT_NAME", newName);
        db.update("Students", studentValues, "DEVICE_ID = ?", new String[]{deviceId});


        ContentValues attendanceValues = new ContentValues();
        attendanceValues.put("STUDENT_NAME", newName);


        db.update("Attendance", attendanceValues, "DEVICE_ID = ? AND SESSION_NAME = ?",
                new String[]{deviceId, sessionId});


        db.delete("Fraud_Log", "DEVICE_ID = ? AND SESSION_NAME = ?", new String[]{deviceId, sessionId});
    }
}