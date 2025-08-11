package com.example.xuemeixue;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ClassActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class);

        boolean isTeacher = getIntent().getBooleanExtra("isTeacher", false);
        Button btnCreateClass = findViewById(R.id.btnCreateClass);
        FloatingActionButton fabCamera = findViewById(R.id.fabCamera);

        // 教师视图
        if (isTeacher) {
            btnCreateClass.setVisibility(View.VISIBLE);
            fabCamera.setVisibility(View.GONE);
            String teacherId = getIntent().getStringExtra("teacherId");
            String password = getIntent().getStringExtra("password");
            // 可以在这里处理教师工号和密码

            // 设置创建新班级按钮的点击事件
            btnCreateClass.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    android.util.Log.d("ClassActivity", "Create class button clicked");
                    Intent intent = new Intent(ClassActivity.this, CreateClassActivity.class);
                    startActivity(intent);
                }
            });
        } else {
            String studentId = getIntent().getStringExtra("studentId");
            String classCode = getIntent().getStringExtra("classCode");
            String password = getIntent().getStringExtra("password");
            // 可以在这里处理学号、班级码和密码
        }

        fabCamera.setOnClickListener(v ->
                startActivity(new Intent(this, CameraActivity.class))
        );
    }
}