package com.yuong.loadapk;

import android.app.Application;
import android.content.Context;
import com.yuong.loadapk.manager.HookManager;
import me.weishu.reflection.Reflection;

/**
 * @author :
 * date   : 2020/6/1
 * desc   :
 */
public class MyApplication extends Application {

    public static boolean isHookFinish = false;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Reflection.unseal(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //hook LoadApk
                    HookManager.getInstance(MyApplication.this).customLoadApkAction();
                    //先hook AMS检查
                    HookManager.getInstance(MyApplication.this).hookAMSAction();
                    //hook ActivityThread
                    HookManager.getInstance(MyApplication.this).hookLaunchActivity();
                    isHookFinish = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }
}
