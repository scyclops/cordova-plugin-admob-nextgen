// js/rewardedInterstitialAd.js

let isRewardedInterstitialLoad = false;

function createRewardedInterstitialAds() {
    if (isDeviceready) {
        if (window.logToScreen) window.logToScreen("Loading Rewarded Interstitial...");
        
        admobNextGen.createRewardedInterstitial({
            adUnitId: Rewarded_Interstitial_ID,
            isAutoShow: false,
            retryInterval: 5000 // Anti-spam interval
        });
    }
}

function showRewardedInterstitialAds() {
    if (isDeviceready && isRewardedInterstitialLoad) {
        if (window.logToScreen) window.logToScreen("Showing Rewarded Interstitial...");
        admobNextGen.showRewardedInterstitial();
    } else {
        if (window.logToScreen) window.logToScreen("Rewarded Interstitial is not loaded yet!", "warn");
    }
}

// ==========================================
// EVENT LISTENERS
// ==========================================

document.addEventListener('on.rewardedInter.loaded', () => {
    isRewardedInterstitialLoad = true;
    console.log("on rewarded interstitial loaded");
    if (window.logToScreen) window.logToScreen("Rew. Interstitial Loaded & Ready", "event");
});

document.addEventListener('on.rewardedInter.failed.load', (e) => {
    var err = e.data || e;
    console.error("on rewarded interstitial failed load", err);
    if (window.logToScreen) window.logToScreen("Rew. Interstitial Load Failed: " + JSON.stringify(err), "warn");
});

document.addEventListener('on.rewardedInter.shown', () => {
    console.log("on rewarded interstitial shown");
    if (window.logToScreen) window.logToScreen("Rew. Interstitial Shown", "event");
});

document.addEventListener('on.rewardedInter.failed.show', (e) => {
    var err = e.data || e;
    console.error("on rewarded interstitial failed show", err);
    if (window.logToScreen) window.logToScreen("Rew. Interstitial Show Failed: " + JSON.stringify(err), "warn");
});

// MOST IMPORTANT EVENT: USERS GET REWARDS
document.addEventListener('on.rewardedInter.earned', (e) => {
    var data = e.data || e;
    console.log("User Earned: " + data.amount + " " + data.type);
    
    // Turn off the load flag if you get a reward (to prevent status bugs on some networks)
    isRewardedInterstitialLoad = false;
    
    if (window.logToScreen) {
        window.logToScreen("🎁 USER EARNED REWARD (Inter): " + data.amount + " " + data.type, "reward");
    }
});

document.addEventListener('on.rewardedInter.dismissed', () => {
    console.log("on rewarded interstitial dismissed");
    isRewardedInterstitialLoad = false;
    if (window.logToScreen) window.logToScreen("Rew. Interstitial Dismissed", "event");
    // createRewardedInterstitialAds(); // Auto reload
});

document.addEventListener('on.rewardedInter.impression', () => {
    console.log("on rewarded interstitial impression");
    if (window.logToScreen) window.logToScreen("Rew. Interstitial Impression", "event");
});

document.addEventListener('on.rewardedInter.clicked', () => {
    console.log("on rewarded interstitial clicked");
    if (window.logToScreen) window.logToScreen("Rew. Interstitial Clicked!", "event");
});

/*
https://support.google.com/admob/answer/11322405
Turn on the setting for impression-level ad revenue in your AdMob account.
*/
document.addEventListener('on.rewardedInter.revenue', (e) => {
    var data = e.data || e;
    console.log("on rewarded interstitial revenue", data);
    
    if (window.logToScreen) {
        var revStr = data.value + " " + data.currency;
        window.logToScreen("💰 REW. INTERSTITIAL REVENUE: " + revStr, "reward");
    }
});
