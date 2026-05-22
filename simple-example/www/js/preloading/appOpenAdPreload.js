
// Starts the preloading process for App Open Ads.

// This will only be triggered once, as it will hold multiple ads ready to be displayed.
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
    if (isDeviceready && isloadAppOpenAd && !isPlatformIOS) {
        if (window.logToScreen) window.logToScreen("Showing App Open Ad Preload...");
        admobNextGen.showPreloadedAppOpenAd();
    } 
}

// admobNextGen.isAppOpenAdAvailable() // It returns a boolean, can replace something like isloadAppOpenAd

// ==========================================
// EVENT LISTENERS
// ==========================================

document.addEventListener('on.appopen.preload.exhausted', (e) => {
    console.log("Background ad buffer is empty!", JSON.stringify(e))
    // startAppOpenPreloadAd();
});

// Other events are the same as the classic method.

// Note: https://github.com/swaplab-engine/cordova-plugin-admob-nextgen/main/README.md#10-preloader-engine-android-only---next-gen-sdk

