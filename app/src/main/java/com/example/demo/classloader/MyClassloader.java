package com.example.demo.classloader;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import dalvik.system.PathClassLoader;

public class MyClassloader extends PathClassLoader {
    private final ClassLoader mOrig;

    public MyClassloader(ClassLoader parent, ClassLoader orig) {
        super("", "", parent);
        mOrig = orig;
//        copyFromOriginal(orig);
    }

//    private void copyFromOriginal(ClassLoader orig) {
//
//        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
//            // Android 2.2 - 2.3.7，有一堆字段，需要逐一复制
//            // 以下方法在较慢的手机上用时：8ms左右
//            copyFieldValue("libPath", orig);
//            copyFieldValue("libraryPathElements", orig);
//            copyFieldValue("mDexs", orig);
//            copyFieldValue("mFiles", orig);
//            copyFieldValue("mPaths", orig);
//            copyFieldValue("mZips", orig);
//        } else {
//            // Android 4.0以上只需要复制pathList即可
//            // 以下方法在较慢的手机上用时：1ms
//            copyFieldValue("pathList", orig);
//        }
//    }
//
//    private void copyFieldValue(String field, ClassLoader orig) {
//        try {
//            // 复制Field中的值到this里
//            Object o = ReflectionUtils.getPrivateFieldAnyway(orig, field);
//            ReflectionUtils.setPrivateFieldAnyway(this, field, o);
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        }
//    }

    public static IMethodCallBack mMethodCallBack;

    public static MyClassloader createClassLoader(ClassLoader parent, ClassLoader original) {
        return new MyClassloader(parent, original);
    }

    @Override
    protected Class<?> findClass(String className) throws ClassNotFoundException {
        // INFO Never reach here since override loadClass , unless not found class
        System.out.println("shizhikang - MyClassloader - findClass - begin: " + className);
        return super.findClass(className);
    }

    /**
     * 使用反射调用时，会触发loadClass
     */
    @Override
    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        Class<?> c = null;
        System.out.println("shizhikang - MyClassloader - loadClass - begin: " + className);
        //如果是需要hook的class，屏蔽classloader父委托机制，加载我们新建的类
        if (className.contains("com.example.classloader.MyClassLoaderTest")) {
            System.out.println("shizhikang - MyClassloader - loadClass: " + className);
            mMethodCallBack = new IMethodCallBack() {
                @Override
                public void call(String method, Object... args) {
                    System.out.println("shizhikang - methodInvoke" + method);
                }
            };
            try {
                c = DexMakerUtil.loadClass(className, mOrig);
                System.out.println("shizhikang - MyClassloader - MyClassLoaderTest : " + c);
                for (Constructor constructor : c.getDeclaredConstructors()) {
                    System.out.println("shizhikang - MyClassloader - MyClassLoaderTest - constructor: " + constructor);
                }
                for (Method method : c.getDeclaredMethods()) {
                    System.out.println("shizhikang - MyClassloader - MyClassLoaderTest - method: " + method);
                }
//                c.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (c != null) {
            return c;
        }
        //恢复父委托机制
        try {
            c = mOrig.loadClass(className);
            return c;
        } catch (Throwable e) {
            //
        }
        //
        return super.loadClass(className, resolve);
    }
}
