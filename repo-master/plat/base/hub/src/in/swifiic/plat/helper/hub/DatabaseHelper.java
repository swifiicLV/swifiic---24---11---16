package in.swifiic.plat.helper.hub;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Level ;
import java.util.logging.Logger;
import java.util.logging.FileHandler ;
import java.util.logging.ConsoleHandler ;

public class DatabaseHelper {
	
	/*
	 * Opens a connection to MySQL Database and returns the 
	 * connection.
	 */
	 
	 //private static final Logger LOGGER = Logger.getLogger(DatabaseHelper.class.getName()) ;
	 private static LogBase logger = new LogBase() ; 
	
	public static Connection connectToDB() {
		Connection connection = null;
		String filePath = " Not Set ";
		String driverClass = " Not Set ";
		String dbUrl = " Not Set ";
		String userName = " Not Set ";
		String password = " Not Set ";
		try {
			Properties dbProperties=new Properties();
			String base = System.getenv("SWIFIIC_HUB_BASE");
			if(null != base) {
				filePath = base + "/properties/";
			} else {
				System.err.println("SWIFIIC_HUB_BASE not set");
			}
			FileInputStream fis = new FileInputStream(filePath + "dbConnection.properties");
			dbProperties.load(fis);
			driverClass=dbProperties.getProperty("dbDriver");
			dbUrl=dbProperties.getProperty("dbUrl");
			userName=dbProperties.getProperty("dbUserName");
			password=dbProperties.getProperty("dbPassword");
			// Registering JDBC Driver
			Class.forName(driverClass);
			// Opens a connection to the database
			connection = DriverManager.getConnection(dbUrl, userName, password);
			return connection;
		} catch (ClassNotFoundException e) {
			//System.err.println("Class not found " + e.getMessage());
			//LOGGER.log(Level.SEVERE,"Class not found " + e.getMessage() + "\n") ;
			logger.logBase("Class not found " + e.getMessage() + "\n") ;
			e.printStackTrace();
		} catch (SQLException e) {
			//System.err.println("SQL exception " + e.getMessage());
			//LOGGER.log(Level.SEVERE,"SQL exception " +e.getMessage() + "\n") ;
			logger.logBase("SQL exception " +e.getMessage() + "\n") ;
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			//System.err.println("File not found " + e.getMessage());
			//LOGGER.log(Level.SEVERE,"File not found" + e.getMessage() + "\n") ;
			logger.logBase("File not found" + e.getMessage() + "\n") ;
			e.printStackTrace();
		} catch (IOException e) {
			//System.err.println("IO exception " + e.getMessage());
			//LOGGER.log(Level.SEVERE,"IO exception " + e.getMessage() + "\n") ;
			logger.logBase("IO exception " + e.getMessage() + "\n") ;
			e.printStackTrace();
		}
		if(null == connection) {
			//System.err.println("Error - values of connection : filePath driverClass dbUrl userName password  are " +
			//			filePath + ", " + driverClass + ", " +  dbUrl + ", " +  userName + ", " +  password);
			//LOGGER.log(Level.SEVERE,"Error - values of connection : filePath driverClass dbUrl userName password  are " +
			//			filePath + ", " + driverClass + ", " +  dbUrl + ", " +  userName + ", " +  password  + "\n") ;
			
		       logger.logBase("Error - values of connection : filePath driverClass dbUrl userName password  are " +
						filePath + ", " + driverClass + ", " +  dbUrl + ", " +  userName + ", " +  password  + "\n") ;
		}
		return connection;
	}
	
	/*
	 * Closes the database connection
	 */
	public static boolean closeDB(Connection link) {
		try {
			link.close();
			return true;
		} catch (SQLException e) {
			return false;
		}
	}
}
