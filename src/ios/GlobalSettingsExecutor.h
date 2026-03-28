#import <Foundation/Foundation.h>
#import <Cordova/CDV.h>
#import <GoogleMobileAds/GoogleMobileAds.h>

@class AdMobNextGen;

@interface GlobalSettingsExecutor : NSObject

- (instancetype)initWithPlugin:(AdMobNextGen *)plugin;

- (void)setAppVolume:(CDVInvokedUrlCommand *)command;
- (void)setAppMuted:(CDVInvokedUrlCommand *)command;
- (void)setRequestConfiguration:(CDVInvokedUrlCommand *)command;

@end
