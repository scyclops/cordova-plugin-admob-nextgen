#import <Foundation/Foundation.h>
#import <Cordova/CDV.h>
#import <GoogleMobileAds/GoogleMobileAds.h>

@class AdMobNextGen;

@interface RewardedInterstitialExecutor : NSObject <GADFullScreenContentDelegate>

- (instancetype)initWithPlugin:(AdMobNextGen *)plugin;
- (void)createRewardedInterstitial:(NSDictionary *)options command:(CDVInvokedUrlCommand *)command;
- (void)showRewardedInterstitial:(CDVInvokedUrlCommand *)command;

@end
