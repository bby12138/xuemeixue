// ResultActivity.java
package com.example.xuemeixue;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ResultActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView tvResult;
    private Button btnRetry;
    private PhotoDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        progressBar = findViewById(R.id.progressBar);
        tvResult = findViewById(R.id.tvResult);
        btnRetry = findViewById(R.id.btnRetry);
        dbHelper = new PhotoDatabaseHelper(this);
        String photoPath = getIntent().getStringExtra("photo_path");
        analyzePhoto(photoPath);

        btnRetry.setOnClickListener(v -> analyzePhoto(photoPath));
    }

    private void analyzePhoto(String photoPath) {
        progressBar.setVisibility(View.VISIBLE);
        tvResult.setText("正在进行人脸识别...");
        btnRetry.setVisibility(View.GONE);

        OkHttpClient client = new OkHttpClient();
        // 替换为实际的 API 地址
        String apiUrl = "https://your-real-api-url.com/api/analyze?photo_path=" + photoPath;
        Request request = new Request.Builder()
                .url(apiUrl)
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
                if (response.isSuccessful()) {
                    String result = response.body().string();
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        if (result.equals("success")) {
                            tvResult.setText("签到成功");
                        } else {
                            tvResult.setText("签到失败");
                        }
                        // 将照片路径插入数据库
                        insertPhotoPathToDatabase(photoPath);
                    });
                } else {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        tvResult.setText("分析失败: " + response.message());
                        btnRetry.setVisibility(View.VISIBLE);
                    });
                }
            }
        });
    }

    private void insertPhotoPathToDatabase(String photoPath) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(PhotoDatabaseHelper.COLUMN_PHOTO_PATH, photoPath);
        db.insert(PhotoDatabaseHelper.TABLE_NAME, null, values);
        db.close();
    }
}

// 数据库帮助类
class PhotoDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "photo_database.db";
    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "photos";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_PHOTO_PATH = "photo_path";

    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_PHOTO_PATH + " TEXT NOT NULL);";

    public PhotoDatabaseHelper(android.content.Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}