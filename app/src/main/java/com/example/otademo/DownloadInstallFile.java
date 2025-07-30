package com.example.otademo;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadInstallFile extends AppCompatActivity {

    private final String VERSION_URL = "http://192.168.1.9:8080/latest_version.json";
    private final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_install_file);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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

        Log.e("VERSION_URL","VERSION_URL"+VERSION_URL);
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("onFailure","onFailure"+e.getMessage());
                runOnUiThread(() ->
                        Toast.makeText(DownloadInstallFile.this, "Update check failed", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

                Log.e("response", "response of check update - "+response);

                if (!response.isSuccessful()) {
                    runOnUiThread(() ->
                            Toast.makeText(DownloadInstallFile.this, "Server error", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                String json = response.body().string();
                UpdateInfo info = new Gson().fromJson(json, UpdateInfo.class);
                PackageManager pm = getPackageManager();
                PackageInfo pInfo;

                try {
                    pInfo = pm.getPackageInfo(getPackageName(), 0);
                } catch (PackageManager.NameNotFoundException e) {
                    throw new RuntimeException(e);
                }

                String currentVersion = pInfo.versionName;
                Log.e("currentVersion", currentVersion);
             //   Log.e("latest_version", info.versions.);

                if (/*!currentVersion.equals(info.latest_version)*/true) {
                  //  runOnUiThread(() -> showUpdateDialog(info));
                    runOnUiThread(() -> showVersionSelectorDialog(info,currentVersion));
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
       // builder.setMessage("Version: " + info.latest_version + "\n\n" + info.release_notes);

        builder.setPositiveButton("Update Now", (dialog, which) -> {
           // String apkUrl = "http://192.168.1.9:8080/" + info.apk_filename;
           // downloadAndInstallApk(apkUrl);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showVersionSelectorDialog(UpdateInfo info, String currentVersion) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_version_selector, null);
        RadioGroup radioGroup = dialogView.findViewById(R.id.versionRadioGroup);

        Map<String, VersionDetails> versions = info.versions;
        List<String> rollbackList = new ArrayList<>();
        List<String> upgradeList = new ArrayList<>();

        for (String version : versions.keySet()) {
            int cmp = compareVersions(version, currentVersion);
            if (cmp < 0) rollbackList.add(version);
            else if (cmp > 0) upgradeList.add(version);
        }

        // Sort both lists to show rollback versions first
        Collections.sort(rollbackList);
        Collections.sort(upgradeList);

        // Add rollback options
        for (String version : rollbackList) {
            RadioButton rb = new RadioButton(this);
            rb.setText("Rollback to v" + version + ": " + versions.get(version).notes);
            rb.setTag(version);
            radioGroup.addView(rb);
        }

        // Add upgrade options
        for (String version : upgradeList) {
            RadioButton rb = new RadioButton(this);
            rb.setText("Upgrade to v" + version + ": " + versions.get(version).notes);
            rb.setTag(version);
            radioGroup.addView(rb);
        }

        builder.setView(dialogView)
                .setTitle("Select Version to Install")
                .setPositiveButton("Install", (dialog, which) -> {
                    int selectedId = radioGroup.getCheckedRadioButtonId();
                    RadioButton selectedButton = dialogView.findViewById(selectedId);
                    String selectedVersion = (String) selectedButton.getTag();
                    VersionDetails selectedDetails = versions.get(selectedVersion);
                    downloadAndInstallApk(selectedDetails.apk_url); // Your install logic
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void showVersionSelectorDialog2(UpdateInfo info, String currentVersion) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_version_selector, null);
        RadioGroup radioGroup = dialogView.findViewById(R.id.versionRadioGroup);

        Map<String, VersionDetails> versions = info.versions;

        for (String version : versions.keySet()) {
            if (compareVersions(version, currentVersion) > 0) {
                RadioButton radioButton = new RadioButton(this);
                radioButton.setText("Version " + version + ": " + versions.get(version).notes);
                radioButton.setTag(version);
                radioGroup.addView(radioButton);
            }
        }

        builder.setView(dialogView)
                .setTitle("Select Version to Install")
                .setPositiveButton("Install", (dialog, which) -> {
                    int selectedId = radioGroup.getCheckedRadioButtonId();
                    RadioButton selectedButton = dialogView.findViewById(selectedId);
                    String selectedVersion = (String) selectedButton.getTag();
                    VersionDetails selectedDetails = versions.get(selectedVersion);
                    downloadAndInstallApk(selectedDetails.apk_url); // Your install logic
                })
                .setNegativeButton("Cancel", null)
                .show();
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

                Log.e("response","response - "+response);
                if (!response.isSuccessful()) {
                    runOnUiThread(() ->
                            Toast.makeText(DownloadInstallFile.this, "Server error", Toast.LENGTH_LONG).show()
                    );
                    return;
                }

                String fileName = apkUrl.substring(apkUrl.lastIndexOf("/") + 1);
                File apkFile = new File(getExternalFilesDir(null), fileName);

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

    private int compareVersions(String versionA, String versionB) {
        String[] partsA = versionA.split("\\.");
        String[] partsB = versionB.split("\\.");

        int length = Math.max(partsA.length, partsB.length);

        for (int i = 0; i < length; i++) {
            int vA = i < partsA.length ? Integer.parseInt(partsA[i]) : 0;
            int vB = i < partsB.length ? Integer.parseInt(partsB[i]) : 0;

            if (vA != vB) {
                return vA - vB;
            }
        }
        return 0;
    }

    private int compareVersions1(String versionA, String versionB) {
        String[] partsA = versionA.split("\\.");
        String[] partsB = versionB.split("\\.");

        int length = Math.max(partsA.length, partsB.length);

        for (int i = 0; i < length; i++) {
            int vA = i < partsA.length ? Integer.parseInt(partsA[i]) : 0;
            int vB = i < partsB.length ? Integer.parseInt(partsB[i]) : 0;

            if (vA != vB) {
                return vA - vB;
            }
        }
        return 0;
    }


    // Model class for JSON
   /* public static class UpdateInfo {
        public String app_name;
        public String latest_version;
        public String apk_filename;
        public String release_notes;
    }*/

    public class UpdateInfo {
        public String app_name;
        public Map<String, VersionDetails> versions;
    }

    public class VersionDetails {
        public String apk_filename;
        public String apk_url;
        public boolean rollback_allowed;
        public String notes;
    }

}

