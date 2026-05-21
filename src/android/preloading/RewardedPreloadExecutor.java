package com.emi.cordova.admob.nextgen.preloading;

import static com.emi.cordova.admob.nextgen.AdMobNextGen.isInitialized;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.libraries.ads.mobile.sdk.common.AdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.AdValue;
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration;
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem;
import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdPreloader;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class RewardedPreloadExecutor {

    private static final String TAG = "AdMobRewardedPreload";
    private final CordovaInterface cordova;
    private final CordovaWebView webView;

    private boolean isPreloaderActive = false;
    private String currentAdUnitId;

    private long lastShowTime = 0;
    private long minShowInterval = 5000;
    private boolean isAutoShow = false;

    public RewardedPreloadExecutor(CordovaInterface cordova, CordovaWebView webView) {
        this.cordova = cordova;
        this.webView = webView;
    }

    public void startPreload(JSONArray args, CallbackContext callbackContext) {
        if (!isInitialized) {
            callbackContext.error("SDK not initialized");
            return;
        }
        try {
            JSONObject options = args.getJSONObject(0);
            String adUnitId = options.getString("adUnitId");

            if (isPreloaderActive && adUnitId.equals(currentAdUnitId)) {
                callbackContext.success("Rewarded Preloader already active");
                return;
            }

            if (options.has("retryInterval")) {
                this.minShowInterval = options.getLong("retryInterval");
            }
            if (options.has("isAutoShow")) {
                this.isAutoShow = options.getBoolean("isAutoShow");
            }

            int bufferSize = 1;
            if (options.has("bufferSize")) {
                bufferSize = options.getInt("bufferSize");
            }

            this.currentAdUnitId = adUnitId;
            this.isPreloaderActive = true;
            final int finalBufferSize = bufferSize;

            cordova.getActivity().runOnUiThread(() -> {
                AdRequest adRequest = new AdRequest.Builder(adUnitId).build();
                PreloadConfiguration preloadConfig = new PreloadConfiguration(adRequest, finalBufferSize);

                PreloadCallback preloadCallback = new PreloadCallback() {
                    @Override
                    public void onAdPreloaded(@NonNull String preloadId, @NonNull ResponseInfo responseInfo) {

                        fireEvent("on.rewarded.preload.available", null);

                        if (isAutoShow) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                showPolledAd(null, null);
                            });
                        }
                    }

                    @Override
                    public void onAdFailedToPreload(@NonNull String preloadId, @NonNull LoadAdError adError) {

                        try {
                            JSONObject err = new JSONObject();
                            err.put("message", adError.getMessage());
                            err.put("code", adError.getCode());
                            fireEvent("on.rewarded.preload.failed", err);
                        } catch (JSONException ignored) {}
                    }

                    @Override
                    public void onAdsExhausted(@NonNull String preloadId) {

                        fireEvent("on.rewarded.preload.exhausted", null);
                    }
                };

                RewardedAdPreloader.start(adUnitId, preloadConfig, preloadCallback);
                callbackContext.success("Rewarded Preloader Started");
            });

        } catch (JSONException e) {
            isPreloaderActive = false;
            callbackContext.error("Invalid arguments: " + e.getMessage());
        }
    }

    public void showPolledAd(JSONArray args, CallbackContext callbackContext) {
        if (currentAdUnitId == null || currentAdUnitId.isEmpty() || !isPreloaderActive) {
            if (callbackContext != null) callbackContext.error("Rewarded Preloader engine is not running.");
            return;
        }

        long currentTime = new Date().getTime();
        if ((currentTime - lastShowTime) < minShowInterval) {
            if (callbackContext != null) callbackContext.error("Request too fast. Please wait " + minShowInterval + " ms to prevent invalid traffic.");
            return;
        }

        cordova.getActivity().runOnUiThread(() -> {
            final RewardedAd ad = RewardedAdPreloader.pollAd(currentAdUnitId);

            if (ad == null) {
                if (callbackContext != null) callbackContext.error("Rewarded Pool Empty");
                return;
            }

            this.lastShowTime = currentTime;
            setupAdEvents(ad);

            ad.show(cordova.getActivity(), rewardItem -> {
                try {
                    JSONObject data = new JSONObject();
                    data.put("amount", rewardItem.getAmount());
                    data.put("type", rewardItem.getType());
                    fireEvent("on.rewarded.reward", data);
                } catch (JSONException ignored) {}
            });

            if (callbackContext != null) callbackContext.success("Rewarded Ad Shown from Pool");
        });
    }

    public void checkAdAvailable(JSONArray args, CallbackContext callbackContext) {
        if (currentAdUnitId == null || currentAdUnitId.isEmpty()) {
            callbackContext.success(0);
            return;
        }

        cordova.getActivity().runOnUiThread(() -> {
            boolean isAvailable = RewardedAdPreloader.isAdAvailable(currentAdUnitId);
            callbackContext.success(isAvailable ? 1 : 0);
        });
    }

    public void stopPreloadAndClear(CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(() -> {
            isPreloaderActive = false;
            if (currentAdUnitId != null) {
                RewardedAdPreloader.destroyAll();
                currentAdUnitId = null;
            }
            if (callbackContext != null) {
                callbackContext.success("Rewarded Preloader stopped and cleared");
            }
        });
    }

    public void setAutoShow(boolean shouldAutoShow) {
        this.isAutoShow = shouldAutoShow;
    }

    private void setupAdEvents(RewardedAd ad) {
        ad.setAdEventCallback(new RewardedAdEventCallback() {
            @Override
            public void onAdImpression() {
                fireEvent("on.rewarded.impression", null);
            }

            @Override
            public void onAdShowedFullScreenContent() {
                fireEvent("on.rewarded.opened", null);
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                fireEvent("on.rewarded.closed", null);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError error) {
                try {
                    JSONObject errData = new JSONObject();
                    errData.put("message", error.getMessage());
                    errData.put("code", error.getCode());
                    fireEvent("on.rewarded.failed.show", errData);
                } catch (JSONException ignored) {}
            }

            @Override
            public void onAdPaid(@NonNull AdValue adValue) {
                try {
                    JSONObject data = new JSONObject();
                    data.put("value", adValue.getValueMicros());
                    data.put("currency", adValue.getCurrencyCode());
                    data.put("precision", adValue.getPrecisionType());
                    data.put("source", "preloader");
                    fireEvent("on.rewarded.revenue", data);
                } catch (JSONException ignored) {}
            }
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
