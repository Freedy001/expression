import com.freedy.expression.utils.ReflectionUtils;
import lombok.SneakyThrows;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2022/4/3 9:39
 */
public class Main {

    @SneakyThrows
    public static void main(String[] args) throws IOException {
//        "".startsWith()
        Pattern copile = Pattern.compile("\\d{3}.\\d{3}.\\d{3}");
//        System.out.println(Main.class.getClassLoader().getParent().getParent());
        //
//        Class<?> aClass = Main.class.getClassLoader().loadClass("com.freedy.expression.stander.standerFunc.StandardDefiner");
//        System.out.println(StandardDefiner.class==aClass);
//        System.out.println("haha");
//        Class<?> aClass = Reflection.getCallerClass();
//        System.out.println(aClass);
//        System.out.println(new Expression("def a=12;def b=23;a+b+10;", new StandardEvaluationContext()).getValue());
//        Package.getPackages()

        for (Package aPackage : Main.class.getClassLoader().getDefinedPackages()) {
            System.out.println(aPackage);
            System.out.println(ReflectionUtils.getFieldVal(aPackage.getClass(),aPackage,"module"));
        }
//        Date date = new Date(0);
//        ClassReader classReader = new ClassReader("com.freedy.expression.core.Expression");
//        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
//        ClassVisitor classVisitor = new TestClassVisitor(classWriter);
//        classReader.accept(classVisitor, ClassReader.SKIP_DEBUG);
//        System.out.println(Arrays.toString(classWriter.toByteArray()));
//        byte[] classFile = classWriter.toByteArray();
//        ClassLoader loader = new ClassLoader() {
//            @Override
//            protected Class<?> findClass(String name) throws ClassNotFoundException {
//                if (name.equals("abc123")) {
//                    return defineClass("abc", classFile, 0, classFile.length);
//                }
//                return super.findClass(name);
//            }
//        };
//        Class<?> aClass = loader.loadClass("abc123");
//        System.out.println();
//        System.out.println();
//        System.out.println();
//        System.out.println();
//        aClass.getConstructor().newInstance();
    }
//
//    private static class TestClassVisitor extends ClassVisitor {
//
//        public TestClassVisitor(ClassVisitor classVisitor) {
//            super(Opcodes.ASM9, classVisitor);
//        }
//
//        @Override
//        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
////            System.out.println(version);
////            System.out.println(access);
////            System.out.println(name);
////            System.out.println(signature);
////            System.out.println(superName);
////            System.out.println(Arrays.toString(interfaces));
//            cv.visit(version, access, name, signature, superName, null);
//        }
//
//
//        @Override
//        public MethodVisitor visitMethod(int access, String name,
//                                         String desc, String signature,
//                                         String[] exceptions) {
////            System.out.println(new PlaceholderParser("""
////                    method:?
////                    access:?
////                    desc:?
////                    signature:?
////                    exceptions:?*
////                    """,name,access,desc,signature,exceptions).ifNullFillWith("").ifEmptyFillWith(""));
//            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
//            if ("<init>".equals(name)) {
//                mv = new MainMethodVisitor(mv);
//            }
//            return mv;
//        }
//
//
//    }
//
//    private static class MainMethodVisitor extends MethodVisitor {
//
//        public MainMethodVisitor(MethodVisitor methodVisitor) {
//            super(Opcodes.ASM9, methodVisitor);
//        }
//
//        private void sop(String msg) {
//            mv.visitFieldInsn(Opcodes.GETSTATIC,
//                    Type.getInternalName(System.class), //"java/lang/System"
//                    "out",
//                    Type.getDescriptor(PrintStream.class) //"Ljava/io/PrintStream;"
//            );
//            mv.visitLdcInsn(msg);
//            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
//                    Type.getInternalName(PrintStream.class),
//                    "println",
//                    "(Ljava/lang/String;)V",
//                    false);
//        }
//
//        @Override
//        public void visitCode() {
//            mv.visitCode();
//            System.out.println("method start to insert code");
//            sop("asm insert before");
//        }
//
//        @Override
//        public void visitInsn(int opcode) {
//            if (opcode == Opcodes.RETURN) {
//                System.out.println("method end to insert code");
//                sop("asm insert after");
//            }
//            mv.visitInsn(opcode);
//        }
//    }
//
//
//    private static class MainMethodAdapter extends AdviceAdapter {
//
//        /**
//         * Constructs a new {@link AdviceAdapter}.
//         *
//         * @param mv     the method visitor to which this adapter delegates calls.
//         * @param access the method's access flags (see {@link Opcodes}).
//         * @param name   the method's name.
//         * @param desc   the method's descriptor (see {@link Type Type}).
//         */
//        protected MainMethodAdapter(MethodVisitor mv, int access, String name, String desc) {
//            super(Opcodes.ASM9, mv, access, name, desc);
//        }
//
//        @Override
//        protected void onMethodEnter() {
//            super.onMethodEnter();
//            sop("AdviceAdater: asm insert code");
//        }
//
//        @Override
//        protected void onMethodExit(int opcode) {
//            super.onMethodExit(opcode);
//            sop("AdviceAdater: asm insert code");
//        }
//
//        private void sop(String msg) {
//            mv.visitFieldInsn(Opcodes.GETSTATIC,
//                    Type.getInternalName(System.class), //"java/lang/System"
//                    "out",
//                    Type.getDescriptor(PrintStream.class) //"Ljava/io/PrintStream;"
//            );
//            mv.visitLdcInsn(msg);
//            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
//                    Type.getInternalName(PrintStream.class),
//                    "println",
//                    "(Ljava/lang/String;)V",
//                    false);
//        }
//    }
}
