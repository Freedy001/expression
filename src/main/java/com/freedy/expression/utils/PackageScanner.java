package com.freedy.expression.utils;

import com.freedy.expression.core.Tokenizer;
import lombok.NonNull;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author Freedy
 * @date 2022/3/6 16:04
 */
public class PackageScanner {

    public static List<Class<?>> doScan(@NonNull String packageName) {
        return doScan(new String[]{packageName}, null);
    }

    public static List<Class<?>> doScan(@NonNull String[] PackageNames, String[] exclude) {
        System.getProperties().forEach((k,v)->{

        });
        ClassLoader classLoader = Tokenizer.class.getClassLoader();
        List<Class<?>> list = new ArrayList<>();

        for (String PackageName : PackageNames) {
            try {
                URL url = classLoader.getResource(PackageName.replaceAll("\\.", "/"));
                assert url != null;
                String protocol = url.getProtocol();
                if (protocol.equals("file")) {
                    fileScan(exclude, list, PackageName, url);
                } else if (protocol.equals("jar")) {
                    jarScan(exclude, list, PackageName, classLoader);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return list;
    }

    /**
     * 普通环境扫描
     */
    private static void fileScan(String[] exclude, List<Class<?>> list, String PackageName, URL url) throws IOException, URISyntaxException {
        String[] packSplit = PackageName.split("\\.");
        String lastPackageName = packSplit[packSplit.length - 1];
        Files.walk(Paths.get(url.toURI())).forEach(pa -> {
            if (Files.isRegularFile(pa)) {
                String[] split = pa.toString().split(System.getProperty("os.name").toLowerCase().contains("win") ? "\\\\" : "/");
                int length = split.length;
                int index = length - 1;
                for (; index >= 0; index--) {
                    if (split[index].toLowerCase(Locale.ROOT).equals(lastPackageName.toLowerCase(Locale.ROOT))) {
                        break;
                    }
                }

                if (index == -1) return;
                StringJoiner joiner = new StringJoiner(".");
                for (int i = index + 1; i < length; i++) {
                    if (i == length - 1) {
                        String[] s = split[i].split("\\.");
                        if (!s[1].equals("class")) return;
                        joiner.add(s[0]);
                        break;
                    }
                    joiner.add(split[i]);
                }

                try {
                    String fullClassName = PackageName + "." + joiner;
                    if (exclude != null) {
                        for (String s : exclude) {
                            if (fullClassName.contains(s)) return;
                        }
                    }

                    Class<?> aClass = Class.forName(fullClassName);
                    list.add(aClass);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    /**
     * jar环境扫描
     */
    private static void jarScan(String[] exclude, List<Class<?>> list, String PackageName, ClassLoader loader) throws Exception {
        String pathName = PackageName.replace(".", "/");
        URL url = loader.getResource(pathName);
        assert url != null;
        JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
        JarFile jarFile = jarURLConnection.getJarFile();

        Enumeration<JarEntry> jarEntries = jarFile.entries();
        while (jarEntries.hasMoreElements()) {
            JarEntry jarEntry = jarEntries.nextElement();
            String jarName = jarEntry.getName();
            if (jarName.contains(pathName) &&
                    !jarName.equals(pathName + "/") &&
                    !jarEntry.isDirectory() &&
                    jarName.endsWith(".class")) {
                String fullClazzName = jarName.replace("/", ".").replace(".class", "");
                if (exclude != null) {
                    for (String s : exclude) {
                        if (fullClazzName.contains(s)) return;
                    }
                }
                list.add(Class.forName(fullClazzName));
            }
        }
    }
}
