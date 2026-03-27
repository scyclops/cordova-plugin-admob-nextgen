#import <Foundation/Foundation.h>
#import <Cordova/CDV.h>
#import <GoogleMobileAds/GoogleMobileAds.h>

@class AdMobNextGen;

@interface BannerExecutor : NSObject <GADBannerViewDelegate>

- (instancetype)initWithPlugin:(AdMobNextGen *)plugin;
- (void)createBanner:(NSDictionary *)options command:(CDVInvokedUrlCommand *)command;
- (void)showBanner:(CDVInvokedUrlCommand *)command;
- (void)hideBanner:(CDVInvokedUrlCommand *)command;
- (void)removeBanner:(CDVInvokedUrlCommand *)command;

@end
