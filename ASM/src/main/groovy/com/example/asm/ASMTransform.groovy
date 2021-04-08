package com.example.asm;
import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import org.gradle.api.Project
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
class ASMTransform extends Transform {
    Project project

    public ASMTransform(Project project) {    // 构造函数，我们将Project保存下来备用
        this.project = project
    }

    @Override
    String getName() {
        return "ASMTransform"
    }

    //输入文件类型，有CLASSES和RESOURCES
    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    //    指Transform要操作内容的范围，官方文档Scope有7种类型：
//    EXTERNAL_LIBRARIES        只有外部库
//    PROJECT                       只有项目内容
//    PROJECT_LOCAL_DEPS            只有项目的本地依赖(本地jar)
//    PROVIDED_ONLY                 只提供本地或远程依赖项
//    SUB_PROJECTS              只有子项目。
//    SUB_PROJECTS_LOCAL_DEPS   只有子项目的本地依赖项(本地jar)。
//    TESTED_CODE                   由当前变量(包括依赖项)测试的代码
//    SCOPE_FULL_PROJECT        整个项目
    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    //指明当前Transform是否支持增量编译
    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)

        //inputs中是传过来的输入流，其中有两种格式，一种是jar包格式一种是目录格式。
        def inputs = transformInvocation.getInputs()
        //获取到输出目录，最后将修改的文件复制到输出目录，这一步必须做不然编译会报错
        def outputProvider = transformInvocation.getOutputProvider()

        for (TransformInput input : inputs) {
            for (JarInput jarInput : input.getJarInputs()) {
                handleJarInputs(jarInput, outputProvider)
            }

            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                handleDirectoryInput(directoryInput, outputProvider)
            }
        }
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
        FileUtils.copyFile(jarInput.getFile(), dest);
    }

    /**
     * 处理文件目录下的class文件
     */
    void handleDirectoryInput(DirectoryInput directoryInput, TransformOutputProvider outputProvider) {

        //列出目录所有文件（包含子文件夹，子文件夹内文件）
        directoryInput.file.eachFileRecurse { File file ->
            def fileName = file.name
            if (checkClassFile(fileName)) {
                System.out.println('filename----' + fileName)
                //对class文件进行读取与解析
                ClassReader classReader = new ClassReader(file.bytes)
                //对class文件的写入
                ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
                //访问class文件相应的内容，解析到某一个结构就会通知到ClassVisitor的相应方法
                ClassVisitor classVisitor = new LifecycleClassVisitor(classWriter)
                //依次调用 ClassVisitor接口的各个方法
                classReader.accept(0, ClassReader.EXPAND_FRAMES)
                //toByteArray方法会将最终修改的字节码以 byte 数组形式返回。
                byte[] bytes = classWriter.toByteArray()

                //通过文件流写入方式覆盖掉原先的内容，实现class文件的改写。
//                FileOutputStream outputStream = new FileOutputStream( file.parentFile.absolutePath + File.separator + fileName)
                //这个地址在javac目录下
                FileOutputStream outputStream = new FileOutputStream(file.path)
                outputStream.write(bytes)
                outputStream.close()
            }
        }

        //Transform 拷贝文件到transforms目录
        File dest = outputProvider.getContentLocation(
                directoryInput.getName(),
                directoryInput.getContentTypes(),
                directoryInput.getScopes(),
                Format.DIRECTORY);
        //将修改过的字节码copy到dest，就可以实现编译期间干预字节码的目的了
        FileUtils.copyDirectory(directoryInput.getFile(), dest)
    }

    void copyFile() {

    }

    /**
     * 检查class文件是否符合条件
     * @param name
     * @return
     */
    boolean checkClassFile(String name) {
        return name.endsWith("ASMDemo.class")
    }

    class LifecycleClassVisitor extends ClassVisitor {
        private String className;

        public LifecycleClassVisitor(ClassVisitor cv) {
            /**
             * 参数1：ASM API版本，源码规定只能为4，5，6
             * 参数2：ClassVisitor不能为 null
             */
            super(Opcodes.ASM5, cv);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            System.out.println("------ClassVisitor visit start-------");
            System.out.println(" visit className-------" + name);
            System.out.println(" visit superName-------" + superName);
            this.className = name;
        }



        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            System.out.println("ClassVisitor visitMethod name-------" + name);
            MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
            if (name.startsWith("staticASM")) {
                //处理方法
                return new LifecycleMethodVisitor(mv, className, name);
            }
            return mv;
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            System.out.println("------ClassVisitor visit end-------");
        }


    }

    public class LifecycleMethodVisitor extends MethodVisitor {
        private String className;
        private String methodName;

        public LifecycleMethodVisitor(MethodVisitor methodVisitor, String className, String methodName) {
            super(Opcodes.ASM5, methodVisitor);
            this.className = className;
            this.methodName = methodName;
        }

        //方法执行前插入
        @Override
        public void visitCode() {
            super.visitCode();
            System.out.println("MethodVisitor visitCode------");

//            mv.visitLdcInsn("TAG");
//            mv.visitLdcInsn(className + "------->" + methodName);
//            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/util/Log", "e", "(Ljava/lang/String;Ljava/lang/String;)I", false);
//            mv.visitInsn(Opcodes.POP);
            mv.visitLdcInsn("TAG")
            mv.visitLdcInsn("===== This is just a test message =====")
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "android/util/Log",
                    "e",
                    "(Ljava/lang/String;Ljava/lang/String;)I",
                    false
            )
            mv.visitInsn(Opcodes.POP)
        }

        //方法执行后插入
        @Override
        public void visitInsn(int opcode) {
//        if (opcode==Opcodes.RETURN){
//            mv.visitLdcInsn("TAG");
//            mv.visitLdcInsn(className + "------->" + methodName);
//            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/util/Log", "e", "(Ljava/lang/String;Ljava/lang/String;)I", false);
//            mv.visitInsn(Opcodes.POP);
//        }
            super.visitInsn(opcode);
            System.out.println("MethodVisitor visitInsn------");
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            System.out.println("MethodVisitor visitEnd------");
        }


    }
}