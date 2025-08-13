package com.example.xuemeixue;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class ResultActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView tvResult;
    private Button btnRetry;
    private String photoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        progressBar = findViewById(R.id.progressBar);
        tvResult = findViewById(R.id.tvResult);
        btnRetry = findViewById(R.id.btnRetry);

        photoPath = getIntent().getStringExtra("photo_path");
        if (photoPath != null) {
            analyzePhoto(photoPath);
        } else {
            tvResult.setText("未接收到照片路径");
            progressBar.setVisibility(View.GONE);
        }

        btnRetry.setOnClickListener(v -> analyzePhoto(photoPath));
    }

    private void analyzePhoto(String photoPath) {
        progressBar.setVisibility(View.VISIBLE);
        tvResult.setText("正在进行人脸识别...");
        btnRetry.setVisibility(View.GONE);

        File file = new File(photoPath);
        if (!file.exists()) {
            runOnUiThread(() -> {
                tvResult.setText("照片文件不存在!");
                progressBar.setVisibility(View.GONE);
                btnRetry.setVisibility(View.VISIBLE);
            });
            return;
        }

        OkHttpClient client = new OkHttpClient();

        // 使用 multipart/form-data 構建請求
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", file.getName(),
                        RequestBody.create(MediaType.parse("image/jpeg"), file))
                .build();

        // 使用 AppConstants 中的常量 URL
        Request request = new Request.Builder()
                .url(AppConstants.ATTENDANCE_URL)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvResult.setText("网络请求失败: " + e.getMessage());
                    btnRetry.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseBody = response.body().string();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnRetry.setVisibility(View.GONE);
                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonObject = new JSONObject(responseBody);
                            String result = jsonObject.getString("result");
                            if ("success".equals(result)) {
                                tvResult.setText("签到成功！");
                            } else {
                                tvResult.setText("签到失败！\n" + jsonObject.getString("message"));
                                btnRetry.setVisibility(View.VISIBLE);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            tvResult.setText("解析響應失敗");
                            btnRetry.setVisibility(View.VISIBLE);
                        }
                    } else {
                        tvResult.setText("服務器響應失敗：" + response.code());
                        btnRetry.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }
}