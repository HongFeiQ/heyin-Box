package com.whl.quickjs.android;

/**
 * Created by Harlon Wang on 2022/8/12.
 */
public final class QuickJSLoader {
    public static void init() {
        init(true);
    }

    public static void init(Boolean isStdout) {
        System.loadLibrary("hlhwan");
        if (isStdout) startRedirectingStdoutStderr("QuJs ==> ");
    }

    /**
     * Start threads to show stdout and stderr in logcat.
     *
     * @param tag Android Tag
     */
    public native static void startRedirectingStdoutStderr(String tag);

}
