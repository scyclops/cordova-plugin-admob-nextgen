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
import com.google.android.libraries.ads.mobile.sdk.banner.AdView;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.AdValue;
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;

import java.util.Date;

public class BannerExecutor {

    private static final String TAG = "AdMobBanner";
    private CordovaInterface cordova;
    private CordovaWebView webView;

    private AdView adView;

    private RelativeLayout cordovaAdLayout;
    private FrameLayout capacitorAdLayout;

    private String lastAdUnitId = "";
    private String lastSizeStr = "";
    private AdSize lastAdSize = null;
    private String currentPosition = "bottom";
    private String lastPosition = "bottom";

    private boolean isBannerVisible = false;
    private boolean isLoading = false;
    private boolean isOverlapping = true;
    private boolean isAutoShow = true;
    private boolean isCollapsible = false;
    private boolean isCapacitor = false;

    private int lastAdHeight = 0;

    private int systemSafeTop = 0;
    private int systemSafeBottom = 0;

    private long lastLoadTime = 0;
    private long minLoadInterval = 5000;

    public BannerExecutor(CordovaInterface cordova, CordovaWebView webView) {
        this.cordova = cordova;
        this.webView = webView;
    }

    public void createBanner(JSONArray args, CallbackContext callbackContext) {
        try {
            JSONObject options = args.getJSONObject(0);
            String adUnitId = options.getString("adUnitId");

            String requestedSize = "ADAPTIVE";
            if (options.has("size")) requestedSize = options.getString("size");

            String newPosition = currentPosition;
            if (options.has("position")) newPosition = options.getString("position");

            if (options.has("isOverlapping")) isOverlapping = options.getBoolean("isOverlapping");
            if (options.has("isAutoShow")) isAutoShow = options.getBoolean("isAutoShow");
            else isAutoShow = true;

            if (options.has("collapsible")) this.isCollapsible = options.getBoolean("collapsible");
            else this.isCollapsible = false;

            if (options.has("retryInterval")) this.minLoadInterval = options.getLong("retryInterval");

            if (options.has("isCapacitor")) this.isCapacitor = options.getBoolean("isCapacitor");
            else this.isCapacitor = false;

            final String finalSizeStr = requestedSize;
            final String finalNewPosition = newPosition;

            cordova.getActivity().runOnUiThread(() -> {
                long currentTime = new Date().getTime();

                if (isLoading) {
                    callbackContext.error("Banner loading");
                    return;
                }

                boolean isSameId = lastAdUnitId.equals(adUnitId);
                boolean isSameSize = lastSizeStr.equals(finalSizeStr);
                boolean isSamePos = lastPosition.equals(finalNewPosition);

                if (adView != null && isSameId && isSameSize) {
                    currentPosition = finalNewPosition;

                    if (isAutoShow) {
                        if (!isBannerVisible) {
                            showBannerView();
                            callbackContext.success("Banner Shown (Cached)");
                        } else {
                            if (!isSamePos) {
                                updateBannerLayout();
                                callbackContext.success("Banner Repositioned");
                            } else {
                                callbackContext.success("Banner Already Visible");
                            }
                        }
                    } else {
                        hideBannerView();
                        callbackContext.success("Banner Hidden (Cached)");
                    }

                    lastPosition = currentPosition;
                    if (lastAdSize != null) sendLoadedEvent(lastAdSize, false);
                    return;
                }

                if ((currentTime - lastLoadTime) < minLoadInterval) {
                    callbackContext.error("Request too fast. Wait " + minLoadInterval + "ms");
                    return;
                }

                currentPosition = finalNewPosition;
                lastPosition = currentPosition;

                loadBanner(adUnitId, finalSizeStr, callbackContext);
            });

        } catch (JSONException e) {
            callbackContext.error("Invalid JSON Args: " + e.getMessage());
        }
    }

    private void loadBanner(String adUnitId, String sizeStr, CallbackContext callbackContext) {
        Context context = cordova.getActivity();

        destroyBannerInternal();

        isLoading = true;
        lastLoadTime = new Date().getTime();
        lastAdUnitId = adUnitId;
        lastSizeStr = sizeStr;

        adView = new AdView(context);

        AdSize adSize = getAdSize(context, sizeStr);
        lastAdSize = adSize;

        lastAdHeight = adSize.getHeightInPixels(context);

        BannerAdRequest.Builder builder = new BannerAdRequest.Builder(adUnitId, adSize);

        if (isCollapsible) {
            Bundle extras = new Bundle();
            String anchor = "top".equalsIgnoreCase(currentPosition) ? "top" : "bottom";
            extras.putString("collapsible", anchor);
            builder.setGoogleExtrasBundle(extras);
        }

        BannerAdRequest request = builder.build();

        adView.loadAd(request, new AdLoadCallback<BannerAd>() {
            @Override
            public void onAdLoaded(@NonNull BannerAd bannerAd) {
                cordova.getActivity().runOnUiThread(() -> {
                    isLoading = false;

                    boolean isActualCollapsible = bannerAd.isCollapsible();

                    bannerAd.setAdEventCallback(new BannerAdEventCallback() {
                        @Override public void onAdImpression() { fireEvent("on.banner.impression", null); }
                        @Override public void onAdClicked() { fireEvent("on.banner.clicked", null); }
                        @Override public void onAdShowedFullScreenContent() { fireEvent("on.banner.opened", null); }
                        @Override public void onAdDismissedFullScreenContent() { fireEvent("on.banner.closed", null); }
                        @Override
                        public void onAdPaid(@NonNull AdValue adValue) {
                            try {
                                JSONObject data = new JSONObject();
                                data.put("value", adValue.getValueMicros());
                                data.put("currency", adValue.getCurrencyCode());
                                data.put("precision", adValue.getPrecisionType());
                                fireEvent("on.banner.revenue", data);
                            } catch (JSONException ignored) {}
                        }
                        @Override
                        public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError error) {
                            try {
                                JSONObject errData = new JSONObject();
                                errData.put("message", error.getMessage());
                                fireEvent("on.banner.failed.show", errData);
                            } catch (JSONException ignored) {}
                        }
                    });

                    bannerAd.setBannerAdRefreshCallback(new BannerAdRefreshCallback() {
                        @Override public void onAdRefreshed() { fireEvent("on.banner.refreshed", null); }
                        @Override
                        public void onAdFailedToRefresh(@NonNull LoadAdError loadAdError) {
                            try {
                                JSONObject err = new JSONObject();
                                err.put("code", loadAdError.getCode());
                                err.put("message", loadAdError.getMessage());
                                fireEvent("on.banner.refresh.failed", err);
                            } catch (JSONException ignored) {}
                        }
                    });

                    sendLoadedEvent(adSize, isActualCollapsible);

                    if (isAutoShow) {
                        showBannerView();
                        callbackContext.success("Banner Created & Shown");
                    } else {
                        callbackContext.success("Banner Loaded (Hidden)");
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                cordova.getActivity().runOnUiThread(() -> {
                    isLoading = false;
                    try {
                        JSONObject errData = new JSONObject();
                        errData.put("code", adError.getCode());
                        errData.put("message", adError.getMessage());
                        fireEvent("on.banner.failed", errData);
                    } catch (JSONException ignored) {}
                    callbackContext.error("Failed: " + adError.getMessage());
                });
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
        if (adView == null) return;
        isBannerVisible = true;

        if (isCapacitor) {
            if (capacitorAdLayout == null) {
                capacitorAdLayout = new FrameLayout(cordova.getActivity());
                capacitorAdLayout.setTag("emi_banner_layout");
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
            if (cordovaAdLayout == null) {
                cordovaAdLayout = new RelativeLayout(cordova.getActivity());
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);

                if ("top".equalsIgnoreCase(currentPosition)) {
                    params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                } else {
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                }

                cordovaAdLayout.setClickable(false);
                cordovaAdLayout.setFocusable(false);

                cordova.getActivity().addContentView(cordovaAdLayout, params);
            }
            cordovaAdLayout.bringToFront();
            cordovaAdLayout.setVisibility(View.VISIBLE);
        }

        adView.setVisibility(View.VISIBLE);
        updateBannerLayout();
        updateWebViewMargins();
    }

    private void updateBannerLayout() {
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

            if (cordovaAdLayout == null) return;
            if (adView.getParent() != null && adView.getParent() != cordovaAdLayout) {
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
                cordovaAdLayout.addView(adView, bannerParams);
            } else {
                adView.setLayoutParams(bannerParams);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                cordovaAdLayout.setOnApplyWindowInsetsListener((v, insets) -> {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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
                cordovaAdLayout.requestApplyInsets();
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
                        int statusBarHeight = getStatusBarHeight();
                        int pushDown = lastAdHeight + (isFull ? 0 : statusBarHeight);
                        capLp.topMargin = pushDown;
                        int navBarHeight = !isFull ? getNavigationBarHeight() : 0;
                        capLp.height = screenHeightInPx - pushDown - navBarHeight;
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

    private void hideBannerView() {
        if (adView == null) return;
        isBannerVisible = false;

        if (isCapacitor && capacitorAdLayout != null) {
            capacitorAdLayout.setVisibility(View.GONE);
        } else if (!isCapacitor && cordovaAdLayout != null) {
            cordovaAdLayout.setVisibility(View.GONE);
        }

        adView.setVisibility(View.GONE);
        updateWebViewMargins();
    }

    public void hideBanner(CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(() -> {
            hideBannerView();
            callbackContext.success();
        });
    }

    public void showBanner(CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(() -> {
            if (adView != null) {
                showBannerView();
                callbackContext.success();
            } else {
                callbackContext.error("No banner loaded");
            }
        });
    }

    private void destroyBannerInternal() {
        if (adView != null) {
            isBannerVisible = false;
            updateWebViewMargins();

            if (isCapacitor) {
                if (capacitorAdLayout != null) {
                    capacitorAdLayout.removeView(adView);
                    ViewGroup decorView = (ViewGroup) cordova.getActivity().getWindow().getDecorView();
                    decorView.removeView(capacitorAdLayout);
                    capacitorAdLayout = null;
                }
            } else {
                if (cordovaAdLayout != null) {
                    cordovaAdLayout.removeView(adView);
                    if (cordovaAdLayout.getParent() != null) {
                        ((ViewGroup) cordovaAdLayout.getParent()).removeView(cordovaAdLayout);
                    }
                    cordovaAdLayout = null;
                }
            }

            adView.destroy();
            adView = null;
        }
    }

    public void destroy() {
        cordova.getActivity().runOnUiThread(() -> {
            destroyBannerInternal();
            lastAdUnitId = "";
            lastSizeStr = "";
            lastAdSize = null;
            isBannerVisible = false;
        });
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
