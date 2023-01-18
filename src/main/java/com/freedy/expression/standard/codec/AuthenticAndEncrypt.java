package com.freedy.expression.standard.codec;


import com.freedy.expression.utils.EncryptUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 报文样式: <br/>
 * <pre>
 *      +--------------------------------------------+
 *      |      4 byte    |     32 byte    |  n byte  |
 *      |----------------|----------------|----------|
 *      | message length | authentication |   data   |
 *      +--------------------------------------------+
 * </pre>
 * message length: 报文长度  <br/>
 * authentication: 认证信息 通过对AES KEY进行3次MD5加密得出。 <br/>
 * cmd: 指令消息
 * message length: 真正数据 需要被转发的报文数据,并使用AES对称加密 <br/>
 *
 * @author Freedy
 * @date 2021/11/10 16:26
 */
public class AuthenticAndEncrypt extends MessageToByteEncoder<ByteBuf> {

    private final String aesKey;
    private final byte[] authenticationToken;

    public AuthenticAndEncrypt(String aesKey, byte[] authenticationToken) {
        this.aesKey = aesKey;
        this.authenticationToken = authenticationToken;
    }


    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) {
        //空数据不写回
        int readableBytes = msg.readableBytes();
        if (readableBytes == 0) return;
        // authentication
        out.writeBytes(authenticationToken);
        byte[] bytes = new byte[readableBytes];
        msg.readBytes(bytes);
        // data
        out.writeBytes(EncryptUtil.Encrypt(bytes, aesKey));
    }

}
