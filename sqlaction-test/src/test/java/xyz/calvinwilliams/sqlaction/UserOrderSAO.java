package xyz.calvinwilliams.sqlaction;

import java.math.*;
import java.util.*;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserOrderSAO {

	int				id ; // 编号
	int				userId ; // 用户编号
	String			itemName ; // 商品名称
	int				amount ; // 数量
	BigDecimal		totalPrice ;

	// SELECT * FROM user_order
	public static int SqlAction_SELECT_ALL_FROM_user_order( Connection conn, List<UserOrderSAO> userOrderListForSelectOutput, UserOrderSAO userOrderForWhereInput ) throws Exception {
		Statement stmt = conn.createStatement() ;
		ResultSet rs = stmt.executeQuery("SELECT * FROM user_order") ;
		while( rs.next() ) {
			UserOrderSAO userOrder = new UserOrderSAO() ;
			userOrder.id = rs.getInt( 1 ) ;
			userOrder.userId = rs.getInt( 2 ) ;
			userOrder.itemName = rs.getString( 3 ) ;
			userOrder.amount = rs.getInt( 4 ) ;
			userOrder.totalPrice = rs.getBigDecimal( 5 ) ;
			userOrderListForSelectOutput.add(userOrder) ;
		}
		return userOrderListForSelectOutput.size();
	}

	// SELECT * FROM user_order WHERE user_id=?
	public static int SqlAction_SELECT_ALL_FROM_user_order_WHERE_user_id_E__( Connection conn, List<UserOrderSAO> userOrderListForSelectOutput, UserOrderSAO userOrderForWhereInput ) throws Exception {
		PreparedStatement prestmt = conn.prepareStatement("SELECT * FROM user_order WHERE user_id=?") ;
		prestmt.setInt( 1, userOrderForWhereInput.userId );
		ResultSet rs = prestmt.executeQuery() ;
		while( rs.next() ) {
			UserOrderSAO userOrder = new UserOrderSAO() ;
			userOrder.id = rs.getInt( 1 ) ;
			userOrder.userId = rs.getInt( 2 ) ;
			userOrder.itemName = rs.getString( 3 ) ;
			userOrder.amount = rs.getInt( 4 ) ;
			userOrder.totalPrice = rs.getBigDecimal( 5 ) ;
			userOrderListForSelectOutput.add(userOrder) ;
		}
		return userOrderListForSelectOutput.size();
	}

	// SELECT user.name,user.address,user_order.item_name,user_order.amount,user_order.total_price FROM user,user_order WHERE user.name=? AND user.id=user_order.user_id
	public static int SqlAction_SELECT_user_O_name_J_user_O_address_J_user_order_O_item_name_J_user_order_O_amount_J_user_order_O_total_price_FROM_user_J_user_order_WHERE_user_O_name_E___AND_user_O_id_E_user_order_O_user_id( Connection conn, List<UserSAO> userListForSelectOutput, List<UserOrderSAO> userOrderListForSelectOutput, UserSAO userForWhereInput, UserOrderSAO userOrderForWhereInput ) throws Exception {
		PreparedStatement prestmt = conn.prepareStatement("SELECT user.name,user.address,user_order.item_name,user_order.amount,user_order.total_price FROM user,user_order WHERE user.name=? AND user.id=user_order.user_id") ;
		prestmt.setString( 1, userForWhereInput.name );
		ResultSet rs = prestmt.executeQuery() ;
		while( rs.next() ) {
			UserSAO user = new UserSAO() ;
			UserOrderSAO userOrder = new UserOrderSAO() ;
			user.name = rs.getString( 1 ) ;
			user.address = rs.getString( 2 ) ;
			userOrder.itemName = rs.getString( 3 ) ;
			userOrder.amount = rs.getInt( 4 ) ;
			userOrder.totalPrice = rs.getBigDecimal( 5 ) ;
			userListForSelectOutput.add(user) ;
			userOrderListForSelectOutput.add(userOrder) ;
		}
		return userListForSelectOutput.size();
	}

	// SELECT u.name,u.address,o.item_name,o.amount,o.total_price FROM user u,user_order o WHERE u.name=? AND u.id=o.user_id
	public static int SqlAction_SELECT_u_O_name_J_u_O_address_J_o_O_item_name_J_o_O_amount_J_o_O_total_price_FROM_user_u_J_user_order_o_WHERE_u_O_name_E___AND_u_O_id_E_o_O_user_id( Connection conn, List<UserSAO> userListForSelectOutput, List<UserOrderSAO> userOrderListForSelectOutput, UserSAO userForWhereInput, UserOrderSAO userOrderForWhereInput ) throws Exception {
		PreparedStatement prestmt = conn.prepareStatement("SELECT u.name,u.address,o.item_name,o.amount,o.total_price FROM user u,user_order o WHERE u.name=? AND u.id=o.user_id") ;
		prestmt.setString( 1, userForWhereInput.name );
		ResultSet rs = prestmt.executeQuery() ;
		while( rs.next() ) {
			UserSAO user = new UserSAO() ;
			UserOrderSAO userOrder = new UserOrderSAO() ;
			user.name = rs.getString( 1 ) ;
			user.address = rs.getString( 2 ) ;
			userOrder.itemName = rs.getString( 3 ) ;
			userOrder.amount = rs.getInt( 4 ) ;
			userOrder.totalPrice = rs.getBigDecimal( 5 ) ;
			userListForSelectOutput.add(user) ;
			userOrderListForSelectOutput.add(userOrder) ;
		}
		return userListForSelectOutput.size();
	}

	// INSERT INTO order
	public static int SqlAction_INSERT_INTO_user_order( Connection conn, UserOrderSAO userOrder ) throws Exception {
		PreparedStatement prestmt = conn.prepareStatement("INSERT INTO user_order (user_id,item_name,amount,total_price) VALUES (?,?,?,?)") ;
		prestmt.setInt( 1, userOrder.userId );
		prestmt.setString( 2, userOrder.itemName );
		prestmt.setInt( 3, userOrder.amount );
		prestmt.setBigDecimal( 4, userOrder.totalPrice );
		return prestmt.executeUpdate() ;
	}

	// UPDATE order SET total_price=? WHERE user_id=?
	public static int SqlAction_UPDATE_user_order_SET_total_price_E___WHERE_user_id_E__( Connection conn, UserOrderSAO userOrderForSetInput, UserOrderSAO userOrderForWhereInput ) throws Exception {
		PreparedStatement prestmt = conn.prepareStatement("UPDATE user_order SET total_price=? WHERE user_id=?") ;
		prestmt.setBigDecimal( 1, userOrderForSetInput.totalPrice );
		prestmt.setInt( 2, userOrderForWhereInput.userId );
		return prestmt.executeUpdate() ;
	}

	// DELETE FROM order
	public static int SqlAction_DELETE_FROM_user_order( Connection conn ) throws Exception {
		PreparedStatement prestmt = conn.prepareStatement("DELETE FROM user_order") ;
		return prestmt.executeUpdate() ;
	}
}