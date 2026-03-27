#import <Foundation/Foundation.h>
#import <Cordova/CDV.h>
#import <UserMessagingPlatform/UserMessagingPlatform.h>

@class AdMobNextGen;

@interface ConsentExecutor : NSObject

- (instancetype)initWithPlugin:(AdMobNextGen *)plugin;

- (void)requestConsentInfo:(NSDictionary *)options command:(CDVInvokedUrlCommand *)command;
- (void)showPrivacyOptionsForm:(CDVInvokedUrlCommand *)command;
- (void)getTCData:(CDVInvokedUrlCommand *)command;
- (void)canRequestAds:(CDVInvokedUrlCommand *)command;

- (void)requestTrackingAuthorization:(CDVInvokedUrlCommand *)command;
- (void)getTrackingAuthorizationStatus:(CDVInvokedUrlCommand *)command;

@end
