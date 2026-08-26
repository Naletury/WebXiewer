package org.dpdns.nlt.app.xweb;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import com.google.android.material.color.DynamicColors;

public class App extends Application {
    public static final String PREFS_NAME = "app_settings";

    public static final String KEY_DYNAMIC_COLOR = "dynamic_color";
    public static final String KEY_NIGHT_MODE = "night_mode";
    public static final String KEY_INTERCEPT_BACK = "back_intercept";
    public static final String KEY_INTERCEPT_CUSTOM_SCHEME = "custom_scheme";
    public static final String KEY_SAVE_STATE_ON_EXIT = "save_state";
    public static final String KEY_SAVED_TAB_URLS = "saved_tab_urls";
    public static final String KEY_NO_RELOAD_ON_BACK = "no_reload_on_back";
    public static final String KEY_USER_AGENT = "user_agent";

    public static final boolean DEFAULT_INTERCEPT_CUSTOM_SCHEME = true;
    public static final boolean DEFAULT_INTERCEPT_BACK = true;
    public static final boolean DEFAULT_SAVE_STATE_ON_EXIT = false;
    public static final boolean DEFAULT_DYNAMIC_COLOR = true;
    public static final int DEFAULT_NIGHT_MODE = 0;
    public static final boolean DEFAULT_NO_RELOAD_ON_BACK = true;

    @Override
    public void onCreate() {
        super.onCreate();

        if (isDynamicColor(this)) {
            DynamicColors.applyToActivitiesIfAvailable(this);
        }

        SettingsActivity.applyAppTheme(getNightMode(this));
    }

    public static boolean isInterceptCustomScheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_INTERCEPT_CUSTOM_SCHEME, DEFAULT_INTERCEPT_CUSTOM_SCHEME);
    }

    public static boolean isNoReloadOnBack(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_NO_RELOAD_ON_BACK, DEFAULT_NO_RELOAD_ON_BACK);
    }

    public static boolean isInterceptBack(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_INTERCEPT_BACK, DEFAULT_INTERCEPT_BACK);
    }

    public static boolean isSaveStateOnExit(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_SAVE_STATE_ON_EXIT, DEFAULT_SAVE_STATE_ON_EXIT);
    }

    public static boolean isDynamicColor(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_DYNAMIC_COLOR, DEFAULT_DYNAMIC_COLOR);
    }

    public static int getNightMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getInt(KEY_NIGHT_MODE, DEFAULT_NIGHT_MODE);
    }
}
