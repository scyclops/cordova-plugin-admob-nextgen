#import "AdMobNextGen.h"
#import "ConsentExecutor.h"
#import "GlobalSettingsExecutor.h"
#import "BannerExecutor.h"
#import "InterstitialExecutor.h"
#import "RewardedExecutor.h"
#import "AppOpenAdExecutor.h"
#import "RewardedInterstitialExecutor.h"

@interface AdMobNextGen()
@property (nonatomic, strong) ConsentExecutor *consentExecutor;
@property (nonatomic, strong) GlobalSettingsExecutor *globalSettingsExecutor;
@property (nonatomic, strong) BannerExecutor *bannerExecutor;
@property (nonatomic, strong) InterstitialExecutor *interstitialExecutor; 
@property (nonatomic, strong) RewardedExecutor *rewardedExecutor;
@property (nonatomic, strong) RewardedInterstitialExecutor *rewardedInterstitialExecutor;
@end

@implementation AdMobNextGen

- (void)pluginInitialize {
    [super pluginInitialize];
    self.consentExecutor = [[ConsentExecutor alloc] initWithPlugin:self];
    self.globalSettingsExecutor = [[GlobalSettingsExecutor alloc] initWithPlugin:self];
    self.bannerExecutor = [[BannerExecutor alloc] initWithPlugin:self];
    self.interstitialExecutor = [[InterstitialExecutor alloc] initWithPlugin:self]; 
    self.rewardedExecutor = [[RewardedExecutor alloc] initWithPlugin:self];
    [[AppOpenAdExecutor sharedInstance] initializeWithPlugin:self];
    self.rewardedInterstitialExecutor = [[RewardedInterstitialExecutor alloc] initWithPlugin:self];
}

- (void)initialize:(CDVInvokedUrlCommand*)command {
    [self.commandDelegate runInBackground:^{
        [[GADMobileAds sharedInstance] startWithCompletionHandler:^(GADInitializationStatus * _Nonnull status) {
            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"Initialization complete."];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }];
    }];
}

#pragma mark - Consent (UMP) Routing

- (void)requestConsentInfo:(CDVInvokedUrlCommand*)command {
    NSDictionary *options = nil;
    if (command.arguments.count > 0) {
        options = [command.arguments objectAtIndex:0];
    }
    [self.consentExecutor requestConsentInfo:options command:command];
}

- (void)showPrivacyOptionsForm:(CDVInvokedUrlCommand*)command {
    [self.consentExecutor showPrivacyOptionsForm:command];
}

- (void)getTCData:(CDVInvokedUrlCommand*)command {
    [self.consentExecutor getTCData:command];
}

- (void)canRequestAds:(CDVInvokedUrlCommand *)command {
    [self.consentExecutor canRequestAds:command];
}

- (void)requestTrackingAuthorization:(CDVInvokedUrlCommand *)command {
    [self.consentExecutor requestTrackingAuthorization:command];
}

- (void)getTrackingAuthorizationStatus:(CDVInvokedUrlCommand *)command {
    [self.consentExecutor getTrackingAuthorizationStatus:command];
}

#pragma mark - Global Settings Routing

- (void)setAppVolume:(CDVInvokedUrlCommand*)command {
    [self.globalSettingsExecutor setAppVolume:command];
}

- (void)setAppMuted:(CDVInvokedUrlCommand*)command {
    [self.globalSettingsExecutor setAppMuted:command];
}

- (void)setRequestConfiguration:(CDVInvokedUrlCommand*)command {
    [self.globalSettingsExecutor setRequestConfiguration:command];
}

#pragma mark - Banner Routing

- (void)createBanner:(CDVInvokedUrlCommand*)command {
    NSDictionary *options = [command.arguments objectAtIndex:0];
    if (options != nil && [options isKindOfClass:[NSDictionary class]]) {
        [self.bannerExecutor createBanner:options command:command];
    } else {
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Invalid options object."];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
}

- (void)showBanner:(CDVInvokedUrlCommand*)command {
    [self.bannerExecutor showBanner:command];
}

- (void)hideBanner:(CDVInvokedUrlCommand*)command {
    [self.bannerExecutor hideBanner:command];
}

- (void)removeBanner:(CDVInvokedUrlCommand*)command {
    [self.bannerExecutor removeBanner:command];
}

#pragma mark - Interstitial Routing

- (void)createInterstitial:(CDVInvokedUrlCommand*)command {
    NSDictionary *options = [command.arguments objectAtIndex:0];
    if (options != nil && [options isKindOfClass:[NSDictionary class]]) {
        [self.interstitialExecutor createInterstitial:options command:command];
    } else {
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Invalid options object."];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
}

- (void)showInterstitial:(CDVInvokedUrlCommand*)command {
    [self.interstitialExecutor showInterstitial:command];
}

- (void)createRewarded:(CDVInvokedUrlCommand*)command {
    NSDictionary *options = [command.arguments objectAtIndex:0];
    if (options != nil) {
        [self.rewardedExecutor createRewarded:options command:command];
    }
}

- (void)showRewarded:(CDVInvokedUrlCommand*)command {
    [self.rewardedExecutor showRewarded:command];
}

- (void)loadAppOpenAd:(CDVInvokedUrlCommand*)command {
    NSDictionary *options = [command.arguments objectAtIndex:0];
    if (options != nil) {
        [[AppOpenAdExecutor sharedInstance] loadAppOpenAd:options command:command];
    }
}

- (void)showAppOpenAd:(CDVInvokedUrlCommand*)command {
    [[AppOpenAdExecutor sharedInstance] showAppOpenAd:command];
}

- (void)createRewardedInterstitial:(CDVInvokedUrlCommand*)command {
    NSDictionary *options = [command.arguments objectAtIndex:0];
    if (options != nil) {
        [self.rewardedInterstitialExecutor createRewardedInterstitial:options command:command];
    }
}

- (void)showRewardedInterstitial:(CDVInvokedUrlCommand*)command {
    [self.rewardedInterstitialExecutor showRewardedInterstitial:command];
}

#pragma mark - Event Emitter

- (void)fireEvent:(NSString *)obj event:(NSString *)eventName withData:(NSString *)jsonStr {
    NSString* js;
    if(obj && [obj isEqualToString:@"window"]) {
        js = [NSString stringWithFormat:@"var evt=document.createEvent(\"UIEvents\");evt.initUIEvent(\"%@\",true,false,window,0);window.dispatchEvent(evt);", eventName];
    } else if(jsonStr && [jsonStr length] > 0) {
        js = [NSString stringWithFormat:@"javascript:cordova.fireDocumentEvent('%@',%@);", eventName, jsonStr];
    } else {
        js = [NSString stringWithFormat:@"javascript:cordova.fireDocumentEvent('%@');", eventName];
    }

    dispatch_async(dispatch_get_main_queue(), ^{
        [self.commandDelegate evalJs:js];
    });
}

@end
