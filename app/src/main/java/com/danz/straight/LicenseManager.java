package com.danz.straight;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import org.json.JSONArray;
import org.json.JSONObject;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public final class LicenseManager {
    private static final String PREF_NAME = "danzstraight_lic_db";
    private static final String KEY_DB    = "keys_table";
    public  static final String DEV_KEY   = "DANZVIP";

    public static final class KeyRecord {
        public final String key;
        public final String boundHwid;
        public final boolean active;

        public KeyRecord(String key, String boundHwid, boolean active) {
            this.key = key;
            this.boundHwid = boundHwid;
            this.active = active;
        }
    }

    private final SharedPreferences prefs;
    private final String deviceHwid;

    public LicenseManager(Context ctx) {
        this.prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String id = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
        this.deviceHwid = (id != null && !id.isEmpty()) ? id : "UNKNOWN_HWID";
    }

    public String getDeviceHwid() {
        return deviceHwid;
    }

    public boolean isDeveloperKey(String input) {
        return DEV_KEY.equals(input != null ? input.trim() : "");
    }

    public synchronized boolean verifyAndBindUserKey(String inputKey) {
        if (inputKey == null || inputKey.trim().isEmpty()) return false;
        String clean = inputKey.trim();
        try {
            JSONArray arr = new JSONArray(prefs.getString(KEY_DB, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                if (clean.equals(obj.getString("key"))) {
                    if (!obj.getBoolean("active")) return false;
                    String bound = obj.optString("hwid", "");
                    if (bound.isEmpty()) {
                        obj.put("hwid", deviceHwid);
                        prefs.edit().putString(KEY_DB, arr.toString()).apply();
                        return true;
                    }
                    return bound.equals(deviceHwid);
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    public synchronized String createNewKey() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder("DANZ-");
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        String newKey = sb.toString();
        try {
            JSONArray arr = new JSONArray(prefs.getString(KEY_DB, "[]"));
            JSONObject obj = new JSONObject();
            obj.put("key", newKey);
            obj.put("hwid", "");
            obj.put("active", true);
            arr.put(obj);
            prefs.edit().putString(KEY_DB, arr.toString()).apply();
        } catch (Exception ignored) {}
        return newKey;
    }

    public synchronized void revokeKey(String targetKey) {
        try {
            JSONArray arr = new JSONArray(prefs.getString(KEY_DB, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                if (targetKey.equals(obj.getString("key"))) {
                    obj.put("active", false);
                    break;
                }
            }
            prefs.edit().putString(KEY_DB, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    public synchronized List<KeyRecord> listKeys() {
        List<KeyRecord> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs.getString(KEY_DB, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                out.add(new KeyRecord(
                    obj.getString("key"),
                    obj.optString("hwid", "UNBOUND"),
                    obj.getBoolean("active")
                ));
            }
        } catch (Exception ignored) {}
        return out;
    }
}
