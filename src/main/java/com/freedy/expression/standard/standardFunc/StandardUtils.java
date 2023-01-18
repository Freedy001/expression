package com.freedy.expression.standard.standardFunc;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.freedy.expression.SysConstant;
import com.freedy.expression.entrance.agent.AgentStarter;
import com.freedy.expression.exception.CombineException;
import com.freedy.expression.exception.EvaluateException;
import com.freedy.expression.standard.AttachInfo;
import com.freedy.expression.standard.CodeDeCompiler;
import com.freedy.expression.standard.ExpressionFunc;
import com.freedy.expression.utils.Color;
import com.freedy.expression.utils.*;
import com.sun.tools.attach.*;
import lombok.Cleanup;
import lombok.SneakyThrows;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.*;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;

import static com.freedy.expression.SysConstant.SEPARATOR;

/**
 * @author Freedy
 * @date 2022/3/6 15:38
 */
public class StandardUtils extends AbstractStandardFunc {

    @ExpressionFunc("list function")
    public void lf(Object o) {
        getMethod(getClassByArg(o)).forEach(System.out::println);
    }

    @ExpressionFunc("list variable")
    public void lv(Object o) {
        Class<?> oClass = getClassByArg(o);
        ReflectionUtils.getFieldsRecursion(oClass).stream().map(field -> {
            field.setAccessible(true);
            Type fieldType = field.getGenericType();
            String fieldTypeName = "?";
            if (fieldType instanceof ParameterizedType parameterizedType) {
                String genericStr = getParameterizedType(parameterizedType);
                fieldTypeName = ((Class<?>) parameterizedType.getRawType()).getSimpleName() + genericStr;
            } else if (fieldType instanceof Class<?> clazz) {
                fieldTypeName = clazz.getSimpleName();
            }
            try {
                return (Modifier.isStatic(field.getModifiers()) ? "\033[92mstatic \033[0;39m" : "") + "\033[91m" + fieldTypeName + "\033[0:39m \033[93m" + field.getName() + "\033[0;39m \033[34m" + (o instanceof Class || o instanceof String ? "null" : (Modifier.isStatic(field.getModifiers()) ? getVarString(field.get(null)) : getVarString(field.get(o)))) + "\033[0;39m";
            } catch (Exception e) {
                throw new EvaluateException("?", e);
            }
        }).forEach(System.out::println);
    }

    @ExpressionFunc("decompile class to java source from agent mode")
    public String jad(Object... o) throws Exception {
        if (o.length != 1 && o.length != 2) {
            throw new EvaluateException("parameters count must be 1 or 2");
        }
        Class<?> classByArg;
        try {
            classByArg = getClassByArg(o[0]);
        } catch (Exception e) {
            String className = (String) o[0];
            Instrumentation inst = AgentStarter.getInst();
            Class<?> aClass = Arrays.stream(inst.getAllLoadedClasses()).filter(clazz -> clazz.getName().equals(className)).findFirst().orElse(null);
            if (aClass == null) throw new ClassNotFoundException(className);
            classByArg = aClass;
        }

        if (o.length == 1) {
            return CodeDeCompiler.getCodeFromAgent(classByArg, false, "");
        }
        return CodeDeCompiler.getCodeFromAgent(classByArg, false, String.valueOf(o[1]));
    }


    @ExpressionFunc("decompile class to java source")
    public String code(Object... o) {
        if (o.length != 1 && o.length != 2) {
            throw new EvaluateException("parameters count must be 1 or 2");
        }
        if (o.length == 1) {
            return CodeDeCompiler.getCode(getClassByArg(o[0]), false, "");
        }
        return CodeDeCompiler.getCode(getClassByArg(o[0]), false, String.valueOf(o[1]));
    }


    @ExpressionFunc("decompile class to java source and dump source file to specific location")
    public void dumpCode(Object... o) {
        if (o.length != 2 && o.length != 3) {
            throw new EvaluateException("parameters count must be 2 or 3");
        }
        Class<?> clazz = getClassByArg(o[0]);
        if (o.length == 2) {
            dump(clazz, "", String.valueOf(o[1]));
            return;
        }
        dump(clazz, String.valueOf(o[1]), String.valueOf(o[2]));
    }

    @ExpressionFunc("list all inner class")
    public List<Class<?>> lic(Object o) {
        return List.of(getClassByArg(o).getDeclaredClasses());
    }

    @ExpressionFunc("detail help")
    public void help(String funcName) {
        context.getSelfFuncHelp().forEach((k, v) -> {
            if (k.toLowerCase(Locale.ROOT).contains(funcName.toLowerCase(Locale.ROOT))) {
                System.out.println("function:\033[95m");
                System.out.println("   " + k + "\033[0;39m");
                System.out.println("explain:\033[34m");
                System.out.println("   " + v.replace("\n", "\n   ") + "\033[0;39m");
                System.out.println();
                System.out.println();
            }
        });
    }

    @ExpressionFunc("clear specific var")
    public void clearVar(String... varName) {
        if (varName == null) return;
        for (String s : varName) {
            context.getVariableMap().remove(context.filterName(s));
        }
    }

    @ExpressionFunc("get length")
    public int len(Object o) {
        if (o instanceof Collection<?> c) {
            return c.size();
        }
        if (o instanceof String s) {
            return s.length();
        }
        if (o.getClass().isArray()) {
            return Array.getLength(o);
        }
        throw new EvaluateException("can not calculate length,please check the type");
    }

    @ExpressionFunc("exit progress")
    public void exit(int status) {
        System.exit(status);
    }

    private static final int U = 1024 * 1024;

    @SuppressWarnings("resource")
    @ExpressionFunc("""
            encrypt the giving path file,then output a encrypted file
            the first param indicate a input path,it could be a file or a directory
            the second param indicate a output path,it must be a directory
            the third param is a aes key used for encryption
            example enc('/path/need/to/be/encrypted','/output/path','--a 16 bit key--');
            """)
    public void enc(String in, String out, String aes) throws Throwable {
        long startTime = System.currentTimeMillis();
        Path inPath = Path.of(in);
        Path outPath = Path.of(out);
        if (!Files.exists(inPath)) throw new IllegalArgumentException("input path does not exist");
        if (!Files.exists(outPath)) throw new IllegalArgumentException("output path does not exist");
        if (Files.isRegularFile(outPath)) throw new IllegalArgumentException("output path must be a directory");
        Path outFilePath = Path.of(out + "\\" + inPath.getFileName() + ".enc");
        if (Files.exists(outFilePath)) Files.delete(outFilePath);
        @Cleanup RandomAccessFile os = new RandomAccessFile(outFilePath.toFile(), "rw");
        os.seek(U);
        List<FileInfo> fileInfos = new ArrayList<>();
        boolean isRegFile = Files.isRegularFile(inPath);

        byte[] buffer = new byte[U];
        Files.walk(inPath).forEach(path -> {
            if (!Files.isRegularFile(path)) return;
            String pName = isRegFile ? inPath.getFileName().toString() : path.subpath(inPath.getNameCount(), path.getNameCount()).toString();
            FileInfo info = new FileInfo(pName);
            long fLen = path.toFile().length() / (U);
            try {
                info.startPos = os.getFilePointer();
                @Cleanup FileInputStream is = new FileInputStream(path.toFile());
                int len;
                for (int i = 0; (len = is.read(buffer)) != -1; i++) {
                    byte[] enc = EncryptUtil.Encrypt(len == U ? buffer : Arrays.copyOfRange(buffer, 0, len), aes);
                    os.writeInt(enc.length);
                    os.write(enc);
                    System.out.print("\rencrypt " + pName + ":" + Math.floor(((double) i / fLen) * 100) + "%");
                }
                System.out.print("\rencrypt " + pName + ":100%\n\r");
                info.endPos = os.getFilePointer();
                fileInfos.add(info);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        os.seek(0);
        os.write(0xFE);
        os.write(0xBA);
        for (FileInfo info : fileInfos) {
            os.writeLong(info.startPos);
            os.writeLong(info.endPos);
            byte[] pathBytes = info.relPath.getBytes(StandardCharsets.UTF_8);
            os.writeInt(pathBytes.length);
            os.write(pathBytes);
            if (os.getFilePointer() >= U) {
                os.close();
                Files.delete(outFilePath);
                throw new IllegalArgumentException("too many item!");
            }
        }
        System.out.println(new PlaceholderParser("encrypt: ? files,total time: ? s", fileInfos.size(), (System.currentTimeMillis() - startTime) / 1000));
    }

    static class FileInfo {
        String relPath;
        long startPos;
        long endPos;

        public FileInfo(String relPath) {
            this.relPath = relPath;
        }
    }

    @ExpressionFunc("""
            decrypt the giving path file,then output a serial of decrypted files
            the first param indicate a input path,it must be a enc file
            the second param indicate a output path,it must be a directory
            the third param is a aes key used for encryption
            example dec('/path/need/to/be/decrypted','/output/path','--a 16 bit key--');
            """)
    public void dec(String in, String out, String aes) throws Throwable {
        long startTime = System.currentTimeMillis();
        Path inPath = Path.of(in);
        Path outPath = Path.of(out);
        if (!Files.exists(inPath)) throw new IllegalArgumentException("input path does not exist");
        if (!Files.exists(outPath)) throw new IllegalArgumentException("output path does not exist");
        if (Files.isRegularFile(outPath)) throw new IllegalArgumentException("output path must be a directory");
        @Cleanup RandomAccessFile rc = new RandomAccessFile(inPath.toFile(), "r");
        if (rc.readByte() != (byte) 0xFE || rc.readByte() != (byte) 0xBA)
            throw new IllegalArgumentException("illegal enc file");
        int desTotalFile = 0;
        for (; ; desTotalFile++) {
            long startPos = rc.readLong();
            long endPos = rc.readLong();
            int len = rc.readInt();
            if (startPos == 0 || endPos == 0 || len == 0) break;
            byte[] relPathByte = new byte[len];
            if (rc.read(relPathByte) != len) throw new IllegalArgumentException();
            Path filePath = Paths.get(out + "/" + new String(relPathByte));
            String[] split = filePath.toString().split(SEPARATOR);
            File subPath = new File(String.join(SEPARATOR, Arrays.copyOfRange(split, 0, split.length - 1)));
            if (!subPath.exists() && !subPath.mkdirs()) {
                throw new IllegalArgumentException("create directory failed!");
            }
            long lastPos = rc.getFilePointer();
            rc.seek(startPos);
            @Cleanup FileOutputStream os = new FileOutputStream(filePath.toFile());
            if (startPos == endPos) {
                if (!filePath.toFile().createNewFile())
                    System.out.println("create empty file failed!");
            } else {
                for (double process = 0; ; ) {
                    int segment = rc.readInt();
                    byte[] buffer = new byte[segment];
                    if (rc.read(buffer) != segment) throw new IllegalArgumentException("illegal enc file");
                    os.write(EncryptUtil.Decrypt(buffer, aes));
                    long segmentEnd = rc.getFilePointer();
                    if (segmentEnd == endPos) break;
                    if (segmentEnd > endPos) throw new IllegalArgumentException("illegal enc file");
                    process += (segment + 4);
                    System.out.print("\rdecrypt " + filePath + ":" + Math.floor((process / (endPos - startPos)) * 100) + "%");
                }
            }
            System.out.print("\rdecrypt " + filePath + ":100%\n\r");
            rc.seek(lastPos);
        }
        System.out.println(new PlaceholderParser("decrypt: ? files,total time: ? s", desTotalFile, (System.currentTimeMillis() - startTime) / 1000));
    }


    private List<String> getMethod(Class<?> aClass) {
        if (aClass == null) return null;
        List<String> list = new ArrayList<>();
        for (Method method : aClass.getDeclaredMethods()) {
            StringJoiner joiner = new StringJoiner(",", "(", ")");
            for (Class<?> type : method.getParameterTypes()) {
                joiner.add(type.getSimpleName());
            }
            list.add((Modifier.isStatic(method.getModifiers()) ? "\033[91mstatic \033[0;39m\033[94m" : "\033[94m") +
                    method.getReturnType().getSimpleName() + "\033[0;39m \033[93m" +
                    method.getName() + "\033[0;39m\033[95m" + joiner + "\033[39m");
        }
        return list;
    }

    private String getParameterizedType(ParameterizedType parameterizedType) {
        StringJoiner joiner = new StringJoiner(",", "<", ">");
        for (Type type : parameterizedType.getActualTypeArguments()) {
            if (type instanceof ParameterizedType sub) {
                joiner.add(((Class<?>) sub.getRawType()).getSimpleName() + getParameterizedType(sub));
            } else if (type instanceof Class<?> clazz) {
                joiner.add(clazz.getSimpleName());
            }
        }
        return joiner.toString();
    }

    private String getVarString(Object obj) {
        String str = String.valueOf(obj);
        if (str.contains("\n")) {
            return "[" + str.replaceAll("\n", ",") + "]";
        }
        return str;
    }

    @SneakyThrows
    private void dump(Class<?> clazz, String method, String path) {
        File file = new File(String.valueOf(path));
        if (!file.isDirectory()) {
            throw new EvaluateException("path must be directory");
        }
        file = new File(file, StringUtils.hasText(method) ? clazz.getSimpleName() + ".txt" : clazz.getSimpleName() + ".java");
        @Cleanup FileOutputStream stream = new FileOutputStream(file);
        stream.write(CodeDeCompiler.getCode(clazz, true, method).getBytes(StandardCharsets.UTF_8));
        System.out.println("dump success to " + file.getAbsolutePath());
    }

    @ExpressionFunc(value = "resolve the given parameters as concrete objects")
    public void clip(Object str) {
        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(str.toString()), null);
    }

    @ExpressionFunc(value = "format to json")
    public String ftj(Object o) {
        return JSON.toJSONString(o, SerializerFeature.PrettyFormat);
    }

    @ExpressionFunc(value = "reference string invoke")
    public Object t(String s, Object... arg) throws Throwable {
        String[] split = Arrays.stream(s.strip().split("#")).map(String::strip).toArray(String[]::new);
        if (split.length != 2) return null;
        Class<?> aClass = context.findClass(split[0]);
        Object filedRet = null;
        Object methodRet = null;
        CombineException ex = new CombineException();
        try {
            filedRet = ReflectionUtils.getter(aClass, null, split[1]);
        } catch (Exception e) {
            ex.addException(e);
        }
        try {
            methodRet = ReflectionUtils.invokeMethod(split[1], aClass, null, arg);
        } catch (Exception e) {
            ex.addException(e);
        }
        if (filedRet != null && methodRet != null) return List.of(filedRet, methodRet);
        if (filedRet != null) return filedRet;
        if (methodRet != null) return methodRet;
        throw ex;
    }

    @ExpressionFunc(value = "attach to target vm", enableCMDParameter = true)
    public String attachAgent(AttachInfo info) throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {
        String id = info.getPid();
        if (id == null) {
            System.out.println(Color.dYellow("======================================================"));
            String name = Optional.of(info.getName()).orElse("*");
            ArrayList<String> pidList = new ArrayList<>();
            int i = 1;
            for (VirtualMachineDescriptor descriptor : VirtualMachine.list()) {
                String s = descriptor.displayName();
                if (!StringUtils.fuzzyEqual(s, name)) continue;
                pidList.add(descriptor.id());
                System.out.println((i == 1 ? "*" : " ") + "[" + i++ + "]: " + descriptor.id() + "   " + (s.length() >= 75 ? s.substring(0, 75) + "..." : s));
            }
            if (pidList.size() == 0) {
                return "not found target application for name " + name + "\t\t\t\t\t\t\t\t\t\t\t\t";
            }
            String num = terminalHandler.stdin("chose a jvm to attach:");
            try {
                if (num.isEmpty()) num = "1";
                id = pidList.get(Integer.parseInt(num) - 1);
            } catch (NumberFormatException e) {
                return "Please input an integer to select pid.\t\t\t\t\t\t\t\t\t\t\t\t";
            }
        }
        String path = info.getAgentPath();
        if (path == null) {
            path = terminalHandler.stdin("input a java agent path(NonNull):");
        }
        String args = info.getAgentArg();
        if (args == null) {
            args = terminalHandler.stdin("input a java agent args(Nullable):");
        }
        VirtualMachine.attach(id).loadAgent(path, args);
        return "success";
    }

    @ExpressionFunc(value = "attach to target vm")
    public String attachSelf(String... name) throws Exception {
        AttachInfo info = new AttachInfo();
        info.setAgentPath(getLocalJarPath());
        info.setAgentArg(SysConstant.DEFAULT_KEY);
        info.setName(name.length == 0 ? "*" : name[0]);
        return attachAgent(info);
    }

    public static String getLocalJarPath() {
        URL localUrl = StandardUtils.class.getProtectionDomain().getCodeSource().getLocation();
        String path;
        path = URLDecoder.decode(localUrl.getFile().replace("+", "%2B"), StandardCharsets.UTF_8);
        File file = new File(path);
        path = file.getAbsolutePath();
        return path;
    }


}
