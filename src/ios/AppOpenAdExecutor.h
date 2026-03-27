#import <Foundation/Foundation.h>
#import <Cordova/CDV.h>
#import <GoogleMobileAds/GoogleMobileAds.h>

@class AdMobNextGen;

@interface AppOpenAdExecutor : NSObject <GADFullScreenContentDelegate>

+ (instancetype)sharedInstance;

- (void)initializeWithPlugin:(AdMobNextGen *)plugin;
- (void)loadAppOpenAd:(NSDictionary *)options command:(CDVInvokedUrlCommand *)command;
- (void)showAppOpenAd:(CDVInvokedUrlCommand *)command;

@end
