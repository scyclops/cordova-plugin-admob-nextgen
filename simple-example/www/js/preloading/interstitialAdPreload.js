
// Starts the preloading process for Interstitial Ads.

// This will only be triggered once, as it will hold multiple ads ready to be displayed.

function startInterstitialPreload() {
    if (typeof isDeviceready !== 'undefined' && isDeviceready) {
        if (window.logToScreen) window.logToScreen("Starting Interstitial Preloader...");
        admobNextGen.startInterstitialPreload({
            adUnitId: Interstitial_ID,
            bufferSize: 1, // Optional: Number of ads to cache in the background: Default: 1 max 3
            isAutoShow: false,
            retryInterval: 5000
        });
    }
}

function showInterstitialPreload() {
    if (typeof isDeviceready !== 'undefined' && isDeviceready && isLoadInterstitial) {
        if (window.logToScreen) window.logToScreen("Showing Preloaded Interstitial...");
        admobNextGen.showPreloadedInterstitial();
    } 
}

// admobNextGen.isInterstitialAdAvailable() // It returns a boolean, can replace something like isLoadInterstitial

/*
function stopInterstitialPreloader() {
    if (typeof isDeviceready !== 'undefined' && isDeviceready) {
        isLoadInterstitial = false;
        admobNextGen.stopInterstitialPreload();
        if (window.logToScreen) window.logToScreen("Interstitial Preloader Stopped");
    }
}
*/

// ==========================================
// EVENT LISTENERS (PRELOADER SPECIFIC)
// ==========================================

document.addEventListener('on.interstitial.preload.exhausted', (e) => {
    console.log("Background ad buffer is empty!", JSON.stringify(e))
    // startInterstitialPreload();
});

// Other events are the same as the classic method.

// Note: https://github.com/swaplab-engine/cordova-plugin-admob-nextgen#10-preloader-engine-android-only---next-gen-sdk


