// js/bannerAd.js

// new large banner adaptive min plugin version: 1.2+
/*
LARGE_LANDSCAPE_ANCHORED_ADAPTIVE
LARGE_PORTRAIT_ANCHORED_ADAPTIVE
CURRENT_ORIENTATION_INLINE_ADAPTIVE
LARGE_ANCHORED_ADAPTIVE
PORTRAIT_INLINE_ADAPTIVE
*/

function createBannerAds() {
    if (isDeviceready) {
        if (window.logToScreen) window.logToScreen("Creating Banner Ad...");

        // Grab values dynamically from UI
        var selectedPosition = document.getElementById('banner-preload-pos').value;
        var isOverlappingSelected = document.getElementById('banner-preload-overlap').checked;
        var isCollapsibleSelected = document.getElementById('banner-preload-collapse').checked;
        var isAutoShowSelected = document.getElementById('banner-preload-autoshow').checked;

        admobNextGen.createBanner({
            adUnitId: Banner_ID,
            position: selectedPosition, // 'top' or 'bottom'
            size: 'ADAPTIVE',  // 'BANNER', 'LARGE_BANNER', 'MEDIUM_RECTANGLE', 'ADAPTIVE', 'FULL_BANNER', 'LEADERBOARD'
            isOverlapping: isOverlappingSelected,     // true = Overlay, false = Push Webview
            collapsible: isCollapsibleSelected,       // true = Enable Collapsible Format (High Revenue)
            retryInterval: 5000,      // Anti-spam delay (ms)
            isAutoShow: isAutoShowSelected
        });
    }
}

function showBannerAds() {
    if (isDeviceready) {
        if (window.logToScreen) window.logToScreen("Showing Banner Ad...");
        admobNextGen.showBanner();
    }
}

function hideBannerAds() {
    if (isDeviceready) {
        if (window.logToScreen) window.logToScreen("Hiding Banner Ad...");
        admobNextGen.hideBanner();
    }
}

function removeBannerAds() {
    if (isDeviceready) {
        if (window.logToScreen) window.logToScreen("Removing Banner Ad...");
        admobNextGen.removeBanner();
    }
}

// ==========================================
// EVENT LISTENERS
// ==========================================

document.addEventListener('on.banner.load', (e) => {
    var data = e.data || e;
    console.log("Banner Loaded: " + data.width + "x" + data.height);
    
    if (window.logToScreen) {
        window.logToScreen("Banner Loaded: " + data.width + "x" + data.height + " (Collapsible: " + data.isCollapsible + ")", "event");
    }
});

document.addEventListener('on.banner.failed', (e) => {
    var err = e.data || e;
    console.error(err);
    if (window.logToScreen) {
        window.logToScreen("Banner Failed: " + JSON.stringify(err), "warn");
    }
});

document.addEventListener('on.banner.clicked', () => {
    console.log("Clicked");
    if (window.logToScreen) window.logToScreen("Banner Clicked!", "event");
});

document.addEventListener('on.banner.impression', () => {
    console.log("Impression");
    if (window.logToScreen) window.logToScreen("Banner Impression Recorded", "event");
});

document.addEventListener('on.banner.opened', () => {
    if (window.logToScreen) window.logToScreen("Banner Opened (Fullscreen overlay)", "event");
});

document.addEventListener('on.banner.closed', () => {
    if (window.logToScreen) window.logToScreen("Banner Closed", "event");
});

/*
https://support.google.com/admob/answer/11322405

Turn on the setting for impression-level ad revenue in your AdMob account:
Sign in to your AdMob account at https://apps.admob.com.
Click Settings in the sidebar.
Click the Account tab.
In the Account controls section, click the Impression-level ad revenue toggle to turn on this setting.
*/

document.addEventListener('on.banner.revenue', (e) => {
    var data = e.data || e;
    console.log("on banner revenue", data);
    
    if (window.logToScreen) {
        var revStr = data.value + " " + data.currency;
        window.logToScreen("💰 BANNER REVENUE: " + revStr, "reward");
    }
});



