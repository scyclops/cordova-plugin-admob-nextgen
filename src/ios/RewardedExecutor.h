#import <Foundation/Foundation.h>
#import <Cordova/CDV.h>
#import <GoogleMobileAds/GoogleMobileAds.h>

@class AdMobNextGen;

@interface RewardedExecutor : NSObject <GADFullScreenContentDelegate>

- (instancetype)initWithPlugin:(AdMobNextGen *)plugin;
- (void)createRewarded:(NSDictionary *)options command:(CDVInvokedUrlCommand *)command;
- (void)showRewarded:(CDVInvokedUrlCommand *)command;

@end
