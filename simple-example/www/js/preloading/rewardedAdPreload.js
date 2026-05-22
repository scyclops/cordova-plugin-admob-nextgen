
// Starts the preloading process for Rewarded Ads.

// This will only be triggered once, as it will hold multiple ads ready to be displayed.

function startRewardedPreload() {
    if (typeof isDeviceready !== 'undefined' && isDeviceready) {
        if (window.logToScreen) window.logToScreen("Starting Rewarded Preloader...");
        
        admobNextGen.startRewardedPreload({
            adUnitId: Rewarded_ID,
            bufferSize: 1, // Optional: Number of ads to cache in the background: Default: 1 max 3
            isAutoShow: false,
            retryInterval: 5000
        });
    }
}

function showRewardedPreload() {
    if (typeof isDeviceready !== 'undefined' && isDeviceready && isRewardedLoad) {
        if (window.logToScreen) window.logToScreen("Showing Preloaded Rewarded...");
        admobNextGen.showPreloadedRewarded();
    } 
}

// admobNextGen.isRewardedAdAvailable() // It returns a boolean, can replace something like isRewardedLoad

/*
function stopRewardedPreloader() {
    if (typeof isDeviceready !== 'undefined' && isDeviceready) {
        admobNextGen.stopRewardedPreload();
        isRewardedLoad = false;
        if (window.logToScreen) window.logToScreen("Rewarded Preloader Stopped");
    }
}
*/

// ==========================================
// EVENT LISTENERS (PRELOADER SPECIFIC)
// ==========================================

document.addEventListener('on.rewarded.preload.exhausted', (e) => {
    console.log("Background ad buffer is empty!", JSON.stringify(e))
    // startRewardedPreload();
});

// Other events are the same as the classic method.

// Note: https://github.com/swaplab-engine/cordova-plugin-admob-nextgen/main/README.md#10-preloader-engine-android-only---next-gen-sdk


