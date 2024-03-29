package com.freedy.expression.jline;

import com.freedy.expression.standard.StandardEvaluationContext;
import com.freedy.expression.utils.ReflectionUtils;
import com.freedy.expression.utils.StringUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Freedy
 * @date 2022/8/29 1:09
 */
public class LocalJlineTerminal extends JlineTerminal {
    protected final static Pattern staticPattern = Pattern.compile("^T *?\\((.*?)\\)");
    @Getter
    @Setter
    protected StandardEvaluationContext context;

    public LocalJlineTerminal(StandardEvaluationContext context) {
        this.context = context;
    }

    @Override
    @SneakyThrows
    public void suggest(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        String lineStr = line.word();
        if (StringUtils.isEmpty(lineStr)) {
            candidates.addAll(getDefaultTipSerializableCandidate("", ""));
        } else {
            String subLine = lineStr.substring(0, line.wordCursor());
            String suffix = lineStr.substring(line.wordCursor());
            int[] str = findEvaluateStr(subLine);
            if (str == null) {
                Matcher matcher = staticPattern.matcher(subLine);
                if (!matcher.find() || subLine.indexOf(".", matcher.end(1)) == -1) return;
                String resultStr = subLine.substring(0, subLine.lastIndexOf("."));
                String[] varArr = resultStr.split("\\.");
                String className = matcher.group(1).strip();
                injectTips(candidates, suffix, resultStr, varArr, context.findClass(className), true);
                return;
            }
            if (str.length == 1) {
                String varPrefix = subLine.substring(str[0], str[0] + 1);
                Set<Candidate> set = getDefaultTipSerializableCandidate(subLine.substring(0, str[0]), varPrefix.matches("[#$@]") ? varPrefix : "");
                candidates.addAll(set);
            } else {
                String resultStr = subLine.substring(str[0], str[1]);
                String baseStr = lineStr.substring(0, str[0]) + resultStr;
                String[] varArr = resultStr.split("\\.");
                Object variable = context.getVariable(varArr[0]);
                if (variable == null) return;
                injectTips(candidates, suffix, baseStr, varArr, variable.getClass(), false);
            }
        }
    }


    protected void injectTips(List<Candidate> candidates, String suffix, String baseStr, String[] varArr, Class<?> varType, boolean staticType) {
        int len = varArr.length;
        for (int i = 1; i < len; i++) {
            Field field = ReflectionUtils.getFieldRecursion(varType, varArr[i]);
            if (field == null) return;
            varType = field.getType();
        }
        doInjectTips(candidates, varType, baseStr, suffix, staticType);
    }

    protected void doInjectTips(List<Candidate> candidates, Class<?> varType, String baseStr, String suffix, boolean staticType) {
        candidates.addAll((staticType ? ReflectionUtils.getStaticFieldsRecursion(varType) : ReflectionUtils.getFieldsRecursion(varType)).stream().map(field -> {
            String tip = baseStr + "." + field.getName() + suffix;
            return new Candidate(tip, tip, "variable", null, null, null, false, 0);
        }).collect(Collectors.toSet()));
        candidates.addAll((staticType ? ReflectionUtils.getStaticMethodsRecursion(varType) : ReflectionUtils.getMethodsRecursion(varType)).stream().map(method -> {
            int count = method.getParameterCount();
            String tip = baseStr + "." + method.getName() + "(" + ",".repeat(count <= 1 ? 0 : count - 1) + ")" + suffix;
            return new Candidate(tip, tip, "function", null, null, null, true, 1);
        }).collect(Collectors.toSet()));
    }


    protected Set<Candidate> getDefaultTipSerializableCandidate(String base, String varPrefix) {
        Set<Candidate> set = context.getVariableMap().keySet().stream().map(var -> new Candidate(base + varPrefix + var, base + varPrefix + var, "_variable", null, null, null, false, 0)).collect(Collectors.toSet());
        context.getFunMap().forEach((k, v) -> {
            int params = v.getClass().getDeclaredMethods()[0].getParameterCount();
            String funStr = base + k + "(" + ",".repeat(params <= 1 ? 0 : params - 1) + ")";
            set.add(new Candidate(funStr, funStr, "function", null, null, null, true, 1));
        });
        for (Field field : ReflectionUtils.getFieldsRecursion(context.getRoot().getClass())) {
            set.add(new Candidate(field.getName(), field.getName(), "root", null, null, null, false, 0));
        }
        set.addAll(List.of(
                new Candidate("for()", "for", "keyword", null, null, null, false, 0),
                new Candidate("if()", "if", "keyword", null, null, null, false, 0),
                new Candidate("def", "def", "keyword", null, null, null, false, 0),
                new Candidate("T()", "T()", "keyword", null, null, null, false, 0),
                new Candidate("[]", "[]", "keyword", null, null, null, false, 0),
                new Candidate("{}", "{}", "keyword", null, null, null, false, 0),
                new Candidate("@block{", "@block{", "keyword", null, null, null, false, 0),
                new Candidate("continue;", "continue", "keyword", null, null, null, false, 0),
                new Candidate("break;", "break", "keyword", null, null, null, false, 0),
                new Candidate("return;", "return", "keyword", null, null, null, false, 0)
        ));
        return set;
    }

    protected int[] findEvaluateStr(String line) {
        char[] chars = line.toCharArray();
        int len = chars.length;
        int lastDot = -1;
        int i = len - 1;
        for (; i >= 0; i--) {
            if (chars[i] == ')') {
                return null;
            }
            if (chars[i] == '(' || chars[i] == '=' || chars[i] == ',') {
                break;
            }
            if (chars[i] == '.' && lastDot == -1) {
                lastDot = i;
            }
        }
        return lastDot == -1 ? new int[]{i + 1} : new int[]{i + 1, lastDot};
    }


}
