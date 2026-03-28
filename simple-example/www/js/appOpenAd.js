// js/appOpenAd.js

let isloadAppOpenAd = false;

function loadAppOpenAds() {
    if (isDeviceready) {
        if (window.logToScreen) window.logToScreen("Loading App Open Ad...");
        admobNextGen.loadAppOpenAd({
            adUnitId: App_Open_ID,
            isAutoShow: false, // Automatically show when app resumes from background
            retryInterval: 5000
        });
    }
}

function showAppOpenAds() {
    if (isDeviceready && isloadAppOpenAd) {
        if (window.logToScreen) window.logToScreen("Showing App Open Ad...");
        admobNextGen.showAppOpenAd();
    } else {
        if (window.logToScreen) window.logToScreen("App Open Ad is not loaded yet!", "warn");
    }
}

// ==========================================
// EVENT LISTENERS
// ==========================================

document.addEventListener('on.appopen.loaded', () => {
    isloadAppOpenAd = true;
    console.log("on appopen loaded");
    if (window.logToScreen) window.logToScreen("App Open Ad Loaded & Ready", "event");
});

document.addEventListener('on.appopen.failed.load', (e) => {
    var err = e.data || e;
    console.error("on appopen failed load", err);
    if (window.logToScreen) window.logToScreen("App Open Load Failed: " + JSON.stringify(err), "warn");
});

document.addEventListener('on.appopen.shown', () => {
    console.log("on appopen shown");
    if (window.logToScreen) window.logToScreen("App Open Ad Shown Fullscreen", "event");
});

document.addEventListener('on.appopen.failed.show', (e) => {
    var err = e.data || e;
    console.error("on appopen failed show", err);
    if (window.logToScreen) window.logToScreen("App Open Show Failed: " + JSON.stringify(err), "warn");
});

document.addEventListener('on.appopen.dismissed', () => {
    console.log("on appopen dismissed");
    isloadAppOpenAd = false;
    if (window.logToScreen) window.logToScreen("App Open Ad Dismissed", "event");
    // loadAppOpenAds(); // Auto reload
});

document.addEventListener('on.appopen.impression', () => {
    console.log("on appopen impression");
    if (window.logToScreen) window.logToScreen("App Open Impression Recorded", "event");
});

document.addEventListener('on.appopen.clicked', () => {
    console.log("on appopen clicked");
    if (window.logToScreen) window.logToScreen("App Open Ad Clicked!", "event");
});

/*
https://support.google.com/admob/answer/11322405

Turn on the setting for impression-level ad revenue in your AdMob account:
Sign in to your AdMob account at https://apps.admob.com.
Click Settings in the sidebar.
Click the Account tab.
In the Account controls section, click the Impression-level ad revenue toggle to turn on this setting.
*/
document.addEventListener('on.appopen.revenue', (e) => {
    var data = e.data || e;
    console.log("on appopen revenue", data);
    if (window.logToScreen) {
        var revStr = data.value + " " + data.currency;
        window.logToScreen("💰 APPOPEN REVENUE: " + revStr, "reward");
    }
});


