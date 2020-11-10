package com.yuong.loadapk.manager;

import android.annotation.SuppressLint;
import android.app.AppOpsManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Log;
import androidx.annotation.NonNull;
import com.yuong.loadapk.Constans;
import com.yuong.loadapk.PluginClassLoader;
import com.yuong.loadapk.proxy.ProxyActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * @author :
 * date   : 2020/6/1
 * desc   :
 */
public class HookManager {
    private Context context;
    private static HookManager instance;

    private HookManager(Context context) {
        this.context = context;
    }

    public static HookManager getInstance(Context context) {
        if (instance == null) {
            synchronized (HookManager.class) {
                if (instance == null) {
                    instance = new HookManager(context);
                }
            }
        }
        return instance;
    }



    /**
     * 自定义一个 LoadedApk 再自定义一个ClassLoader 将 LoadedApk 添加到 mPackages，此 LoadedApk 专门用来加载插件的 class
     */
    public void customLoadApkAction() throws Exception {
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "plugin2.apk");
        if (!file.exists()) {
            throw new FileNotFoundException("插件包不存在");
        }
        //获取 ActivityThread 类
        Class<?> mActivityThreadClass = Class.forName("android.app.ActivityThread");
        //获取 ActivityThread 的 currentActivityThread() 方法
        Method currentActivityThread = mActivityThreadClass.getDeclaredMethod("currentActivityThread");
        currentActivityThread.setAccessible(true);
        //获取 ActivityThread 实例
        Object mActivityThread = currentActivityThread.invoke(null);

        //final ArrayMap<String, WeakReference<LoadedApk>> mPackages = new ArrayMap<>();
        //获取 mPackages 属性
        Field mPackagesField = mActivityThreadClass.getDeclaredField("mPackages");
        mPackagesField.setAccessible(true);
        //获取 mPackages 属性的值
        ArrayMap<String, Object> mPackages = (ArrayMap<String, Object>) mPackagesField.get(mActivityThread);
//        if (mPackages.size() >= 2) {
//            return;
//        }

        //自定义一个 LoadedApk，系统是如何创建的我们就如何创建
        //执行下面的方法会返回一个 LoadedApk，我们就仿照系统执行此方法
        /*
              this.packageInfo = client.getPackageInfoNoCheck(activityInfo.applicationInfo,
                    compatInfo);
              public final LoadedApk getPackageInfo(ApplicationInfo ai, CompatibilityInfo compatInfo,
                    int flags)
         */
        Class<?> mCompatibilityInfoClass = Class.forName("android.content.res.CompatibilityInfo");
        Method getLoadedApkMethod = mActivityThreadClass.getDeclaredMethod("getPackageInfoNoCheck",
                ApplicationInfo.class, mCompatibilityInfoClass);

        /*
             public static final CompatibilityInfo DEFAULT_COMPATIBILITY_INFO = new CompatibilityInfo() {};
         */
        //以上注释是获取默认的 CompatibilityInfo 实例
        Field mCompatibilityInfoDefaultField = mCompatibilityInfoClass.getDeclaredField("DEFAULT_COMPATIBILITY_INFO");
        Object mCompatibilityInfo = mCompatibilityInfoDefaultField.get(null);

        //获取一个 ApplicationInfo实例
        ApplicationInfo applicationInfo = getAppInfo(file);
//        applicationInfo.uid = context.getApplicationInfo().uid;
        //执行此方法，获取一个 LoadedApk
        Object mLoadedApk = getLoadedApkMethod.invoke(mActivityThread, applicationInfo, mCompatibilityInfo);

        //自定义一个 ClassLoader
        String optimizedDirectory = context.getDir("plugin", Context.MODE_PRIVATE).getAbsolutePath();
        PluginClassLoader classLoader = new PluginClassLoader(file.getAbsolutePath(), optimizedDirectory,
                null, context.getClassLoader());

        //private ClassLoader mClassLoader;
        //获取 LoadedApk 的 mClassLoader 属性
        Field mClassLoaderField = mLoadedApk.getClass().getDeclaredField("mClassLoader");
        mClassLoaderField.setAccessible(true);
        //设置自定义的 classLoader 到 mClassLoader 属性中
        mClassLoaderField.set(mLoadedApk, classLoader);

        WeakReference loadApkReference = new WeakReference(mLoadedApk);
        //添加自定义的 LoadedApk
        mPackages.put(applicationInfo.packageName, loadApkReference);
        //重新设置 mPackages
        mPackagesField.set(mActivityThread, mPackages);
        Thread.sleep(2000);
    }

    /**
     * 获取 ApplicationInfo 实例
     *
     * @return
     */
    private ApplicationInfo getAppInfo(File file) throws Exception {
        /*
            执行此方法获取 ApplicationInfo
            public static ApplicationInfo generateApplicationInfo(Package p, int flags,PackageUserState state)
         */
        Class<?> mPackageParserClass = Class.forName("android.content.pm.PackageParser");
        Class<?> mPackageClass = Class.forName("android.content.pm.PackageParser$Package");
        Class<?> mPackageUserStateClass = Class.forName("android.content.pm.PackageUserState");
        //获取 generateApplicationInfo 方法
        Method generateApplicationInfoMethod = mPackageParserClass.getDeclaredMethod("generateApplicationInfo",
                mPackageClass, int.class, mPackageUserStateClass);

        //创建 PackageParser 实例
        Object mmPackageParser = mPackageParserClass.newInstance();

        //获取 Package 实例
        /*
            执行此方法获取一个 Package 实例
            public Package parsePackage(File packageFile, int flags)
         */
        //获取 parsePackage 方法
        Method parsePackageMethod = mPackageParserClass.getDeclaredMethod("parsePackage", File.class, int.class);
        //执行 parsePackage 方法获取 Package 实例
        Object mPackage = parsePackageMethod.invoke(mmPackageParser, file, PackageManager.GET_ACTIVITIES);

        //执行 generateApplicationInfo 方法，获取 ApplicationInfo 实例
        ApplicationInfo applicationInfo = (ApplicationInfo) generateApplicationInfoMethod.invoke(null, mPackage, 0,
                mPackageUserStateClass.newInstance());
        //我们获取的 ApplicationInfo 默认路径是没有设置的，我们要自己设置
        // applicationInfo.sourceDir = 插件路径;
        // applicationInfo.publicSourceDir = 插件路径;
        applicationInfo.sourceDir = file.getAbsolutePath();
        applicationInfo.publicSourceDir = file.getAbsolutePath();
        return applicationInfo;
    }


    public void hookAMSAction() throws Exception {
        //动态代理
        Class<?> mIActivityManagerClass;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mIActivityManagerClass = Class.forName("android.app.IActivityTaskManager");
        } else {
            mIActivityManagerClass = Class.forName("android.app.IActivityManager");
        }
        //获取 ActivityManager 或 ActivityManagerNative 或 ActivityTaskManager
        Class<?> mActivityManagerClass;
        Method getActivityManagerMethod;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            mActivityManagerClass = Class.forName("android.app.ActivityManagerNative");
            getActivityManagerMethod = mActivityManagerClass.getDeclaredMethod("getDefault");
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1 && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            mActivityManagerClass = Class.forName("android.app.ActivityManager");
            getActivityManagerMethod = mActivityManagerClass.getDeclaredMethod("getService");
        } else {
            mActivityManagerClass = Class.forName("android.app.ActivityTaskManager");
            getActivityManagerMethod = mActivityManagerClass.getDeclaredMethod("getService");
        }
        getActivityManagerMethod.setAccessible(true);
        //这个实例本质是 IActivityManager或者IActivityTaskManager
        final Object IActivityManager = getActivityManagerMethod.invoke(null);

        //创建动态代理
        Object mActivityManagerProxy = Proxy.newProxyInstance(
                context.getClassLoader(),
                new Class[]{mIActivityManagerClass},//要监听的回调接口
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                        if ("startActivity".equals(method.getName())) {
                            //做自己的业务逻辑
                            //换成可以通过AMS检测的Activity
                            Intent intent = new Intent(context, ProxyActivity.class);
                            intent.putExtra("actonIntent", (Intent) args[2]);
                            args[2] = intent;
                        }
                        //让程序继续能够执行下去
                        return method.invoke(IActivityManager, args);
                    }
                }
        );

        //获取 IActivityTaskManagerSingleton 或者 IActivityManagerSingleton 或者 gDefault 属性
        Field mSingletonField;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            mSingletonField = mActivityManagerClass.getDeclaredField("gDefault");
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1 && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            mSingletonField = mActivityManagerClass.getDeclaredField("IActivityManagerSingleton");
        } else {
            mSingletonField = mActivityManagerClass.getDeclaredField("IActivityTaskManagerSingleton");
        }
        mSingletonField.setAccessible(true);
        Object mSingleton = mSingletonField.get(null);

        //替换点
        Class<?> mSingletonClass = Class.forName(Constans.SINGLETON);
        Field mInstanceField = mSingletonClass.getDeclaredField("mInstance");
        mInstanceField.setAccessible(true);
        //将我们创建的动态代理设置到 mInstance 属性当中
        mInstanceField.set(mSingleton, mActivityManagerProxy);
    }

    public void hookLaunchActivity() throws Exception {
        //获取 ActivityThread 类
        Class<?> mActivityThreadClass = Class.forName("android.app.ActivityThread");

        //获取 ActivityThread 的 currentActivityThread() 方法
        Method currentActivityThread = mActivityThreadClass.getDeclaredMethod("currentActivityThread");
        currentActivityThread.setAccessible(true);
        //获取 ActivityThread 实例
        Object mActivityThread = currentActivityThread.invoke(null);

        //获取 ActivityThread 的 mH 属性
        Field mHField = mActivityThreadClass.getDeclaredField("mH");
        mHField.setAccessible(true);
        Handler mH = (Handler) mHField.get(mActivityThread);

        //获取 Handler 的 mCallback 属性
        Field mCallbackField = Handler.class.getDeclaredField("mCallback");
        mCallbackField.setAccessible(true);
        //设置我们自定义的 CallBack
        mCallbackField.set(mH, new MyCallBack());
    }

    class MyCallBack implements Handler.Callback {

        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (msg.what == Constans.EXECUTE_TRANSACTION) {
                try {
                    Field mActivityCallbacksField = msg.obj.getClass().getDeclaredField("mActivityCallbacks");
                    mActivityCallbacksField.setAccessible(true);
                    List<Object> mActivityCallbacks = (List<Object>) mActivityCallbacksField.get(msg.obj);
                    if (mActivityCallbacks != null && mActivityCallbacks.size() > 0) {
                        Object mClientTransactionItem = mActivityCallbacks.get(0);
                        Class<?> mLaunchActivityItemClass = Class.forName("android.app.servertransaction.LaunchActivityItem");
                        if (mLaunchActivityItemClass.isInstance(mClientTransactionItem)) {
                            //获取 LaunchActivityItem 的 mIntent 属性
                            Field mIntentField = mClientTransactionItem.getClass().getDeclaredField("mIntent");
                            mIntentField.setAccessible(true);
                            Intent intent = (Intent) mIntentField.get(mClientTransactionItem);
                            //取出我们传递的值
                            Intent actonIntent = intent.getParcelableExtra("actonIntent");

                            /**
                             * 我们在以下代码中，对插件 和 宿主进行区分
                             */
                            Field mActivityInfoField = mClientTransactionItem.getClass().getDeclaredField("mInfo");
                            mActivityInfoField.setAccessible(true);
                            ActivityInfo mActivityInfo = (ActivityInfo) mActivityInfoField.get(mClientTransactionItem);

                            if (actonIntent != null) {
                                //替换掉原来的intent属性的值
                                mIntentField.set(mClientTransactionItem, actonIntent);
                                //证明是插件
                                if (actonIntent.getPackage() == null) {
                                    mActivityInfo.applicationInfo.packageName = actonIntent.getComponent().getPackageName();
                                    hookGlobalProviderHolder();
                                    hookSystemProviderHolder();
                                    //hook 拦截 getPackageInfo 做自己的逻辑
                                    hookGetPackageInfo();
                                } else {
                                    //宿主的
                                    mActivityInfo.applicationInfo.packageName = actonIntent.getPackage();
                                }
                            }

                            mActivityInfoField.set(mClientTransactionItem, mActivityInfo);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else if (msg.what == Constans.LAUNCH_ACTIVITY) {
                /*
                    7.0以下代码
                     case LAUNCH_ACTIVITY: {
                    Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "activityStart");
                    final ActivityClientRecord r = (ActivityClientRecord) msg.obj;

                    r.packageInfo = getPackageInfoNoCheck(
                            r.activityInfo.applicationInfo, r.compatInfo);
                    handleLaunchActivity(r, null, "LAUNCH_ACTIVITY");
                    Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
                } break;

                 */
                try {
                    //获取 ActivityClientRecord 的 intent 属性
                    Field intentField = msg.obj.getClass().getDeclaredField("intent");
                    intentField.setAccessible(true);
                    Intent intent = (Intent) intentField.get(msg.obj);
                    //取出我们传递的值
                    Intent actonIntent = intent.getParcelableExtra("actonIntent");

                    /**
                     * 我们在以下代码中，对插件 和 宿主进行区分
                     */
                    Field mActivityInfoField = msg.obj.getClass().getDeclaredField("activityInfo");
                    mActivityInfoField.setAccessible(true);
                    ActivityInfo mActivityInfo = (ActivityInfo) mActivityInfoField.get(msg.obj);

                    if (actonIntent != null) {
                        //替换掉原来的intent属性的值
                        intentField.set(msg.obj, actonIntent);
                        //证明是插件
                        if (actonIntent.getPackage() == null) {
                            mActivityInfo.applicationInfo.packageName = actonIntent.getComponent().getPackageName();
                            hookGetPackageInfo();
                        } else {
                            //宿主的
                            mActivityInfo.applicationInfo.packageName = actonIntent.getPackage();
                        }
                    }
                    mActivityInfoField.set(msg.obj, mActivityInfo);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return false;
        }
    }

    private void hookGetPackageInfo() throws Exception {
        //static volatile IPackageManager sPackageManager;
        //获取 系统的 sPackageManager 把它替换成我们自己的动态代理
        Class<?> mActivityThreadClass = Class.forName("android.app.ActivityThread");
        Field mPackageManager1Field = mActivityThreadClass.getDeclaredField("sPackageManager");
        mPackageManager1Field.setAccessible(true);
        final Object sPackageManager = mPackageManager1Field.get(null);

        //public static IPackageManager getPackageManager()
        // 获取getPackageManager方法
//        Method getPackageManagerMethod = mActivityThreadClass.getDeclaredMethod("getPackageManager");
//        //执行 getPackageManager方法，得到 sPackageManager
//        final Object sPackageManager = getPackageManagerMethod.invoke(null);

        Class<?> mIPackageManagerClass = Class.forName("android.content.pm.IPackageManager");

        //实现动态代理
        Object mPackageManagerProxy = Proxy.newProxyInstance(
                context.getClassLoader(),
                new Class[]{mIPackageManagerClass},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if ("getPackageInfo".equals(method.getName())) {
                            //如果是 getPackageInfo 方法才做我们的逻辑
                            //如何才能绕过 PMS检查呢
                            //pi != null
                            //直接返回一个 PackageInfo
                            Log.e("yuongzw", method.getName());
                            return new PackageInfo();

                        }
                        return method.invoke(sPackageManager, args);
                    }
                }
        );

        //替换 换成我们自己的动态代理
        mPackageManager1Field.set(null, mPackageManagerProxy);

    }

    @SuppressLint("PrivateApi")
    private void hookGlobalProviderHolder() throws Exception{
        Class<?> mIContentProviderClass = Class.forName("android.content.IContentProvider");

        Field sProviderHolderFiled = Settings.Global.class.getDeclaredField("sProviderHolder");
        sProviderHolderFiled.setAccessible(true);
        Object sProviderHolder = sProviderHolderFiled.get(null);
        Method getProviderMethod = sProviderHolder.getClass().getDeclaredMethod("getProvider", ContentResolver.class);
        getProviderMethod.setAccessible(true);
        final Object iContentProvider = getProviderMethod.invoke(sProviderHolder, context.getContentResolver());
        Field mContentProviderFiled = sProviderHolder.getClass().getDeclaredField("mContentProvider");
        mContentProviderFiled.setAccessible(true);

        Object mContentProviderProxy = Proxy.newProxyInstance(
                context.getClassLoader(),
                new Class[]{mIContentProviderClass},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (method.getName().equals("call")) {
                            Log.d("yuongzw", method.getName());
                            args[0] = context.getPackageName();
                        }
                        return method.invoke(iContentProvider, args);
                    }
                }
        );
        mContentProviderFiled.set(sProviderHolder, mContentProviderProxy);
    }

    private void hookSystemProviderHolder() throws Exception{
        Class<?> mIContentProviderClass = Class.forName("android.content.IContentProvider");

        Field sProviderHolderFiled = Settings.System.class.getDeclaredField("sProviderHolder");
        sProviderHolderFiled.setAccessible(true);
        Object sProviderHolder = sProviderHolderFiled.get(null);
        Method getProviderMethod = sProviderHolder.getClass().getDeclaredMethod("getProvider", ContentResolver.class);
        getProviderMethod.setAccessible(true);
        final Object iContentProvider = getProviderMethod.invoke(sProviderHolder, context.getContentResolver());
        Field mContentProviderFiled = sProviderHolder.getClass().getDeclaredField("mContentProvider");
        mContentProviderFiled.setAccessible(true);

        Object mContentProviderProxy = Proxy.newProxyInstance(
                context.getClassLoader(),
                new Class[]{mIContentProviderClass},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (method.getName().equals("call")) {
                            Log.d("yuongzw", method.getName());
                            args[0] = context.getPackageName();
                        }
                        return method.invoke(iContentProvider, args);
                    }
                }
        );
        mContentProviderFiled.set(sProviderHolder, mContentProviderProxy);
    }

}
