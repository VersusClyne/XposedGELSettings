package de.theknut.xposedgelsettings.hooks;

import android.content.Context;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.theknut.xposedgelsettings.hooks.androidintegration.AppInfo;
import de.theknut.xposedgelsettings.hooks.androidintegration.QuickSettings;
import de.theknut.xposedgelsettings.hooks.androidintegration.SystemBars;
import de.theknut.xposedgelsettings.hooks.androidintegration.SystemUIHooks;
import de.theknut.xposedgelsettings.hooks.androidintegration.SystemUIReceiver;
import de.theknut.xposedgelsettings.hooks.appdrawer.AppDrawerHooks;
import de.theknut.xposedgelsettings.hooks.general.ContextMenu;
import de.theknut.xposedgelsettings.hooks.general.GeneralHooks;
import de.theknut.xposedgelsettings.hooks.gestures.GestureHooks;
import de.theknut.xposedgelsettings.hooks.googlesearchbar.GoogleSearchBarHooks;
import de.theknut.xposedgelsettings.hooks.homescreen.HomescreenHooks;
import de.theknut.xposedgelsettings.hooks.icon.IconHooks;
import de.theknut.xposedgelsettings.hooks.notificationbadges.NotificationBadgesHooks;
import de.theknut.xposedgelsettings.hooks.pagindicator.PageIndicatorHooks;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public class GELSettings extends XC_MethodHook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {

        // only hook to supported launchers
        if (lpparam.packageName.equals(Common.PACKAGE_NAME)) {

            // tells the UI that the module is active and running
            findAndHookMethod(Common.PACKAGE_NAME + ".ui.FragmentWelcome", lpparam.classLoader, "isXGELSActive", XC_MethodReplacement.returnConstant(true));
            return;

        } else if (lpparam.packageName.equals("com.android.systemui")) {

            PreferencesHelper.init();
            SystemUIReceiver.initAllHooks(lpparam);
            QuickSettings.initAllHooks(lpparam);
            return;

        } else if (lpparam.packageName.equals("com.android.settings")) {

            PreferencesHelper.init();
            AppInfo.initAllHooks(lpparam);
            return;

        } else if (!Common.PACKAGE_NAMES.contains(lpparam.packageName)) {
            return;
        }

        int versionIdx;
        Common.HOOKED_PACKAGE = lpparam.packageName;
        if (PreferencesHelper.Debug) XposedBridge.log("XGELS: GELSettings.handleLoadPackage: hooked package -> " + lpparam.packageName);

        if (Common.HOOKED_PACKAGE.equals(Common.GEL_PACKAGE)) {
            // thanks to KeepChat for the following snippet:
            // http://git.io/JJZPaw
            try {
                Object activityThread = callStaticMethod(findClass("android.app.ActivityThread", null), "currentActivityThread");
                Context context = (Context) callMethod(activityThread, "getSystemContext");

                String versionName = context.getPackageManager().getPackageInfo(lpparam.packageName, 0).versionName;
                int versionCode = Common.GNL_VERSION = context.getPackageManager().getPackageInfo(lpparam.packageName, 0).versionCode;

                versionIdx = ObfuscationHelper.getVersionIndex(versionCode);
                if (versionIdx > 0) Common.PACKAGE_OBFUSCATED = true;
                if (versionCode >= ObfuscationHelper.GNL_4_0_26) Common.IS_PRE_GNL_4 = false;

                if (PreferencesHelper.Debug) XposedBridge.log("XGELS: " + Common.HOOKED_PACKAGE + " V" + versionName + "(" + versionCode + ")");
            } catch (Exception e) {
                XposedBridge.log("XGELS: exception while trying to get version info. (" + e.getMessage() + ")");
                return;
            }
        } else {
            Common.IS_TREBUCHET = Common.HOOKED_PACKAGE.equals(Common.TREBUCHET_PACKAGE);
            versionIdx = 0;
        }

        PreferencesHelper.init();
        ObfuscationHelper.init(lpparam, versionIdx);

        // init all hooks...
        GeneralHooks.initAllHooks(lpparam);
        ContextMenu.initAllHooks(lpparam);
        GoogleSearchBarHooks.initAllHooks(lpparam);
        PageIndicatorHooks.initAllHooks(lpparam);
        HomescreenHooks.initAllHooks(lpparam);
        SystemUIHooks.initAllHooks(lpparam);
        SystemBars.initAllHooks(lpparam);
        AppDrawerHooks.initAllHooks(lpparam);
        GestureHooks.initAllHooks(lpparam);
        NotificationBadgesHooks.initAllHooks(lpparam);
        IconHooks.initAllHooks(lpparam);
    }
}