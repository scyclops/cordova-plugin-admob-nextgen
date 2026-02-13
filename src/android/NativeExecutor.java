package com.emi.cordova.admob.nextgen;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date; 
import java.util.List;

import com.google.android.libraries.ads.mobile.sdk.common.AdValue;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.common.VideoController;
import com.google.android.libraries.ads.mobile.sdk.common.VideoOptions;
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaContent;
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView;

public class NativeExecutor {

    private static final String TAG = "AdMobNative";
    private CordovaInterface cordova;
    private CordovaWebView webView;

    private NativeAdView nativeAdView;
    private RelativeLayout adContainer;
    private NativeAd mNativeAd;

    private boolean isOverlapping = true;
    private String currentPreset = "";
    private int currentAdHeightPixels = 0;

    private boolean isLoading = false; 
    private long lastLoadTime = 0;     
    private long minLoadInterval = 5000; 

    public NativeExecutor(CordovaInterface cordova, CordovaWebView webView) {
        this.cordova = cordova;
        this.webView = webView;
    }

    public void createNativeAd(JSONArray args, CallbackContext callbackContext) {
        try {
            JSONObject options = args.getJSONObject(0);
            String adUnitId = options.getString("adUnitId");

            if (options.has("retryInterval")) {
                this.minLoadInterval = options.getLong("retryInterval");
            }

            long currentTime = new Date().getTime();

            if (isLoading) {

                callbackContext.error("Ad is loading");
                return;
            }

            if ((currentTime - lastLoadTime) < minLoadInterval) {

                callbackContext.error("Request too fast");
                return;
            }

            String viewMode = options.optString("view", "custom");
            this.currentPreset = viewMode;
            this.isOverlapping = options.optBoolean("isOverlapping", true);

            DisplayMetrics metrics = cordova.getActivity().getResources().getDisplayMetrics();
            float density = metrics.density;

            int screenWidthDp = (int) (metrics.widthPixels / density);
            int screenHeightDp = (int) (metrics.heightPixels / density);

            int x, y, width, height;

            if ("banner_bottom".equalsIgnoreCase(viewMode)) {
                height = 120;
                width = screenWidthDp;
                x = 0;
                y = screenHeightDp - height;

            } else if ("banner_top".equalsIgnoreCase(viewMode)) {
                height = 120;
                width = screenWidthDp;
                x = 0;
                y = 0;

            } else if ("modal_center".equalsIgnoreCase(viewMode)) {
                width = screenWidthDp - 40;
                height = 350;
                x = 20;
                y = (screenHeightDp - height) / 2;

            } else {
                x = options.optInt("x", 0);
                y = options.optInt("y", 0);
                width = options.optInt("width", 300);
                height = options.optInt("height", 300);
            }

            final int finalX = x;
            final int finalY = y;
            final int finalW = width;
            final int finalH = height;

            isLoading = true;
            lastLoadTime = currentTime;

            cordova.getThreadPool().execute(() -> {
                loadNativeAd(adUnitId, finalX, finalY, finalW, finalH, callbackContext);
            });

        } catch (JSONException e) {
            callbackContext.error("Invalid Args: " + e.getMessage());
        }
    }

    private void loadNativeAd(String adUnitId, int x, int y, int w, int h, CallbackContext callbackContext) {

        VideoOptions videoOptions = new VideoOptions.Builder()
                .setStartMuted(true)
                .build();

        List<NativeAd.NativeAdType> types = new ArrayList<>();
        types.add(NativeAd.NativeAdType.NATIVE);

        NativeAdRequest request = new NativeAdRequest.Builder(adUnitId, types)
                .setVideoOptions(videoOptions)
                .build();

        NativeAdLoaderCallback loaderCallback = new NativeAdLoaderCallback() {
            @Override
            public void onNativeAdLoaded(@NonNull NativeAd nativeAd) {

                cordova.getActivity().runOnUiThread(() -> {

                    isLoading = false;

                    removeNativeAd();
                    mNativeAd = nativeAd;

                    setupEventCallback(nativeAd);
                    showNativeAdView(nativeAd, x, y, w, h);

                    fireEvent("on.native.loaded", null);
                    callbackContext.success("Native Ad Shown");
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                cordova.getActivity().runOnUiThread(() -> {

                    isLoading = false;

                    try {
                        JSONObject err = new JSONObject();
                        err.put("code", loadAdError.getCode());
                        err.put("message", loadAdError.getMessage());
                        fireEvent("on.native.failed", err);
                    } catch (JSONException e) {}
                    callbackContext.error(loadAdError.getMessage());
                });
            }
        };

        NativeAdLoader.load(request, loaderCallback);
    }

    private void setupEventCallback(NativeAd nativeAd) {
        nativeAd.setAdEventCallback(new NativeAdEventCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                fireEvent("on.native.shown", null);
            }
            @Override
            public void onAdDismissedFullScreenContent() {
                fireEvent("on.native.dismissed", null);
            }
            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError error) {

                try {
                    JSONObject errData = new JSONObject();
                    errData.put("message", error.getMessage());
                    fireEvent("on.native.show.failed", errData);
                } catch (JSONException e) {}
            }
            @Override
            public void onAdImpression() {
                fireEvent("on.native.impression", null);
            }
            @Override
            public void onAdClicked() {
                fireEvent("on.native.clicked", null);
            }
            @Override
            public void onAdPaid(@NonNull AdValue adValue) {
                try {
                    JSONObject data = new JSONObject();
                    data.put("value", adValue.getValueMicros());
                    data.put("currency", adValue.getCurrencyCode());
                    data.put("precision", adValue.getPrecisionType());

                    fireEvent("on.native.revenue", data);
                } catch (JSONException e) {}
            }
        });
    }

    private void showNativeAdView(NativeAd nativeAd, int x, int y, int width, int height) {
        float density = cordova.getActivity().getResources().getDisplayMetrics().density;

        int finalX = (int) (x * density);
        int finalY = (int) (y * density);
        int finalW = (int) (width * density);
        int finalH = (int) (height * density);

        this.currentAdHeightPixels = finalH;

        boolean isSmallMode = (height < 150);

        nativeAdView = new NativeAdView(cordova.getActivity());

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(finalW, finalH);
        params.leftMargin = finalX;
        params.topMargin = finalY;

        LinearLayout mainLayout = new LinearLayout(cordova.getActivity());
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.WHITE);
        mainLayout.setElevation(8f);
        mainLayout.setPadding((int)(10*density), (int)(10*density), (int)(10*density), (int)(10*density));

        LinearLayout headerLayout = new LinearLayout(cordova.getActivity());
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(Gravity.CENTER_VERTICAL);

        ImageView iconView = new ImageView(cordova.getActivity());
        if (nativeAd.getIcon() != null) {
            iconView.setImageDrawable(nativeAd.getIcon().getDrawable());
            headerLayout.addView(iconView, new LinearLayout.LayoutParams((int)(40*density), (int)(40*density)));
        }

        LinearLayout textContainer = new LinearLayout(cordova.getActivity());
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setPadding((int)(10*density), 0, 0, 0);

        TextView headlineView = new TextView(cordova.getActivity());
        headlineView.setText(nativeAd.getHeadline());
        headlineView.setTypeface(null, Typeface.BOLD);
        headlineView.setTextColor(Color.BLACK);
        headlineView.setMaxLines(1);
        headlineView.setEllipsize(TextUtils.TruncateAt.END);
        textContainer.addView(headlineView);

        TextView adBadge = new TextView(cordova.getActivity());
        adBadge.setText("Ad");
        adBadge.setTextSize(10);
        adBadge.setTextColor(Color.WHITE);
        adBadge.setBackgroundColor(0xFFFCC133);
        adBadge.setPadding(5, 0, 5, 0);
        textContainer.addView(adBadge, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        headerLayout.addView(textContainer);
        mainLayout.addView(headerLayout);

        MediaView mediaView = new MediaView(cordova.getActivity());
        LinearLayout.LayoutParams mediaParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);

        if (isSmallMode) {
            mediaParams.weight = 0;
            mediaParams.height = 0;
            mediaView.setVisibility(View.GONE);
        } else {
            mediaParams.weight = 1;
            mediaParams.topMargin = (int)(10*density);
            mediaParams.bottomMargin = (int)(10*density);
            mediaView.setVisibility(View.VISIBLE);
        }

        mainLayout.addView(mediaView, mediaParams);

        TextView bodyView = new TextView(cordova.getActivity());
        bodyView.setText(nativeAd.getBody());
        bodyView.setMaxLines(2);
        bodyView.setEllipsize(TextUtils.TruncateAt.END);
        bodyView.setTextSize(12);
        bodyView.setTextColor(Color.DKGRAY);
        mainLayout.addView(bodyView);

        Button ctaView = new Button(cordova.getActivity());
        ctaView.setText(nativeAd.getCallToAction());
        ctaView.setBackgroundColor(0xFF4285F4);
        ctaView.setTextColor(Color.WHITE);
        ctaView.setAllCaps(true);
        if (isSmallMode) {
            ctaView.setTextSize(12);
            ctaView.setPadding(0,0,0,0);
        }
        mainLayout.addView(ctaView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        nativeAdView.addView(mainLayout);

        nativeAdView.setIconView(iconView);
        nativeAdView.setHeadlineView(headlineView);
        nativeAdView.setBodyView(bodyView);
        nativeAdView.setCallToActionView(ctaView);
        nativeAdView.registerNativeAd(nativeAd, mediaView);

        MediaContent mediaContent = nativeAd.getMediaContent();
        if (mediaContent != null && mediaContent.getHasVideoContent()) {
            VideoController vc = mediaContent.getVideoController();
        }

        if (adContainer == null) {
            adContainer = new RelativeLayout(cordova.getActivity());
            ViewGroup.LayoutParams containerParams = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            cordova.getActivity().addContentView(adContainer, containerParams);
        }

        adContainer.addView(nativeAdView, params);
        adContainer.bringToFront();

        updateWebViewMargins();
    }

    public void removeNativeAd() {
        cordova.getActivity().runOnUiThread(() -> {
            if (nativeAdView != null) {
                nativeAdView.destroy();
                if (adContainer != null) {
                    adContainer.removeView(nativeAdView);
                }
                nativeAdView = null;
                mNativeAd = null;

                resetWebViewMargins();
            }
        });
    }

    private void updateWebViewMargins() {
        if (webView == null || webView.getView() == null) return;

        View webViewView = webView.getView();
        ViewGroup.LayoutParams lp = webViewView.getLayoutParams();

        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) lp;

            if (isOverlapping) {
                params.setMargins(0, 0, 0, 0);
            } else {
                if ("banner_top".equalsIgnoreCase(currentPreset)) {
                    params.setMargins(0, currentAdHeightPixels, 0, 0);
                } else if ("banner_bottom".equalsIgnoreCase(currentPreset)) {
                    params.setMargins(0, 0, 0, currentAdHeightPixels);
                } else {
                    params.setMargins(0, 0, 0, 0);
                }
            }

            webViewView.setLayoutParams(params);
            webViewView.requestLayout();
        }
    }

    private void resetWebViewMargins() {
        if (webView == null || webView.getView() == null) return;
        View webViewView = webView.getView();
        ViewGroup.LayoutParams lp = webViewView.getLayoutParams();

        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) lp;
            params.setMargins(0, 0, 0, 0);
            webViewView.setLayoutParams(params);
            webViewView.requestLayout();
        }
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
