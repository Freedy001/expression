package com.freedy.expression.standard.standardFunc;


import com.freedy.expression.SysConstant;
import com.freedy.expression.entrance.cmd.TerminalExpr;
import com.freedy.expression.entrance.cmd.TerminalHandler;
import com.freedy.expression.entrance.cmd.TerminalStarter;
import com.freedy.expression.jline.ClientJlineTerminal;
import com.freedy.expression.jline.LocalJlineTerminal;
import com.freedy.expression.jline.SerializableCandidate;
import com.freedy.expression.jline.SuggestionMetadata;
import com.freedy.expression.log.LogRecorder;
import com.freedy.expression.standard.HttpObject;
import com.freedy.expression.standard.*;
import com.freedy.expression.standard.codec.AuthenticAndDecrypt;
import com.freedy.expression.standard.codec.AuthenticAndEncrypt;
import com.freedy.expression.utils.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.codec.string.StringDecoder;
import lombok.*;
import org.jline.reader.Candidate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static com.freedy.expression.SysConstant.CHARSET;

public class StandardNet extends AbstractStandardFunc {

    @ExpressionFunc(value = "connect to a remote server use a default config")
    public String connect0() throws Exception {
        return connect(SysConstant.DEFAULT_CONNECT_CONFIG);
    }

    @ExpressionFunc(value = "start a netty server use a config file")
    public String connectByFile(String fileName) throws Exception {
        return connect(getConnectConfig(fileName));
    }

    @ExpressionFunc(value = "connect to a remote server and run script on remote server", enableCMDParameter = true)
    private String connect(ConnectConfig config) throws Exception {
        supplyLackConfig(config);
        byte[] bytes = EncryptUtil.stringToMD5(config.getMd5Key()).getBytes(StandardCharsets.UTF_8);
        boolean[] shutDown = new boolean[]{true};
        NioEventLoopGroup group = new NioEventLoopGroup();
        Channel channel = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel channel) {
                        channel.pipeline().addLast(
                                new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4),
                                new LengthFieldPrepender(4),
                                new AuthenticAndEncrypt(config.aesKey, bytes),
                                new AuthenticAndDecrypt(config.aesKey, bytes),
                                new ObjectEncoder(),
                                new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.cacheDisabled(TerminalStarter.class.getClassLoader())),
                                new SimpleChannelInboundHandler<String>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String s) {
                                        synchronized (channel) {
                                            System.out.print(s);
                                            channel.notifyAll();
                                        }
                                    }

                                    @Override
                                    public void channelInactive(ChannelHandlerContext ctx) {
                                        synchronized (channel) {
                                            System.out.println(Color.dYellow("disconnect from server --> " + channel.remoteAddress()));
                                            shutDown[0] = false;
                                            channel.notifyAll();
                                        }
                                    }

                                    @Override
                                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                        if (cause.getMessage().equals("Connection reset")) {
                                            channelInactive(ctx);
                                        }
                                        System.out.println(new PlaceholderParser("EXCEPTION ?", cause.getMessage()).configPlaceholderHighLight(PlaceholderParser.PlaceholderHighLight.HIGH_LIGHT_RED));
                                    }
                                });
                    }
                }).connect(config.ip, Integer.parseInt(config.port)).sync().channel();
        TerminalHandler handler = new TerminalHandler(new ClientJlineTerminal(channel));
        synchronized (channel) {
            try {
                handler.collectScript(completeScript -> {
                    channel.writeAndFlush(completeScript);
                    try {
                        channel.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return shutDown[0];
                }, line -> {
                    if (line.endsWith(".fun")) {
                        try {
                            @Cleanup FileInputStream stream = new FileInputStream(line);
                            channel.writeAndFlush(new String(stream.readAllBytes(), CHARSET));
                            channel.wait();
                        } catch (Exception e) {
                            System.out.println(Color.dRed(e.getMessage()));
                        }
                        return false;
                    }
                    return true;
                }, "fun@" + config.ip + ":" + config.port + "> ");
            } catch (Exception ignore) {
            }
        }
        group.shutdownGracefully();
        return "client shut down!";
    }


    @ExpressionFunc("start a netty server and act as a remote script runner")
    public String asServiceByFile(String fileName) throws Exception {
        return asService(getConnectConfig(fileName));
    }

    @ExpressionFunc(value = "start a netty server and act as a remote script runner", enableCMDParameter = true)
    public String asService(ConnectConfig config) {
        supplyLackConfig(config);
        return doAsService(Integer.parseInt(config.port), config.aesKey, config.md5Key, ctx -> System.out.println(new PlaceholderParser("one client? connect", ctx.channel().remoteAddress()).configPlaceholderHighLight(PlaceholderParser.PlaceholderHighLight.HIGH_LIGHT_BLUE)));
    }

    @ExpressionFunc("shutdown a netty server")
    public String shutDownServer() {
        if (pc != null) pc.close();
        if (boss != null) boss.shutdownGracefully();
        if (worker != null) worker.shutdownGracefully();
        return "ok!";
    }


    private Channel pc;
    private NioEventLoopGroup boss;
    private NioEventLoopGroup worker;

    @SneakyThrows
    private String doAsService(int port, String aesKey, String md5AuthStr, Consumer<ChannelHandlerContext> interceptor) {
        TerminalExpr expr = new TerminalExpr(context);
        LocalJlineTerminal terminal = new LocalJlineTerminal(context);

        LogRecorder LOG_RECORDER = new LogRecorder();
        byte[] auth = EncryptUtil.stringToMD5(md5AuthStr).getBytes(StandardCharsets.UTF_8);
        pc = new ServerBootstrap().option(ChannelOption.SO_BACKLOG, 10240)
                .channel(NioServerSocketChannel.class)
                .group(boss = new NioEventLoopGroup(1), worker = new NioEventLoopGroup())
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel channel) {
                        channel.pipeline().addLast(
                                new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4),
                                new LengthFieldPrepender(4),
                                new AuthenticAndEncrypt(aesKey, auth),
                                new AuthenticAndDecrypt(aesKey, auth),
                                new ObjectEncoder(),
                                new ObjectDecoder(ClassResolvers.cacheDisabled(TerminalStarter.class.getClassLoader())),
                                new SimpleChannelInboundHandler<String>() {

                                    @Override
                                    public void channelActive(ChannelHandlerContext ctx) {
                                        interceptor.accept(ctx);
                                    }

                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, String s) {
                                        String cmd = s.trim().toLowerCase(Locale.ROOT);
                                        if (cmd.equals("::shutdown::")) {
                                            ctx.channel().close();
                                            pc.close();
                                            return;
                                        }
                                        //清空日志缓存
                                        LOG_RECORDER.getLog();
                                        expr.eval(s, Color.dPink("server start success on port"));
                                        ctx.channel().writeAndFlush(LOG_RECORDER.getLog());
                                    }

                                    @Override
                                    public void channelInactive(ChannelHandlerContext ctx) {
                                        System.out.println(new PlaceholderParser("client[?] disconnected!", ctx.channel().remoteAddress()));
                                    }

                                    @Override
                                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                        cause.printStackTrace();
                                        ctx.channel().writeAndFlush(cause.getMessage());
                                    }
                                }, new SimpleChannelInboundHandler<SuggestionMetadata>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, SuggestionMetadata msg) {
                                        if (msg == null) return;
                                        ArrayList<Candidate> list = new ArrayList<>();
                                        terminal.suggest(null, msg, list);
                                        ctx.channel().writeAndFlush(list.stream().map(ca -> ReflectionUtils.copyReference(ca, SerializableCandidate.class)).toList());
                                    }
                                }
                        );
                    }
                }).bind(port).sync().channel();
        return Color.dPink("server start success on port " + port);
    }


    @ExpressionFunc(value = "resolve the given parameters as concrete objects", enableCMDParameter = true)
    public HttpReqParam parseRequest(HttpReqParam obj) {
        return obj;
    }


    @ExpressionFunc(value = "send a http request by HttpReqParam", enableCMDParameter = true)
    public HttpObject http(HttpReqParam obj) throws InterruptedException {
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
                                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
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


    private void supplyLackConfig(ConnectConfig config) {
        if (StringUtils.isEmpty(config.ip) || StringUtils.isEmpty(config.port)) {
            String line = terminalHandler.stdin("address(ip:port):");
            config.ip = line.split(":")[0];
            config.port = line.split(":")[1];
        }
        if (StringUtils.isEmpty(config.aesKey)) {
            while (true) {
                config.aesKey = terminalHandler.stdin("aes-key:");
                if (config.aesKey.length() != 16) {
                    System.out.println(Color.dPink("the length of aes-key must be 16!"));
                } else {
                    break;
                }
            }
        }
        if (StringUtils.isEmpty(config.md5Key)) {
            config.md5Key = terminalHandler.stdin("auth-key:");
        }
    }


    private ConnectConfig getConnectConfig(String fileName) throws IOException {
        ConnectConfig connectConfig;
        File file = new File(StringUtils.hasText(fileName) ? fileName : "./encrypt.txt");
        @Cleanup FileInputStream is = new FileInputStream(file);
        String strip = new String(is.readAllBytes()).trim().strip();
        List<ConnectConfig> list = Arrays.stream(strip.split("\n")).map(ConnectConfig::new).toList();

        if (list.size() == 1) {
            connectConfig = list.get(0);
        } else {
            for (int i = 0; i < list.size(); i++) {
                ConnectConfig config = list.get(i);
                System.out.println(i + ": " + config.getTagName() + "(" + config.getIp() + ":" + config.getPort() + ")");
            }
            String configNum = null;
            do {
                if (configNum != null) System.out.println(Color.dPink("illegal input!"));
                configNum = terminalHandler.stdin(Color.dPink("please select one config:"));
            } while (!configNum.matches("\\d+"));
            connectConfig = list.get(Integer.parseInt(configNum));
        }
        return connectConfig;
    }

    private Channel lc;
    private NioEventLoopGroup lb;
    private NioEventLoopGroup lw;

    @ExpressionFunc(value = "listener a port and echo msg", enableCMDParameter = true)
    public String listen(int port) throws InterruptedException {
        lc = new ServerBootstrap().option(ChannelOption.SO_BACKLOG, 10240)
                .channel(NioServerSocketChannel.class)
                .group(lb = new NioEventLoopGroup(1), lw = new NioEventLoopGroup())
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel channel) {
                        channel.pipeline().addLast(new StringDecoder()).addLast(
                                new SimpleChannelInboundHandler<String>() {

                                    @Override
                                    public void channelActive(ChannelHandlerContext ctx) {
                                        System.out.println(new PlaceholderParser("client[?] establish connection!", ctx.channel().remoteAddress()));
                                    }

                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, String s) {
                                        System.out.println(Color.dRed("receive msg below -> "));
                                        System.out.println(Color.dYellow(s));
                                    }

                                    @Override
                                    public void channelInactive(ChannelHandlerContext ctx) {
                                        System.out.println(new PlaceholderParser("client[?] disconnected!", ctx.channel().remoteAddress()));
                                    }

                                    @Override
                                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                        cause.printStackTrace();
                                        ctx.channel().writeAndFlush(cause.getMessage());
                                    }
                                }
                        );
                    }
                }).bind(port).sync().channel();
        return "bind success on " + port;
    }

    @ExpressionFunc(value = "stop listener server", enableCMDParameter = true)
    public String stopListen() {
        if (lc != null) lc.close();
        if (lb != null) lb.shutdownGracefully();
        if (lw != null) lw.shutdownGracefully();
        return "ok!";
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectConfig {
        @CMDParameter("-t")
        String tagName;
        @CMDParameter("-ip")
        String ip;
        @CMDParameter("-p")
        String port;
        @CMDParameter("-aes")
        String aesKey;
        @CMDParameter("-auth")
        String md5Key;


        public ConnectConfig(String str) {
            String[] item = str.split(";");
            if (item.length != 4) throw new java.lang.IllegalArgumentException("illegal pattern str");
            this.tagName = item[0];
            String[] ipInfo = item[1].split(":");
            if (ipInfo.length != 2) throw new java.lang.IllegalArgumentException("illegal pattern str");
            this.ip = ipInfo[0];
            this.port = ipInfo[1];
            this.aesKey = item[2];
            this.md5Key = item[3];
        }

        @Override
        public String toString() {
            return new PlaceholderParser("-t ? -ip ? -p ? -aes ? -auth ?", tagName, ip, port, aesKey, md5Key).toString();
        }
    }

}
