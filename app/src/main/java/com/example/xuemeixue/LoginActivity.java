package com.example.xuemeixue;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etTeacherId;
    private EditText etStudentId;
    private EditText etPassword;
    private RadioGroup roleGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        roleGroup = findViewById(R.id.roleGroup);
        etTeacherId = findViewById(R.id.etTeacherId);
        etStudentId = findViewById(R.id.etStudentId);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);

        // 新增的注册按钮
        Button btnRegisterLink = findViewById(R.id.btnRegisterLink);

        roleGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.btnTeacher) {
                    etTeacherId.setVisibility(View.VISIBLE);
                    etStudentId.setVisibility(View.GONE);
                    etPassword.setVisibility(View.VISIBLE);
                } else if (checkedId == R.id.btnStudent) {
                    etTeacherId.setVisibility(View.GONE);
                    etStudentId.setVisibility(View.VISIBLE);
                    etPassword.setVisibility(View.VISIBLE);
                }
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String role = (roleGroup.getCheckedRadioButtonId() == R.id.btnTeacher) ? "teacher" : "student";
                String id = role.equals("teacher") ?
                        etTeacherId.getText().toString() :
                        etStudentId.getText().toString();
                String password = etPassword.getText().toString();

                if (id.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "請填寫完整信息", Toast.LENGTH_SHORT).show();
                    return;
                }

                OkHttpClient client = new OkHttpClient();
                FormBody.Builder formBuilder = new FormBody.Builder();

                // 學生和教師登入都只傳送 username 和 password
                formBuilder.add("username", id)
                        .add("password", password);

                Request request = new Request.Builder()
                        .url(AppConstants.LOGIN_URL)
                        .post(formBuilder.build())
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() ->
                                Toast.makeText(LoginActivity.this, "網絡請求失敗: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            String responseData = response.body().string();
                            try {
                                JSONObject jsonObject = new JSONObject(responseData);
                                String message = jsonObject.optString("message", "登入失敗");

                                // 檢查後端回傳的訊息是否為成功
                                if (message.equals("登入成功")) {
                                    final String token = jsonObject.getString("token");
                                    // 新增: 獲取班級代碼
                                    final String classCode = jsonObject.optString("class_code", "");
                                    final String role = jsonObject.optString("role", "");

                                    runOnUiThread(() -> {
                                        Toast.makeText(LoginActivity.this, "登入成功", Toast.LENGTH_SHORT).show();

                                        SharedPreferences sharedPreferences = getSharedPreferences("login_status", MODE_PRIVATE);
                                        SharedPreferences.Editor editor = sharedPreferences.edit();
                                        editor.putBoolean("is_logged_in", true);
                                        editor.putString("role", role);
                                        // 新增: 保存班級代碼到 SharedPreferences
                                        editor.putString("class_code", classCode);
                                        editor.apply();

                                        // 修改: 將班級代碼傳遞給跳轉方法
                                        saveLoginStatusAndJump(role, id, classCode);
                                    });
                                } else {
                                    runOnUiThread(() ->
                                            Toast.makeText(LoginActivity.this, "登入失敗: " + message, Toast.LENGTH_SHORT).show()
                                    );
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                runOnUiThread(() ->
                                        Toast.makeText(LoginActivity.this, "解析響應失敗", Toast.LENGTH_SHORT).show()
                                );
                            }
                        } else {
                            runOnUiThread(() ->
                                    Toast.makeText(LoginActivity.this, "登入失败: " + response.message(), Toast.LENGTH_SHORT).show()
                            );
                        }
                    }
                });
            }
        });

        // 为新添加的注册按钮设置点击事件
        btnRegisterLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
            startActivity(intent);
        });
    }

    // 新增: 接受班級代碼參數
    private void saveLoginStatusAndJump(String role, String id, String classCode) {
        Intent intent = new Intent(LoginActivity.this, ClassActivity.class);
        intent.putExtra("isTeacher", role.equals("teacher"));
        intent.putExtra("id", id);
        // 新增: 傳遞班級代碼到下一個 Activity
        if (role.equals("student")) {
            intent.putExtra("class_code", classCode);
        }
        startActivity(intent);
        finish();
    }
}