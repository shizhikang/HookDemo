package com.example.demo;

import android.app.Activity;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.example.classloader.MyClassLoaderTest;
import com.example.demo.methodinterceptproxy.Test;
import com.example.demo.proxy.ServiceManagerUtils;
import com.mdit.library.proxy.Enhancer;
import com.mdit.library.proxy.MethodInterceptor;
import com.mdit.library.proxy.MethodProxy;

import java.lang.reflect.Method;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MyClassLoaderTest classLoaderTest = new MyClassLoaderTest();
        classLoaderTest.testClassLoader();

        testProxy();
        testMethodInterceptProxy();
    }

    private void testMethodInterceptProxy() {
        Enhancer enhancer = new Enhancer(this);
        enhancer.setSuperclass(Test.class);
        enhancer.setCallback(new MethodInterceptor() {
            @Override
            public Object intercept(Object object, Object[] args, MethodProxy methodProxy) throws Exception {
                Log.e("TAG","intercept  -- before---");
                Object obj = methodProxy.invokeSuper(object, args);
                Log.e("TAG","intercept  -- after---");
                return obj;
            }
        });
        Test test = (Test) enhancer.create();

        test.toast1(this);
    }

    private void testProxy() {
        try {
            IBinder binder = ServiceManagerUtils.hookActivityManager(new ServiceManagerUtils.IServiceInvokeCallBack() {
                @Override
                public boolean isHook(Object proxy, Method method, Object[] args) {
                    String methodString = method.toString();
                    if (methodString.contains("finishActivity")) {//拦截finish
                        return true;
                    }
                    return false;
                }

                @Override
                public Object hookMethod(Object proxy, Method method, Object[] args) throws Throwable {
                    String methodString = method.toString();
                    if (methodString.contains("finishActivity")) {
                        return true;
                    }
                    return null;
                }
            });
            //此时由于与AMS通信的binder被动态代理，调用到finishActivity时是不会去执行AMS相关的方法
            finish();
            ServiceManagerUtils.resetActivityManager(binder);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}