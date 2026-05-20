package com.emi.cordova.admob.nextgen;

import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.libraries.ads.mobile.sdk.MobileAds;
import com.google.android.libraries.ads.mobile.sdk.common.RequestConfiguration;
import com.google.android.libraries.ads.mobile.sdk.common.AgeRestrictedTreatment;

import java.util.ArrayList;
import java.util.List;

public class GlobalSettingsExecutor {

    private static final String TAG = "AdMobSettings";
    private CordovaInterface cordova;

    public GlobalSettingsExecutor(CordovaInterface cordova) {
        this.cordova = cordova;
    }

    public void setAppVolume(JSONArray args, CallbackContext callbackContext) {
        try {
            double volume = args.getDouble(0);
            MobileAds.setUserControlledAppVolume((float) volume);
            callbackContext.success();
        } catch (JSONException e) {
            callbackContext.error("Invalid volume value");
        }
    }

    public void setAppMuted(JSONArray args, CallbackContext callbackContext) {
        try {
            boolean isMuted = args.getBoolean(0);
            MobileAds.setUserMutedApp(isMuted);
            callbackContext.success();
        } catch (JSONException e) {
            callbackContext.error("Invalid mute value");
        }
    }

    public void setRequestConfiguration(JSONArray args, CallbackContext callbackContext) {
        try {
            JSONObject config = args.getJSONObject(0);
            RequestConfiguration requestConfiguration = buildRequestConfiguration(config);

            MobileAds.setRequestConfiguration(requestConfiguration);
            callbackContext.success("Configuration updated successfully");
        } catch (JSONException e) {
            callbackContext.error("Configuration error: " + e.getMessage());
        }
    }

    public static RequestConfiguration buildRequestConfiguration(JSONObject config) {
        RequestConfiguration.Builder builder = new RequestConfiguration.Builder();

        try {
            Boolean isChildDirected = parseBoolean(config, "tagForChildDirectedTreatment");
            Boolean isUnderAgeOfConsent = parseBoolean(config, "tagForUnderAgeOfConsent");

            if (Boolean.TRUE.equals(isChildDirected)) {
                builder.setAgeRestrictedTreatment(AgeRestrictedTreatment.CHILD);
            } else if (Boolean.TRUE.equals(isUnderAgeOfConsent)) {
                builder.setAgeRestrictedTreatment(AgeRestrictedTreatment.TEEN);
            } else if (Boolean.FALSE.equals(isChildDirected) || Boolean.FALSE.equals(isUnderAgeOfConsent)) {

                builder.setAgeRestrictedTreatment(AgeRestrictedTreatment.UNSPECIFIED);
            }

            if (config.has("maxAdContentRating")) {
                String rating = config.getString("maxAdContentRating");
                switch (rating.toUpperCase()) {
                    case "G":
                        builder.setMaxAdContentRating(RequestConfiguration.MaxAdContentRating.MAX_AD_CONTENT_RATING_G);
                        break;
                    case "PG":
                        builder.setMaxAdContentRating(RequestConfiguration.MaxAdContentRating.MAX_AD_CONTENT_RATING_PG);
                        break;
                    case "T":
                        builder.setMaxAdContentRating(RequestConfiguration.MaxAdContentRating.MAX_AD_CONTENT_RATING_T);
                        break;
                    case "MA":
                        builder.setMaxAdContentRating(RequestConfiguration.MaxAdContentRating.MAX_AD_CONTENT_RATING_MA);
                        break;
                    case "":
                    default:
                        builder.setMaxAdContentRating(RequestConfiguration.MaxAdContentRating.MAX_AD_CONTENT_RATING_UNSPECIFIED);
                        break;
                }
            }

            if (config.has("testDeviceIds")) {
                JSONArray ids = config.getJSONArray("testDeviceIds");
                List<String> testDevices = new ArrayList<>();
                for (int i = 0; i < ids.length(); i++) {
                    testDevices.add(ids.getString(i));
                }
                builder.setTestDeviceIds(testDevices);
            }
        } catch (JSONException e) {

        }

        return builder.build();
    }

    private static Boolean parseBoolean(JSONObject config, String key) {
        if (!config.has(key) || config.isNull(key)) {
            return null;
        }

        Object value = config.opt(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        String valueString = config.optString(key);
        if ("true".equalsIgnoreCase(valueString)) return true;
        if ("false".equalsIgnoreCase(valueString)) return false;

        return null;
    }
}
