package com.freedy.expression.stander;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.freedy.expression.utils.Color;
import com.freedy.expression.utils.PlaceholderParser;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import lombok.Data;

import java.util.Optional;

/**
 * @author Freedy
 * @date 2022/6/30 1:28
 */
@Data
public final class HttpResult implements HttpObject {
    private HttpReqParam req;
    private int respCode;
    private HttpHeaders respHeaders;
    private Object respContent;


    public HttpResult(HttpReqParam req, int respCode, HttpHeaders respHeaders, String respContent) {
        this.req = req;
        this.respCode = respCode;
        this.respHeaders = respHeaders;
        if (respHeaders != null && Optional.ofNullable(respHeaders.get(HttpHeaderNames.CONTENT_TYPE)).orElse("").contains(HttpHeaderValues.APPLICATION_JSON)) {
            this.respContent = JSON.parse(respContent);
        } else {
            this.respContent = respContent;
        }
    }

    public String toString() {
        return new PlaceholderParser("""
                ?->
                status:?
                ?*
                                
                ?
                """,
                super.toString(),
                Color.dGreen(respCode),
                respHeaders.entries().stream().map(e -> e.getKey() + ": " + e.getValue()).toList(),
                Color.dPink(respContent instanceof JSON ? JSON.toJSONString(respContent, SerializerFeature.PrettyFormat) : respContent)
        ).serialParamsSplit("\n").ifEmptyFillWith("").toString();
    }
}
