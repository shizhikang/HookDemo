package com.example.demo.classloader;

import android.app.Activity;
import android.content.Context;

import com.android.dx.Code;
import com.android.dx.DexMaker;
import com.android.dx.FieldId;
import com.android.dx.Local;
import com.android.dx.MethodId;
import com.android.dx.TypeId;
import com.example.demo.MyApplication;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Modifier;

public class DexMakerUtil {
    public static Class loadClass(String className, ClassLoader mOrig) throws Exception {
        Class clazz = null;
        DexMaker dexMaker = new DexMaker();
        className = className.replace(".", "_");
        TypeId<?> cls = TypeId.get("L"+className+";");
        dexMaker.declare(cls, "", Modifier.PUBLIC, TypeId.get(Activity.class));
//        //定义一个methodCallBack变量，调用method时，回调methodCallBack
//        FieldId fieldId = cls.getField(TypeId.get(IMethodCallBack.class), "methodCallBack");
//
        //设置默认无参constructor 否则newInstance 会报has no zero argument constructor
        Code code = dexMaker.declare(cls.getConstructor(), Modifier.PUBLIC);
        Local<?> thisRef = code.getThis(cls);
        TypeId typeActivity = TypeId.get(Activity.class);
        code.invokeDirect(typeActivity.getConstructor(), null, thisRef);
        code.returnVoid();
        //method
        generateMethod(dexMaker, cls);

        File outputDir = new File(MyApplication.getMyContext().getDir("path", Context.MODE_PRIVATE).getPath());
        if (outputDir.exists()) {
            File[] fs = outputDir.listFiles();
            for (File f : fs) {
                f.delete();
            }
        }
        ClassLoader loader = dexMaker.generateAndLoad(MyApplication.getMyContext().getClassLoader(), outputDir);
        clazz = loader.loadClass(className);

        return clazz;
    }
    private static void generateMethod(DexMaker dexMaker, TypeId<?> declaringType) {
        //method testClassLoader
        //方法声明
        MethodId testClassLoader = declaringType.getMethod(TypeId.VOID, "testClassLoader");
        Code code = dexMaker.declare(testClassLoader, Modifier.PUBLIC);

//        //要先将local全部声明完毕，才能设置值
//        Local localString = code.newLocal(TypeId.STRING);
//
//        TypeId MyClassLoader = TypeId.get(MyClassloader.class);
//        FieldId mMethodCallBack = MyClassLoader.getField(TypeId.get(IMethodCallBack.class), "mMethodCallBack");
//        TypeId callback = TypeId.get(IMethodCallBack.class);
//        Local localCallback = code.newLocal(callback);
//        //IMethodCallBack localCallback = MyClassloader.mMethodCallBack
//        code.sget(mMethodCallBack, localCallback);
//        //String localString = "testClassLoader"
//        code.loadConstant(localString, "testClassLoader");
//
//        //localCallback.call(localString)
//        MethodId call = callback.getMethod(TypeId.VOID, "call", TypeId.STRING, TypeId.get(Object[].class));
//        code.invokeVirtual(call, null, localCallback, localString);


        // System.out.println(s);
        TypeId<System> systemType = TypeId.get(System.class);
        TypeId<PrintStream> printStreamType = TypeId.get(PrintStream.class);
        Local<PrintStream> localSystemOut = code.newLocal(printStreamType);
        Local<String> s = code.newLocal(TypeId.STRING);
        code.loadConstant(s, "hello dexmaker");
        FieldId<System, PrintStream> systemOutField = systemType.getField(printStreamType, "out");
        code.sget(systemOutField, localSystemOut);
        MethodId<PrintStream, Void> printlnMethod = printStreamType.getMethod(
                TypeId.VOID, "println", TypeId.STRING);
        code.invokeVirtual(printlnMethod, null, localSystemOut, s);
        // return;
        code.returnVoid();
    }
}
