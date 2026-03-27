#import <Foundation/Foundation.h>
#import <Cordova/CDV.h>
#import <GoogleMobileAds/GoogleMobileAds.h>

@class AdMobNextGen;

@interface InterstitialExecutor : NSObject <GADFullScreenContentDelegate>

- (instancetype)initWithPlugin:(AdMobNextGen *)plugin;
- (void)createInterstitial:(NSDictionary *)options command:(CDVInvokedUrlCommand *)command;
- (void)showInterstitial:(CDVInvokedUrlCommand *)command;

@end
