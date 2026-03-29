package com.emi.cordova.admob.nextgen;

import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
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
    private BannerAd currentBannerAd;

    private boolean isPreloaderActive = false;
    private String currentAdUnitId;

    private String currentPosition = "bottom";
    private boolean isOverlapping = true;
    private boolean isBannerVisible = false;

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
                        } catch (JSONException e) {}
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

    private void showBannerView() {
        if (currentBannerAd == null) return;

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

        isBannerVisible = true;
        updateBannerLayout();

        adLayout.bringToFront();
        adLayout.setVisibility(View.VISIBLE);

        View adView = currentBannerAd.getView(cordova.getActivity());
        if (adView != null) adView.setVisibility(View.VISIBLE);

        updateWebViewMargins();
    }

    private void updateBannerLayout() {
        if (currentBannerAd == null || adLayout == null) return;

        View adView = currentBannerAd.getView(cordova.getActivity());
        if (adView == null) return;

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
                    systemSafeTop = insets.getInsets(android.view.WindowInsets.Type.systemBars()).top;
                    systemSafeBottom = insets.getInsets(android.view.WindowInsets.Type.systemBars()).bottom;
                } else {
                    systemSafeTop = insets.getSystemWindowInsetTop();
                    systemSafeBottom = insets.getSystemWindowInsetBottom();
                }

                if (adView != null && adView.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
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

    private void updateWebViewMargins() {
        if (webView == null || webView.getView() == null) return;

        View webViewView = webView.getView();
        ViewGroup.LayoutParams lp = webViewView.getLayoutParams();

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

    private void destroyCurrentBanner() {
        if (currentBannerAd != null) {
            isBannerVisible = false;
            updateWebViewMargins();

            View adView = currentBannerAd.getView(cordova.getActivity());
            if (adView != null && adView.getParent() != null) {
                ((ViewGroup) adView.getParent()).removeView(adView);
            }
            currentBannerAd.destroy();
            currentBannerAd = null;
        }
    }

    public void stopPreloadAndClear() {
        cordova.getActivity().runOnUiThread(() -> {
            isPreloaderActive = false;
            destroyCurrentBanner();
            if (adLayout != null) {
                adLayout.removeAllViews();
                ((ViewGroup)adLayout.getParent()).removeView(adLayout);
                adLayout = null;
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
                } catch (JSONException e) {}
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
                } catch (JSONException e) {}
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
                } catch (JSONException e) {}
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
        } catch (JSONException e) {}
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
