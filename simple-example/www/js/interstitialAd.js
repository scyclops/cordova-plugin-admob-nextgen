// js/interstitialAd.js

let isLoadInterstitial = false;

function createInterstitialAds() {
    if (isDeviceready) {
        if (window.logToScreen) window.logToScreen("Loading Interstitial Ad...");
        admobNextGen.createInterstitial({
            adUnitId: Interstitial_ID,
            isAutoShow: false, // Show immediately when loaded
            retryInterval: 5000
        });
    }
}

function showInterstitialAds() {
    if (isDeviceready && isLoadInterstitial) {
        if (window.logToScreen) window.logToScreen("Showing Interstitial Ad...");
        admobNextGen.showInterstitial();
    } else {
        if (window.logToScreen) window.logToScreen("Interstitial Ad is not loaded yet!", "warn");
    }
}

// ==========================================
// EVENT LISTENERS
// ==========================================

document.addEventListener('on.interstitial.loaded', () => {
    isLoadInterstitial = true;
    console.log("on interstitial loaded");
    if (window.logToScreen) window.logToScreen("Interstitial Ad Loaded & Ready", "event");
});

document.addEventListener('on.interstitial.failed.load', (e) => {
    var err = e.data || e;
    console.error("on interstitial failed load", err);
    if (window.logToScreen) window.logToScreen("Interstitial Load Failed: " + JSON.stringify(err), "warn");
});

document.addEventListener('on.interstitial.shown', () => {
    console.log("on interstitial shown");
    if (window.logToScreen) window.logToScreen("Interstitial Ad Shown Fullscreen", "event");
});

document.addEventListener('on.interstitial.failed.show', (e) => {
    var err = e.data || e;
    console.error("on interstitial failed show", err);
    if (window.logToScreen) window.logToScreen("Interstitial Show Failed: " + JSON.stringify(err), "warn");
});

document.addEventListener('on.interstitial.dismissed', () => {
    console.log("on interstitial dismissed");
    isLoadInterstitial = false;
    if (window.logToScreen) window.logToScreen("Interstitial Ad Dismissed", "event");
    // createInterstitialAds(); // Auto reload
});

document.addEventListener('on.interstitial.impression', () => {
    console.log("on interstitial impression");
    if (window.logToScreen) window.logToScreen("Interstitial Impression Recorded", "event");
});

document.addEventListener('on.interstitial.clicked', () => {
    console.log("on interstitial clicked");
    if (window.logToScreen) window.logToScreen("Interstitial Ad Clicked!", "event");
});

/*
https://support.google.com/admob/answer/11322405

Turn on the setting for impression-level ad revenue in your AdMob account:
Sign in to your AdMob account at https://apps.admob.com.
Click Settings in the sidebar.
Click the Account tab.
In the Account controls section, click the Impression-level ad revenue toggle to turn on this setting.
*/
document.addEventListener('on.interstitial.revenue', (e) => {
    var data = e.data || e;
    console.log("on interstitial revenue", data);
    
    if (window.logToScreen) {
        var revStr = data.value + " " + data.currency;
        window.logToScreen("💰 INTERSTITIAL REVENUE: " + revStr, "reward");
    }
});



