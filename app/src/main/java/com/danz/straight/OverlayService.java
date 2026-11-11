package com.danz.straight;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class OverlayService extends Service {
    private WindowManager wm;
    private View floatingMenu;
    private View crosshairView;

    private static final class CrosshairCanvas extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public CrosshairCanvas(Context ctx) {
            super(ctx);
            paint.setColor(Color.parseColor("#00E5FF"));
            paint.setStrokeWidth(3.5f);
            paint.setStyle(Paint.Style.STROKE);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() * 0.5f;
            float cy = getHeight() * 0.5f;
            canvas.drawLine(cx - 24f, cy, cx - 6f, cy, paint);
            canvas.drawLine(cx + 6f, cy, cx + 24f, cy, paint);
            canvas.drawLine(cx, cy - 24f, cx, cy - 6f, paint);
            canvas.drawLine(cx, cy + 6f, cx, cy + 24f, paint);
            canvas.drawCircle(cx, cy, 2.5f, paint);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        buildCrosshairOverlay();
        buildDanzStraightFloatingMenu();
    }

    private void buildCrosshairOverlay() {
        crosshairView = new CrosshairCanvas(this);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                120, 120,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        lp.gravity = Gravity.CENTER;
        crosshairView.setVisibility(View.GONE);
        wm.addView(crosshairView, lp);
    }

    private void buildDanzStraightFloatingMenu() {
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 50;
        params.y = 120;

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundColor(Color.argb(235, 10, 12, 18));
        panel.setPadding(32, 24, 32, 24);

        TextView dragHeader = new TextView(this);
        dragHeader.setText("≡ DANZSTRAIGHT [SHIZUKU UID 2000]");
        dragHeader.setTextColor(Color.parseColor("#00E5FF"));
        dragHeader.setTextSize(14f);

        final LinearLayout contentBox = new LinearLayout(this);
        contentBox.setOrientation(LinearLayout.VERTICAL);

        contentBox.addView(makeShizukuToggle("1. Aim Legit", 1));
        contentBox.addView(makeShizukuToggle("2. Aimtrick", 2));
        contentBox.addView(makeShizukuToggle("3. Headtrik", 3));
        contentBox.addView(makeShizukuToggle("4. Smooth Aim", 4));

        final TextView sensLabel = new TextView(this);
        sensLabel.setText("5. Sensitivity Game Lab: Level 1");
        sensLabel.setTextColor(Color.WHITE);
        SeekBar sensBar = new SeekBar(this);
        sensBar.setMax(6);
        sensBar.setProgress(0);
        sensBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int lvl = progress + 1;
                sensLabel.setText("5. Sensitivity Game Lab: Level " + lvl);
                NativeEngine.sendCommand(5, lvl);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        contentBox.addView(sensLabel);
        contentBox.addView(sensBar);

        contentBox.addView(makeShizukuToggle("6. Game Space Grafik MC", 6));
        contentBox.addView(makeShizukuToggle("7. Boost Performance (CPU/GPU Max)", 7));

        CheckBox crossToggle = new CheckBox(this);
        crossToggle.setText("8. Crosshair In-Game");
        crossToggle.setTextColor(Color.WHITE);
        crossToggle.setOnCheckedChangeListener((btn, isChecked) ->
                crosshairView.setVisibility(isChecked ? View.VISIBLE : View.GONE));
        contentBox.addView(crossToggle);

        Button minBtn = new Button(this);
        minBtn.setText("HIDE / SHOW MENU");
        minBtn.setOnClickListener(v ->
                contentBox.setVisibility(contentBox.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));

        dragHeader.setOnTouchListener(new View.OnTouchListener() {
            private int initX, initY;
            private float touchX, touchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initX = params.x;
                        initY = params.y;
                        touchX = event.getRawX();
                        touchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initX + (int) (event.getRawX() - touchX);
                        params.y = initY + (int) (event.getRawY() - touchY);
                        wm.updateViewLayout(floatingMenu, params);
                        return true;
                }
                return false;
            }
        });

        panel.addView(dragHeader);
        panel.addView(minBtn);
        panel.addView(contentBox);

        floatingMenu = panel;
        wm.addView(floatingMenu, params);
    }

    private CheckBox makeShizukuToggle(String title, final int cmdId) {
        CheckBox cb = new CheckBox(this);
        cb.setText(title);
        cb.setTextColor(Color.WHITE);
        cb.setOnCheckedChangeListener((btn, isChecked) ->
                NativeEngine.sendCommand(cmdId, isChecked ? 1 : 0));
        return cb;
    }

    @Override
    public void onDestroy() {
        ShizukuShell.stopUid2000Daemon();
        if (floatingMenu != null) wm.removeView(floatingMenu);
        if (crosshairView != null) wm.removeView(crosshairView);
        super.onDestroy();
    }
}
