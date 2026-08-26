package org.dpdns.nlt.app.xweb;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class WebFragment extends Fragment {

    private static final String ARG_URL = "url";
    private WebView webView;
    private String targetUrl;
    private OnWebEventListener listener;

    public interface OnWebEventListener {
        void onTitleAndUrlChanged(String title, String url);

        void onNewTabRequested(String url);

        void onProgressChanged(int progress);
    }

    public void setOnWebEventListener(OnWebEventListener listener) {
        this.listener = listener;
    }

    public static WebFragment newInstance(String url) {
        WebFragment fragment = new WebFragment();
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            targetUrl = getArguments().getString(ARG_URL);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable
                    ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_web, container, false);
        webView = root.findViewById(R.id.fragment_webview);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);

        SharedPreferences prefs = requireContext().getSharedPreferences(App.PREFS_NAME, Context.MODE_PRIVATE);
        String customUA = prefs.getString(App.KEY_USER_AGENT, null);
        if (customUA != null && !customUA.isEmpty()) {
            settings.setUserAgentString(customUA);
        }

        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request != null && request.getUrl() != null) {
                    return handleCustomScheme(request.getUrl().toString(), view);
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (listener != null && isAdded()) {
                    listener.onTitleAndUrlChanged(view.getTitle(), url);
                }
            }

            @Override
            public void onFormResubmission(WebView view, Message dontResend, Message resend) {
                resend.sendToTarget();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);

                if (request != null && request.isForMainFrame()) {
                    CharSequence description = error != null ? error.getDescription() : null;
                    if (description != null && description.toString().contains("ERR_CACHE_MISS")) {
                        view.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
                        view.reload();
                    }
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (listener != null && isAdded()) {
                    listener.onProgressChanged(newProgress);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                if (listener != null && isAdded()) {
                    listener.onTitleAndUrlChanged(title, view.getUrl());
                }
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                WebView.HitTestResult result = view.getHitTestResult();
                String data = result.getExtra();

                if (data != null && listener != null && isAdded()) {
                    listener.onNewTabRequested(data);
                    return true;
                }

                WebView tempWebView = new WebView(requireContext());
                tempWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
                        if (req != null && req.getUrl() != null) {
                            String url = req.getUrl().toString();
                            if (handleCustomScheme(url, view)) {
                                tempWebView.destroy();
                                return true;
                            }
                            if (listener != null && isAdded()) {
                                listener.onNewTabRequested(url);
                            }
                        }
                        tempWebView.destroy();
                        return true;
                    }
                });

                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(tempWebView);
                resultMsg.sendToTarget();
                return true;
            }
        });

        if (savedInstanceState == null && targetUrl != null) {
            webView.loadUrl(targetUrl);
        } else {
            webView.restoreState(savedInstanceState);
        }

        return root;
    }

    private boolean handleCustomScheme(String url, WebView view) {
        if (url == null) return false;

        if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("about:blank")) {
            return false;
        }

        boolean interceptScheme = App.isInterceptCustomScheme(requireContext());

        if (interceptScheme) {
            try {
                Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                if (fallbackUrl != null && !fallbackUrl.isEmpty()) {
                    view.loadUrl(fallbackUrl);
                    return true;
                }
            } catch (Exception ignored) {
            }

            return true;
        }

        try {
            Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.setComponent(null);
            intent.setSelector(null);

            if (requireContext().getPackageManager().resolveActivity(intent, 0) != null) {
                startActivity(intent);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public void reload() {
        if (webView != null) {
            webView.reload();
        }
    }

    public boolean canGoBack() {
        return webView != null && webView.canGoBack();
    }

    public void goBack() {
        if (webView != null && webView.canGoBack()) {
            boolean noReload = App.isNoReloadOnBack(requireContext());

            if (noReload) {
                webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                webView.goBack();
                webView.postDelayed(() -> {
                    if (webView != null) {
                        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
                    }
                }, 1000);
            } else {
                webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
                webView.goBack();
            }
        }
    }

    public String getCurrentUrl() {
        return webView != null ? webView.getUrl() : targetUrl;
    }

    public String getCurrentTitle() {
        return webView != null ? webView.getTitle() : "新标签页";
    }
}
