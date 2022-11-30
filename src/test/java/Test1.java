import com.freedy.expression.utils.PlaceholderParser;
import com.freedy.expression.utils.StringUtils;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Freedy
 * @date 2022/7/15 14:57
 */
public class Test1 {


    @SuppressWarnings("static-access")
    public static void main(String[] args) throws Exception {
        System.out.println(URLDecoder.decode("__$%7BT(java.lang.Runtime).getRuntime().exec(%22calc%22)%7D__::.x"));
        System.out.println(URLDecoder.decode("__$%7BT(java.lang.Runtime).getRuntime().exec('open%20-a%20Calculator')%7D__::.x"));
        InetAddress ia = null;
        try {
            ia = ia.getLocalHost();
            String localname = ia.getHostName();
            String localip = ia.getHostAddress();
            System.out.println("本机名称是：" + localname);
            System.out.println("本机的ip是 ：" + localip);
        } catch (Exception e) {
            e.printStackTrace();
        }
        InetAddress ia1 = InetAddress.getLocalHost();// 获取本地IP对象
        System.out.println("本机的MAC是 ：" + getMACAddress(ia1));
    }



    // 获取MAC地址的方法
    private static String getMACAddress(InetAddress ia) throws Exception {
        // 获得网络接口对象（即网卡），并得到mac地址，mac地址存在于一个byte数组中。
        byte[] mac = NetworkInterface.getByInetAddress(ia).getHardwareAddress();
        // 下面代码是把mac地址拼装成String
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < mac.length; i++) {
            if (i != 0) {
                sb.append("-");
            }
            // mac[i] & 0xFF 是为了把byte转化为正整数
            String s = Integer.toHexString(mac[i] & 0xFF);
            // System.out.println("--------------");
            // System.err.println(s);
            sb.append(s.length() == 1 ? 0 + s : s);
        }
        // 把字符串所有小写字母改为大写成为正规的mac地址并返回
        return sb.toString().toUpperCase();
    }



    @SneakyThrows
    public static void transform(Path in, Path out, List<Path> exclude) {
        if (!Files.isDirectory(in)) return;
        //noinspection resource
        Files.walk(in).forEach(pa -> {
            try {
                if (Files.isDirectory(pa)) return;
                for (Path path : exclude) {
                    if (path.equals(pa)) return;
                    Path relativize = path.relativize(pa);
                    if (StringUtils.hasText(relativize.toString()) && !relativize.toString().startsWith(".")) {
                        return;
                    }
                }
                Path path = out.resolve(in.relativize(pa));
                Files.deleteIfExists(path);
                if (!Files.exists(path.getParent())) Files.createDirectories(path.getParent());

                if (pa.getFileName().toString().endsWith(".java")) {
                    String javaCode = transformJavaCode(Files.readString(pa));
                    Files.writeString(path, javaCode);
                    System.out.println(new PlaceholderParser("transform ? to ?", pa, path));
                } else {
                    Files.copy(pa, path);
                    System.out.println(new PlaceholderParser("copy ? to ?", pa, path));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });


    }

    public static String transformJavaCode(String code) {
        List<String> lines = Arrays.stream(code.split("\n")).collect(Collectors.toList());
        processRecord(lines);
        processCase(lines);
        processSealed(lines);
        processInstanceOfBLock(lines);
        processStringBLock(lines);
        apiChange(lines);
        return String.join("\n", lines);
    }


    static final Pattern record = Pattern.compile(".*record +([a-zA-Z_$][\\w$]*) *\\((.*)\\) *\\{");

    public static void processRecord(List<String> code) {
        for (int i = 0; i < code.size(); i++) {
            String line = code.get(i);
            Matcher matcher = record.matcher(line);
            if (!matcher.find()) continue;
            String className = matcher.group(1);
            if (StringUtils.isEmpty(className)) {
                System.err.println("convert failed on line " + line);
                continue;
            }
            code.set(i, line.replace("record", "class").replaceAll("\\(.*?\\)", ""));
            String group = matcher.group(2);
            StringBuilder filedBuilder = new StringBuilder();
            if (StringUtils.hasText(group)) {
                String[] args = group.split(",");
                List<RecordNode> filedList = new ArrayList<>();
                for (String s : args) {
                    List<String> arg = Arrays.stream(s.split(" ")).filter(StringUtils::hasText).toList();
                    filedBuilder.append("public final ").append(arg.get(0)).append(" ").append(arg.get(1)).append(";\n");
                    filedList.add(new RecordNode(arg.get(0), arg.get(1)));
                }
                filedBuilder.append(new PlaceholderParser("""
                        public ?(?){
                            ?
                        }
                        """, className, filedList.stream().map(Object::toString).collect(Collectors.joining(",")), filedList.stream().map(n -> new PlaceholderParser("this.?=?;", n.name, n.name).toString()).collect(Collectors.joining("\n"))));
            } else {
                filedBuilder.append(new PlaceholderParser("public ?(){}", className));
            }
            code.add(i + 1, filedBuilder.toString());
        }
    }

    @AllArgsConstructor
    static class RecordNode {
        String type;
        String name;

        @Override
        public String toString() {
            return type + " " + name;
        }
    }

    // case "||" -> {
    static final Pattern case_ = Pattern.compile(".*(?:case +(.*)|default) +->(.*)");


    public static void processCase(List<String> code) {
        for (int i = 0; i < code.size(); i++) {
            String line = code.get(i);
            Matcher matcher = case_.matcher(line.strip());
            if (!matcher.find()) continue;
            String group = matcher.group(1);
            String[] cases = group == null ? new String[1] : StringUtils.splitWithoutQuote(group, ',');
            if (line.contains("case \"Integer\"")) {
                System.out.println();
            }
            if (cases.length == 1) {
                code.set(i, line.replace("->", ":"));
                addBreak(code, i, line);
                continue;
            }
            StringBuilder builder = new StringBuilder();
            for (String aCase : cases) {
                builder.append("case ").append(aCase).append(" : ");
            }
            builder.append(matcher.group(2));
            code.set(i, builder.toString());
            addBreak(code, i, line);
        }
    }

    private static void addBreak(List<String> code, int i, String line) {
        if (line.contains("case")) {
            for (int j = i + 1; j < code.size(); j++) {
                line = code.get(j);
                if (line.contains("return ")) return;
                if (line.contains("case") || line.contains("default")) {
                    line = code.get(j - 1);
                    int i1 = line.lastIndexOf("}");
                    if (i1 == -1) {
                        code.set(j - 1, line + " break;");
                        return;
                    }
                    code.set(j - 1, line.substring(0, i1) + " break; " + line.substring(i1));
                    return;
                }
            }
        }
    }


    public static void processSealed(List<String> code) {
        for (int i = 0; i < code.size(); i++) {
            String line = code.get(i);
            line = line.replaceAll("sealed|non-sealed", "")
                    .replaceAll("permits.*\\{", "{");
            code.set(i, line);
        }
    }

    final static Pattern instanceOf = Pattern.compile(".*[ (]([a-zA-Z_$][\\w$]*) +instanceof +([a-zA-Z_$][\\w$<?>]*) +([a-zA-Z_$][\\w$]*).*");

    public static void processInstanceOfBLock(List<String> code) {
        for (int i = 0; i < code.size(); i++) {
            String line = code.get(i);
            Matcher matcher = instanceOf.matcher(line);
            if (!matcher.find()) continue;
            String originVar = matcher.group(1);
            String type = matcher.group(2);
            String newVar = matcher.group(3);
            int start = matcher.start(3);
            int end = matcher.end(3);
            code.set(i, line.substring(0, start) + line.substring(end));
            code.add(i + 1, new PlaceholderParser("? ? = (?) ?;", type, newVar, type, originVar).toString());
        }
    }


    final static String block = "\"\"\"";

    public static void processStringBLock(List<String> code) {
        StringBuilder builder = new StringBuilder();
        int blockLine = -1;
        for (int i = 0; i < code.size(); i++) {
            String line = code.get(i);
            if (builder.isEmpty() && !line.contains(block)) continue;
            int i1 = line.indexOf(block);
            if (i1 != -1) {
                int end = line.indexOf(block, i1 + 3);
                if (builder.isEmpty() && end != -1) {
                    code.set(i, line.replace(block, "\"\""));
                    continue;
                }
                if (builder.isEmpty()) {
                    builder.append("\"").append(line.substring(i1 + 3).trim().replace("\"", "\\\"")).append("\\n").append("\"").append("+");
                    blockLine = i;
                } else {
                    builder.append("\"").append(line.substring(0, i1).trim().replace("\"", "\\\"")).append("\"");
                    String s = code.get(blockLine);
                    int i2 = s.indexOf(block);
                    code.set(blockLine, s.substring(0, i2) + builder + line.substring(i1 + 3));
                    code.remove(i);
                    i--;
                    builder = new StringBuilder();
                }
            } else {
                builder.append("\"").append(line.trim().replace("\"", "\\\"")).append("\\n").append("\"").append("+");
                code.remove(i);
                i--;
            }
        }
    }

    final static Pattern api = Pattern.compile("Set *\\. *of\\(\\)");

    public static void apiChange(List<String> code) {
        for (int i = 0; i < code.size(); i++) {
            String line = code.get(i);
            code.set(i, line.replace("strip", "trim"));
            if (line.matches(".*Set *\\. *of\\(.*")) {

            }
        }
    }

    //Set.of(); ---> Collections.unmodifiableSet(new HashSet<>(Arrays.asList("")));

}
