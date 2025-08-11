package com.example.xuemeixue;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

public class CreateClassActivity extends AppCompatActivity {

    private EditText etClassCode;
    private EditText etClassName;
    private Button btnCreate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_create_class);

            etClassCode = findViewById(R.id.etClassCode);
            etClassName = findViewById(R.id.etClassName);
            btnCreate = findViewById(R.id.btnCreate);

            btnCreate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String classCode = etClassCode.getText().toString();
                    String className = etClassName.getText().toString();

                    // 这里可以添加班级码和班级名称的验证逻辑

                    Intent intent = new Intent();
                    intent.putExtra("className", className);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            });
        } catch (Exception e) {
            Log.e("CreateClassActivity", "Error in onCreate: " + e.getMessage());
        }
    }
}