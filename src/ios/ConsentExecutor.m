#import "ConsentExecutor.h"
#import "AdMobNextGen.h"
#import <CommonCrypto/CommonDigest.h>
#import <AppTrackingTransparency/AppTrackingTransparency.h>
#import <AdSupport/AdSupport.h>

@interface ConsentExecutor()
@property (nonatomic, weak) AdMobNextGen *plugin;
@end

@implementation ConsentExecutor

- (instancetype)initWithPlugin:(AdMobNextGen *)plugin {
    self = [super init];
    if (self) {
        self.plugin = plugin;
    }
    return self;
}

- (void)requestConsentInfo:(NSDictionary *)options command:(CDVInvokedUrlCommand *)command {
    dispatch_async(dispatch_get_main_queue(), ^{
        @try {
            BOOL debugMode = options[@"debug"] ? [options[@"debug"] boolValue] : NO;
            NSString *manualTestDeviceId = options[@"testDeviceId"] ? options[@"testDeviceId"] : @"";
            BOOL resetConsent = options[@"reset"] ? [options[@"reset"] boolValue] : NO;
            BOOL tagForUnderAgeOfConsent = options[@"tagForUnderAgeOfConsent"] ? [options[@"tagForUnderAgeOfConsent"] boolValue] : NO;

            if (resetConsent) {
                [UMPConsentInformation.sharedInstance reset];

            }

            UMPRequestParameters *parameters = [[UMPRequestParameters alloc] init];
            parameters.tagForUnderAgeOfConsent = tagForUnderAgeOfConsent;

            if (debugMode) {
                UMPDebugSettings *debugSettings = [[UMPDebugSettings alloc] init];
                debugSettings.geography = UMPDebugGeographyEEA;

                if (manualTestDeviceId.length > 0) {
                    debugSettings.testDeviceIdentifiers = @[manualTestDeviceId];

                } else {
                    NSString *deviceId = [self getDeviceId];
                    if (deviceId) {
                        debugSettings.testDeviceIdentifiers = @[deviceId];

                    }
                }
                parameters.debugSettings = debugSettings;
            }

            [UMPConsentInformation.sharedInstance requestConsentInfoUpdateWithParameters:parameters completionHandler:^(NSError *_Nullable requestConsentError) {

                if (requestConsentError) {

                    [self sendErrorEvent:requestConsentError];
                    if (command) {
                        CDVPluginResult* errRes = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:requestConsentError.localizedDescription];
                        [self.plugin.commandDelegate sendPluginResult:errRes callbackId:command.callbackId];
                    }
                    return;
                }

                [self.plugin fireEvent:@"document" event:@"on.consent.info.update" withData:nil];

                [UMPConsentForm loadAndPresentIfRequiredFromViewController:self.plugin.viewController completionHandler:^(NSError * _Nullable loadAndPresentError) {

                    if (loadAndPresentError) {

                        [self sendErrorEvent:loadAndPresentError];
                        if (command) {
                            CDVPluginResult* errRes = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:loadAndPresentError.localizedDescription];
                            [self.plugin.commandDelegate sendPluginResult:errRes callbackId:command.callbackId];
                        }
                    } else {

                        [self.plugin fireEvent:@"document" event:@"on.consent.form.dismissed" withData:nil];

                        [self sendConsentStatus:command];
                    }
                }];
            }];

        } @catch (NSException *exception) {
            if (command) {
                CDVPluginResult* errRes = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:exception.reason];
                [self.plugin.commandDelegate sendPluginResult:errRes callbackId:command.callbackId];
            }
        }
    });
}

- (void)getTCData:(CDVInvokedUrlCommand *)command {
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        @try {
            NSUserDefaults *prefs = [NSUserDefaults standardUserDefaults];
            NSMutableDictionary *tcData = [NSMutableDictionary dictionary];

            NSString *tcString = [prefs stringForKey:@"IABTCF_TCString"] ?: @"";
            NSString *purposeConsents = [prefs stringForKey:@"IABTCF_PurposeConsents"] ?: @"";
            NSString *vendorConsents = [prefs stringForKey:@"IABTCF_VendorConsents"] ?: @"";
            NSString *additionalConsent = [prefs stringForKey:@"IABTCF_AddtlConsent"] ?: @"";
            NSInteger gdprApplies = [prefs integerForKey:@"IABTCF_gdprApplies"]; 

            tcData[@"tcString"] = tcString;
            tcData[@"purposeConsents"] = purposeConsents;
            tcData[@"vendorConsents"] = vendorConsents;
            tcData[@"additionalConsent"] = additionalConsent;
            tcData[@"gdprApplies"] = @(gdprApplies);

            BOOL isPersonalizedAllowed = NO;
            NSString *statusMessage = @"Unknown";

            if (gdprApplies == 0) {

                isPersonalizedAllowed = YES;
                statusMessage = @"Not GDPR region. Personalized Ads allowed by default.";
            } else {

                if (purposeConsents.length > 0) {

                    if ([purposeConsents hasPrefix:@"1"]) {
                        isPersonalizedAllowed = YES;
                        statusMessage = @"Purpose 1 Granted. Personalized Ads allowed.";
                    } else {
                        isPersonalizedAllowed = NO;
                        statusMessage = @"Purpose 1 Denied. Non-Personalized / Limited Ads only.";
                    }
                } else {
                    isPersonalizedAllowed = NO;
                    statusMessage = @"No consent data found (User hasn't answered yet).";
                }
            }

            BOOL isACVersion2 = NO;
            if (additionalConsent.length > 0) {
                isACVersion2 = [additionalConsent hasPrefix:@"2"];
            }

            tcData[@"isPersonalizedAllowed"] = @(isPersonalizedAllowed);
            tcData[@"statusMessage"] = statusMessage;
            tcData[@"isACVersion2"] = @(isACVersion2);

            if (command) {
                CDVPluginResult* res = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:tcData];
                [self.plugin.commandDelegate sendPluginResult:res callbackId:command.callbackId];
            }
        } @catch (NSException *exception) {
            if (command) {
                CDVPluginResult* errRes = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:exception.reason];
                [self.plugin.commandDelegate sendPluginResult:errRes callbackId:command.callbackId];
            }
        }
    });
}

- (void)showPrivacyOptionsForm:(CDVInvokedUrlCommand *)command {
    dispatch_async(dispatch_get_main_queue(), ^{
        [UMPConsentForm presentPrivacyOptionsFormFromViewController:self.plugin.viewController completionHandler:^(NSError * _Nullable formError) {
            if (formError) {
                [self sendErrorEvent:formError];
                if (command) {
                    CDVPluginResult* errRes = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:formError.localizedDescription];
                    [self.plugin.commandDelegate sendPluginResult:errRes callbackId:command.callbackId];
                }
            } else {
                [self.plugin fireEvent:@"document" event:@"on.consent.form.dismissed" withData:nil];
                [self sendConsentStatus:command];
            }
        }];
    });
}

- (void)canRequestAds:(CDVInvokedUrlCommand *)command {
    BOOL canRequest = UMPConsentInformation.sharedInstance.canRequestAds;
    CDVPluginResult* res = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsInt:canRequest ? 1 : 0];
    [self.plugin.commandDelegate sendPluginResult:res callbackId:command.callbackId];
}

#pragma mark - Helpers

- (void)sendConsentStatus:(CDVInvokedUrlCommand *)command {
    NSMutableDictionary *result = [NSMutableDictionary dictionary];

    BOOL canRequestAds = UMPConsentInformation.sharedInstance.canRequestAds;
    result[@"canRequestAds"] = @(canRequestAds);

    UMPPrivacyOptionsRequirementStatus reqStatus = UMPConsentInformation.sharedInstance.privacyOptionsRequirementStatus;

    NSString *reqStatusString = @"UNKNOWN";
    if (reqStatus == UMPPrivacyOptionsRequirementStatusRequired) reqStatusString = @"REQUIRED";
    else if (reqStatus == UMPPrivacyOptionsRequirementStatusNotRequired) reqStatusString = @"NOT_REQUIRED";

    result[@"privacyOptionsRequirementStatus"] = reqStatusString;
    result[@"isPrivacyOptionsRequired"] = @(reqStatus == UMPPrivacyOptionsRequirementStatusRequired);
    result[@"consentStatus"] = @(UMPConsentInformation.sharedInstance.consentStatus);

    NSError *error;
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:result options:0 error:&error];
    if (!error && jsonData) {
        NSString *jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
        [self.plugin fireEvent:@"document" event:@"on.consent.status.change" withData:jsonString];
    }

    if (command) {
        CDVPluginResult* res = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
        [self.plugin.commandDelegate sendPluginResult:res callbackId:command.callbackId];
    }
}

- (void)sendErrorEvent:(NSError *)error {
    NSString *jsonStr = [NSString stringWithFormat:@"{\"code\":%ld, \"message\":\"%@\"}", (long)error.code, error.localizedDescription];
    [self.plugin fireEvent:@"document" event:@"on.consent.error" withData:jsonStr];
}

- (NSString *)getDeviceId {

    NSString *uuid = [[[UIDevice currentDevice] identifierForVendor] UUIDString];
    if (!uuid) return nil;

    const char *cStr = [uuid UTF8String];
    unsigned char digest[CC_SHA256_DIGEST_LENGTH]; 
    CC_SHA256(cStr, (CC_LONG)strlen(cStr), digest);

    NSMutableString *output = [NSMutableString stringWithCapacity:CC_SHA256_DIGEST_LENGTH * 2];
    for(int i = 0; i < CC_SHA256_DIGEST_LENGTH; i++)
        [output appendFormat:@"%02X", digest[i]];

    return [output uppercaseString];
}

#pragma mark - App Tracking Transparency (ATT / IDFA)

- (void)requestTrackingAuthorization:(CDVInvokedUrlCommand *)command {
    if (@available(iOS 14.0, *)) {
        [ATTrackingManager requestTrackingAuthorizationWithCompletionHandler:^(ATTrackingManagerAuthorizationStatus status) {

            NSString *statusString = [self getATTStatusString:status];

            NSString *jsonStr = [NSString stringWithFormat:@"{\"status\":\"%@\"}", statusString];
            [self.plugin fireEvent:@"document" event:@"on.consent.att.status" withData:jsonStr];

            if (command) {
                CDVPluginResult* res = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:statusString];
                [self.plugin.commandDelegate sendPluginResult:res callbackId:command.callbackId];
            }
        }];
    } else {

        NSString *statusString = @"AUTHORIZED";
        if (command) {
            CDVPluginResult* res = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:statusString];
            [self.plugin.commandDelegate sendPluginResult:res callbackId:command.callbackId];
        }
    }
}

- (void)getTrackingAuthorizationStatus:(CDVInvokedUrlCommand *)command {
    if (@available(iOS 14.0, *)) {
        ATTrackingManagerAuthorizationStatus status = [ATTrackingManager trackingAuthorizationStatus];
        NSString *statusString = [self getATTStatusString:status];

        if (command) {
            CDVPluginResult* res = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:statusString];
            [self.plugin.commandDelegate sendPluginResult:res callbackId:command.callbackId];
        }
    } else {
        if (command) {
            CDVPluginResult* res = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"AUTHORIZED"];
            [self.plugin.commandDelegate sendPluginResult:res callbackId:command.callbackId];
        }
    }
}

- (NSString *)getATTStatusString:(NSUInteger)status {
    if (@available(iOS 14.0, *)) {
        switch (status) {
            case ATTrackingManagerAuthorizationStatusAuthorized:
                return @"AUTHORIZED";
            case ATTrackingManagerAuthorizationStatusDenied:
                return @"DENIED";
            case ATTrackingManagerAuthorizationStatusNotDetermined:
                return @"NOT_DETERMINED";
            case ATTrackingManagerAuthorizationStatusRestricted:
                return @"RESTRICTED";
            default:
                return @"UNKNOWN";
        }
    }
    return @"AUTHORIZED";
}

@end
