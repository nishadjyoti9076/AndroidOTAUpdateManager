package com.example.otademo;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadInstallFile extends AppCompatActivity {

    private final String VERSION_URL = "http://192.168.29.142:8080/latest_version.json";
    private final int PERMISSION_REQUEST_CODE = 100;
    private final String FILE_NAME = "OTADemo_v2.apk";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_install_file);

        Button btnCheckUpdate = findViewById(R.id.btnCheckUpdate);
        btnCheckUpdate.setOnClickListener(v -> checkPermissionsAndDownload());
    }

    private void checkPermissionsAndDownload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
            checkForUpdateAndDownload();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkForUpdateAndDownload();
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkForUpdateAndDownload() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(VERSION_URL).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(DownloadInstallFile.this, "Update check failed", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() ->
                            Toast.makeText(DownloadInstallFile.this, "Server error", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                String json = response.body().string();
                UpdateInfo info = new Gson().fromJson(json, UpdateInfo.class);
                PackageManager pm = getPackageManager();
                PackageInfo pInfo = null;
                try {
                    pInfo = pm.getPackageInfo(getPackageName(), 0);
                } catch (PackageManager.NameNotFoundException e) {
                    throw new RuntimeException(e);
                }
                String currentVersion = pInfo.versionName;
              //  String currentVersion = com.example.otademo.BuildConfig.VERSION_NAME;

                Log.e("currentVersion",currentVersion);
                Log.e("latest_version",info.latest_version);

                if (!currentVersion.equals(info.latest_version)) {
                    runOnUiThread(() -> showUpdateDialog(info));
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(DownloadInstallFile.this, "App is already up to date", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void showUpdateDialog(UpdateInfo info) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Available");
        builder.setMessage("Version: " + info.latest_version + "\n\n" + info.release_notes);

        builder.setPositiveButton("Update Now", (dialog, which) -> {
            String apkUrl = "http://192.168.29.142:8080/" + info.apk_filename;
            downloadAndInstallApk(apkUrl);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void downloadAndInstallApk(String apkUrl) {
        Toast.makeText(this, "Downloading APK...", Toast.LENGTH_SHORT).show();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(apkUrl).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("onFailure", Objects.requireNonNull(e.getMessage()));
                runOnUiThread(() ->
                        Toast.makeText(DownloadInstallFile.this, "Download failed", Toast.LENGTH_LONG).show()
                );
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() ->
                            Toast.makeText(DownloadInstallFile.this, "Server error", Toast.LENGTH_LONG).show()
                    );
                    return;
                }

                File apkFile = new File(getExternalFilesDir(null), FILE_NAME);
                InputStream is = response.body().byteStream();
                FileOutputStream fos = new FileOutputStream(apkFile);
                byte[] buffer = new byte[2048];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                is.close();

                installApk(apkFile);
            }
        });
    }

    private void installApk(File file) {
        Uri apkUri = FileProvider.getUriForFile(this,
                getPackageName() + ".provider", file);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    // Model class for JSON
    public static class UpdateInfo {
        public String app_name;
        public String latest_version;
        public String apk_filename;
        public String release_notes;
    }
}
