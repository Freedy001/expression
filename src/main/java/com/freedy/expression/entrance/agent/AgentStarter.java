package com.freedy.expression.entrance.agent;

import com.freedy.expression.utils.PlaceholderParser;
import com.freedy.expression.utils.ReflectionUtils;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class AgentStarter {
    @Getter
    private static final TreeMap<String, byte[]> classBytes = new TreeMap<>();
    @Getter
    private static Instrumentation inst;
    private static AgentIsolateClassLoader loader;

    public static void agentmain(String agentArg, Instrumentation inst) {
        premain(agentArg, inst);
    }

    @SneakyThrows
    public static void premain(String agentArg, Instrumentation inst) {
        if (agentArg != null && agentArg.equals("uninstall")) {
            if (loader != null) {
                AgentStarter.inst = null;
                loader.close();
                loader = null;
                System.gc();
                System.out.println("uninstall success!");
                return;
            }
        }
        if (AgentStarter.inst != null) {
            System.out.println("has start agent already!");
            return;
        }
        loader = new AgentIsolateClassLoader();
        Class<?> exprClazz = loader.loadClass("com.freedy.expression.entrance.cmd.TerminalExpr");
        Class<?> ctxClazz = loader.loadClass("com.freedy.expression.standard.StandardEvaluationContext");
        Object o = ReflectionUtils.invokeMethod("<init>", ctxClazz, (Object) null);
        Object expr = ReflectionUtils.invokeMethod("<init>", exprClazz, (Object) null, o);
        if (agentArg != null) {
            ReflectionUtils.invokeMethod("eval", expr, "asService# " + agentArg);
        } else if (Files.exists(Path.of("./encrypt.txt"))) {
            ReflectionUtils.invokeMethod("eval", expr, "asServiceByFile# #null");
        } else {
            String key = new PlaceholderParser("asService#  -p ? -aes ? -auth ?", randomPort(), random16Str(), random16Str()).toString();
            try {
                ReflectionUtils.invokeMethod("eval", expr, key);
            } catch (Exception ignore) {
                key = new PlaceholderParser("asService#  -p ? -aes ? -auth ?", randomPort(), random16Str(), random16Str()).toString();
                ReflectionUtils.invokeMethod("eval", expr, key);
            }
            System.out.println(new PlaceholderParser("use random config(?)", key));
        }
        Set<ClassLoader> loaders = Collections.newSetFromMap(new WeakHashMap<>());
        ReflectionUtils.setter(ReflectionUtils.getter(expr, "context"), "loaderSet", loaders);
        AgentStarter.inst = inst;
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                if (loader != null) {
                    loaders.add(loader);
                }
                classBytes.put(className.replace("/", "."), classfileBuffer);
                return null;
            }
        }, true);
    }

    //33-126
    private static String random16Str() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            builder.append((char) (Math.random() * 93 + 33));
        }
        return builder.toString();
    }


    private static int randomPort() {
        Process[] start = {null};
        FutureTask<Integer> task = new FutureTask<>(() -> {
            int port = -1;
            try {
                ProcessBuilder pb = new ProcessBuilder("netstat", "-ano").redirectErrorStream(true);
                start[0] = pb.start();
                Set<Integer> portSet = Arrays.stream(new String(start[0].getInputStream().readAllBytes())
                                .split("\n"))
                        .flatMap(s -> Arrays.stream(s.split(" ")))
                        .map(String::strip)
                        .filter(s -> s.contains(":") && s.substring(s.indexOf(":") + 1).matches("\\d+"))
                        .map(s -> Integer.parseInt(s.substring(s.indexOf(":") + 1)))
                        .collect(Collectors.toSet());
                start[0].destroy();
                //noinspection StatementWithEmptyBody
                while (portSet.contains(port = (int) (Math.random() * 65535))) ;
            } catch (IOException ignore) {
            }
            return port;
        });
        new Thread(task).start();
        try {
            Integer p = task.get(5, TimeUnit.SECONDS);
            if (p != -1) return p;
        } catch (InterruptedException | ExecutionException | TimeoutException ignore) {
            start[0].destroy();
        }
        throw new RuntimeException("can not acquire port,please retry!");
    }
}
