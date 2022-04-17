import java.sql.Timestamp;
import java.util.*;

public class SQLConstructor {

    public static void main(String[] args) {
        Map<String, Object> map = new HashMap<>();

        map.put("startTime", "1648650282195");
        map.put("DIC_TYPE", "AB,CD,EF");
        map.put("ITEM_ID", "ASD");
        map.put("ORG_ID", "DSA");
        map.put("COMPANY_ID", "AA");
        map.put("DIC_CODE", "BB");
        ArrayList<Object> list = new ArrayList<>();

        System.out.println(getCondition(map, list));
        System.out.println(list);

    }

    /**
     * 构造查询SQL语句
     *
     * @param searchParams 1
     * @param values       1
     * @return 返回SQL
     */
    private static String getCondition(Map<String, Object> searchParams, List<Object> values) {

        StringBuilder sb = new StringBuilder();

        for (String s : searchParams.keySet()) {
            if ("startTime".equals(s)) {
                if (String.valueOf(searchParams.get("startTime")).contains(":")) {
                    throw new RuntimeException("请使用时间进行查询！");
                } else {
                    sb.append("UPDATE_TIME >= ? AND ");
                    values.add(parseDateFromLongStr(searchParams.get(s).toString()));
                }
            } else if ("DIC_TYPE".equals(s)) {
                sb.append(" DIC_TYPE IN (?,?,?) AND ");
                String[] split = String.valueOf(searchParams.get(s)).split(",");
                if (split.length == 3) {
                    values.addAll(Arrays.asList(split));
                } else if (split.length < 3) {
                    // TODO 处理非3的情况
                    ArrayList<String> list = new ArrayList<>(Arrays.asList(split));
                    for (int i = 0; i < 3 - split.length; i++) {
                        list.add("");
                    }
                    values.addAll(list);
                }
            } else if ("ITEM_ID".equals(s)) {
                sb.append("ITEM_ID =? AND ");
                values.add(searchParams.get(s));
            } else if ("ORG_ID".equals(s) || "COMPANY_ID".equals(s)) {
                sb.append(s).append(" =? AND ");
                values.add(searchParams.get(s));
            }

        }
        if (!searchParams.containsKey("DIC_CODE")) {
            sb.append(" DIC_CODE IN ('JOB_DUTY','GROUP_JOB_DUTY','USER_POST') AND ");
        }
        return sb.toString();

    }

    public static Date parseDateFromLongStr(String dateStr) {
        Date date;
        try {
            long timestamp = Long.parseLong(dateStr);
            date = new Timestamp(timestamp);
        } catch (Exception e) {
            throw new RuntimeException(
                    "请检查日期格式(需使用时间戳格式的时间)");
        }
        return date;
    }
}