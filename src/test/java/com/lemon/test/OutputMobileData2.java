package com.lemon.test;

import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Created with IntelliJ IDEA.
 * User: sunbo
 * Date: 13-4-22
 * Time: 下午2:04
 * To change this template use File | Settings | File Templates.
 */
public class OutputMobileData2 {
    private static String url = "jdbc:mysql://211.144.137.66:3306/lemon2";
    private static String user = "lemon";
    private static String password = "lemon001)(";
    private static String fileName = "d:\\output_2.txt";

    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        Connection conn = DriverManager.getConnection(url, user, password);
        PreparedStatement pstmt = conn.prepareStatement("" +
                "select l.mobile,l.subtime from (select mobile,count(1) from lez_service_log " +
                "where subtime between '2013-06-01 00:00:00' and now() " +
                "group by mobile having count(1)>=3 ) b,lez_service_log l where l.mobile=b.mobile and l.subtime between '2013-05-01 00:00:00' and '2013-05-20 00:00:00'");
        ResultSet rs = pstmt.executeQuery();
        FileWriter fw = new FileWriter(new File(fileName));
        while (rs.next()) {
            fw.write(rs.getString(1) + "\t" + rs.getString(2) + "\r\n");
        }
        fw.close();
        rs.close();
        pstmt.close();
        conn.close();
    }
}
