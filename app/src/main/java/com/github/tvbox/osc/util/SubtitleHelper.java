package com.github.tvbox.osc.util;

import androidx.appcompat.app.AppCompatActivity;

import com.orhanobut.hawk.Hawk;

public class SubtitleHelper {

    public static int getSubtitleTextAutoSize(AppCompatActivity activity) {
        double screenSqrt = ScreenUtils.getSqrt(activity);
        int subtitleTextSize = 20;
        if (screenSqrt > 7.0 && screenSqrt <= 13.0) {
            subtitleTextSize = 24;
        } else if (screenSqrt > 13.0 && screenSqrt <= 50.0) {
            subtitleTextSize = 36;
        } else if (screenSqrt > 50.0) {
            subtitleTextSize = 46;
        }
        return subtitleTextSize;
    }

    public static int getTextSize(AppCompatActivity activity) {
        int autoSize = getSubtitleTextAutoSize(activity);
        int subtitleConfigSize = Hawk.get(HawkConfig.SUBTITLE_TEXT_SIZE, autoSize);
        return subtitleConfigSize;
    }

    public static void setTextSize(int size) {
        Hawk.put(HawkConfig.SUBTITLE_TEXT_SIZE, size);
    }

    public static int getTimeDelay() {
        int subtitleConfigTimeDelay = Hawk.get(HawkConfig.SUBTITLE_TIME_DELAY, 0);
        return subtitleConfigTimeDelay;
    }

    public static void setTimeDelay(int delay) {
        Hawk.put(HawkConfig.SUBTITLE_TIME_DELAY, delay);
    }

}