package com.emi.cordova.admob.nextgen;

import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.libraries.ads.mobile.sdk.MobileAds;
import com.google.android.libraries.ads.mobile.sdk.common.RequestConfiguration;

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
            boolean muted = args.getBoolean(0);
            MobileAds.setUserMutedApp(muted);
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
            callbackContext.success("Configuration Updated");
        } catch (JSONException e) {
            callbackContext.error("Config Error: " + e.getMessage());
        }
    }

    public static RequestConfiguration buildRequestConfiguration(JSONObject config) {
        RequestConfiguration.Builder builder = new RequestConfiguration.Builder();

        try {

            RequestConfiguration.TagForChildDirectedTreatment coppaTag = parseChildDirectedTag(config, "tagForChildDirectedTreatment");
            builder.setTagForChildDirectedTreatment(coppaTag);

            RequestConfiguration.TagForUnderAgeOfConsent tfuaTag = parseUnderAgeTag(config, "tagForUnderAgeOfConsent");
            builder.setTagForUnderAgeOfConsent(tfuaTag);

            if (config.has("maxAdContentRating")) {
                String rating = config.getString("maxAdContentRating");
                if ("G".equals(rating)) builder.setMaxAdContentRating(RequestConfiguration.MaxAdContentRating.MAX_AD_CONTENT_RATING_G);
                else if ("PG".equals(rating)) builder.setMaxAdContentRating(RequestConfiguration.MaxAdContentRating.MAX_AD_CONTENT_RATING_PG);
                else if ("T".equals(rating)) builder.setMaxAdContentRating(RequestConfiguration.MaxAdContentRating.MAX_AD_CONTENT_RATING_T);
                else if ("MA".equals(rating)) builder.setMaxAdContentRating(RequestConfiguration.MaxAdContentRating.MAX_AD_CONTENT_RATING_MA);
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

    private static RequestConfiguration.TagForChildDirectedTreatment parseChildDirectedTag(JSONObject config, String key) {
        if (!config.has(key) || config.isNull(key)) {
            return RequestConfiguration.TagForChildDirectedTreatment.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED;
        }

        if (config.optBoolean(key, false) || !config.optBoolean(key, true)) { 
            Object val = config.opt(key);
            if (val instanceof Boolean) {
                return (Boolean) val ?
                        RequestConfiguration.TagForChildDirectedTreatment.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE :
                        RequestConfiguration.TagForChildDirectedTreatment.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE;
            }
        }

        String valStr = config.optString(key);
        if ("true".equalsIgnoreCase(valStr)) {
            return RequestConfiguration.TagForChildDirectedTreatment.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE;
        }
        if ("false".equalsIgnoreCase(valStr)) {
            return RequestConfiguration.TagForChildDirectedTreatment.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE;
        }

        return RequestConfiguration.TagForChildDirectedTreatment.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED;
    }

    private static RequestConfiguration.TagForUnderAgeOfConsent parseUnderAgeTag(JSONObject config, String key) {
        if (!config.has(key) || config.isNull(key)) {
            return RequestConfiguration.TagForUnderAgeOfConsent.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED;
        }

        Object val = config.opt(key);
        if (val instanceof Boolean) {
            return (Boolean) val ?
                    RequestConfiguration.TagForUnderAgeOfConsent.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE :
                    RequestConfiguration.TagForUnderAgeOfConsent.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE;
        }

        String valStr = config.optString(key);
        if ("true".equalsIgnoreCase(valStr)) {
            return RequestConfiguration.TagForUnderAgeOfConsent.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE;
        }
        if ("false".equalsIgnoreCase(valStr)) {
            return RequestConfiguration.TagForUnderAgeOfConsent.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE;
        }

        return RequestConfiguration.TagForUnderAgeOfConsent.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED;
    }
}
