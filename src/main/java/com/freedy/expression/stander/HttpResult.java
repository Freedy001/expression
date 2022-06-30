package com.freedy.expression.stander;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.freedy.expression.utils.Color;
import com.freedy.expression.utils.PlaceholderParser;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author Freedy
 * @date 2022/6/30 1:28
 */
@Data
@AllArgsConstructor
public class HttpResult implements HttpObject {
    private HttpReqParam req;
    private int respCode;
    private HttpHeaders respHeaders;
    private String respContent;

    public String toString() {
        return new PlaceholderParser("""
                ?->
                status:?
                ?*
                
                ?
                """,
                super.toString(),
                Color.dGreen(respCode),
                respHeaders.entries().stream().map(e -> e.getKey() +": "+ e.getValue()).toList(),
                Color.dPink(respHeaders.get(HttpHeaderNames.CONTENT_TYPE).contains(HttpHeaderValues.APPLICATION_JSON) ?
                        JSON.toJSONString(JSON.parse(respContent), SerializerFeature.PrettyFormat) : respContent)
        ).serialParamsSplit("\n").ifEmptyFillWith("").toString();
    }
}
