package com.danz.straight;

import android.content.Context;
import android.content.pm.PackageManager;
import java.io.File;
import java.lang.reflect.Method;
import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuRemoteProcess;

public final class ShizukuShell {

    public static boolean isShizukuReady() {
        try {
            if (!Shizuku.pingBinder()) return false;
            return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void requestPermission(int requestCode) {
        try {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(requestCode);
            }
        } catch (Throwable ignored) {}
    }

    public static boolean startUid2000Daemon(Context ctx) {
        if (!isShizukuReady()) return false;
        try {
            String nativeDir = ctx.getApplicationInfo().nativeLibraryDir;
            File daemonBin = new File(nativeDir, "libdanz_daemon.so");
            String cmd = daemonBin.getAbsolutePath() + " >/dev/null 2>&1 &";

            Method newProcess = Shizuku.class.getDeclaredMethod(
                    "newProcess", String[].class, String[].class, String.class);
            newProcess.setAccessible(true);
            ShizukuRemoteProcess proc = (ShizukuRemoteProcess) newProcess.invoke(
                    null, new String[]{"sh", "-c", cmd}, null, null);
            if (proc != null) {
                proc.waitFor();
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static void stopUid2000Daemon() {
        NativeEngine.sendCommand(99, 0);
    }
}
