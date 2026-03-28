# Cordova AdMob Next-Gen Plugin

[![Android](https://img.shields.io/badge/Platform-Android-green?logo=android)](https://www.android.com)
[![iOS](https://img.shields.io/badge/Platform-iOS-lightgrey?logo=apple)](https://www.apple.com/ios)
[![AdMob Next Gen](https://img.shields.io/badge/SDK-Google%20Mobile%20Ads%20Next--Gen-blue)](https://ads-developers.googleblog.com/2026/01/announcing-google-mobile-ads-next-gen.html)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

[![NPM version](https://img.shields.io/npm/v/cordova-plugin-admob-nextgen.svg)](https://www.npmjs.com/package/cordova-plugin-admob-nextgen)
[![Downloads](https://img.shields.io/npm/dm/cordova-plugin-admob-nextgen.svg)](https://www.npmjs.com/package/cordova-plugin-admob-nextgen)
[![License](https://img.shields.io/npm/l/cordova-plugin-admob-nextgen.svg)](https://github.com/swaplab-engine/cordova-plugin-admob-nextgen/blob/main/LICENSE)

**The Ultimate AdMob Monetization Solution for Cordova/Capacitor/Framework7.**

This plugin is a complete **rewrite and re-architecture** of the classic AdMob integration, built specifically for the **[Google Mobile Ads Next Generation SDK](https://ads-developers.googleblog.com/2026/01/announcing-google-mobile-ads-next-gen.html)**. It moves away from legacy implementations to modern `SurfaceControl`, optimized layouts, and background threading, ensuring maximum performance, stability, and revenue.

> **Maintained by the original creator of [EMI-INDO/emi-indo-cordova-plugin-admob](https://github.com/EMI-INDO/emi-indo-cordova-plugin-admob).** This is not just an update; it is a brand new engine designed for 2026 and beyond.

---

## 🚀 Why Next-Gen?

Google has introduced a fundamental shift in how ads are rendered on Android. This plugin aligns perfectly with those changes to solve the most critical issues developers face today:

1.  **Lifecycle Stability:** Solves the notorious issue where Full-Screen Ads (Interstitial, Rewarded, App Open) would crash or disappear when the app goes to the background and returns to the foreground.
2.  **Android 15+ Compatibility:** Includes a built-in workaround for the [Android 15 Edge-to-Edge enforcement](https://groups.google.com/g/google-admob-ads-sdk/c/WyGsRV--EoE). It ensures the "Close/X" buttons on full-screen ads are never hidden behind the system navigation bar (API Level 35 fix).
3.  **Clean Architecture:** Written from scratch using the official [Next Gen SDK Examples](https://github.com/googleads/gma-next-gen-sdk-android-examples), ensuring long-term compliance with Google Policies.

---

## 💰 Mission: Revenue Maximization

This plugin is designed with one goal: **increasing your eCPM and fill rates**. We have implemented advanced formats and technologies that are proven to outperform standard implementations.

### 1. True Next-Gen Speed: Ad Preloading API ⚡
This is the **real** Next-Gen feature. We have implemented the specific Preload API methods: `startBannerPreload` and `showPreloadedBanner`.
* **Zero Latency:** Instead of loading an ad when you need it, the plugin maintains a "pool" of ready-to-show ads in the background.
* **Instant Show:** When you call `showPreloadedBanner`, the ad appears instantly (0ms delay) from the pool.

### 2. Collapsible Banners (The Revenue Booster)
Standard banners are often ignored by users. This plugin supports **Collapsible Banners** natively.
* **Impact:** Typically delivers **3-5% higher revenue** than standard banners.
* **Mechanism:** Shows a larger ad initially (anchored top/bottom) that can be collapsed by the user, drastically increasing visibility and click-through rates (CTR).

### 3. Native Advanced Overlay (The Banner Killer)
Move beyond simple 320x50 banners. The Native Overlay feature allows you to render high-performance Native Ads that look like system notifications.
* **Impact:** Can yield **5-10% higher revenue** compared to standard banners due to higher advertiser demand for Native assets.
* **Smart Templates:**
    * `banner_bottom`: A sleek, notification-style ad docked at the bottom.
    * `banner_top`: Docked at the top.
    * `modal_center`: A popup-style native ad.
    * **Policy Safe:** Includes an `isOverlapping` parameter. You can choose to overlay the ad (float) or push the webview content (safe layout), preventing accidental clicks.

---


## 🛡️ Safety & Reliability

We prioritize the safety of your AdMob account and the stability of your app.

* **Smart Throttling (`retryInterval`)**: Prevents accidental spamming of ad requests using a global interval validation.
* **Background Thread Loading**: All ad requests are dispatched on background threads (`cordova.getThreadPool()`), ensuring your app's UI never freezes.

---


## Cordova/Capacitor/Framework7 AdMob Next-Gen: Installation & Usage Guide

## 1. Cordova or Framework7

### Option A: Via CLI
Install the plugin directly using the Cordova CLI. You must provide your AdMob App ID.

    cordova plugin add cordova-plugin-admob-nextgen --save --variable APP_ID_ANDROID="ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy" --variable APP_ID_IOS="ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy"

### Option B: Via config.xml
Add this to your `config.xml` to restore the plugin automatically.

    <plugin name="cordova-plugin-admob-nextgen" spec="latest">
        <variable name="APP_ID_ANDROID" value="ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy" />
        <variable name="APP_ID_IOS" value="ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy" />
    </plugin>


## For Capacitor users
**[⚡ Capacitor Integration: Automated Setup for AdMob Next Gen ](https://github.com/swaplab-engine/cordova-plugin-admob-nextgen/discussions/1)**.

**[⚡ AdMob Next Gen - Starter Templates 🚀 ](https://github.com/swaplab-engine/cordova-plugin-admob-nextgen-template)**.

---

**[⚡ FULL Cordova - simple example 🚀 ](https://github.com/swaplab-engine/cordova-plugin-admob-nextgen/tree/main/simple-example/www/js)**.

## 2. Configuration & Initialization (CRITICAL)

**IMPORTANT:** Configure Global Settings and handle Privacy Consent (UMP) **BEFORE** initializing the SDK.

### Step 1: Global Configuration (Run First)

    admobNextGen.setRequestConfiguration({
        maxAdContentRating: 'G',        // 'G', 'PG', 'T', 'MA'
        tagForChildDirectedTreatment: true, // true/false/null
        tagForUnderAgeOfConsent: true       // true/false/null
    });

    // Optional: Mute ads globally
    admobNextGen.setAppVolume(0.5);
    admobNextGen.setAppMuted(true);

### Step 2: Request Consent (UMP) & Initialize

    // 1. Request Consent Information
    admobNextGen.requestConsentInfo({
        debug: false, // Set true for testing
        reset: false
    }, function() {
        console.log("Consent Info Ready.");
        startSdk();

    }, function(err) {
        console.error("Consent Error", err);
        startSdk();
    });


    
    // --- Consent Events ---
    document.addEventListener('on.consent.status.change', (data) => {
        console.log(data);
    });
    on.consent.info.update
    on.consent.form.dismissed
    on.consent.status.change  (obj)
    on.consent.error  (obj)
    
   


    // 3. Initialize the SDK
    function startSdk() {
        admobNextGen.initialize(function() {
            console.log(">>> AdMob SDK Initialized & Ready <<<");
        }, function(err) {
            console.error("SDK Init Failed", err);
        });
    }


    // 4. Privacy options
        admobNextGen.showPrivacyOptionsForm(function() {
            startSdk();
        }, function(err) {
            startSdk();
        });


### App Tracking Transparency (ATT / IDFA) - iOS Only
For iOS 14+, Apple requires you to prompt the user before tracking their IDFA. 
The plugin provides manual control over this prompt so you can display it at the right time in your app's lifecycle.

```
function checkAppTracking() {
    admobNextGen.requestTrackingAuthorization(
        function(status) {
            console.log("ATT Status: " + status); 
            // Returns: 'AUTHORIZED', 'DENIED', 'NOT_DETERMINED', 'RESTRICTED'
            
            // Regardless of the ATT choice, initialize the SDK
            startSdk();
        },
        function(err) {
            console.warn("ATT Request Failed", err);
            startSdk();
        }
    );
}

// You can also silently check the status anytime:
// admobNextGen.getTrackingAuthorizationStatus(success, error);
```   

### Step 3: Check Consent Status (Smart Analysis)
You can check if the user has granted permission for Personalized Ads without parsing complex IAB strings manually.

    admobNextGen.getTCData(function(data) {
        // --- SMART ANALYSIS (Easy) ---
        console.log("Personalized Allowed: " + data.isPersonalizedAllowed); // Boolean: true/false
        console.log("Status: " + data.statusMessage);

        // --- RAW DATA (Expert) ---
        console.log("TC String: " + data.tcString);
        console.log("GDPR Applies: " + data.gdprApplies); // 1=Yes, 0=No
        console.log("Purpose Consents: " + data.purposeConsents);
    });

---

## 3. Banner Ads

Supports **Adaptive**, **Standard**, and **Collapsible** banners.

### Create Banner

    admobNextGen.createBanner({
        adUnitId: 'ca-app-pub-xxx/xxx',
        position: 'bottom',       // 'top' or 'bottom'
        size: 'ADAPTIVE',         // 'BANNER', 'LARGE_BANNER', 'MEDIUM_RECTANGLE', 'ADAPTIVE', 'FULL_BANNER', 'LEADERBOARD'
        isOverlapping: false,     // true = Overlay, false = Push Webview
        collapsible: true,        // true = Enable Collapsible Format (High Revenue)
        retryInterval: 5000,      // Anti-spam delay (ms)
        isAutoShow: true
    });

### Banner Methods

    admobNextGen.showBanner();
    admobNextGen.hideBanner();
    admobNextGen.removeBanner();

### Banner Events

    document.addEventListener('on.banner.load', (data) => {
        console.log("Banner Loaded: " + data.width + "x" + data.height);
        console.log("Collapsible: " + data.isCollapsible);
    });
    document.addEventListener('on.banner.failed', (err) => console.error(err));
    document.addEventListener('on.banner.revenue', (data) => {
        console.log("Revenue: " + data.value + " " + data.currency);
    });
    document.addEventListener('on.banner.clicked', () => console.log("Clicked"));
    document.addEventListener('on.banner.impression', () => console.log("Impression"));

    // else
    on.banner.opened
    on.banner.closed
    on.banner.failed.show
    on.banner.refreshed
    on.banner.refresh.failed

---

## 4. Native Ads (Advanced Overlay) - Android Only

High-performance native templates.

### Example A: Using Templates (Recommended)

    admobNextGen.createNativeAd({
        adUnitId: 'ca-app-pub-xxx/xxx',
        view: 'banner_bottom',    // Presets: 'banner_top', 'banner_bottom', 'modal_center'
        isOverlapping: false,     // true = Overlay, false = Push Content
        retryInterval: 5000
    });

### Example B: Custom Position (Manual Coordinates)

    admobNextGen.createNativeAd({
        adUnitId: 'ca-app-pub-xxx/xxx',
        view: 'custom',
        x: 20,              // X Position in dp
        y: 100,             // Y Position in dp
        width: 300,         // Width in dp
        height: 300,        // Height in dp
        isOverlapping: true // Typically true for custom floating ads
    });

### Native Methods & Events

    admobNextGen.removeNativeAd();

    document.addEventListener('on.native.loaded', () => console.log("Native Loaded"));
    document.addEventListener('on.native.revenue', (data) => console.log("Revenue:", data.value));

    // else
    on.native.shown
    on.native.failed
    on.native.dismissed
    on.native.show.failed  (obj)
    on.native.impression
    on.native.clicked


---

## 5. Ad Preloading (Banner) - Android Only

Use the background engine to pool ads for 0ms latency.

### Usage

    // 1. Start Engine
    admobNextGen.startBannerPreload({
        adUnitId: 'ca-app-pub-xxx/xxx',
        size: 'ADAPTIVE',     // 'BANNER', 'LARGE_BANNER', 'MEDIUM_RECTANGLE', 'ADAPTIVE', 'FULL_BANNER', 'LEADERBOARD'
        position: 'bottom',   // 'top' or 'bottom'
        collapsible: false,
        isOverlapping: false // Coba mode Push
    });

    // 2. Show Instantly (from pool)
    admobNextGen.showPreloadedBanner();

    // 3. Stop Engine
    admobNextGen.stopBannerPreload();


    ### Banner Events

    document.addEventListener('on.banner.load', (data) => {
        console.log("Banner Loaded: " + data.width + "x" + data.height);
        console.log("Collapsible: " + data.isCollapsible);
    });
    document.addEventListener('on.preload.failed', (err) => console.error(err));
    document.addEventListener('on.banner.revenue', (data) => {
        console.log("Revenue: " + data.value + " " + data.currency);
    });
    document.addEventListener('on.banner.clicked', () => console.log("Clicked"));
    document.addEventListener('on.banner.impression', () => console.log("Impression"));

    // else
    on.preload.available
    on.preload.failed
    on.preload.exhausted
    on.banner.opened
    on.banner.closed
    on.banner.failed.show
    on.banner.refreshed
    on.banner.refresh.failed

---

## 6. App Open Ads

Supports Auto-Resume logic.

### Load App Open

    admobNextGen.loadAppOpenAd({
        adUnitId: 'ca-app-pub-xxx/xxx',
        isAutoShow: true, // Automatically show when app resumes from background
        retryInterval: 5000
    });

### Manual Show

    admobNextGen.showAppOpenAd();

### App Open Events

    document.addEventListener('on.appopen.revenue', (data) => console.log(data));
    document.addEventListener('on.appopen.dismissed', () => console.log("Closed"));

    // else
    on.appopen.loaded
    on.appopen.failed.load (obj)
    on.appopen.failed.show (obj)
    on.appopen.revenue (obj)
    on.appopen.shown
    on.appopen.dismissed
    on.appopen.failed.show (obj)
    on.appopen.impression
    on.appopen.clicked


---

## 7. Interstitial Ads

### Load & Show

    admobNextGen.createInterstitial({
        adUnitId: 'ca-app-pub-xxx/xxx',
        isAutoShow: true, // Show immediately when loaded
        retryInterval: 5000
    });

    // Manual Show
    // admobNextGen.showInterstitial();

### Events

    document.addEventListener('on.interstitial.revenue', (data) => console.log(data));
    document.addEventListener('on.interstitial.dismissed', () => console.log("Closed"));

    // else
    on.interstitial.loaded
    on.interstitial.failed.load (obj)
    on.interstitial.failed.show (obj)
    on.interstitial.revenue (obj)
    on.interstitial.shown
    on.interstitial.dismissed
    on.interstitial.failed.show (obj)
    on.interstitial.impression
    on.interstitial.clicked

---

## 8. Rewarded Video

### Load & Show

    admobNextGen.createRewarded({
        adUnitId: 'ca-app-pub-xxx/xxx',
        retryInterval: 5000,  // Anti-spam interval
        isAutoShow: false     // Default false (User must opt-in)
    });

    // Show manually when ready
    admobNextGen.showRewarded();

### Events

    document.addEventListener('on.rewarded.earned', (data) => {
        console.log("User Earned: " + data.amount + " " + data.type);
    });
    document.addEventListener('on.rewarded.revenue', (data) => console.log(data));

    // else
    on.rewarded.loaded
    on.rewarded.failed.load (obj)
    on.rewarded.failed.show (obj)
    on.rewarded.revenue (obj)
    on.rewarded.shown
    on.rewarded.dismissed
    on.rewarded.failed.show (obj)
    on.rewarded.impression
    on.rewarded.clicked

---

## 9. Rewarded Interstitial

Auto-showing rewarded format (no opt-in).

### Load & Show

    admobNextGen.createRewardedInterstitial({
        adUnitId: 'ca-app-pub-xxx/xxx',
        isAutoShow: true,
        retryInterval: 5000 // Anti-spam interval
    });

### Events

    document.addEventListener('on.rewardedInter.earned', (data) => console.log(data));
    document.addEventListener('on.rewardedInter.revenue', (data) => console.log(data));

    // else
    on.rewardedInter.loaded
    on.rewardedInter.failed.load (obj)
    on.rewardedInter.failed.show (obj)
    on.rewardedInter.revenue (obj)
    on.rewardedInter.shown
    on.rewardedInter.dismissed
    on.rewardedInter.failed.show (obj)
    on.rewardedInter.impression
    on.rewardedInter.clicked


---

## ⚖️ Revenue Policy

**No Ad-Sharing. No Hidden Fees.**

Unlike many other free plugins, this project is clean:
* **0% Revenue Share:** We do not inject our own Ad Unit IDs into your traffic.
* **100% Control:** Every impression and click goes directly to your AdMob account.
* **Transparent Source:** The code is open source. You can verify that no third-party SDKs or hidden backdoors exist.

---

## ❤️ Support the Project

This plugin is developed and maintained in my free time. If it saved you hours of work, consider supporting the development!

<a href="https://www.buymeacoffee.com/emi.indo" target="_blank">
  <img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" style="height: 60px !important;width: 217px !important;" >
</a>

Your support helps me keep the dependencies updated and the cleaner script running smoothly.


<p align="center">
  Built with ❤️ by <b>Swaplab Engine</b> (formerly EMI-INDO).
</p>