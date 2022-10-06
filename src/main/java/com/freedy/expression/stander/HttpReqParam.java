package com.freedy.expression.stander;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.freedy.expression.utils.Color;
import com.freedy.expression.utils.PlaceholderParser;
import com.freedy.expression.utils.StringUtils;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author Freedy
 * @date 2022/6/28 12:29
 */
@Getter
public final class HttpReqParam implements HttpObject {
    @Setter
    @CMDParameter(value = "-protocol", helpText = "request protocol,it may http or https")
    private String protocol;
    @Setter
    @CMDParameter(value = "-i", helpText = "request host or ip")
    private String ip;
    @Setter
    @CMDParameter(value = "-p", helpText = "request port")
    private int port;
    @Setter
    @CMDParameter(value = "-uri", helpText = "request uri")
    private String uri="/";
    private HttpMethod method=HttpMethod.GET;
    private final List<Map.Entry<String, String>> headers = new ArrayList<>();
    @JSONField(serializeUsing = ContentSerializer.class)
    private byte[] content=new byte[0];
    private String contentType="";
    private long length;
    @SuppressWarnings("FieldMayBeFinal")
    @CMDParameter(value = "-t", helpText = "timeout,time unit is second")
    private int timeout = 10;


    @CMDParameter(value = "-u", helpText = "request url")
    public void setUrl(String url) {
        try {
            String urlWithoutProtocol;
            if (url.startsWith("http://") || url.startsWith("https://")) {
                int i = url.indexOf("://");
                protocol = url.substring(0, i);
                urlWithoutProtocol = url.substring(i + 3);
            } else {
                protocol = "http";
                urlWithoutProtocol = url;
            }

            String[] split = urlWithoutProtocol.split("/", 2);
            if (split.length == 1 || StringUtils.isEmpty(split[1])) {
                uri = "/";
            } else {
                uri = "/" + split[1];
            }

            split = split[0].split(":");
            if (split.length == 2) {
                port = Integer.parseInt(split[1]);
            } else {
                port = protocol.equalsIgnoreCase("http") ? 80 : 443;
            }
            ip = split[0];
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("illegal url pattern:" + url);
        }
    }

    @CMDParameter(value = "-m", helpText = "request method")
    public void setMethod(String method) {
        try {
            this.method = (HttpMethod) HttpMethod.class.getDeclaredField(method.toUpperCase(Locale.ROOT)).get(null);
        } catch (Exception e) {
            throw new IllegalArgumentException("illegal request method:" + method);
        }
    }

    @CMDParameter(value = "-j", helpText = "json content")
    public void setJsonContent(String content) {
        JSON.parse(content);
        setContent(content.getBytes(StandardCharsets.UTF_8));
        contentType = "application/json";
    }

    @CMDParameter(value = "-tc", helpText = "text content")
    public void setTextContent(String content) {
        setContent(content.getBytes(StandardCharsets.UTF_8));
        contentType = "text/plain";
    }

    @CMDParameter(value = "-x", helpText = "xml content")
    public void setXmlContent(String content) {
        setContent(content.getBytes(StandardCharsets.UTF_8));
        contentType = "application/xml";
    }

    @CMDParameter(value = "-h", helpText = "set header")
    public void addHeader(String name, String value) {
        headers.add(new Entry(name, value));
    }

    @CMDParameter(value = "-mh", helpText = "set multi header")
    public void addHeader(String name, String[] value) {
        for (String val : value) {
            headers.add(new Entry(name, val));
        }
    }


    private void setContent(byte[] content) {
        this.content = content;
        this.length = content.length;
    }

    @Override
    public String toString() {
        return new PlaceholderParser("""
                ?->
                ?? ? HTTP/1.1?
                ?*
                
                ?
                """,
                super.toString(),
                Color.WHITE,
                method,
                uri,
                Color.END,
                headers.stream().map(e -> e.getKey() + ": " + e.getValue()).toList(),
                Color.dPink(contentType.contains(HttpHeaderValues.APPLICATION_JSON) ?
                        JSON.toJSONString(JSON.parse(new String(content)), SerializerFeature.PrettyFormat) : new String(content))
        ).ifEmptyFillWith("").serialParamsSplit("\n").toString();
    }


    @AllArgsConstructor
    private static class Entry implements Map.Entry<String, String> {
        String key;
        String value;

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public String setValue(String value) {
            String val = this.value;
            this.value = value;
            return val;
        }
    }

    static {
        SerializeConfig.getGlobalInstance().put(HttpMethod.class, (serializer, object, fieldName, fieldType, features) -> {
            serializer.write(((HttpMethod) object).name());
        });
    }

    public static class ContentSerializer implements ObjectSerializer {
        @Override
        public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
            serializer.write(new String((byte[]) object));
        }
    }
}
