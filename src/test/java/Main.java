import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.freedy.expression.stander.CMDParameter;
import com.freedy.expression.utils.ReflectionUtils;
import com.freedy.expression.utils.StringUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import lombok.SneakyThrows;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;

/**
 * @author Freedy
 * @date 2022/4/3 9:39
 */
public class Main {


    public static void main(String[] args) throws Throwable {

//        http();
//        HttpReqParam object = parseRequestArgs(StringUtils.splitWithoutBracket("""
//                -m post -url http://127.0.0.1/test -header token abcsasdasdasdas auth zxczxczxczxcz -json '{"articleId": "1388865816372539452","username": "TEST","email": "985948228@qq.com","content": "哈哈,你好啊"}'
//                """, '[', ']', ' '), HttpReqParam.class);
//        System.out.println(object);
//        http(object);
    }




}
