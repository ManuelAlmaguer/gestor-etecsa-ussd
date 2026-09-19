package com.manu.etecsaussd;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.webkit.WebViewAssetLoader;

import com.manu.etecsaussd.sync.EtecsaSyncWorker;
import com.manu.etecsaussd.web.EtecsaJsBridge;

public final class MainActivity extends ComponentActivity {
    private static final int PHONE_PERMISSION_REQUEST = 7001;
    private static final String LOCAL_ASSET_URL = "https://appassets.androidplatform.net/assets/index.html";

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);
        configureWebView(webView);

        EtecsaApplication application = (EtecsaApplication) getApplication();
        webView.addJavascriptInterface(
                new EtecsaJsBridge(webView, application.getRepository(), application.getUssdExecutor()),
                "AndroidBridge"
        );
        webView.loadUrl(LOCAL_ASSET_URL);

        requestPhonePermissionsIfNeeded();
        EtecsaSyncWorker.enqueueDailySync(this);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView(WebView targetWebView) {
        WebSettings settings = targetWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setSafeBrowsingEnabled(true);
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();
        targetWebView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(
                    WebView view,
                    WebResourceRequest request
            ) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            @SuppressWarnings("deprecation")
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                return assetLoader.shouldInterceptRequest(Uri.parse(url));
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("https".equals(uri.getScheme()) && !uri.getHost().equals("appassets.androidplatform.net")) {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    return true;
                }
                return false;
            }
        });
    }

    private void requestPhonePermissionsIfNeeded() {
        boolean callGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED;
        boolean readPhoneGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED;
        if (!callGranted || !readPhoneGranted) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE},
                    PHONE_PERMISSION_REQUEST
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PHONE_PERMISSION_REQUEST && webView != null) {
            webView.post(() -> webView.evaluateJavascript(
                    "window.EtecsaApp && window.EtecsaApp.refreshNativeState && window.EtecsaApp.refreshNativeState();",
                    null
            ));
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.removeJavascriptInterface("AndroidBridge");
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
