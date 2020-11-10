package com.yuong.plugin.base;

import android.app.Activity;
import android.app.Application;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;

import java.lang.reflect.Field;

/**
 * @author : zhiwen.yang
 * date   : 2020/6/5
 * desc   :
 */
public abstract class BaseAppCompatActivity extends Activity {

    protected ContextThemeWrapper mContext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Resources resources = PluginManager.getInstance(getApplication()).getResource();
        Application application = getApplication();
        Resources resources = application.getResources();
        mContext = new ContextThemeWrapper(getBaseContext(), 0);
        //替換插件的
        Class<? extends ContextThemeWrapper> clazz = mContext.getClass();
        try {
            Field mResourcesFiled = clazz.getDeclaredField("mResources");
            mResourcesFiled.setAccessible(true);
            mResourcesFiled.set(mContext, resources);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        try {
//            Class<?> superclass = getClass().getSuperclass().getSuperclass();
//            Field mParentResourcesFiled = superclass.getDeclaredField("mResources");
//            mParentResourcesFiled.setAccessible(true);
//            mParentResourcesFiled.set(this, resources);
////
////            Field mDelegateFiled = superclass.getDeclaredField("mDelegate");
////            mDelegateFiled.setAccessible(true);
////            Object mDelegateImpl = mDelegateFiled.get(this);
////            Field mDelegateContextFiled = mDelegateImpl.getClass().getDeclaredField("mContext");
////            mDelegateContextFiled.setAccessible(true);
////            mDelegateContextFiled.set(mDelegateImpl, mContext);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

//    @Override
//    public Resources getResources() {
//        if (getApplication() != null && getApplication().getResources() != null) {
//            //如果不为空，就说明已经被添加到了宿主当中
////            Application application = getApplication();
////            Resources resources = application.getResources();
//            return getApplication().getResources();
//        }
//        return super.getResources();
//    }
//
//    @Override
//    public AssetManager getAssets() {
//        if (getApplication() != null && getApplication().getAssets() !=  null) {
//            //如果不为空，就说明已经被添加到了宿主当中
////            Application application = getApplication();
////            AssetManager assets = application.getAssets();
//            return getApplication().getAssets();
//        }
//        return super.getAssets();
//    }
////
//    @Override
//    public Resources.Theme getTheme() {
//        if (getApplication() != null && getApplication().getTheme() !=  null) {
//            //如果不为空，就说明已经被添加到了宿主当中
////            Application application = getApplication();
////            Resources.Theme theme = application.getTheme();
//            return getApplication().getTheme();
//        }
//        return super.getTheme();
//    }

}
