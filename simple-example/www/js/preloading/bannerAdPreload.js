
/*
LARGE_LANDSCAPE_ANCHORED_ADAPTIVE
LARGE_PORTRAIT_ANCHORED_ADAPTIVE
CURRENT_ORIENTATION_INLINE_ADAPTIVE
LARGE_ANCHORED_ADAPTIVE
PORTRAIT_INLINE_ADAPTIVE
*/

// Starts the preloading process for Banner Ads.

// This will only be triggered once, as it will hold multiple ads ready to be displayed.
function startBannerPreload() {
    if (isDeviceready) {
        if (window.logToScreen) window.logToScreen("Start Banner Preload Banner Ad...");

        // Grab values dynamically from UI
        var selectedPosition = document.getElementById('banner-preload-pos').value;
        var isOverlappingSelected = document.getElementById('banner-preload-overlap').checked;
        var isCollapsibleSelected = document.getElementById('banner-preload-collapse').checked;
        var isAutoShowSelected = document.getElementById('banner-preload-autoshow').checked;

        admobNextGen.startBannerPreload({
            adUnitId: Banner_ID,
            position: selectedPosition,    // 'top' or 'bottom'
            size: 'ADAPTIVE',              // 'BANNER', 'LARGE_BANNER', 'MEDIUM_RECTANGLE', 'ADAPTIVE', 'FULL_BANNER', 'LEADERBOARD'
            isOverlapping: isOverlappingSelected,     // true = Overlay, false = Push Webview
            collapsible: isCollapsibleSelected,       // true = Enable Collapsible Format (High Revenue)
            retryInterval: 5000,           // Anti-spam delay (ms)
            isAutoShow: isAutoShowSelected
        });
    }
}

function showBannerPreload() {
    if (isDeviceready) {
        if (window.logToScreen) window.logToScreen("Showing Preload Banner Ad...");
        admobNextGen.showPreloadedBanner();
    }
}

function stopBannerPreload() {
    if (isDeviceready) {
        if (window.logToScreen) window.logToScreen("Removing Preload Banner Ad...");
        admobNextGen.stopBannerPreload();
    }
}

function hideBannerPreload() {
    if (isDeviceready) {
        if (window.logToScreen) window.logToScreen("Hiding Preload Banner Ad...");
        admobNextGen.hideBannerPreload();
    }
}

// ==========================================
// EVENT LISTENERS
// ==========================================

document.addEventListener('on.banner.preload.exhausted', (e) => {
    console.log("Background ad buffer is empty!", JSON.stringify(e))
    // startBannerPreload();
});

// Other events are the same as the classic method.

// Note: https://github.com/swaplab-engine/cordova-plugin-admob-nextgen/main/README.md#10-preloader-engine-android-only---next-gen-sdk



