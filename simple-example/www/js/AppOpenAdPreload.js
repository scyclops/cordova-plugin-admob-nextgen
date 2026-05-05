// js/AppOpenAdPreload.js

// Only Android: https://github.com/googleads/gma-next-gen-sdk-android-examples/tree/main/java/NextGenExample/app/src/main/java/com/example/nextgenexample/preloading

// fastest, App Open ads will appear very quickly.

let isStartAppOpenAdPreload = false;

function startAppOpenPreloadAd() {
    if (isDeviceready && !isPlatformIOS) {
        if (window.logToScreen) window.logToScreen("Loading App Open Ad Preload...");
        admobNextGen.startAppOpenPreload({
            adUnitId: App_Open_ID,
            isAutoShow: false, // optional: Automatically show when app resumes from background
            retryInterval: 5000 // optional: anti spam
        });
    }
}

function showAppOpenAdPreload() {
    if (isDeviceready && isStartAppOpenAdPreload && !isPlatformIOS) {
        if (window.logToScreen) window.logToScreen("Showing App Open Ad Preload...");
        admobNextGen.showPreloadedAppOpenAd();
    } else {
        if (window.logToScreen) window.logToScreen("App Open Ad Preload is not loaded yet!", "warn");
    }
}

// ==========================================
// EVENT LISTENERS
// ==========================================

document.addEventListener('on.appopen.preload.exhausted', (e) => {
    var err = e.data || e;
    console.error("Preloaded app open ads exhausted: ", err.preloadId);
    if (window.logToScreen) window.logToScreen("Preloaded app open ads exhausted: " + JSON.stringify(err), "warn");
});

document.addEventListener('on.appopen.preload.loaded', (data) => {
    isStartAppOpenAdPreload = true;
    console.log("App open ad preloaded successfully: ", data.preloadId);
    if (window.logToScreen) window.logToScreen("App open ad preloaded successfully: " + data.preloadId);
});

document.addEventListener('on.appopen.preload.failed.show', (e) => {
     var data = e.data || e;
     console.error("No preloaded app open ads available: ", JSON.stringify(data));
    if (window.logToScreen) window.logToScreen("No preloaded app open ads available: " + e.message);
});

document.addEventListener('on.appopen.failed.load', (e) => {
    var err = e.data || e;
    console.error("on appopen failed load", err);
    if (window.logToScreen) window.logToScreen("App Open Load Failed: " + JSON.stringify(err), "warn");
});

document.addEventListener('on.appopen.preload.shown', () => {
    console.log("on appopen preload shown");
    if (window.logToScreen) window.logToScreen("App Open Ad preload Shown Fullscreen", "event");
});


document.addEventListener('on.appopen.preload.dismissed', () => {
    console.log("on appopen preload dismissed");
    isStartAppOpenAdPreload = false;
    if (window.logToScreen) window.logToScreen("App Open Ad preload Dismissed", "event");
});

document.addEventListener('on.appopen.preload.impression', () => {
    console.log("on appopen preload impression");
    if (window.logToScreen) window.logToScreen("App Open preload Impression Recorded", "event");
});

document.addEventListener('on.appopen.preload.clicked', () => {
    console.log("on appopen preload clicked");
    if (window.logToScreen) window.logToScreen("App Open Ad preload Clicked!", "event");
});

/*
https://support.google.com/admob/answer/11322405

Turn on the setting for impression-level ad revenue in your AdMob account:
Sign in to your AdMob account at https://apps.admob.com.
Click Settings in the sidebar.
Click the Account tab.
In the Account controls section, click the Impression-level ad revenue toggle to turn on this setting.
*/
document.addEventListener('on.appopen.preload.revenue', (e) => {
    var data = e.data || e;
    console.log("on appopen preload revenue", data);
    if (window.logToScreen) {
        var revStr = data.value + " " + data.currency;
        window.logToScreen("💰 APPOPEN REVENUE: " + revStr, "reward");
    }
});


