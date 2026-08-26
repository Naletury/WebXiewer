package org.dpdns.nlt.app.xweb;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private TextView tvPageTitle;
    private TextView tvPageUrl;
    private EditText etUrlInput;
    private LinearLayout layoutTitleBar;
    private LinearLayout layoutEditBar;
    private ImageButton btnReload;
    private ImageButton btnAddTab;
    private ImageButton btnEnter;
    private ImageButton btnSettings;
    private LinearProgressIndicator progressBar;
    private BrowserPagerAdapter pagerAdapter;
    private TabLayoutMediator tabLayoutMediator;
    private long lastBackPressedTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int nightMode = App.getNightMode(this);
        SettingsActivity.applyAppTheme(nightMode);

        if (DynamicColors.isDynamicColorAvailable() && App.isDynamicColor(this)) {
            DynamicColors.applyToActivityIfAvailable(this);
        }

        super.onCreate(savedInstanceState);
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.view_pager);
        tabLayout = findViewById(R.id.tabs);
        tvPageTitle = findViewById(R.id.tv_page_title);
        tvPageUrl = findViewById(R.id.tv_page_url);
        etUrlInput = findViewById(R.id.et_url_input);
        layoutTitleBar = findViewById(R.id.layout_title_bar);
        layoutEditBar = findViewById(R.id.layout_edit_bar);
        btnReload = findViewById(R.id.btn_reload);
        btnAddTab = findViewById(R.id.btn_add_tab);
        btnEnter = findViewById(R.id.btn_enter);
        btnSettings = findViewById(R.id.btn_settings);
        progressBar = findViewById(R.id.progress_bar);

        pagerAdapter = new BrowserPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        layoutTitleBar.setOnClickListener(v -> {
            int currentPos = viewPager.getCurrentItem();
            WebFragment currentFragment = pagerAdapter.getFragment(currentPos);
            if (currentFragment != null) {
                String url = currentFragment.getCurrentUrl();
                if (url != null && !url.isEmpty()) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("URL", url);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(this, "已复制网址到剪贴板", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        btnReload.setOnClickListener(v -> reloadCurrentTab());
        btnAddTab.setOnClickListener(v -> showEditMode());
        btnEnter.setOnClickListener(v -> performNavigation());

        etUrlInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                performNavigation();
                return true;
            }
            return false;
        });

        btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateTopBarInfo(position);
                showTitleMode();
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (layoutEditBar.getVisibility() == View.VISIBLE) {
                    showTitleMode();
                    return;
                }

                int currentItem = viewPager.getCurrentItem();
                WebFragment currentFragment = pagerAdapter.getFragment(currentItem);

                boolean shouldIntercept = App.isInterceptBack(MainActivity.this);

                if (shouldIntercept && currentFragment != null && currentFragment.canGoBack()) {
                    currentFragment.goBack();
                } else if (currentItem > 0) {
                    viewPager.setCurrentItem(currentItem - 1, true);
                } else {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastBackPressedTime < 2000) {
                        setEnabled(false);
                        getOnBackPressedDispatcher().onBackPressed();
                        setEnabled(true);
                    } else {
                        lastBackPressedTime = currentTime;
                        Toast.makeText(MainActivity.this, "再按一次退出应用", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        restoreTabsState();
        attachTabLayout();
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveTabsState();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (layoutEditBar != null && layoutEditBar.getVisibility() == View.VISIBLE) {
                Rect rect = new Rect();
                layoutEditBar.getGlobalVisibleRect(rect);
                if (!rect.contains((int) ev.getX(), (int) ev.getY())) {
                    showTitleMode();
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void reloadCurrentTab() {
        int currentPos = viewPager.getCurrentItem();
        WebFragment currentFragment = pagerAdapter.getFragment(currentPos);
        if (currentFragment != null) {
            currentFragment.reload();
        }
    }

    private void showEditMode() {
        etUrlInput.setText("");
        layoutTitleBar.setVisibility(View.GONE);
        layoutEditBar.setVisibility(View.VISIBLE);
        etUrlInput.requestFocus();

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(etUrlInput, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void showTitleMode() {
        layoutEditBar.setVisibility(View.GONE);
        layoutTitleBar.setVisibility(View.VISIBLE);

        etUrlInput.clearFocus();

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            View view = getCurrentFocus();
            if (view == null) {
                view = etUrlInput;
            }
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void performNavigation() {
        String url = etUrlInput.getText().toString().trim();
        if (url.isEmpty()) {
            showTitleMode();
            return;
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        addNewTab(url);
        showTitleMode();
    }

    private void closeTabAt(int position) {
        if (pagerAdapter.getItemCount() <= 1) {
            finish();
            return;
        }

        pagerAdapter.removeFragment(position);
        int nextPos = Math.min(position, pagerAdapter.getItemCount() - 1);
        viewPager.setCurrentItem(nextPos, false);
        attachTabLayout();
        updateTopBarInfo(nextPos);
    }

    private void closeOtherTabs(int keepPosition) {
        WebFragment keepFragment = pagerAdapter.getFragment(keepPosition);
        if (keepFragment == null) return;

        for (int i = pagerAdapter.getItemCount() - 1; i >= 0; i--) {
            if (i != keepPosition) {
                pagerAdapter.removeFragment(i);
            }
        }
        attachTabLayout();
        viewPager.setCurrentItem(0, false);
        updateTopBarInfo(0);
    }

    private void addNewTab(String url) {
        WebFragment fragment = WebFragment.newInstance(url);
        fragment.setOnWebEventListener(new WebFragment.OnWebEventListener() {
            @Override
            public void onTitleAndUrlChanged(String title, String currentUrl) {
                int currentPos = viewPager.getCurrentItem();
                if (pagerAdapter.getFragment(currentPos) == fragment) {
                    tvPageTitle.setText(title != null ? title : "X");
                    tvPageUrl.setText(currentUrl);
                }
                int pos = getFragmentPosition(fragment);
                if (pos != -1 && tabLayout.getTabAt(pos) != null) {
                    tabLayout.getTabAt(pos).setText(title != null && !title.isEmpty() ? title : "标签页");
                }
            }

            @Override
            public void onNewTabRequested(String newUrl) {
                addNewTab(newUrl);
            }

            @Override
            public void onProgressChanged(int progress) {
                int currentPos = viewPager.getCurrentItem();
                if (pagerAdapter.getFragment(currentPos) == fragment) {
                    if (progress < 100) {
                        progressBar.animate().cancel();
                        progressBar.setAlpha(1.0f);
                        progressBar.setVisibility(View.VISIBLE);
                        progressBar.setProgress(progress, true);
                    } else {
                        progressBar.setProgress(100, true);
                        progressBar.animate()
                                .alpha(0.0f)
                                .setDuration(300)
                                .withEndAction(() -> {
                                    progressBar.setVisibility(View.GONE);
                                    progressBar.setAlpha(1.0f);
                                })
                                .start();
                    }
                }
            }
        });

        pagerAdapter.addFragment(fragment);
        attachTabLayout();
        viewPager.setCurrentItem(pagerAdapter.getItemCount() - 1, true);
    }

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private void attachTabLayout() {
        if (tabLayoutMediator != null) {
            tabLayoutMediator.detach();
        }
        tabLayoutMediator = new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            WebFragment frag = pagerAdapter.getFragment(position);
            tab.setText(frag != null ? frag.getCurrentTitle() : "标签 " + (position + 1));
        });
        tabLayoutMediator.attach();

        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            final int pos = i;
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if (tab != null && tab.view != null) {
                final float[] touchPoint = new float[2];

                tab.view.setOnTouchListener((v, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        touchPoint[0] = event.getX();
                        touchPoint[1] = event.getY();
                    }
                    return false;
                });

                tab.view.setOnClickListener(v -> viewPager.setCurrentItem(pos, true));
                tab.view.setOnLongClickListener(v -> {
                    showTabContextMenu(v, pos, touchPoint[0], touchPoint[1]);
                    return true;
                });
            }
        }
    }

    private void showTabContextMenu(View anchor, int position, float touchX, float touchY) {
        androidx.appcompat.widget.ListPopupWindow popupWindow = new androidx.appcompat.widget.ListPopupWindow(this);

        java.util.List<String> options = new java.util.ArrayList<>();
        options.add("关闭此标签");
        if (pagerAdapter.getItemCount() > 1) {
            options.add("关闭其他标签");
        }

        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
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
            if (itemPosition == 0) {
                closeTabAt(position);
            } else if (itemPosition == 1) {
                closeOtherTabs(position);
            }
        });

        popupWindow.show();
    }

    private void updateTopBarInfo(int position) {
        WebFragment frag = pagerAdapter.getFragment(position);
        if (frag != null) {
            tvPageTitle.setText(frag.getCurrentTitle());
            tvPageUrl.setText(frag.getCurrentUrl());
        }
    }

    private int getFragmentPosition(WebFragment fragment) {
        for (int i = 0; i < pagerAdapter.getItemCount(); i++) {
            if (pagerAdapter.getFragment(i) == fragment) {
                return i;
            }
        }
        return -1;
    }

    private void saveTabsState() {
        boolean saveState = App.isSaveStateOnExit(this);

        SharedPreferences prefs = getSharedPreferences(App.PREFS_NAME, MODE_PRIVATE);
        if (!saveState) {
            prefs.edit().remove(App.KEY_SAVED_TAB_URLS).apply();
            return;
        }

        StringBuilder urls = new StringBuilder();
        for (int i = 0; i < pagerAdapter.getItemCount(); i++) {
            WebFragment frag = pagerAdapter.getFragment(i);
            if (frag != null && frag.getCurrentUrl() != null) {
                urls.append(frag.getCurrentUrl()).append(",");
            }
        }
        prefs.edit().putString(App.KEY_SAVED_TAB_URLS, urls.toString()).apply();
    }

    private void restoreTabsState() {
        boolean saveState = App.isSaveStateOnExit(this);

        SharedPreferences prefs = getSharedPreferences(App.PREFS_NAME, MODE_PRIVATE);
        String savedUrls = prefs.getString(App.KEY_SAVED_TAB_URLS, null);

        if (saveState && savedUrls != null && !savedUrls.isEmpty()) {
            String[] urlArray = savedUrls.split(",");
            for (String url : urlArray) {
                if (!url.trim().isEmpty()) {
                    addNewTab(url.trim());
                }
            }
        }

        if (pagerAdapter.getItemCount() == 0) {
            addNewTab("https://x.com");
        }
    }
}
