package com.dodola.watchdogkiller;

import android.content.Context;

import com.tencent.tinker.lib.tinker.Tinker;
import com.tencent.tinker.lib.tinker.TinkerInstaller;
import com.tencent.tinker.loader.app.ApplicationLike;

public class TinkerManager {

    private static boolean isInstall = false;
    private static ApplicationLike mAppLike;

    /**
     * 完成Tinker的初始化
     */
    public static void installTinker(ApplicationLike applicationLike) {
        mAppLike = applicationLike;
        if (isInstall) {
            return;
        }
        TinkerInstaller.install(applicationLike);//完成tinker的初始化
        isInstall = true;
    }

    /**
     * 发起补丁修复请求
     */
    public static void loadPatch(String path) {
        if (Tinker.isTinkerInstalled()) {
            TinkerInstaller.onReceiveUpgradePatch(getApplicationContext(), path);
        }
    }

    //获取上下文
    private static Context getApplicationContext() {
        if (mAppLike != null) {
            return mAppLike.getApplication().getApplicationContext();
        }
        return null;
    }
}
