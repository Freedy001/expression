package com.freedy.expression.stander.standerFunc;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.freedy.expression.exception.EvaluateException;
import com.freedy.expression.stander.HttpObject;
import com.freedy.expression.stander.*;
import com.freedy.expression.utils.Color;
import com.freedy.expression.utils.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import lombok.Cleanup;
import lombok.SneakyThrows;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.freedy.expression.SysConstant.SEPARATOR;
/**
 * @author Freedy
 * @date 2022/3/6 15:38
 */
public class StanderUtils extends AbstractStanderFunc {

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

    @ExpressionFunc(value = "resolve the given parameters as concrete objects", enableCMDParameter = true)
    public HttpReqParam parseRequest(HttpReqParam obj) {
        return obj;
    }

    @ExpressionFunc(value = "send a http request by HttpReqParam", enableCMDParameter = true)
    public HttpObject http(HttpReqParam obj) throws InterruptedException, TimeoutException {
        Bootstrap bootstrap = new Bootstrap();
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        Exchanger<HttpResult> exchanger = new Exchanger<>();
        Channel channel = bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(
                                new HttpRequestEncoder(),
                                new HttpResponseDecoder(),
                                new HttpObjectAggregator(Integer.MAX_VALUE),
                                new SimpleChannelInboundHandler<FullHttpResponse>() {
                                    @Override
                                    public void channelActive(ChannelHandlerContext ctx) {
                                        System.out.println("connect->" + ctx.channel().remoteAddress());
                                    }

                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws InterruptedException {
                                        exchanger.exchange(new HttpResult(
                                                obj,
                                                msg.status().code(),
                                                msg.headers(),
                                                msg.content().toString(Charset.defaultCharset())
                                        ));
                                    }

                                    @Override
                                    public void channelInactive(ChannelHandlerContext ctx) throws InterruptedException {
                                        exchanger.exchange(null);
                                    }

                                    @Override
                                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws InterruptedException {
                                        System.out.println(Color.dRed("error->" + cause.getMessage()));
                                    }
                                }
                        );
                    }
                }).connect(obj.getIp(), obj.getPort()).sync().channel();
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                obj.getMethod(),
                obj.getUri());
        HttpHeaders headers = request.headers();
        headers.set(HttpHeaderNames.USER_AGENT, "FUN_HTTP_CLIENT(https://github.com/Freedy001/expression)");
        headers.set(HttpHeaderNames.ACCEPT, "*/*");
        headers.set(HttpHeaderNames.ACCEPT_ENCODING, "gzip, deflate, br");
        headers.set(HttpHeaderNames.CONNECTION, "close");
        headers.set(HttpHeaderNames.HOST, obj.getIp());
        if (obj.getLength() > 0) {
            headers.set(HttpHeaderNames.CONTENT_LENGTH, obj.getLength());
        }
        if (StringUtils.hasText(obj.getContentType())) {
            headers.set(HttpHeaderNames.CONTENT_TYPE, obj.getContentType());
        }
        List<Map.Entry<String, String>> reqHeaders = obj.getHeaders();
        for (Map.Entry<String, String> entry : reqHeaders) {
            headers.set(entry.getKey(), entry.getValue());
        }
        if (obj.getContent() != null) {
            request.content().writeBytes(obj.getContent());
        }
        channel.writeAndFlush(request);
        HttpResult res;
        try {
            res = exchanger.exchange(null, obj.getTimeout(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            System.out.println(Color.dRed("request timeout!"));
            return obj;
        }
        if (res == null) {
            System.out.println(Color.dRed("connect refuse!"));
            return obj;
        }
        group.shutdownGracefully();
        return res;
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

}
