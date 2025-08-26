package com.example.xuemeixue;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AttendanceActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_CAMERA = 1;
    private static final String TAG = "AttendanceActivity";

    private TextView tvStudentInfo;
    private TextView tvAttendanceStatus;
    private Button btnStartAttendance;
    private ProgressBar progressBar;

    private String studentId;
    private String studentName;
    private String classCode;
    private String photoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        tvStudentInfo = findViewById(R.id.tvStudentInfo);
        tvAttendanceStatus = findViewById(R.id.tvAttendanceStatus);
        btnStartAttendance = findViewById(R.id.btnStartAttendance);
        progressBar = findViewById(R.id.progressBar);

        // 從 Intent 中獲取登入資訊
        Intent intent = getIntent();
        if (intent != null) {
            studentId = intent.getStringExtra("studentId");
            studentName = intent.getStringExtra("studentName");
            classCode = intent.getStringExtra("classCode");

            if (studentId != null && studentName != null && classCode != null) {
                String info = "學號: " + studentId + "\n姓名: " + studentName + "\n班級代碼: " + classCode;
                tvStudentInfo.setText(info);
            } else {
                tvStudentInfo.setText("使用者資訊讀取失敗");
            }
        }

        btnStartAttendance.setOnClickListener(v -> {
            // 點擊按鈕後，啟動相機 Activity
            Intent cameraIntent = new Intent(AttendanceActivity.this, RegistrationCameraActivity.class);
            startActivityForResult(cameraIntent, REQUEST_CODE_CAMERA);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CAMERA && resultCode == RESULT_OK && data != null) {
            // 從相機 Activity 獲取照片路徑
            photoPath = data.getStringExtra("photo_path");
            if (photoPath != null) {
                // 如果照片拍攝成功，開始上傳並進行人臉識別
                recognizeFace(photoPath);
            } else {
                Toast.makeText(this, "無法獲取照片路徑，請重試", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void recognizeFace(String photoPath) {
        progressBar.setVisibility(View.VISIBLE);
        tvAttendanceStatus.setText("正在進行人臉識別...");
        btnStartAttendance.setEnabled(false);

        OkHttpClient client = new OkHttpClient();
        File photoFile = new File(photoPath);

        // 建立上傳請求，包含照片檔案和班級代碼
        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("photo", photoFile.getName(),
                        RequestBody.create(photoFile, MediaType.parse("image/jpeg")))
                .addFormDataPart("class_code", classCode)
                .build();

        Request request = new Request.Builder()
                .url(AppConstants.RECOGNIZE_URL)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnStartAttendance.setEnabled(true);
                    tvAttendanceStatus.setText("網絡請求失敗: " + e.getMessage());
                    tvAttendanceStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                });
                Log.e(TAG, "Recognition failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnStartAttendance.setEnabled(true);

                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String responseData = response.body().string();
                            JSONObject jsonObject = new JSONObject(responseData);
                            boolean recognized = jsonObject.getBoolean("recognized");
                            String message = jsonObject.optString("message", "未知錯誤");

                            if (recognized) {
                                // 成功識別，考勤成功
                                tvAttendanceStatus.setText("考勤成功！");
                                tvAttendanceStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                                Toast.makeText(AttendanceActivity.this, "考勤成功！", Toast.LENGTH_SHORT).show();
                            } else {
                                // 未成功識別
                                tvAttendanceStatus.setText("考勤失敗: " + message);
                                tvAttendanceStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                                Toast.makeText(AttendanceActivity.this, "考勤失敗: " + message, Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException | IOException e) {
                            tvAttendanceStatus.setText("解析響應失敗，後端回傳格式不正確。");
                            tvAttendanceStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                            Log.e(TAG, "JSON parsing error: " + e.getMessage());
                        }
                    } else {
                        tvAttendanceStatus.setText("人臉識別失敗: " + response.message());
                        tvAttendanceStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    }
                });
                // 處理完畢後刪除臨時照片
                File fileToDelete = new File(photoPath);
                if (fileToDelete.exists()) {
                    fileToDelete.delete();
                }
            }
        });
    }
}