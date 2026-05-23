package com.emi.cordova.admob.nextgen;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView; 
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;

public class ConsentExecutor {

    private static final String TAG = "AdMobConsent";
    private CordovaInterface cordova;
    private CordovaWebView webView; 
    private ConsentInformation consentInformation;

    public ConsentExecutor(CordovaInterface cordova, CordovaWebView webView) {
        this.cordova = cordova;
        this.webView = webView;
        this.consentInformation = UserMessagingPlatform.getConsentInformation(cordova.getActivity());
    }

    public void requestConsentInfo(JSONArray args, CallbackContext callbackContext) {
            try {
                JSONObject options = args.optJSONObject(0);

                boolean debugMode = false;
                String manualTestDeviceId = "";
                boolean resetConsent = false;
                boolean tagForUnderAgeOfConsent = false;

                if (options != null) {
                    debugMode = options.optBoolean("debug", false);
                    manualTestDeviceId = options.optString("testDeviceId", "");
                    resetConsent = options.optBoolean("reset", false);

                    if (options.has("tagForUnderAgeOfConsent")) {
                        tagForUnderAgeOfConsent = options.getBoolean("tagForUnderAgeOfConsent");
                    }
                }

                if (resetConsent) {
                    consentInformation.reset();

                }

                ConsentRequestParameters.Builder paramsBuilder = new ConsentRequestParameters.Builder();
                paramsBuilder.setTagForUnderAgeOfConsent(tagForUnderAgeOfConsent);

                if (debugMode) {
                    ConsentDebugSettings.Builder debugSettingsBuilder = new ConsentDebugSettings.Builder(cordova.getActivity())
                            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA);

                    if (!manualTestDeviceId.isEmpty()) {
                        debugSettingsBuilder.addTestDeviceHashedId(manualTestDeviceId);

                    } else {
                        String deviceId = getDeviceId();
                        if (deviceId != null) {
                            debugSettingsBuilder.addTestDeviceHashedId(deviceId);

                        }
                    }

                    paramsBuilder.setConsentDebugSettings(debugSettingsBuilder.build());
                }

                ConsentRequestParameters params = paramsBuilder.build();
                Activity activity = cordova.getActivity();

                activity.runOnUiThread(() -> {
                    consentInformation.requestConsentInfoUpdate(
                            activity,
                            params,
                            () -> {

                                fireEvent("on.consent.info.update", null);

                                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                                        activity,
                                        (FormError loadAndShowError) -> {
                                            if (loadAndShowError != null) {

                                                sendErrorEvent(loadAndShowError);
                                                callbackContext.error(loadAndShowError.getMessage());
                                            } else {

                                                fireEvent("on.consent.form.dismissed", null);

                                                sendConsentStatus(callbackContext);
                                            }
                                        }
                                );
                            },
                            (FormError requestConsentError) -> {

                                sendErrorEvent(requestConsentError);
                                callbackContext.error(requestConsentError.getMessage());
                            }
                    );
                });

            } catch (Exception e) {
                callbackContext.error("Exception: " + e.getMessage());
            }
    }

    public void getTCData(CallbackContext callbackContext) {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(cordova.getActivity());
            JSONObject tcData = new JSONObject();

            String tcString = prefs.getString("IABTCF_TCString", "");
            String purposeConsents = prefs.getString("IABTCF_PurposeConsents", "");
            String vendorConsents = prefs.getString("IABTCF_VendorConsents", "");
            int gdprApplies = prefs.getInt("IABTCF_gdprApplies", 0);

            tcData.put("tcString", tcString);
            tcData.put("purposeConsents", purposeConsents);
            tcData.put("vendorConsents", vendorConsents);
            tcData.put("gdprApplies", gdprApplies);

            boolean isPersonalizedAllowed = false;
            String statusMessage = "Unknown";

            if (gdprApplies == 0) {

                isPersonalizedAllowed = true;
                statusMessage = "Not GDPR region. Personalized Ads allowed by default.";
            } else {

                if (purposeConsents != null && purposeConsents.length() > 0) {
                    char p1 = purposeConsents.charAt(0);
                    if (p1 == '1') {
                        isPersonalizedAllowed = true;
                        statusMessage = "Purpose 1 Granted. Personalized Ads allowed.";
                    } else {
                        isPersonalizedAllowed = false;
                        statusMessage = "Purpose 1 Denied. Non-Personalized / Limited Ads only.";
                    }
                } else {

                    isPersonalizedAllowed = false;
                    statusMessage = "No consent data found (User hasn't answered yet).";
                }
            }

            tcData.put("isPersonalizedAllowed", isPersonalizedAllowed); 
            tcData.put("statusMessage", statusMessage); 

            callbackContext.success(tcData);
        } catch (Exception e) {
            callbackContext.error("Failed to read TC Data: " + e.getMessage());
        }
    }

    private String getDeviceId() {
        try {
            Context context = cordova.getActivity();
            @SuppressLint("HardwareIds") String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            if (androidId == null || androidId.isEmpty()) return null;
            return md5(androidId).toUpperCase(Locale.getDefault());
        } catch (Exception e) {
            return null;
        }
    }

    private String md5(String s) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String h = Integer.toHexString(0xFF & b);
                while (h.length() < 2) h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    private void sendConsentStatus(CallbackContext callbackContext) {
        try {
            JSONObject result = new JSONObject();
            boolean canRequestAds = consentInformation.canRequestAds();
            result.put("canRequestAds", canRequestAds);

            ConsentInformation.PrivacyOptionsRequirementStatus requirementStatus =
                    consentInformation.getPrivacyOptionsRequirementStatus();
            result.put("privacyOptionsRequirementStatus", requirementStatus.name());
            result.put("isPrivacyOptionsRequired",
                    requirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED);

            result.put("consentStatus", consentInformation.getConsentStatus());

            fireEvent("on.consent.status.change", result);

            if (callbackContext != null) {
                callbackContext.success(result);
            }
        } catch (JSONException e) {
            if (callbackContext != null) callbackContext.error("JSON Error");
        }
    }

    public void canRequestAds(CallbackContext callbackContext) {
        callbackContext.success(consentInformation.canRequestAds() ? 1 : 0);
    }

    public void showPrivacyOptionsForm(CallbackContext callbackContext) {
        Activity activity = cordova.getActivity();
        cordova.getActivity().runOnUiThread(() -> {
            UserMessagingPlatform.showPrivacyOptionsForm(
                    activity,
                    (FormError formError) -> {
                        if (formError != null) {
                            sendErrorEvent(formError);
                            callbackContext.error(formError.getMessage());
                        } else {

                            fireEvent("on.consent.form.dismissed", null);
                            sendConsentStatus(callbackContext);
                        }
                    }
            );
        });
    }

    private void sendErrorEvent(FormError error) {
        try {
            JSONObject errData = new JSONObject();
            errData.put("code", error.getErrorCode());
            errData.put("message", error.getMessage());
            fireEvent("on.consent.error", errData);
        } catch (JSONException e) {}
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

            String jsCommand = js.toString();
            if (webView != null) webView.loadUrl(jsCommand);
        });
    }
}
