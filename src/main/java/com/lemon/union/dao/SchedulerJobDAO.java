package com.lemon.union.dao;

import com.lemon.union.job.A1;
import com.lemon.union.job.A2;
import com.lemon.union.job.Lez_webowner_channel_day;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: sunbo
 * Date: 13-3-4
 * Time: 下午6:46
 * To change this template use File | Settings | File Templates.
 */
@Repository
public class SchedulerJobDAO {


    @Autowired
    JdbcTemplate j;

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public void doScheduler(String subdate) {
        String sql = "select servicecode, wid, pid, channel, sum(totalincome) as totalincome, sum(feeincome) as feeincome, count(*) as feecount, count(distinct mobile) as feeusers from lez_service_log where subtime between ? and ? group by pid, wid, channel, servicecode";
        List<Lez_webowner_channel_day> list1 = j.query(sql, new Object[]{subdate + " 00:00:00", subdate + " 23:59:59"}, new BeanPropertyRowMapper<Lez_webowner_channel_day>(Lez_webowner_channel_day.class));
        Map<String, Lez_webowner_channel_day> list = new HashMap<String, Lez_webowner_channel_day>();
        for (Lez_webowner_channel_day bill : list1) {
            bill.setSubdate(subdate);
            bill.setSubtime(sdf.format(new Date()));
            list.put(bill.getPid() + "|-|" + bill.getServicecode() + "|-|" + bill.getWid() + "|-|" + bill.getChannel(), bill);
        }
        String sql2 = "select servicecode, wid, pid, channel, sum(feeincome) as showincome, count(*) as showcount from lez_service_log where feeflag = 1 and subtime between ? and ? group by pid, wid, channel, servicecode";

        List<Object[]> data2 = j.query(sql2, new Object[]{subdate + " 00:00:00", subdate + " 23:59:59"}, new RowMapper<Object[]>() {
            @Override
            public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
                Object[] o = new Object[6];
                o[0] = rs.getString(1);
                o[1] = rs.getString(2);
                o[2] = rs.getString(3);
                o[3] = rs.getString(4);
                o[4] = rs.getString(5);
                o[5] = rs.getString(6);
                return o;  //To change body of implemented methods use File | Settings | File Templates.
            }
        });

        for (Object[] d : data2) {
            String servicecode = (String) d[0];
            int wid = new Integer((String) d[1]);
            int pid = new Integer((String) d[2]);
            String channel = (String) d[3];
            float showincome = new Float((String) d[4]);
            int showcount = new Integer((String) d[5]);
            Lez_webowner_channel_day bill = (Lez_webowner_channel_day) list.get(pid + "|-|" + servicecode + "|-|" + wid + "|-|" + channel);

            bill.setShowcount(showcount);
            bill.setShowincome(showincome);
            list.put(pid + "|-|" + servicecode + "|-|" + wid + "|-|" + channel, bill);
        }

        String sql3 = "delete from lez_bill_day where subdate = ?";
        j.update(sql3, new Object[]{subdate});

        for (String key : list.keySet()) {
            final Lez_webowner_channel_day bill = (Lez_webowner_channel_day) list.get(key);
            final String sql4 = "insert lez_bill_day(subdate, servicecode, wid, pid, channel, " +
                    "feecount, feeusers, feeincome," +
                    " showcount, showincome, totalincome, subtime) values(?,?,?,?,?,?,?,?,?,?,?,?)";
            j.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection con) throws SQLException {

                    PreparedStatement pstmt = con.prepareStatement(sql4);
                    pstmt.setString(1, bill.getSubdate());
                    pstmt.setString(2, bill.getServicecode());
                    pstmt.setInt(3, Integer.valueOf(bill.getWid()));
                    pstmt.setInt(4, Integer.valueOf(bill.getPid()));
                    pstmt.setString(5, bill.getChannel());
                    pstmt.setInt(6, Integer.valueOf(bill.getFeecount()));
                    pstmt.setInt(7, Integer.valueOf(bill.getFeeusers()));
                    pstmt.setFloat(8, Float.valueOf(bill.getFeeincome()));
                    pstmt.setInt(9, Integer.valueOf(bill.getShowcount()));
                    pstmt.setFloat(10, Float.valueOf(bill.getShowincome()));
                    pstmt.setFloat(11, Float.valueOf(bill.getTotalincome()));
                    pstmt.setString(12, bill.getSubtime());
                    return pstmt;
                }
            });

        }

        analyzeChannel(subdate);
        analyzeWebowner(subdate);
    }


    public void analyzeChannel(final String subdate) {
        try {
            String sql1 = "delete from lez_webowner_channel_day where subdate = ?";
            j.update(sql1, new Object[]{subdate});
            final String sql2 = "select subdate, wid, pid, channel, sum(feecount) as feecount, sum(feeusers) as feeusers, " +
                    "sum(feeincome) as feeincome, sum(showcount) as showcount, sum(showincome) as showincome, sum(totalincome) as totalincome, now() as subtime from lez_bill_day where subdate = ? group by subdate, wid, pid, channel";

            List<A1> list = j.query(sql2, new Object[]{subdate}, new BeanPropertyRowMapper<A1>(A1.class));
            for (A1 a : list) {
                String s2 = "insert lez_webowner_channel_day (subdate, wid, pid, channel, feecount, feeusers, feeincome, showcount, showincome, totalincome, subtime) values(" +
                        "?,?,?,?,?,?,?,?,?,?,?)";
                j.update(s2, new Object[]{a.getSubdate(), a.getWid(), a.getPid(), a.getChannel(), a.getFeecount(), a.getFeeusers(),
                        a.getFeeincome(), a.getShowcount(), a.getShowincome(), a.getTotalincome(), a.getNow()});
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }



    public void analyzeWebowner(String subdate) {
        try {
            String sql1 = "delete from lez_webowner_bill where billdate = ? and payflag = 0";
            j.update(sql1, new Object[]{subdate});

            String sql2 = " select subdate, wid, pid, sum(feecount) as feecount, sum(feeincome) as feeincome, sum(showcount) as showcount, sum(showincome) as showincome, sum(totalincome) as totalincome, now() as now" +
                    " from lez_bill_day where subdate = ? group by subdate, wid, pid";
            List<A2> list = j.query(sql2, new Object[]{subdate}, new BeanPropertyRowMapper<A2>(A2.class));
            for (A2 a : list) {
                String s2 = "REPLACE lez_webowner_bill(billdate, wid, pid, feecount, feeincome, showcount, showincome, totalincome, subtime) values " +
                        "(?,?,?,?,?,?,?,?,?)";
                j.update(s2, new Object[]{a.getSubdate(), a.getWid(), a.getPid(), a.getFeecount(), a.getFeeincome(), a.getShowcount(), a.getShowincome(), a.getTotalincome(), a.getNow()});
            }
            String sql3 = "update lez_webowner_bill set payflag = 1, paytime = now() where wid in (1000, 1001)";

            j.update(sql3);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
