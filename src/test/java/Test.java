import lombok.SneakyThrows;

/**
 * @author Freedy
 * @date 2022/7/7 2:37
 */
public class Test {

    @SneakyThrows
    public static void main(String[] args) {
//        LogRecorder recorder = new LogRecorder(System.out);
//        ASMifier.main(new String[]{"-nodebug", "ASMTest"});
//        new StandardUtils().clip(recorder.getLog());
//        ClassLoader loader = new ClassLoader() {
//            @Override
//            protected Class<?> findClass(String name) throws ClassNotFoundException {
//                if (name.equals("ASMTest")) {
//                    byte[] dump = dump();
//                    return defineClass(name, dump, 0, dump.length);
//                }
//                return super.findClass(name);
//            }
//        };
//        Class<?> aClass = loader.loadClass("ASMTest");
//        aClass.getConstructor().newInstance();
//        ReflectionUtils.invokeMethod("main", aClass,null, (Object) new String[]{});
//        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
//            try {
//                Thread.sleep(2000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//            System.out.println("stage a");
//            return "stage a return";
//        }).thenCompose(a -> {
//            System.out.println(a);
//            try {
//                Thread.sleep(2000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//            System.out.println("stage b");
//            return CompletableFuture.supplyAsync(()-> "fin");
//        });
//        CompletableFuture.allOf().thenApply()
//        System.out.println("fin->"+future.get());
//        System.out.println("fin");

    }


//    public static byte[] dump() {

//        ClassWriter classWriter = new ClassWriter(0);
//        FieldVisitor fieldVisitor;
//        RecordComponentVisitor recordComponentVisitor;
//        MethodVisitor methodVisitor;
//        AnnotationVisitor annotationVisitor0;
//
//        classWriter.visit(V17, ACC_PUBLIC | ACC_SUPER, "ASMTest", null, "java/lang/Object", null);
//
//        {
//            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
//            methodVisitor.visitCode();
//            methodVisitor.visitVarInsn(ALOAD, 0);
//            methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
//            methodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
//            methodVisitor.visitLdcInsn("123321312312");
//            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
//            methodVisitor.visitInsn(RETURN);
//            methodVisitor.visitMaxs(2, 1);
//            methodVisitor.visitEnd();
//        }
//        {
//            methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
//            methodVisitor.visitCode();
//            methodVisitor.visitInsn(ICONST_0);
//            methodVisitor.visitVarInsn(ISTORE, 1);
//            methodVisitor.visitInsn(ICONST_1);
//            methodVisitor.visitVarInsn(ISTORE, 2);
//            methodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
//            methodVisitor.visitVarInsn(ILOAD, 1);
//            methodVisitor.visitIincInsn(1, 1);
//            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(I)V", false);
//            methodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
//            methodVisitor.visitIincInsn(2, 1);
//            methodVisitor.visitVarInsn(ILOAD, 2);
//            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(I)V", false);
//            methodVisitor.visitInsn(RETURN);
//            methodVisitor.visitMaxs(2, 3);
//            methodVisitor.visitEnd();
//        }
//        classWriter.visitEnd();
//
//        return classWriter.toByteArray();
//    }


}
