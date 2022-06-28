import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.annotation.JSONType;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.freedy.expression.stander.CMDParameter;
import com.freedy.expression.utils.StringUtils;
import io.netty.handler.codec.http.HttpMethod;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author Freedy
 * @date 2022/6/28 12:29
 */
@Getter
public class RequestObject {
    @Setter
    @CMDParameter(value = "-protocol", helpText = "request protocol,it may http or https")
    private String protocol;
    @Setter
    @CMDParameter(value = "-h", helpText = "request host or ip")
    private String ip;
    @Setter
    @CMDParameter(value = "-p", helpText = "request port")
    private int port;
    @Setter
    @CMDParameter(value = "-uri", helpText = "request uri")
    private String uri;
    private HttpMethod method;
    private final Map<String, List<String>> headers = new HashMap<>();
    @JSONField(serializeUsing = ContentSerializer.class)
    private byte[] content;
    private String contentType;
    private long length;

    @CMDParameter(value = "-url", helpText = "request url")
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

    @CMDParameter(value = "-json", helpText = "json content")
    public void setJsonContent(String content) {
        JSON.parse(content);
        setContent(content.getBytes(StandardCharsets.UTF_8));
        contentType = "application/json";
    }

    @CMDParameter(value = "-text", helpText = "text content")
    public void setTextContent(String content) {
        setContent(content.getBytes(StandardCharsets.UTF_8));
        contentType = "text/plain";
    }

    @CMDParameter(value = "-xml", helpText = "xml content")
    public void setXmlContent(String content) {
        setContent(content.getBytes(StandardCharsets.UTF_8));
        contentType = "application/xml";
    }

    @CMDParameter(value = "-header", helpText = "xml content")
    public void addHeader(String name, String value) {
        headers.put(name, List.of(value));
    }

    @CMDParameter(value = "-multiHeader", helpText = "xml content")
    public void addHeader(String name, List<String> value) {
        headers.put(name, value);
    }


    private void setContent(byte[] content) {
        this.content = content;
        this.length = content.length;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this, SerializerFeature.PrettyFormat);
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
