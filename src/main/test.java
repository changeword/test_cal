package main;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class test {
    private static String USERNAMR = "toptest";
    private static String PASSWORD = "123456";
    private static String DRVIER = "oracle.jdbc.OracleDriver";
    private static String URL = "jdbc:oracle:thin:@127.0.0.1:1521:orcl";

    // 创建一个数据库连接
    Connection connection = null;
    // 创建预编译语句对象，一般都是用这个而不用Statement
    PreparedStatement pstm = null;
    // 创建一个结果集对象
    ResultSet rs = null;

    public static void main(String args[]) throws Exception {
        test test = new test();
        String select_sql = "select t1.week,t2.expression from top_week_timeline t1,top_taglib_week t2 where t1.weeklabel5 = t2.urid ";
        String update_sql = "update top_week_timeline t set t.forecast_money = ";
        List<Map<String,String>> list = test.queryForFormula(select_sql);
        for(Map<String,String> map:list){
            BigDecimal money = test.testWeek(map.get("expression"));
            String sql_temp = update_sql + money + " where t.week = '" + map.get("week") + "'" + " and t.fundtype = '正常赔款' ";
            int a = test.update(sql_temp);
            System.out.println(a);
        }
    }


    public BigDecimal testWeek(String formulaname) throws Exception {
        String sql = "select sum(money) money from all_data_fund_tmp where fundtype = '正常赔款' ";
        if (formulaname != null & !"".equals(formulaname)) {
            List<String> list1;
            List<String> list2 = new ArrayList<>();
            List<String> reverseList = new ArrayList<>();
            //过滤条件
            String IP4 = "[\\+\\-\\*\\/]";
            String[] ss = formulaname.split(IP4);
            if (ss.length > 0) {
                list1 = transferArrayToList(ss);
                Iterator<String> it = list1.iterator();
                while (it.hasNext()) {
                    String[] con = it.next().split("\\|");
                    List<String> list3 = transferArrayToList(con);
                    StringBuilder follow = new StringBuilder();
                    for (int i = 0; i < list3.size() / 2; i++) {
                        if (i % 2 == 0) {
                            follow = new StringBuilder();
                        }
                        follow.append(" and ");
                        follow.append(list3.get(2 * i + 1));
                        follow.append(" = ");
                        follow.append(list3.get(2 * i));
                        if (i % 2 == 1) {
                            follow.append(" group by " + list3.get(2 * i + 1) + "," + list3.get(2 * i - 1));
                            String sql_temp = sql + follow.toString();
                            String money = query(sql_temp).get("money");
                            reverseList.add(money);
                        }
                    }
                    if (con.length > 1) {
                        it.remove();
                    }
                }
                //此处存在配置Bug,当所乘的比例不是放在末尾的话,所拼接的公式顺序就会错乱
                if (list1.size() > 0) {
                    reverseList.addAll(list1);
                }
            }
            //运算符号
            Pattern pa = Pattern.compile("[\\+\\-\\*\\/]");
            Matcher m = pa.matcher(formulaname);
            while (m.find()) {
                list2.add(m.group());
            }
            //数字与运算符号组合
            StringBuilder cal = new StringBuilder();
            for (int i = 0; i < reverseList.size() - 1; i++) {
                cal.append(reverseList.get(i) + list2.get(i));
            }
            cal.append(reverseList.get(reverseList.size() - 1));
            //System.out.println(cal);

            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine se = manager.getEngineByName("js");
            try {
                BigDecimal result = new BigDecimal((Double) se.eval(cal.toString())).setScale(2, BigDecimal.ROUND_UP);
                System.out.println(formulaname+"\n"+cal+"\n"+result.toPlainString());
                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static <E> List<E> transferArrayToList(E[] array) {
        /*List<E> transferedList = new ArrayList<>();
        Arrays.stream(array).forEach(arr -> transferedList.add(arr));
        return transferedList;*/
        List<E> transferedList  = new ArrayList<E>();
        for (E i : array) {
            transferedList.add(i);
        }
        return transferedList;
    }

    private Map<String,String> query(String sql) throws Exception {
        Map<String,String> map = new HashMap<>(1);
        connection = getConnection();
        try {
            pstm = connection.prepareStatement(sql);
            rs = pstm.executeQuery();
            while (rs.next()) {
                String money = rs.getString("money");
                map.put("money",money);
                return map;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ReleaseResource();
        }
        return null;
    }

    private List<Map<String,String>> queryForFormula(String sql) throws Exception {
        List<Map<String,String>> list = new ArrayList<>();
        connection = getConnection();
        try {
            pstm = connection.prepareStatement(sql);
            rs = pstm.executeQuery();
            while (rs.next()) {
                Map<String,String> map = new HashMap<>(16);
                String week = rs.getString("week");
                String expression = rs.getString("expression");
                map.put("week",week);
                map.put("expression",expression);
                list.add(map);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ReleaseResource();
        }
        return list;
    }

    private Integer update(String sql) throws Exception{
        connection = getConnection();
        try {
            pstm = connection.prepareStatement(sql);
            return pstm.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ReleaseResource();
        }
        return null;
    }

    public Connection getConnection() throws Exception {
        try {
            Class.forName(DRVIER);
            connection = DriverManager.getConnection(URL, USERNAMR, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("class not find !", e);
        }

        return connection;
    }

    public void ReleaseResource() {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (pstm != null) {
            try {
                pstm.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
