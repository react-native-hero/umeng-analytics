#import <React/RCTBridgeModule.h>

@interface RNTUmengAnalytics : NSObject <RCTBridgeModule>

+ (void)init:(NSString *)appKey channel:(NSString *)channel debug:(BOOL)debug;

+ (void)analytics;

@end
