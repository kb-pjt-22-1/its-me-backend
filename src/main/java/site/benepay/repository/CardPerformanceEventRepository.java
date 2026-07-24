package site.benepay.repository;
import java.sql.*;
public class CardPerformanceEventRepository {
    public boolean insertIfAbsent(Connection c,long paymentId,String eventType)throws SQLException{
        try(PreparedStatement ps=c.prepareStatement("INSERT INTO card_performance_events(payment_id,event_type,processed_at) VALUES(?,?,CURRENT_TIMESTAMP)")){ps.setLong(1,paymentId);ps.setString(2,eventType);ps.executeUpdate();return true;}catch(SQLException e){if(isDuplicate(e))return false;throw e;}
    }
    private boolean isDuplicate(SQLException e){String state=e.getSQLState();return "23505".equals(state)||e.getErrorCode()==1062||String.valueOf(e.getMessage()).toLowerCase().contains("unique");}
}
