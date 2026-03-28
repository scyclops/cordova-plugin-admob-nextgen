
// Android Only

function createNativeAds() {

     if (isDeviceready && !isPlatformIOS) {
          admobNextGen.createNativeAd({
               adUnitId: NativeAd_ID,
               view: 'banner_bottom',    // Presets: 'banner_top', 'banner_bottom', 'modal_center'
               isOverlapping: false,     // true = Overlay, false = Push Content
               retryInterval: 5000
          });
     }
}


/*
admobNextGen.createNativeAd({
    adUnitId: 'ca-app-pub-xxx/xxx',
    view: 'custom',
    x: 20,              // X Position in dp
    y: 100,             // Y Position in dp
    width: 300,         // Width in dp
    height: 300,        // Height in dp
    isOverlapping: true // Typically true for custom floating ads
});
*/

function removeNativeAds() {

     if (isDeviceready && !isPlatformIOS) {
          admobNextGen.removeNativeAd();
     }
}


/*
on.native.shown
on.native.failed
on.native.dismissed
on.native.show.failed  (obj)
on.native.impression
on.native.clicked
*/


document.addEventListener('on.native.loaded', () => console.log("Native Loaded"));



/*
https://support.google.com/admob/answer/11322405

Turn on the setting for impression-level ad revenue in your AdMob account:
Sign in to your AdMob account at https://apps.admob.com.
Click Settings in the sidebar.
Click the Account tab.
In the Account controls section, click the Impression-level ad revenue toggle to turn on this setting.
*/

document.addEventListener('on.native.revenue', (data) => {
     console.log("on native revenue")
     console.log(data.value)
     console.log(data.currency)
     console.log(data.precision)
});
