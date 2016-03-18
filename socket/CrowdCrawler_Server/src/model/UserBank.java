package model;

import java.sql.Connection;

public class UserBank {
	
	public static boolean login(String username, String pwd) {
		try {
			Connection conn = DBConnector.createConnection();
			System.out.println(conn);
			conn.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return true;
	}

	public static boolean register(String username, String pwd) {
		return true;
	}

}
