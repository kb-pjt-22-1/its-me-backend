package site.benepay.repository;

import site.benepay.domain.OutboxEvent;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class OutboxRepository {
    public long insert(Connection c,long paymentId,String eventType,String payload)throws SQLException{
        String sql="INSERT INTO payment_delivery_outbox(payment_id,event_type,payload_json,delivery_status,retry_count,next_retry_at,created_at) VALUES(?,?,?,'PENDING',0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)";
        try(PreparedStatement ps=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){ps.setLong(1,paymentId);ps.setString(2,eventType);ps.setString(3,payload);ps.executeUpdate();try(ResultSet rs=ps.getGeneratedKeys()){if(!rs.next())throw new SQLException("Outbox ID not generated");return rs.getLong(1);}}
    }
    public OutboxEvent findByPaymentEvent(Connection c,long paymentId,String eventType)throws SQLException{
        String sql="SELECT outbox_id,payment_id,event_type,payload_json,delivery_status,retry_count,next_retry_at FROM payment_delivery_outbox WHERE payment_id=? AND event_type=?";
        try(PreparedStatement ps=c.prepareStatement(sql)){ps.setLong(1,paymentId);ps.setString(2,eventType);try(ResultSet rs=ps.executeQuery()){return rs.next()?map(rs):null;}}
    }
    public List<OutboxEvent> findRetryable(Connection c,int limit)throws SQLException{
        String sql="SELECT outbox_id,payment_id,event_type,payload_json,delivery_status,retry_count,next_retry_at FROM payment_delivery_outbox WHERE delivery_status IN('PENDING','FAILED') AND next_retry_at<=CURRENT_TIMESTAMP ORDER BY outbox_id LIMIT ?";
        List<OutboxEvent> list=new ArrayList<OutboxEvent>();try(PreparedStatement ps=c.prepareStatement(sql)){ps.setInt(1,limit);try(ResultSet rs=ps.executeQuery()){while(rs.next())list.add(map(rs));}}return list;
    }
    public void markSent(Connection c,long id)throws SQLException{try(PreparedStatement ps=c.prepareStatement("UPDATE payment_delivery_outbox SET delivery_status='SENT',delivered_at=CURRENT_TIMESTAMP,last_error=NULL WHERE outbox_id=?")){ps.setLong(1,id);ps.executeUpdate();}}
    public void markFailed(Connection c,long id,int retry,String error)throws SQLException{
        int delay=Math.min(300,(int)Math.pow(2,Math.min(retry,8)));
        try(PreparedStatement ps=c.prepareStatement("UPDATE payment_delivery_outbox SET delivery_status='FAILED',retry_count=?,next_retry_at=?,last_error=? WHERE outbox_id=?")){ps.setInt(1,retry);ps.setTimestamp(2,Timestamp.valueOf(LocalDateTime.now().plusSeconds(delay)));ps.setString(3,error==null?"delivery failed":error.substring(0,Math.min(error.length(),500)));ps.setLong(4,id);ps.executeUpdate();}
    }
    private OutboxEvent map(ResultSet rs)throws SQLException{OutboxEvent x=new OutboxEvent();x.setOutboxId(rs.getLong("outbox_id"));x.setPaymentId(rs.getLong("payment_id"));x.setEventType(rs.getString("event_type"));x.setPayloadJson(rs.getString("payload_json"));x.setDeliveryStatus(rs.getString("delivery_status"));x.setRetryCount(rs.getInt("retry_count"));Timestamp t=rs.getTimestamp("next_retry_at");x.setNextRetryAt(t==null?null:t.toLocalDateTime());return x;}
}
