const fs = require('fs');
const path = require('path');

const rootPath = process.cwd();
const configPath = fs.existsSync(path.join(rootPath, 'capacitor.config.ts'))
    ? path.join(rootPath, 'capacitor.config.ts')
    : path.join(rootPath, 'capacitor.config.json');

const manifestPath = path.join(rootPath, 'android/app/src/main/AndroidManifest.xml');
const gradlePath = path.join(rootPath, 'android/app/build.gradle');

/**
 * Updates AndroidManifest.xml with the AdMob App ID
 */
function updateAndroidManifest(appId) {
    if (!fs.existsSync(manifestPath)) return;
    let content = fs.readFileSync(manifestPath, 'utf8');
    const appIdRegex = /<meta-data\s+android:name="com.google.android.gms.ads.APPLICATION_ID"\s+android:value=".*?"\s*\/>/;
    const newTag = `<meta-data android:name="com.google.android.gms.ads.APPLICATION_ID" android:value="${appId}"/>`;

    if (appIdRegex.test(content)) {
        content = content.replace(appIdRegex, newTag);
    } else {
        content = content.replace('</application>', `    ${newTag}\n    </application>`);
    }
    fs.writeFileSync(manifestPath, content, 'utf8');
    console.log(`[AdMob Hook] Success: Updated App ID to ${appId}`);
}

/**
 * Updates build.gradle with specific SDK and UMP versions
 */
function updateGradleDependencies(nextGenVersion, umpVersion) {
    if (!fs.existsSync(gradlePath)) return;
    let content = fs.readFileSync(gradlePath, 'utf8');

    const adsRegex = /implementation\s+['"]com\.google\.android\.libraries\.ads\.mobile\.sdk:ads-mobile-sdk:.*?['"]/;
    const umpRegex = /implementation\s+['"]com\.google\.android\.ump:user-messaging-platform:.*?['"]/;

    const newAds = `implementation "com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:${nextGenVersion}"`;
    const newUmp = `implementation "com.google.android.ump:user-messaging-platform:${umpVersion}"`;

    if (adsRegex.test(content)) content = content.replace(adsRegex, newAds);
    if (umpRegex.test(content)) content = content.replace(umpRegex, newUmp);

    fs.writeFileSync(gradlePath, content, 'utf8');
    console.log(`[AdMob Hook] Success: Updated Next Gen SDK to ${nextGenVersion} and UMP to ${umpVersion}`);
}

function injectExclusionRules() {
    if (!fs.existsSync(gradlePath)) return;
    let content = fs.readFileSync(gradlePath, 'utf8');

    if (content.includes('exclude group: "com.google.android.gms", module: "play-services-ads"')) {
        return;
    }

    const exclusionBlock = `
    // [AdMob Next Gen] Exclude Legacy SDK to prevent duplicates
    configurations.configureEach {
    exclude group: "com.google.android.gms", module: "play-services-ads"
    exclude group: "com.google.android.gms", module: "play-services-ads-lite"
    }
`;

    content += exclusionBlock;

    fs.writeFileSync(gradlePath, content, 'utf8');
    console.log('[AdMob Hook] Success: Injected legacy SDK exclusion rules into build.gradle');
}

function run() {
    try {
        if (!fs.existsSync(configPath)) return;

        const configContent = fs.readFileSync(configPath, 'utf8');
        let admob;

        if (configPath.endsWith('.json')) {
            admob = JSON.parse(configContent).plugins?.AdMob;
        } else {
            // Extracting values from .ts file using regex
            const appId = configContent.match(/APP_ID_ANDROID:\s*['"](.*?)['"]/);
            const sdk = configContent.match(/NEXT_GEN_SDK_VERSION:\s*['"](.*?)['"]/);
            const ump = configContent.match(/UMP_VERSION:\s*['"](.*?)['"]/);
            admob = {
                APP_ID_ANDROID: appId ? appId[1] : null,
                NEXT_GEN_SDK_VERSION: sdk ? sdk[1] : "0.23.0-beta01",
                UMP_VERSION: ump ? ump[1] : "4.0.0"
            };
        }

        if (admob?.APP_ID_ANDROID) updateAndroidManifest(admob.APP_ID_ANDROID);

        injectExclusionRules();

        if (admob?.NEXT_GEN_SDK_VERSION || admob?.UMP_VERSION) {
            updateGradleDependencies(
                admob.NEXT_GEN_SDK_VERSION || "0.23.0-beta01",
                admob.UMP_VERSION || "4.0.0"
            );
        }

    } catch (err) {
        console.error('[AdMob Hook] Error:', err.message);
    }
}

run();