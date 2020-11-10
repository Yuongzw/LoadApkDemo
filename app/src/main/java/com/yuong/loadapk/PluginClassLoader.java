package com.yuong.loadapk;

import dalvik.system.DexClassLoader;

/**
 * @author :
 * date   : 2020/6/3
 * desc   : 专门加载插件里面的class
 */
public class PluginClassLoader extends DexClassLoader {

    public PluginClassLoader(String dexPath, String optimizedDirectory, String librarySearchPath, ClassLoader parent) {
        super(dexPath, optimizedDirectory, librarySearchPath, parent);
    }
}
