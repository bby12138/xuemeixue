package com.example.xuemeixue;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

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
    private EditText etClassCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        RadioGroup roleGroup = findViewById(R.id.roleGroup);
        etTeacherId = findViewById(R.id.etTeacherId);
        etStudentId = findViewById(R.id.etStudentId);
        etPassword = findViewById(R.id.etPassword);
        etClassCode = findViewById(R.id.etClassCode);
        Button btnLogin = findViewById(R.id.btnLogin);

        // 角色选择切换逻辑
        roleGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.btnTeacher) {
                    etTeacherId.setVisibility(View.VISIBLE);
                    etStudentId.setVisibility(View.GONE);
                    etClassCode.setVisibility(View.GONE);
                    etPassword.setVisibility(View.VISIBLE);
                } else if (checkedId == R.id.btnStudent) {
                    etTeacherId.setVisibility(View.GONE);
                    etStudentId.setVisibility(View.VISIBLE);
                    etClassCode.setVisibility(View.VISIBLE);
                    etPassword.setVisibility(View.VISIBLE);
                }
            }
        });

        // 登录按钮点击事件
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 获取用户输入
                String role = (roleGroup.getCheckedRadioButtonId() == R.id.btnTeacher) ? "teacher" : "student";
                String id = role.equals("teacher") ?
                        etTeacherId.getText().toString() :
                        etStudentId.getText().toString();
                String password = etPassword.getText().toString();
                String classCode = role.equals("student") ? etClassCode.getText().toString() : "";

                // 简单验证
                if (id.isEmpty() || password.isEmpty() || (role.equals("student") && classCode.isEmpty())) {
                    Toast.makeText(LoginActivity.this, "请填写完整信息", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 发送网络请求
                OkHttpClient client = new OkHttpClient();
                // 构建请求体
                FormBody formBody = new FormBody.Builder()
                        .add("role", role)
                        .add("id", id)
                        .add("password", password)
                        .add("classCode", classCode)
                        .build();

                // 注意：localhost在模拟器中应改为10.0.2.2，真机需用电脑实际IP
                Request request = new Request.Builder()
                        .url("http://10.0.2.2:8848/api/test/login")
                        .post(formBody)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() ->
                                Toast.makeText(LoginActivity.this, "网络请求失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            String responseData = response.body().string();
                            runOnUiThread(() -> {
                                Toast.makeText(LoginActivity.this, "登录请求已提交", Toast.LENGTH_SHORT).show();
                                // 保存登录状态并跳转
                                saveLoginStatusAndJump(role, id, classCode);
                            });
                        } else {
                            runOnUiThread(() ->
                                    Toast.makeText(LoginActivity.this, "登录失败: " + response.message(), Toast.LENGTH_SHORT).show()
                            );
                        }
                    }
                });
            }
        });
    }

    // 注意：这个方法应该定义在onCreate方法外面，作为类的成员方法
    private void saveLoginStatusAndJump(String role, String id, String classCode) {
        SharedPreferences sharedPreferences = getSharedPreferences("login_status", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("is_logged_in", true);
        editor.putString("role", role);
        editor.apply();

        Intent intent = new Intent(LoginActivity.this, ClassActivity.class);
        intent.putExtra("isTeacher", role.equals("teacher"));
        if (role.equals("teacher")) {
            intent.putExtra("teacherId", id);
        } else {
            intent.putExtra("studentId", id);
            intent.putExtra("classCode", classCode);
        }
        startActivity(intent);
        finish();
    }
}
