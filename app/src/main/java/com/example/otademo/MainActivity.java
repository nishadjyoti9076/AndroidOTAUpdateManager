package com.example.otademo;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        PackageManager pm = getPackageManager();
        PackageInfo pInfo;

        try {
            pInfo = pm.getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }

        String currentVersion = pInfo.versionName;
        TextView tv = findViewById(R.id.tvVersionDetails);
        tv.setText("Version : "+currentVersion);

        Button btnNext = findViewById(R.id.btnNext);
        btnNext.setOnClickListener(v -> nextToDashboard());

        Button btnCheckUpdate = findViewById(R.id.btnCheckUpdate);
        btnCheckUpdate.setOnClickListener(v -> checkForUpdate());
    }

    public void nextToDashboard(){
        Intent intent=new Intent(this,DashboardActivity.class);
        startActivity(intent);
    }

    public void checkForUpdate(){
        Intent intent = new Intent(this, DownloadInstallFile.class);
        startActivity(intent);
    }
}