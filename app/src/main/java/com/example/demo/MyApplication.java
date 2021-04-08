package com.example.demo;

import android.app.Application;
import android.content.Context;

import com.example.demo.classloader.MyClassloader;
import com.example.demo.utils.ReflectionUtils;

public class MyApplication extends Application {
    private static Context mContext;
    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        setClassLoader();
    }

    private void setClassLoader(){
        try {
            Context oBase = getBaseContext();
            Object oPackageInfo = ReflectionUtils.getPrivateFieldAnyway(oBase, "mPackageInfo");
            ClassLoader oClassLoader = (ClassLoader) ReflectionUtils.getPrivateFieldAnyway(oPackageInfo, "mClassLoader");
            ClassLoader cl = MyClassloader.createClassLoader(oClassLoader.getParent(), oClassLoader);
            ReflectionUtils.setPrivateFieldAnyway(oPackageInfo, "mClassLoader", cl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Context getMyContext() {
        return mContext;
    }
}
