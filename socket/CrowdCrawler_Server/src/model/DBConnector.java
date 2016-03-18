package model;

import java.sql.Connection;
import java.sql.DriverManager;

import basic.Config;

public class DBConnector {
	public static Connection createConnection (){
		try{
			Class.forName("com.mysql.jdbc.Driver");
			System.out.println(Config.getString("DB_URL"));
			Connection conn=DriverManager.getConnection(
					Config.getString("DB_URL"),
					Config.getString("DB_USER"),
					Config.getString("DB_PWD"));
			return conn;
		}catch (Exception ex){
			ex.printStackTrace();
			return null;
		}
	}
}
