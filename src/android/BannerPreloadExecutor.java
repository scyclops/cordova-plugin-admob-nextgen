package com.emi.cordova.admob.nextgen;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.libraries.ads.mobile.sdk.banner.AdSize;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdPreloader;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.AdValue;
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration;
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo;

import java.util.Date;

public class BannerPreloadExecutor {

    private static final String TAG = "AdMobBannerPreload";
    private CordovaInterface cordova;
    private CordovaWebView webView;

    private RelativeLayout adLayout;

    private FrameLayout capacitorAdLayout;

    private BannerAd currentBannerAd;

    private boolean isPreloaderActive = false;
    private String currentAdUnitId;

    private String currentPosition = "bottom";
    private boolean isOverlapping = true;
    private boolean isBannerVisible = false;
    private boolean isCapacitor = false;

    private int lastAdHeight = 0;

    private int systemSafeTop = 0;
    private int systemSafeBottom = 0;

    private long lastShowTime = 0;
    private long minShowInterval = 5000; 

    public BannerPreloadExecutor(CordovaInterface cordova, CordovaWebView webView) {
        this.cordova = cordova;
        this.webView = webView;
    }

    public void startPreload(JSONArray args, CallbackContext callbackContext) {
        try {
            JSONObject options = args.getJSONObject(0);
            String adUnitId = options.getString("adUnitId");

            if (isPreloaderActive && adUnitId.equals(currentAdUnitId)) {
                callbackContext.success("Preloader already active");
                return;
            }

            isPreloaderActive = true;
            this.currentAdUnitId = adUnitId;

            if (options.has("position")) this.currentPosition = options.getString("position");
            if (options.has("isOverlapping")) this.isOverlapping = options.getBoolean("isOverlapping");
            if (options.has("isCapacitor")) this.isCapacitor = options.getBoolean("isCapacitor");

            if (options.has("retryInterval")) this.minShowInterval = options.getLong("retryInterval");

            String requestedSize = "ADAPTIVE";
            if (options.has("size")) requestedSize = options.getString("size");

            boolean isCollapsible = false;
            if (options.has("collapsible")) isCollapsible = options.getBoolean("collapsible");

            final String finalSizeStr = requestedSize;
            final boolean finalIsCollapsible = isCollapsible;
            final String finalPosition = this.currentPosition;

            cordova.getActivity().runOnUiThread(() -> {
                Context context = cordova.getActivity();
                AdSize adSize = getAdSize(context, finalSizeStr);

                this.lastAdHeight = adSize.getHeightInPixels(context);

                BannerAdRequest.Builder requestBuilder = new BannerAdRequest.Builder(adUnitId, adSize);

                if (finalIsCollapsible) {
                    Bundle extras = new Bundle();
                    String anchor = "top".equalsIgnoreCase(finalPosition) ? "top" : "bottom";
                    extras.putString("collapsible", anchor);
                    requestBuilder.setGoogleExtrasBundle(extras);
                }

                BannerAdRequest adRequest = requestBuilder.build();
                PreloadConfiguration preloadConfig = new PreloadConfiguration(adRequest);

                PreloadCallback preloadCallback = new PreloadCallback() {
                    @Override
                    public void onAdPreloaded(@NonNull String preloadId, @NonNull ResponseInfo responseInfo) {
                        fireEvent("on.preload.available", null);
                    }
                    @Override
                    public void onAdFailedToPreload(@NonNull String preloadId, @NonNull LoadAdError loadAdError) {
                        try {
                            JSONObject err = new JSONObject();
                            err.put("message", loadAdError.getMessage());
                            fireEvent("on.preload.failed", err);
                        } catch (JSONException ignored) {}
                    }
                    @Override
                    public void onAdsExhausted(@NonNull String preloadId) {
                        fireEvent("on.preload.exhausted", null);
                    }
                };

                BannerAdPreloader.start(adUnitId, preloadConfig, preloadCallback);
                callbackContext.success("Preloader Started");
            });

        } catch (JSONException e) {
            isPreloaderActive = false;
            callbackContext.error(e.getMessage());
        }
    }

    public void showPolledAd(JSONArray args, CallbackContext callbackContext) {
        if (currentAdUnitId == null || currentAdUnitId.isEmpty()) {
            callbackContext.error("Preloader engine is not running.");
            return;
        }

        String newPosition = this.currentPosition;
        boolean newIsOverlapping = this.isOverlapping;

        if (args != null && args.length() > 0) {
            try {
                JSONObject options = args.getJSONObject(0);
                if (options.has("position")) newPosition = options.getString("position");
                if (options.has("isOverlapping")) newIsOverlapping = options.getBoolean("isOverlapping");
                if (options.has("isCapacitor")) this.isCapacitor = options.getBoolean("isCapacitor");
            } catch (JSONException ignored) {}
        }

        if (currentBannerAd != null && isBannerVisible) {
            boolean isSamePos = newPosition.equalsIgnoreCase(this.currentPosition);
            boolean isSameOverlap = (newIsOverlapping == this.isOverlapping);

            if (isSamePos && isSameOverlap) {
                callbackContext.success("Banner Already Visible (Flicker Prevented)");
                return;
            } else {
                this.currentPosition = newPosition;
                this.isOverlapping = newIsOverlapping;

                cordova.getActivity().runOnUiThread(() -> {
                    updateBannerLayout();
                    updateWebViewMargins();
                    callbackContext.success("Banner Repositioned (No Pool Exhaustion)");
                });
                return;
            }
        }

        long currentTime = new Date().getTime();
        if ((currentTime - lastShowTime) < minShowInterval) {
            callbackContext.error("Spam protection active: Please wait " + minShowInterval + "ms.");
            return;
        }

        this.currentPosition = newPosition;
        this.isOverlapping = newIsOverlapping;
        this.lastShowTime = currentTime;

        cordova.getActivity().runOnUiThread(() -> {
            BannerAd ad = BannerAdPreloader.pollAd(currentAdUnitId);

            if (ad == null) {
                callbackContext.error("Pool Empty");
                return;
            }

            destroyCurrentBanner();
            this.currentBannerAd = ad;

            this.lastAdHeight = ad.getAdSize().getHeightInPixels(cordova.getActivity());

            setupAdEvents(ad);
            showBannerView();
            sendLoadedEvent(ad.getAdSize(), ad.isCollapsible());

            callbackContext.success("Ad Shown from Pool");
        });
    }

    private int getScreenHeightInPx() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.view.WindowMetrics windowMetrics = cordova.getActivity().getWindowManager().getCurrentWindowMetrics();
            android.graphics.Insets insets = windowMetrics.getWindowInsets().getInsets(android.view.WindowInsets.Type.systemBars());
            return windowMetrics.getBounds().height() - insets.top - insets.bottom;
        } else {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            cordova.getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            return displayMetrics.heightPixels;
        }
    }

    private int getNavigationBarHeight() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.view.WindowMetrics windowMetrics = cordova.getActivity().getSystemService(WindowManager.class).getCurrentWindowMetrics();
            android.graphics.Insets insets = windowMetrics.getWindowInsets().getInsetsIgnoringVisibility(android.view.WindowInsets.Type.navigationBars());
            return insets.bottom;
        } else {
            @SuppressLint("InternalInsetResource") int resourceId = cordova.getActivity().getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            return resourceId > 0 ? cordova.getActivity().getResources().getDimensionPixelSize(resourceId) : 0;
        }
    }

    private int getStatusBarHeight() {
        @SuppressLint({"DiscouragedApi", "InternalInsetResource"}) int resourceId = cordova.getActivity().getResources().getIdentifier("status_bar_height", "dimen", "android");
        return resourceId > 0 ? cordova.getActivity().getResources().getDimensionPixelSize(resourceId) : 0;
    }

    private boolean isFullScreenMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.view.WindowInsets insets = cordova.getActivity().getWindow().getDecorView().getRootWindowInsets();
            return insets != null && !insets.isVisible(android.view.WindowInsets.Type.statusBars());
        } else {
            return (cordova.getActivity().getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0;
        }
    }

    private void showBannerView() {
        if (currentBannerAd == null) return;
        View adView = currentBannerAd.getView(cordova.getActivity());
        if (adView == null) return;

        isBannerVisible = true;

        if (isCapacitor) {

            if (capacitorAdLayout == null) {
                capacitorAdLayout = new FrameLayout(cordova.getActivity());
                capacitorAdLayout.setTag("emi_banner_preload_layout");
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                );
                ViewGroup decorView = (ViewGroup) cordova.getActivity().getWindow().getDecorView();
                decorView.addView(capacitorAdLayout, params);
            }
            capacitorAdLayout.bringToFront();
            capacitorAdLayout.setVisibility(View.VISIBLE);
        } else {

            if (adLayout == null) {
                adLayout = new RelativeLayout(cordova.getActivity());
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);

                if ("top".equalsIgnoreCase(currentPosition)) {
                    params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                } else {
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                }

                adLayout.setClickable(false);
                adLayout.setFocusable(false);
                cordova.getActivity().addContentView(adLayout, params);
            }
            adLayout.bringToFront();
            adLayout.setVisibility(View.VISIBLE);
        }

        adView.setVisibility(View.VISIBLE);
        updateBannerLayout();
        updateWebViewMargins();
    }

    private void updateBannerLayout() {
        if (currentBannerAd == null) return;
        View adView = currentBannerAd.getView(cordova.getActivity());
        if (adView == null) return;

        if (isCapacitor) {

            if (capacitorAdLayout == null) return;
            if (adView.getParent() != null && adView.getParent() != capacitorAdLayout) {
                ((ViewGroup) adView.getParent()).removeView(adView);
            }

            FrameLayout.LayoutParams bannerParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            );

            int statusBarHeight = getStatusBarHeight();
            boolean isFull = isFullScreenMode();
            int navBarHeight = !isFull ? getNavigationBarHeight() : 0;

            if ("top".equalsIgnoreCase(currentPosition)) {
                bannerParams.gravity = android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL;
                bannerParams.setMargins(0, isFull ? 0 : statusBarHeight, 0, 0);
            } else {
                bannerParams.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL;
                bannerParams.setMargins(0, 0, 0, navBarHeight);
            }

            if (adView.getParent() == null) {
                capacitorAdLayout.addView(adView, bannerParams);
            } else {
                adView.setLayoutParams(bannerParams);
            }
            capacitorAdLayout.requestLayout();

        } else {

            if (adLayout == null) return;

            if (adView.getParent() != null && adView.getParent() != adLayout) {
                ((ViewGroup) adView.getParent()).removeView(adView);
            }

            RelativeLayout.LayoutParams bannerParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);

            bannerParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
            if ("top".equalsIgnoreCase(currentPosition)) {
                bannerParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            } else {
                bannerParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            }

            if (adView.getParent() == null) {
                adLayout.addView(adView, bannerParams);
            } else {
                adView.setLayoutParams(bannerParams);
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                adLayout.setOnApplyWindowInsetsListener((v, insets) -> {

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        android.graphics.Insets sysInsets = insets.getInsets(android.view.WindowInsets.Type.systemBars());
                        systemSafeTop = sysInsets.top;
                        systemSafeBottom = sysInsets.bottom;
                    } else {
                        systemSafeTop = insets.getSystemWindowInsetTop();
                        systemSafeBottom = insets.getSystemWindowInsetBottom();
                    }

                    if (adView.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) adView.getLayoutParams();
                        if ("top".equalsIgnoreCase(currentPosition)) {
                            params.topMargin = systemSafeTop;
                            params.bottomMargin = 0;
                        } else {
                            params.bottomMargin = systemSafeBottom;
                            params.topMargin = 0;
                        }
                        adView.setLayoutParams(params);
                    }

                    updateWebViewMargins();
                    return insets;
                });
                adLayout.requestApplyInsets();
            }
        }
    }

    private void updateWebViewMargins() {
        if (webView == null || webView.getView() == null) return;

        View webViewView = webView.getView();
        ViewGroup.LayoutParams lp = webViewView.getLayoutParams();

        if (isCapacitor) {

            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams capLp = (ViewGroup.MarginLayoutParams) lp;
                int screenHeightInPx = getScreenHeightInPx();
                boolean isFull = isFullScreenMode();

                if (!isBannerVisible || isOverlapping) {
                    capLp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    capLp.topMargin = 0;
                } else {
                    if ("top".equalsIgnoreCase(currentPosition)) {

                        capLp.topMargin = lastAdHeight;
                        capLp.bottomMargin = 0;
                        capLp.height = screenHeightInPx - lastAdHeight;
                    } else {
                        int webViewHeight = screenHeightInPx - lastAdHeight;
                        capLp.height = webViewHeight;
                        capLp.topMargin = 0;
                    }
                }

                webViewView.setLayoutParams(capLp);
                webViewView.requestLayout();
            }
        } else {

            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) lp;
                if ("top".equalsIgnoreCase(currentPosition)) {
                    params.setMargins(0, 0, 0, 0);

                    if (isBannerVisible && !isOverlapping) {
                        float shift = (float) (lastAdHeight + systemSafeTop);
                        webViewView.setTranslationY(shift);
                    } else {
                        webViewView.setTranslationY(0);
                    }
                } else {
                    webViewView.setTranslationY(0);

                    if (!isBannerVisible || isOverlapping) {
                        params.setMargins(0, 0, 0, 0);
                    } else {
                        int finalBottom = lastAdHeight + systemSafeBottom;
                        params.setMargins(0, 0, 0, finalBottom);
                    }
                }
                webViewView.setLayoutParams(params);
                webViewView.requestLayout();
            }
        }
    }

    public void hideBanner(CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(() -> {
            hideBannerView();
            if (callbackContext != null) {
                callbackContext.success();
            }
        });
    }

    private void hideBannerView() {
        isBannerVisible = false;

        if (isCapacitor) {
            if (capacitorAdLayout != null) capacitorAdLayout.setVisibility(View.GONE);
        } else {
            if (adLayout != null) adLayout.setVisibility(View.GONE);
        }

        if (currentBannerAd != null) {
            View adView = currentBannerAd.getView(cordova.getActivity());
            if (adView != null) adView.setVisibility(View.GONE);
        }
        updateWebViewMargins();
    }

    private void destroyCurrentBanner() {
        if (currentBannerAd != null) {
            isBannerVisible = false;
            updateWebViewMargins();

            View adView = currentBannerAd.getView(cordova.getActivity());
            if (adView != null) {
                if (isCapacitor) {
                    if (capacitorAdLayout != null) {
                        capacitorAdLayout.removeView(adView);
                    }
                } else {
                    if (adLayout != null) {
                        adLayout.removeView(adView);
                    }
                }
            }

            currentBannerAd.destroy();
            currentBannerAd = null;
        }
    }

    public void stopPreloadAndClear() {
        cordova.getActivity().runOnUiThread(() -> {
            isPreloaderActive = false;
            destroyCurrentBanner();

            if (isCapacitor) {
                if (capacitorAdLayout != null) {
                    capacitorAdLayout.removeAllViews();
                    ViewGroup decorView = (ViewGroup) cordova.getActivity().getWindow().getDecorView();
                    decorView.removeView(capacitorAdLayout);
                    capacitorAdLayout = null;
                }
            } else {
                if (adLayout != null) {
                    adLayout.removeAllViews();
                    if (adLayout.getParent() != null) {
                        ((ViewGroup) adLayout.getParent()).removeView(adLayout);
                    }
                    adLayout = null;
                }
            }
        });
    }

    private void setupAdEvents(BannerAd ad) {
        ad.setAdEventCallback(new BannerAdEventCallback() {
            @Override
            public void onAdImpression() { fireEvent("on.banner.impression", null); }

            @Override
            public void onAdClicked() { fireEvent("on.banner.clicked", null); }

            @Override
            public void onAdPaid(@NonNull AdValue adValue) {
                try {
                    JSONObject data = new JSONObject();
                    data.put("value", adValue.getValueMicros());
                    data.put("currency", adValue.getCurrencyCode());
                    data.put("precision", adValue.getPrecisionType());
                    data.put("source", "preloader");
                    fireEvent("on.banner.revenue", data);
                } catch (JSONException ignored) {}
            }

            @Override
            public void onAdShowedFullScreenContent() { fireEvent("on.banner.opened", null); }

            @Override
            public void onAdDismissedFullScreenContent() { fireEvent("on.banner.closed", null); }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError error) {
                try {
                    JSONObject errData = new JSONObject();
                    errData.put("message", error.getMessage());
                    fireEvent("on.banner.failed.show", errData);
                } catch (JSONException ignored) {}
            }
        });

        ad.setBannerAdRefreshCallback(new BannerAdRefreshCallback() {
            @Override
            public void onAdRefreshed() { fireEvent("on.banner.refreshed", null); }

            @Override
            public void onAdFailedToRefresh(@NonNull LoadAdError loadAdError) {
                try {
                    JSONObject err = new JSONObject();
                    err.put("message", loadAdError.getMessage());
                    fireEvent("on.banner.refresh.failed", err);
                } catch (JSONException ignored) {}
            }
        });
    }

    private void sendLoadedEvent(AdSize adSize, boolean isCollapsible) {
        try {
            Context context = cordova.getActivity();
            JSONObject data = new JSONObject();
            data.put("width", adSize.getWidth());
            data.put("height", adSize.getHeight());
            data.put("widthPixels", adSize.getWidthInPixels(context));
            data.put("heightPixels", adSize.getHeightInPixels(context));
            data.put("isCollapsible", isCollapsible);

            fireEvent("on.banner.load", data);
        } catch (JSONException ignored) {}
    }

    private AdSize getAdSize(Context context, String sizeStr) {
        if ("BANNER".equalsIgnoreCase(sizeStr)) return AdSize.BANNER;
        else if ("LARGE_BANNER".equalsIgnoreCase(sizeStr)) return AdSize.LARGE_BANNER;
        else if ("MEDIUM_RECTANGLE".equalsIgnoreCase(sizeStr)) return AdSize.MEDIUM_RECTANGLE;
        else if ("FULL_BANNER".equalsIgnoreCase(sizeStr)) return AdSize.FULL_BANNER;
        else if ("LEADERBOARD".equalsIgnoreCase(sizeStr)) return AdSize.LEADERBOARD;
        else return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, getAdWidth());
    }

    private int getAdWidth() {
        DisplayMetrics displayMetrics = cordova.getActivity().getResources().getDisplayMetrics();
        return (int) (displayMetrics.widthPixels / displayMetrics.density);
    }

    private void fireEvent(String eventName, JSONObject data) {
        cordova.getActivity().runOnUiThread(() -> {
            StringBuilder js = new StringBuilder();
            js.append("javascript:cordova.fireDocumentEvent('");
            js.append(eventName);
            js.append("'");
            if (data != null) {
                js.append(", ");
                js.append(data.toString());
            }
            js.append(");");
            if (webView != null) webView.loadUrl(js.toString());
        });
    }
}
