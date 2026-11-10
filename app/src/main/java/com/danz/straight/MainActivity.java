package com.danz.straight;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {
    private static final int REQ_SHIZUKU = 6767;
    private LicenseManager licManager;
    private LinearLayout devListContainer;
    private TextView shizukuStatusText;

    private final Shizuku.OnRequestPermissionResultListener permListener =
            (requestCode, grantResult) -> {
                if (requestCode == REQ_SHIZUKU) {
                    updateShizukuBadge();
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        licManager = new LicenseManager(this);
        Shizuku.addRequestPermissionResultListener(permListener);
        renderLoginGate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateShizukuBadge();
    }

    @Override
    protected void onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permListener);
        super.onDestroy();
    }

    private void updateShizukuBadge() {
        if (shizukuStatusText == null) return;
        if (ShizukuShell.isShizukuReady()) {
            shizukuStatusText.setText("SHIZUKU STATUS: CONNECTED (ADB UID 2000 READY)");
            shizukuStatusText.setTextColor(Color.parseColor("#00FF9D"));
        } else {
            shizukuStatusText.setText("SHIZUKU STATUS: NOT AUTHORIZED (REQUIRED TO ENTER)");
            shizukuStatusText.setTextColor(Color.parseColor("#FF4444"));
        }
    }

    private void renderLoginGate() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.parseColor("#0A0C10"));
        root.setPadding(64, 64, 64, 64);

        TextView title = new TextView(this);
        title.setText("DANZSTRAIGHT — NO ROOT (SHIZUKU)");
        title.setTextColor(Color.parseColor("#00E5FF"));
        title.setTextSize(20f);
        title.setGravity(Gravity.CENTER);

        shizukuStatusText = new TextView(this);
        shizukuStatusText.setGravity(Gravity.CENTER);
        shizukuStatusText.setPadding(0, 16, 0, 16);
        updateShizukuBadge();

        Button bindShizukuBtn = new Button(this);
        bindShizukuBtn.setText("1. AUTHORIZE SHIZUKU ACCESS");
        bindShizukuBtn.setOnClickListener(v -> {
            if (!Shizuku.pingBinder()) {
                Toast.makeText(this, "Start Shizuku via Wireless Debugging first!", Toast.LENGTH_LONG).show();
                return;
            }
            ShizukuShell.requestPermission(REQ_SHIZUKU);
            updateShizukuBadge();
        });

        TextView hwidView = new TextView(this);
        hwidView.setText("DEVICE HWID: " + licManager.getDeviceHwid());
        hwidView.setTextColor(Color.GRAY);
        hwidView.setGravity(Gravity.CENTER);
        hwidView.setPadding(0, 16, 0, 24);

        EditText keyInput = new EditText(this);
        keyInput.setHint("Masukkan Key Lisensi / Developer Key...");
        keyInput.setHintTextColor(Color.DKGRAY);
        keyInput.setTextColor(Color.WHITE);
        keyInput.setSingleLine(true);

        Button loginBtn = new Button(this);
        loginBtn.setText("2. LOGIN & START DANZSTRAIGHT");
        loginBtn.setOnClickListener(v -> {
            if (!ShizukuShell.isShizukuReady()) {
                Toast.makeText(this, "WAJIB AKTIFKAN SHIZUKU AGAR BISA MASUK!", Toast.LENGTH_LONG).show();
                ShizukuShell.requestPermission(REQ_SHIZUKU);
                return;
            }

            String key = keyInput.getText().toString().trim();
            if (licManager.isDeveloperKey(key)) {
                renderDeveloperConsole();
            } else if (licManager.verifyAndBindUserKey(key)) {
                startShizukuAndOverlay();
            } else {
                Toast.makeText(this, "Key Salah, Nonaktif, atau Terkunci di Device Lain!", Toast.LENGTH_SHORT).show();
            }
        });

        root.addView(title);
        root.addView(shizukuStatusText);
        root.addView(bindShizukuBtn);
        root.addView(hwidView);
        root.addView(keyInput);
        root.addView(loginBtn);
        setContentView(root);
    }

    private void renderDeveloperConsole() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#0A0C10"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 48, 48, 48);

        TextView header = new TextView(this);
        header.setText("DANZSTRAIGHT — DEVELOPER PANEL (DANZVIP)");
        header.setTextColor(Color.parseColor("#FFD600"));
        header.setTextSize(17f);

        Button createKeyBtn = new Button(this);
        createKeyBtn.setText("+ CREATE KEY (1 KEY 1 DEVICE)");
        createKeyBtn.setOnClickListener(v -> {
            String k = licManager.createNewKey();
            Toast.makeText(this, "Key Dibuat: " + k, Toast.LENGTH_SHORT).show();
            refreshDevKeyList();
        });

        Button launchOverlayBtn = new Button(this);
        launchOverlayBtn.setText("MASUK GAME SPACE & BUKA OVERLAY");
        launchOverlayBtn.setOnClickListener(v -> startShizukuAndOverlay());

        devListContainer = new LinearLayout(this);
        devListContainer.setOrientation(LinearLayout.VERTICAL);
        devListContainer.setPadding(0, 24, 0, 24);

        root.addView(header);
        root.addView(createKeyBtn);
        root.addView(launchOverlayBtn);
        root.addView(devListContainer);
        scroll.addView(root);
        setContentView(scroll);

        refreshDevKeyList();
    }

    private void refreshDevKeyList() {
        devListContainer.removeAllViews();
        List<LicenseManager.KeyRecord> records = licManager.listKeys();
        for (LicenseManager.KeyRecord rec : records) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 12, 0, 12);

            TextView info = new TextView(this);
            String status = rec.active ? "AKTIF" : "DIHENTIKAN";
            String hwid = rec.boundHwid.isEmpty() ? "BELUM TERIKAT" : rec.boundHwid;
            info.setText(rec.key + " | " + status + " | HWID: " + hwid);
            info.setTextColor(rec.active ? Color.GREEN : Color.RED);
            info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            Button stopBtn = new Button(this);
            stopBtn.setText("HENTIKAN KEY");
            stopBtn.setEnabled(rec.active);
            stopBtn.setOnClickListener(v -> {
                licManager.revokeKey(rec.key);
                refreshDevKeyList();
            });

            row.addView(info);
            row.addView(stopBtn);
            devListContainer.addView(row);
        }
    }

    private void startShizukuAndOverlay() {
        if (!ShizukuShell.isShizukuReady()) {
            Toast.makeText(this, "Shizuku terputus! Nyalakan kembali.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            Toast.makeText(this, "Izinkan Overlay DANZSTRAIGHT lalu klik lagi.", Toast.LENGTH_LONG).show();
            return;
        }

        ShizukuShell.startUid2000Daemon(this);
        startService(new Intent(this, OverlayService.class));

        Intent ffIntent = getPackageManager().getLaunchIntentForPackage("com.dts.freefireth");
        if (ffIntent == null) {
            ffIntent = getPackageManager().getLaunchIntentForPackage("com.dts.freefiremax");
        }
        if (ffIntent != null) {
            startActivity(ffIntent);
        } else {
            Toast.makeText(this, "Overlay DANZSTRAIGHT Aktif! Buka Free Fire.", Toast.LENGTH_SHORT).show();
        }
    }
}
