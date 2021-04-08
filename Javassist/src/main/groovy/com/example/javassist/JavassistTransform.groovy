package com.example.javassist

import com.android.build.api.transform.*
import com.google.common.collect.Sets
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

public class JavassistTransform extends Transform {
    Project project

    public JavassistTransform(Project project) {    // 构造函数，我们将Project保存下来备用
        this.project = project
    }

    @Override
    String getName() {// 设置我们自定义的Transform对应的Task名称
        return "JavassistTrans"
    }


    @Override
    // 指定输入的类型，通过这里的设定，可以指定我们要处理的文件类型这样确保其他类型的文件不会传入
    Set<QualifiedContent.ContentType> getInputTypes() {
        return Sets.immutableEnumSet(QualifiedContent.DefaultContentType.CLASSES)
    }


    @Override
// 指定Transform的作用范围
    Set<QualifiedContent.Scope> getScopes() {
        return Sets.immutableEnumSet(QualifiedContent.Scope.PROJECT, QualifiedContent.Scope.PROJECT_LOCAL_DEPS,
                QualifiedContent.Scope.SUB_PROJECTS, QualifiedContent.Scope.SUB_PROJECTS_LOCAL_DEPS,
                QualifiedContent.Scope.EXTERNAL_LIBRARIES)
    }

    //指明当前Transform是否支持增量编译
    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs,
                   Collection<TransformInput> referencedInputs,
                   TransformOutputProvider outputProvider, boolean isIncremental)
            throws IOException, TransformException, InterruptedException {
        def startTime = System.currentTimeMillis();

        // Transform的inputs有两种类型，一种是目录，一种是jar包，要分开遍历
        inputs.each { TransformInput input ->
            try {
                //对 jar包 类型的inputs 进行遍历
                input.jarInputs.each { JarInput jarInput ->
                    handleJarInputs(jarInput, outputProvider)
                }
            } catch (Exception e) {
                project.logger.err e.getMessage()
            }
            //对类型为“文件夹”的input进行遍历
            input.directoryInputs.each { DirectoryInput directoryInput ->
                //文件夹里面包含的是我们手写的类以及R.class、BuildConfig.class以及R$XXX.class等
                //D:\data\code\TestCode\Demo\app\build\intermediates\transforms\desugar\debug\0
                File dir = directoryInput.file;
                dir.eachFileRecurse { File file ->
                    if (file.isFile() && file.getName().contains("JavassistDemo")) {
                        //修改JavassistDemo.class
                        ClassPool pool = ClassPool.getDefault()
                        pool.appendClassPath(dir.absolutePath)
                        CtClass clazz = pool.getCtClass("com.example.demo.javassist.JavassistDemo")
                        if (clazz.isFrozen()) clazz.defrost()//解冻
                        CtMethod staticJavassist = clazz.getDeclaredMethod("staticJavassist")
                        staticJavassist.insertBefore("System.out.println(\"JavassistDemo -> staticJavassist - before\");\n")
                        staticJavassist.insertAfter("System.out.println(\"JavassistDemo -> staticJavassist - end\");\n")

                        clazz.writeFile(dir.absolutePath);
                        clazz.detach()//用完一定记得要卸载，否则pool里的永远是旧的代码
                    }

                }
                // 获取output目录
                def dest = outputProvider.getContentLocation(directoryInput.name,
                        directoryInput.contentTypes, directoryInput.scopes,
                        Format.DIRECTORY)

                // 将input的目录复制到output指定目录
                FileUtils.copyDirectory(directoryInput.file, dest)
            }

        }


        ClassPool.getDefault().clearImportedPackages();
        project.logger.error("JavassistTransform cast :" + (System.currentTimeMillis() - startTime) / 1000 + " secs");
    }

    /**
     * 处理Jar中的class文件
     */
    void handleJarInputs(JarInput jarInput, TransformOutputProvider outputProvider) {
        File dest = outputProvider.getContentLocation(
                jarInput.getName(),
                jarInput.getContentTypes(),
                jarInput.getScopes(),
                Format.JAR);
        //将修改过的字节码copy到dest，就可以实现编译期间干预字节码的目的了
        com.android.utils.FileUtils.copyFile(jarInput.getFile(), dest);
    }
}