package com.example.xuemeixue;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

public class RegistrationActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_CAMERA = 1;
    private static final String TAG = "RegistrationActivity";

    private EditText etStudentNumber, etStudentName, etPassword;
    private Button btnRegister, btnTakePhoto;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private String photoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        etStudentNumber = findViewById(R.id.etStudentNumber);
        etStudentName = findViewById(R.id.etStudentName);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);

        btnTakePhoto.setOnClickListener(v -> {
            // 呼叫為註冊功能設計的專用相機頁面
            Intent intent = new Intent(RegistrationActivity.this, RegistrationCameraActivity.class);
            startActivityForResult(intent, REQUEST_CODE_CAMERA);
        });

        btnRegister.setOnClickListener(v -> {
            String studentNumber = etStudentNumber.getText().toString().trim();
            String studentName = etStudentName.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (studentNumber.isEmpty() || studentName.isEmpty() || password.isEmpty() || photoPath == null) {
                Toast.makeText(this, "請填寫所有欄位並拍照", Toast.LENGTH_SHORT).show();
                return;
            }

            registerStudent(studentNumber, studentName, password, photoPath);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CAMERA && resultCode == RESULT_OK && data != null) {
            photoPath = data.getStringExtra("photo_path");
            if (photoPath != null) {
                tvStatus.setText("照片已拍攝，可以註冊了。");
                tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else {
                Toast.makeText(this, "無法獲取照片路徑，請重試", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void registerStudent(String studentNumber, String studentName, String password, String photoPath) {
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText("正在註冊...");
        btnRegister.setEnabled(false);

        OkHttpClient client = new OkHttpClient();
        File photoFile = new File(photoPath);

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("student_number", studentNumber)
                .addFormDataPart("student_name", studentName)
                .addFormDataPart("password", password)
                .addFormDataPart("photo", photoFile.getName(),
                        RequestBody.create(photoFile, MediaType.parse("image/jpeg")))
                .build();

        Request request = new Request.Builder()
                .url(AppConstants.REGISTER_URL)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnRegister.setEnabled(true);
                    tvStatus.setText("註冊失敗: " + e.getMessage());
                    tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                });
                Log.e(TAG, "Registration failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseBody = response.body().string();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnRegister.setEnabled(true);

                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonObject = new JSONObject(responseBody);

                            boolean success = jsonObject.getBoolean("success");
                            if (success) {
                                tvStatus.setText("註冊成功！請返回登入頁面");
                                tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                                Toast.makeText(RegistrationActivity.this, "註冊成功！", Toast.LENGTH_LONG).show();
                            } else {
                                // 如果 'success' 是 false，則獲取錯誤訊息
                                String errorMessage = jsonObject.optString("message", "未知錯誤");
                                tvStatus.setText("註冊失敗: " + errorMessage);
                                tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                            }
                        } catch (JSONException e) {
                            // 如果後端沒有回傳 'success' 鍵，會拋出此異常
                            tvStatus.setText("解析響應失敗，後端回傳格式不正確。");
                            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                            Log.e(TAG, "JSON parsing error: " + e.getMessage());
                        }
                    } else {
                        // 處理 HTTP 狀態碼非 200 的情況
                        tvStatus.setText("註冊失敗: " + response.message());
                        tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    }
                });
            }
        });
    }
}