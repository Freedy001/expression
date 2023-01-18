package com.freedy.expression.entrance.agent;

import com.freedy.expression.entrance.cmd.TerminalExpr;
import com.freedy.expression.standard.StandardEvaluationContext;
import com.freedy.expression.utils.PlaceholderParser;
import lombok.Getter;

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

    public static void agentmain(String agentArg, Instrumentation inst) {
        premain(agentArg, inst);
    }

    public static void premain(String agentArg, Instrumentation inst) {
        TerminalExpr expr = new TerminalExpr(new StandardEvaluationContext());
        if (agentArg != null) {
            expr.eval("asService# " + agentArg);
        } else if (Files.exists(Path.of("./encrypt.txt"))) {
            expr.eval("asServiceByFile# #null");
        } else {
            String key = new PlaceholderParser("asService#  -p ? -aes ? -auth ?", randomPort(), random16Str(), random16Str()).toString();
            try {
                expr.eval(key);
            } catch (Exception ignore) {
                key = new PlaceholderParser("asService#  -p ? -aes ? -auth ?", randomPort(), random16Str(), random16Str()).toString();
                expr.eval(key);
            }
            System.out.println(new PlaceholderParser("use random config(?)", key));
        }
        Set<ClassLoader> loaders = Collections.newSetFromMap(new WeakHashMap<>());
        expr.getContext().setLoaderSet(loaders);
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
