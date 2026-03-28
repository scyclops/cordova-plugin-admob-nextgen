// js/rewardedAd.js

let isRewardedLoad = false;

function createRewardedAds() {
    if (isDeviceready) {
        if (window.logToScreen) window.logToScreen("Loading Rewarded Ad...");
        admobNextGen.createRewarded({
            adUnitId: Rewarded_ID,
            retryInterval: 5000,  // Anti-spam interval
            isAutoShow: false     // Default false (User must opt-in)
        });
    }
}

function showRewardedAds() {
    if (isDeviceready && isRewardedLoad) {
        if (window.logToScreen) window.logToScreen("Showing Rewarded Ad...");
        admobNextGen.showRewarded();
    } else {
        if (window.logToScreen) window.logToScreen("Rewarded Ad is not loaded yet!", "warn");
    }
}

// ==========================================
// EVENT LISTENERS
// ==========================================

document.addEventListener('on.rewarded.loaded', () => {
    isRewardedLoad = true;
    console.log("on rewarded loaded");
    if (window.logToScreen) window.logToScreen("Rewarded Ad Loaded & Ready", "event");
});

document.addEventListener('on.rewarded.failed.load', (e) => {
    var err = e.data || e;
    console.error("on rewarded failed load", err);
    if (window.logToScreen) window.logToScreen("Rewarded Load Failed: " + JSON.stringify(err), "warn");
});

document.addEventListener('on.rewarded.shown', () => {
    console.log("on rewarded shown");
    if (window.logToScreen) window.logToScreen("Rewarded Ad Shown", "event");
});

document.addEventListener('on.rewarded.failed.show', (e) => {
    var err = e.data || e;
    console.error("on rewarded failed show", err);
    if (window.logToScreen) window.logToScreen("Rewarded Show Failed: " + JSON.stringify(err), "warn");
});

// MOST IMPORTANT EVENT: USERS GET REWARDS
document.addEventListener('on.rewarded.earned', (e) => {
    var data = e.data || e;
    console.log("User Earned: " + data.amount + " " + data.type);
    
    if (window.logToScreen) {
        window.logToScreen("🎁 USER EARNED REWARD: " + data.amount + " " + data.type, "reward");
    }
});

document.addEventListener('on.rewarded.dismissed', () => {
    console.log("on rewarded dismissed");
    isRewardedLoad = false;
    if (window.logToScreen) window.logToScreen("Rewarded Ad Dismissed", "event");
    // createRewardedAds(); // Auto reload
});

document.addEventListener('on.rewarded.impression', () => {
    console.log("on rewarded impression");
    if (window.logToScreen) window.logToScreen("Rewarded Impression Recorded", "event");
});

document.addEventListener('on.rewarded.clicked', () => {
    console.log("on rewarded clicked");
    if (window.logToScreen) window.logToScreen("Rewarded Ad Clicked!", "event");
});

/*
https://support.google.com/admob/answer/11322405
Turn on the setting for impression-level ad revenue in your AdMob account.
*/
document.addEventListener('on.rewarded.revenue', (e) => {
    var data = e.data || e;
    console.log("on rewarded revenue", data);
    
    if (window.logToScreen) {
        var revStr = data.value + " " + data.currency;
        window.logToScreen("💰 REWARDED REVENUE: " + revStr, "reward");
    }
});
