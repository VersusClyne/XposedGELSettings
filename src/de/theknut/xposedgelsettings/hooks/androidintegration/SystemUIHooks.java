package de.theknut.xposedgelsettings.hooks.androidintegration;

import android.animation.TimeInterpolator;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.theknut.xposedgelsettings.hooks.Common;
import de.theknut.xposedgelsettings.hooks.HooksBaseClass;
import de.theknut.xposedgelsettings.hooks.ObfuscationHelper.Classes;
import de.theknut.xposedgelsettings.hooks.ObfuscationHelper.Fields;
import de.theknut.xposedgelsettings.hooks.ObfuscationHelper.Methods;
import de.theknut.xposedgelsettings.hooks.PreferencesHelper;
import de.theknut.xposedgelsettings.hooks.general.ContextMenu;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

public class SystemUIHooks extends HooksBaseClass {

    static ActivityManager activityManager = null;

    public static void initAllHooks(LoadPackageParam lpparam) {

        XposedBridge.hookAllMethods(Classes.Launcher, "onBackPressed", new XC_MethodHook() {

            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                boolean isDefaultHomescreen = (getIntField(Common.WORKSPACE_INSTANCE, Fields.pvCurrentPage) == (PreferencesHelper.defaultHomescreen - 1))
                        || (getIntField(Common.WORKSPACE_INSTANCE, Fields.pvNextPage) == (PreferencesHelper.defaultHomescreen - 1));

                if (DEBUG) log("SystemUIHooks: onBackPressed " + (PreferencesHelper.defaultHomescreen - 1) + " : " + isDefaultHomescreen);

                if (ContextMenu.isOpen()) {
                    ContextMenu.closeAndRemove();
                } else if (PreferencesHelper.dynamicBackbutton
                        && !((Boolean) callMethod(param.thisObject, Methods.lIsAllAppsVisible))
                        && callMethod(Common.WORKSPACE_INSTANCE, Methods.wGetOpenFolder) == null
                        && (isDefaultHomescreen || PreferencesHelper.dynamicBackButtonOnEveryScreen)
                        && getObjectField(Common.WORKSPACE_INSTANCE, Fields.wState).toString().equals("NORMAL")) {

                    Intent myIntent = new Intent();
                    myIntent.putExtra(Common.XGELS_ACTION_EXTRA, Common.XGELS_ACTION_OTHER);
                    myIntent.putExtra(Common.XGELS_ACTION, "GO_TO_SLEEP");
                    myIntent.setAction(Common.XGELS_INTENT);
                    Common.LAUNCHER_CONTEXT.sendBroadcast(myIntent);
                }
            }
        });

        XposedBridge.hookAllMethods(Classes.Launcher, "onNewIntent", new XC_MethodHook() {

            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (DEBUG) log("SystemUIHooks: onNewIntent");

                if (ContextMenu.isOpen()) {
                    ContextMenu.closeAndRemove();
                } else if (PreferencesHelper.dynamicHomebutton
                        && Intent.ACTION_MAIN.equals(((Intent)param.args[0]).getAction())
                        && !((Boolean) callMethod(param.thisObject, Methods.lIsAllAppsVisible))
                        && callMethod(Common.WORKSPACE_INSTANCE, Methods.wGetOpenFolder) == null) {

                    int currentPage = getIntField(Common.WORKSPACE_INSTANCE, Fields.pvCurrentPage);

                    if ((currentPage == (PreferencesHelper.defaultHomescreen - 1)
                            || getIntField(Common.WORKSPACE_INSTANCE, Fields.pvNextPage) == (PreferencesHelper.defaultHomescreen - 1))
                            && getBooleanField(Common.LAUNCHER_INSTANCE, Fields.lHasFocus)
                            && getObjectField(Common.WORKSPACE_INSTANCE, Fields.wState).toString().equals("NORMAL")) {

                        callMethod(Common.LAUNCHER_INSTANCE, "onClickAllAppsButton", new View(Common.LAUNCHER_CONTEXT));
                        param.setResult(null);
                    }
                }
            }
        });

        if (!PreferencesHelper.hideClock && !PreferencesHelper.dynamicBackbutton && !PreferencesHelper.dynamicHomebutton)
        {
            return;
        }

        XC_MethodHook snapToPageHook = new XC_MethodHook() {

            boolean gnow = true;

            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                gnow = (Boolean) callMethod(Common.LAUNCHER_INSTANCE, Methods.lHasCustomContentToLeft);
                String launcherState = getObjectField(Common.LAUNCHER_INSTANCE, Fields.lState).toString();

                try {
                    if (!((Boolean) callMethod(Common.LAUNCHER_INSTANCE, Methods.lIsAllAppsVisible))
                            && getObjectField(Common.WORKSPACE_INSTANCE, Fields.wState).toString().equals("NORMAL")
                            && (launcherState.equals("WORKSPACE") || launcherState.equals("NORMAL"))) {

                        Intent myIntent = new Intent();
                        myIntent.putExtra(Common.XGELS_ACTION_EXTRA, Common.XGELS_ACTION_NAVBAR);

                        if ((Integer) param.args[0] == (PreferencesHelper.defaultHomescreen - 1)) {

                            myIntent.putExtra(Common.XGELS_ACTION, "ON_DEFAULT_HOMESCREEN");
                            myIntent.setAction(Common.XGELS_INTENT);
                            Common.LAUNCHER_CONTEXT.sendBroadcast(myIntent);
                        }
                        else if ((Integer) param.args[0] == 0 && gnow) {

                            if (PreferencesHelper.dynamicBackButtonOnEveryScreen) {
                                myIntent.putExtra(Common.XGELS_ACTION, "HOME_ORIG");
                            } else {
                                myIntent.putExtra(Common.XGELS_ACTION, "BACK_HOME_ORIG");
                            }

                            myIntent.setAction(Common.XGELS_INTENT);
                            Common.LAUNCHER_CONTEXT.sendBroadcast(myIntent);
                        }
                    }
                } catch (Throwable e) { }
            }
        };

        if (Common.PACKAGE_OBFUSCATED) {
            findAndHookMethod(Classes.PagedView, Methods.pvSnapToPage, Integer.TYPE, Integer.TYPE, Integer.TYPE, boolean.class, TimeInterpolator.class, snapToPageHook);
        } else {
            XposedBridge.hookAllMethods(Classes.PagedView, Methods.pvSnapToPage, snapToPageHook);
        }

        XposedBridge.hookAllMethods(Classes.PagedView, Methods.pvPageBeginMoving, new XC_MethodHook() {

            int TOUCH_STATE_REST = 0;

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (DEBUG) log("SystemUIHooks: onPageBeginMoving TouchState " + getObjectField(Common.WORKSPACE_INSTANCE, Fields.wTouchState));

                int currentPage = getIntField(param.thisObject, Fields.pvCurrentPage);

                if (!((Boolean) callMethod(Common.LAUNCHER_INSTANCE, Methods.lIsAllAppsVisible))
                        && getObjectField(Common.WORKSPACE_INSTANCE, Fields.wState).toString().equals("NORMAL")
                        && getIntField(Common.WORKSPACE_INSTANCE, Fields.wTouchState) != TOUCH_STATE_REST
                        && getIntField(Common.WORKSPACE_INSTANCE, Fields.pvNextPage) != currentPage) {


                    Intent myIntent = new Intent();
                    myIntent.putExtra(Common.XGELS_ACTION_EXTRA, Common.XGELS_ACTION_NAVBAR);

                    if (currentPage == (PreferencesHelper.defaultHomescreen - 1)) {

                        if (PreferencesHelper.dynamicBackButtonOnEveryScreen) {
                            myIntent.putExtra(Common.XGELS_ACTION, "HOME_ORIG");
                        } else {
                            myIntent.putExtra(Common.XGELS_ACTION, "BACK_HOME_ORIG");
                        }

                        myIntent.setAction(Common.XGELS_INTENT);
                        Common.LAUNCHER_CONTEXT.sendBroadcast(myIntent);
                    }
                }
            }
        });

        XposedBridge.hookAllMethods(Classes.PagedView, Methods.pvPageEndMoving, new XC_MethodHook() {

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (DEBUG) log("SystemUIHooks: onPageEndMoving TouchState " + getObjectField(Common.WORKSPACE_INSTANCE, Fields.wTouchState) + " " + getIntField(param.thisObject, Fields.pvCurrentPage) + " " + getObjectField(Common.WORKSPACE_INSTANCE, Fields.wState));

                if (!((Boolean) callMethod(Common.LAUNCHER_INSTANCE, Methods.lIsAllAppsVisible))
                        && getObjectField(Common.WORKSPACE_INSTANCE, Fields.wState).toString().equals("NORMAL")) {

                    int currPage = getIntField(Common.WORKSPACE_INSTANCE, Fields.pvCurrentPage);
                    Intent myIntent = new Intent();
                    myIntent.setAction(Common.XGELS_INTENT);
                    myIntent.putExtra(Common.XGELS_ACTION_EXTRA, Common.XGELS_ACTION_NAVBAR);

                    if (currPage == (PreferencesHelper.defaultHomescreen - 1)) {

                        myIntent.putExtra(Common.XGELS_ACTION, "ON_DEFAULT_HOMESCREEN");
                        Common.LAUNCHER_CONTEXT.sendBroadcast(myIntent);
                    } else {
                        if (PreferencesHelper.dynamicBackButtonOnEveryScreen) {
                            myIntent.putExtra(Common.XGELS_ACTION, "BACK_POWER_OFF");
                            Common.LAUNCHER_CONTEXT.sendBroadcast(myIntent);
                        }
                    }
                }
            }
        });

        XposedBridge.hookAllMethods(Classes.Workspace, Methods.wEnterOverviewMode, new XC_MethodHook() {

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (DEBUG) log("SystemUIHooks: enterOverviewMode");

                Intent myIntent = new Intent();
                myIntent.putExtra(Common.XGELS_ACTION_EXTRA, Common.XGELS_ACTION_NAVBAR);
                myIntent.putExtra(Common.XGELS_ACTION, "BACK_HOME_ORIG");
                myIntent.setAction(Common.XGELS_INTENT);
                Common.LAUNCHER_CONTEXT.sendBroadcast(myIntent);
            }
        });

        XposedBridge.hookAllMethods(Classes.Launcher, "onPause", new XC_MethodHook() {

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (DEBUG) log("SystemUIHooks: onPause");

                String launcherState = getObjectField(Common.LAUNCHER_INSTANCE, Fields.lState).toString();
                if (launcherState.equals("NORMAL") || launcherState.equals("WORKSPACE")) {
                    Intent myIntent = new Intent();
                    myIntent.putExtra(Common.XGELS_ACTION_EXTRA, Common.XGELS_ACTION_NAVBAR);
                    myIntent.putExtra(Common.XGELS_ACTION, "BACK_HOME_ORIG");
                    myIntent.setAction(Common.XGELS_INTENT);
                    Common.LAUNCHER_CONTEXT.sendBroadcast(myIntent);
                }
            }
        });

        XposedBridge.hookAllMethods(Classes.Launcher, "onStop", new XC_MethodHook() {

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (DEBUG) log("SystemUIHooks: onStop isPaused " + getBooleanField(param.thisObject, Fields.lPaused));

                String launcherState = getObjectField(Common.LAUNCHER_INSTANCE, Fields.lState).toString();
                if (launcherState.equals("NORMAL") || launcherState.equals("WORKSPACE")) {

                    Intent myIntent = new Intent();
                    myIntent.putExtra(Common.XGELS_ACTION_EXTRA, Common.XGELS_ACTION_NAVBAR);
                    myIntent.putExtra(Common.XGELS_ACTION, "BACK_HOME_ORIG");
                    myIntent.setAction(Common.XGELS_INTENT);
                    Common.LAUNCHER_CONTEXT.sendBroadcast(myIntent);
                }
            }
        });

        XposedBridge.hookAllMethods(Classes.Launcher, "onStart", new XC_MethodHook() {

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (DEBUG) log("SystemUIHooks: onStart");

                boolean isDefaultHomescreen = getIntField(Common.WORKSPACE_INSTANCE, Fields.pvCurrentPage) == (PreferencesHelper.defaultHomescreen - 1);

                if (activityManager == null) {
                    activityManager = (ActivityManager) Common.LAUNCHER_CONTEXT.getSystemService(Context.ACTIVITY_SERVICE);
                }

                List<RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();

                if (!(Boolean) callMethod(param.thisObject, Methods.lIsAllAppsVisible)
                        && isDefaultHomescreen
                        && Common.PACKAGE_NAMES.contains(appProcesses.get(0).processName.replace(":search", ""))
                        && getObjectField(Common.WORKSPACE_INSTANCE, Fields.wState).toString().equals("NORMAL")) {

                    Intent myIntent = new Intent();
                    myIntent.putExtra(Common.XGELS_ACTION_EXTRA, Common.XGELS_ACTION_NAVBAR);
                    myIntent.putExtra(Common.XGELS_ACTION, "ON_DEFAULT_HOMESCREEN");
                    myIntent.setAction(Common.XGELS_INTENT);
                    Common.LAUNCHER_CONTEXT.sendBroadcast(myIntent);
                }
            }
        });

        XposedBridge.hookAllMethods(Classes.Launcher, "onResume", new XC_MethodHook() {

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (DEBUG) log("SystemUIHooks: onResume currentPage" + getIntField(Common.WORKSPACE_INSTANCE, Fields.pvCurrentPage));

                if (!((Boolean) callMethod(param.thisObject, Methods.lIsAllAppsVisible))
                        && !(Boolean) callMethod(Common.WORKSPACE_INSTANCE, Methods.wIsOnOrMovingToCustomContent)
                        && getObjectField(Common.WORKSPACE_INSTANCE, Fields.wState).toString().equals("NORMAL")) {

                    int currentPage = getIntField(Common.WORKSPACE_INSTANCE, Fields.pvCurrentPage);

                    if (currentPage == (PreferencesHelper.defaultHomescreen - 1)) {

                        Intent myIntent = new Intent();
                        myIntent.putExtra(Common.XGELS_ACTION_EXTRA, Common.XGELS_ACTION_NAVBAR);
                        myIntent.putExtra(Common.XGELS_ACTION, "ON_DEFAULT_HOMESCREEN");
                        myIntent.setAction(Common.XGELS_INTENT);
                        Common.LAUNCHER_CONTEXT.sendBroadcast(myIntent);
                    }
                }
            }
        });

        findAndHookMethod(Classes.Launcher, Methods.lShowWorkspace, boolean.class, Runnable.class, new XC_MethodHook() {

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (DEBUG) log("SystemUIHooks: onWorkspaceShown");

                int currentPage = getIntField(Common.WORKSPACE_INSTANCE, Fields.pvCurrentPage);

                if (activityManager == null) {
                    activityManager = (ActivityManager) Common.LAUNCHER_CONTEXT.getSystemService(Context.ACTIVITY_SERVICE);
                }

                List<RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();

                if (Common.PACKAGE_NAMES.contains(appProcesses.get(0).processName.replace(":search", ""))
                        && !getBooleanField(param.thisObject, Fields.lPaused)
                        && !getObjectField(Common.WORKSPACE_INSTANCE, Fields.wState).toString().equals("OVERVIEW")) {

                    Intent myIntent = new Intent();
                    myIntent.putExtra(Common.XGELS_ACTION_EXTRA, Common.XGELS_ACTION_NAVBAR);
                    myIntent.setAction(Common.XGELS_INTENT);

                    if (currentPage == (PreferencesHelper.defaultHomescreen - 1)) {
                        myIntent.putExtra(Common.XGELS_ACTION, "ON_DEFAULT_HOMESCREEN");
                    } else {
                        if (PreferencesHelper.dynamicBackButtonOnEveryScreen) {
                            myIntent.putExtra(Common.XGELS_ACTION, "BACK_POWER_OFF");
                            Common.LAUNCHER_CONTEXT.sendBroadcast(myIntent);
                        }
                    }

                    Common.LAUNCHER_CONTEXT.sendBroadcast(myIntent);
                }
            }
        });

        XposedBridge.hookAllMethods(Classes.Launcher, Methods.wMoveToCustomContentScreen, new XC_MethodHook() {

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (DEBUG) log("SystemUIHooks: moveToCustomContentScreen");

                Intent myIntent = new Intent();
                myIntent.putExtra(Common.XGELS_ACTION_EXTRA, Common.XGELS_ACTION_NAVBAR);
                myIntent.putExtra(Common.XGELS_ACTION, "BACK_HOME_ORIG");
                myIntent.setAction(Common.XGELS_INTENT);
                Common.LAUNCHER_CONTEXT.sendBroadcast(myIntent);
            }
        });

        findAndHookMethod(Classes.Workspace, Methods.wOnLauncherTransitionEnd, Classes.Launcher, boolean.class, boolean.class, new XC_MethodHook() {

            int TOWORKSPACE = 2;

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                int currPage = getIntField(Common.WORKSPACE_INSTANCE, Fields.pvCurrentPage);

                if ((Boolean) param.args[TOWORKSPACE]) {
                    if (DEBUG) log(param, "Transitioning to Workspace " + currPage + " " + (PreferencesHelper.defaultHomescreen - 1));

                } else {
                    if (DEBUG) log(param, "Transitioning to All Apps - BACK_HOME_ORIG");

                    Intent myIntent = new Intent();
                    myIntent.putExtra(Common.XGELS_ACTION_EXTRA, Common.XGELS_ACTION_NAVBAR);
                    myIntent.putExtra(Common.XGELS_ACTION, "BACK_HOME_ORIG");
                    myIntent.setAction(Common.XGELS_INTENT);
                    Common.LAUNCHER_CONTEXT.sendBroadcast(myIntent);
                }
            }
        });

        if (PreferencesHelper.dynamicBackbutton && PreferencesHelper.dynamicIconBackbutton) {

            findAndHookMethod(Classes.Launcher, Methods.lOpenFolder, Classes.FolderIcon, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (DEBUG) log("SystemUIHooks: openFolder");

                    int currentPage = getIntField(Common.WORKSPACE_INSTANCE, Fields.pvCurrentPage);

                    if (currentPage == (PreferencesHelper.defaultHomescreen - 1)
                            || PreferencesHelper.dynamicBackButtonOnEveryScreen) {
                        Intent myIntent = new Intent();
                        myIntent.putExtra(Common.XGELS_ACTION_EXTRA, Common.XGELS_ACTION_NAVBAR);
                        myIntent.putExtra(Common.XGELS_ACTION, "BACK_ORIG");
                        myIntent.setAction(Common.XGELS_INTENT);
                        Common.LAUNCHER_CONTEXT.sendBroadcast(myIntent);
                    }
                }
            });

            XposedBridge.hookAllMethods(Classes.Launcher, Methods.lCloseFolder, new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (DEBUG) log("SystemUIHooks: closeFolder");

                    int currentPage = getIntField(Common.WORKSPACE_INSTANCE, Fields.pvCurrentPage);

                    if (callMethod(Common.WORKSPACE_INSTANCE, Methods.wGetOpenFolder) != null
                            && (currentPage == (PreferencesHelper.defaultHomescreen - 1) || PreferencesHelper.dynamicBackButtonOnEveryScreen)
                            && !getBooleanField(param.thisObject, Fields.lPaused)
                            && getObjectField(Common.WORKSPACE_INSTANCE, Fields.wState).toString().equals("NORMAL")) {

                        Intent myIntent = new Intent();
                        myIntent.putExtra(Common.XGELS_ACTION_EXTRA, Common.XGELS_ACTION_NAVBAR);
                        myIntent.putExtra(Common.XGELS_ACTION, "BACK_POWER_OFF");
                        myIntent.setAction(Common.XGELS_INTENT);
                        Common.LAUNCHER_CONTEXT.sendBroadcast(myIntent);
                    }
                }
            });
        }
    }
}