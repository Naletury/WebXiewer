package org.dpdns.nlt.app.xweb;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.WindowCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "app_settings";

    public static final String KEY_DYNAMIC_COLOR = "dynamic_color";
    public static final String KEY_NIGHT_MODE = "night_mode";
    public static final String KEY_INTERCEPT_BACK = "back_intercept";
    public static final String KEY_INTERCEPT_CUSTOM_SCHEME = "custom_scheme";
    public static final String KEY_SAVE_STATE_ON_EXIT = "save_state";
    public static final String KEY_SAVED_TAB_URLS = "saved_tab_urls";
    public static final String KEY_NO_RELOAD_ON_BACK = "no_reload_on_back";
    public static final String KEY_USER_AGENT = "user_agent";

    private static final String UA_CHROME_DESKTOP = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final String UA_SAFARI_IPHONE = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1";
    private static final String UA_CHROME_MOBILE = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";
    private static final String UA_TWITTER_APP = "Mozilla/5.0 (Linux; Android 13; Pixel 7 Build/TQ3A.230901.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/120.0.0.0 Mobile Safari/537.36 TwitterAndroid";

    private MaterialSwitch switchDynamicColor;
    private MaterialSwitch switchBackIntercept;
    private MaterialSwitch switchCustomScheme;
    private MaterialSwitch switchSaveState;
    private MaterialSwitch switchNoReloadOnBack;

    private LinearLayout layoutNightMode;
    private TextView textNightModeSummary;
    private LinearLayout layoutClearCache;
    private LinearLayout layoutClearCookies;
    private LinearLayout layoutClearData;
    private LinearLayout layoutUaSetting;

    private boolean isUpdatingSwitch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        int nightMode = prefs.getInt(KEY_NIGHT_MODE, 0);
        applyAppTheme(nightMode);

        if (DynamicColors.isDynamicColorAvailable() && prefs.getBoolean(KEY_DYNAMIC_COLOR, true)) {
            DynamicColors.applyToActivityIfAvailable(this);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initViews();
        bindDataAndListeners(prefs);
    }

    private void initViews() {
        switchDynamicColor = findViewById(R.id.switch_dynamic_color);
        switchBackIntercept = findViewById(R.id.switch_back_intercept);
        switchCustomScheme = findViewById(R.id.switch_custom_scheme);
        switchSaveState = findViewById(R.id.switch_save_state);
        switchNoReloadOnBack = findViewById(R.id.switch_no_reload_on_back);

        layoutNightMode = findViewById(R.id.layout_night_mode);
        textNightModeSummary = findViewById(R.id.text_night_mode_summary);
        layoutClearCache = findViewById(R.id.layout_clear_cache);
        layoutClearCookies = findViewById(R.id.layout_clear_cookies);
        layoutClearData = findViewById(R.id.layout_clear_data);
        layoutUaSetting = findViewById(R.id.layout_ua_setting);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void bindDataAndListeners(SharedPreferences prefs) {
        isUpdatingSwitch = true;
        switchDynamicColor.setChecked(prefs.getBoolean(KEY_DYNAMIC_COLOR, true));
        switchBackIntercept.setChecked(prefs.getBoolean(KEY_INTERCEPT_BACK, true));
        switchCustomScheme.setChecked(prefs.getBoolean(KEY_INTERCEPT_CUSTOM_SCHEME, true));
        
        switchSaveState.setChecked(prefs.getBoolean(KEY_SAVE_STATE_ON_EXIT, false));
        
        switchNoReloadOnBack.setChecked(prefs.getBoolean(KEY_NO_RELOAD_ON_BACK, true));
        isUpdatingSwitch = false;

        updateNightModeSummary(prefs.getInt(KEY_NIGHT_MODE, 0));

        switchDynamicColor.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingSwitch) return;

            prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, isChecked).apply();

            showRestartDialog(
                    this::triggerAppRestart,
                    null
            );
        });

        switchBackIntercept.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(KEY_INTERCEPT_BACK, isChecked).apply());

        switchCustomScheme.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(KEY_INTERCEPT_CUSTOM_SCHEME, isChecked).apply());

        switchSaveState.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(KEY_SAVE_STATE_ON_EXIT, isChecked).apply());

        switchNoReloadOnBack.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(KEY_NO_RELOAD_ON_BACK, isChecked).apply());

        final float[] touchPoint = new float[2];
        
        layoutNightMode.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                touchPoint[0] = event.getX();
                touchPoint[1] = event.getY();
            }
            return false;
        });

        layoutNightMode.setOnClickListener(v -> showNightModeContextMenu(v, prefs, touchPoint[0], touchPoint[1]));

        layoutClearCache.setOnClickListener(v -> showConfirmDialog(
                "清除缓存", 
                "确定要清理网页产生的临时缓存文件吗？",
                () -> {
                    new WebView(this).clearCache(true);
                    Toast.makeText(this, "缓存已清理", Toast.LENGTH_SHORT).show();
                }
        ));

        layoutClearCookies.setOnClickListener(v -> showConfirmDialog(
                "清除 Cookies", 
                "确定要清理所有登录状态和 Cookie 凭据吗？",
                () -> {
                    CookieManager.getInstance().removeAllCookies(null);
                    CookieManager.getInstance().flush();
                    Toast.makeText(this, "Cookies 已清理", Toast.LENGTH_SHORT).show();
                }
        ));

        layoutClearData.setOnClickListener(v -> showConfirmDialog(
                "清除所有数据", 
                "此操作将清理缓存、Cookies 及本地存储，该操作不可逆！",
                () -> {
                    new WebView(this).clearCache(true);
                    CookieManager.getInstance().removeAllCookies(null);
                    CookieManager.getInstance().flush();
                    WebStorage.getInstance().deleteAllData();
                    Toast.makeText(this, "所有数据已清理", Toast.LENGTH_SHORT).show();
                }
        ));

        layoutUaSetting.setOnClickListener(v -> showUaSettingDialog(prefs));
    }

    private void showNightModeContextMenu(View anchor, SharedPreferences prefs, float touchX, float touchY) {
        ListPopupWindow popupWindow = new ListPopupWindow(this);

        List<String> options = new ArrayList<>();
        options.add("跟随系统");
        options.add("浅色模式");
        options.add("深色模式");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                options
        );
        popupWindow.setAdapter(adapter);

        popupWindow.setAnchorView(anchor);
        popupWindow.setHorizontalOffset((int) touchX);
        popupWindow.setVerticalOffset((int) touchY - anchor.getHeight());

        popupWindow.setContentWidth(450);
        popupWindow.setModal(true);

        popupWindow.setOnItemClickListener((parent, view, itemPosition, id) -> {
            popupWindow.dismiss();

            int selectedMode = itemPosition;
            int originalMode = prefs.getInt(KEY_NIGHT_MODE, 0);

            if (selectedMode != originalMode) {
                prefs.edit().putInt(KEY_NIGHT_MODE, selectedMode).apply();
                updateNightModeSummary(selectedMode);

                showRestartDialog(
                        this::triggerAppRestart,
                        null
                );
            }
        });

        popupWindow.show();
    }

    private void showRestartDialog(Runnable onRestart, Runnable onCancel) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("需要重启应用")
                .setMessage("设置已修改，需要重新启动应用以使效果完全生效。是否立即重启？")
                .setPositiveButton("立即重启", (dialog, which) -> {
                    if (onRestart != null) onRestart.run();
                })
                .setNegativeButton("稍后", (dialog, which) -> {
                    if (onCancel != null) onCancel.run();
                })
                .setOnCancelListener(dialog -> {
                    if (onCancel != null) onCancel.run();
                })
                .show();
    }
    
    private void triggerAppRestart() {
        Intent intent = getBaseContext().getPackageManager()
                .getLaunchIntentForPackage(getBaseContext().getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            Runtime.getRuntime().exit(0);
        }
    }

    private void showConfirmDialog(String title, String message, Runnable onConfirm) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("确定", (dialog, which) -> {
                    if (onConfirm != null) onConfirm.run();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showUaSettingDialog(SharedPreferences prefs) {
        String defaultSystemUa;
        try {
            defaultSystemUa = WebSettings.getDefaultUserAgent(this);
        } catch (Exception e) {
            defaultSystemUa = UA_CHROME_MOBILE;
        }

        String currentUa = prefs.getString(KEY_USER_AGENT, defaultSystemUa);
        if (TextUtils.isEmpty(currentUa)) {
            currentUa = defaultSystemUa;
        }

        final EditText input = new EditText(this);
        input.setText(currentUa);
        input.setSelection(currentUa.length());

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        int margin = (int) (16 * getResources().getDisplayMetrics().density);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        lp.setMargins(margin, 0, margin, 0);
        input.setLayoutParams(lp);
        container.addView(input);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("设置 User-Agent")
                .setView(container)
                .setPositiveButton("保存", (d, which) -> {
                    String newUa = input.getText().toString().trim();
                    prefs.edit().putString(KEY_USER_AGENT, newUa).apply();
                    Toast.makeText(this, "User-Agent 已保存", Toast.LENGTH_SHORT).show();
                    showRestartDialog(this::triggerAppRestart, null);
                })
                .setNegativeButton("取消", null)
                .setNeutralButton("使用模板", null)
                .create();

        dialog.show();

        View neutralBtn = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
        if (neutralBtn != null) {
            neutralBtn.setOnClickListener(v -> {
                PopupMenu templateMenu = new PopupMenu(this, v);
                templateMenu.getMenu().add(0, 0, 0, "桌面版 Chrome");
                templateMenu.getMenu().add(0, 1, 1, "iPhone Safari");
                templateMenu.getMenu().add(0, 2, 2, "移动版 Chrome");
                templateMenu.getMenu().add(0, 3, 3, "Twitter Android 原生");
                templateMenu.getMenu().add(0, 4, 4, "恢复系统默认");

                templateMenu.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        case 0:
                            input.setText(UA_CHROME_DESKTOP);
                            break;
                        case 1:
                            input.setText(UA_SAFARI_IPHONE);
                            break;
                        case 2:
                            input.setText(UA_CHROME_MOBILE);
                            break;
                        case 3:
                            input.setText(UA_TWITTER_APP);
                            break;
                        case 4:
                            try {
                                input.setText(WebSettings.getDefaultUserAgent(this));
                            } catch (Exception ignored) {
                                input.setText(UA_CHROME_MOBILE);
                            }
                            break;
                    }
                    input.setSelection(input.getText().length());
                    return true;
                });
                templateMenu.show();
            });
        }
    }

    private void updateNightModeSummary(int mode) {
        switch (mode) {
            case 1:
                textNightModeSummary.setText("浅色模式");
                break;
            case 2:
                textNightModeSummary.setText("深色模式");
                break;
            case 0:
            default:
                textNightModeSummary.setText("跟随系统");
                break;
        }
    }

    public static void applyAppTheme(int mode) {
        switch (mode) {
            case 1:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 2:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case 0:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
