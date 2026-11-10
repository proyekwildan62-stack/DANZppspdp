package com.danz.straight;

public final class NativeEngine {
    static {
        System.loadLibrary("danzengine");
    }

    public static native boolean sendCommand(int cmd, int value);
}
