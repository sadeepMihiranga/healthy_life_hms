package lk.healthylife.hms.config;

import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {

    private static DBConnection dbIsntance;
    private static Connection con ;

    final static String DB_URL=   "jdbc:oracle:thin:@healthylife_high?TNS_ADMIN=D:/Wallet_healthylife";
    // Use TNS alias when using tnsnames.ora.  Use it while connecting to the database service on cloud.
    final static String DB_USER                 = "ADMIN";
    final static String DB_PASSWORD             = "Hladm3in@oraClo23";
    final static String CONN_FACTORY_CLASS_NAME = "oracle.jdbc.pool.OracleDataSource";

    private DBConnection() {
        // private constructor //
    }

    public static DBConnection getInstance() {
        if(dbIsntance == null) {
            dbIsntance = new DBConnection();
        }
        return dbIsntance;
    }

    public Connection getConnection() {

        PoolDataSource pds = PoolDataSourceFactory.getPoolDataSource();

        try {
            // Set the connection factory first before all other properties
            pds.setConnectionFactoryClassName(CONN_FACTORY_CLASS_NAME);
            pds.setURL(DB_URL);
            pds.setUser(DB_USER);
            pds.setPassword(DB_PASSWORD);
            pds.setConnectionPoolName("JDBC_UCP_POOL");

            // Default is 0. Set the initial number of connections to be created
            // when UCP is started.
            pds.setInitialPoolSize(5);

            // Default is 0. Set the minimum number of connections
            // that is maintained by UCP at runtime.
            pds.setMinPoolSize(5);

            // Default is Integer.MAX_VALUE (2147483647). Set the maximum number of
            // connections allowed on the connection pool.
            pds.setMaxPoolSize(20);

            // Default is 30secs. Set the frequency in seconds to enforce the timeout
            // properties. Applies to inactiveConnectionTimeout(int secs),
            // AbandonedConnectionTimeout(secs)& TimeToLiveConnectionTimeout(int secs).
            // Range of valid values is 0 to Integer.MAX_VALUE. .
            pds.setTimeoutCheckInterval(5);

            // Default is 0. Set the maximum time, in seconds, that a
            // connection remains available in the connection pool.
            pds.setInactiveConnectionTimeout(10);

            // Set the JDBC connection properties after pool has been created
            Properties connProps = new Properties();
            connProps.setProperty("fixedString", "false");
            connProps.setProperty("remarksReporting", "false");
            connProps.setProperty("restrictGetTables", "false");
            connProps.setProperty("includeSynonyms", "false");
            connProps.setProperty("defaultNChar", "false");
            connProps.setProperty("AccumulateBatchResult", "false");

            // JDBC connection properties will be set on the provided
            // connection factory.
            pds.setConnectionProperties(connProps);
            System.out.println("Available connections before checkout: "
                    + pds.getAvailableConnectionsCount());
            System.out.println("Borrowed connections before checkout: "
                    + pds.getBorrowedConnectionsCount());
            // Get the database connection from UCP.

            con = pds.getConnection();

            /*if (con == null) {
                try {
                    String host = "jdbc:derby://localhost:1527/yourdatabasename";
                    String username = "yourusername";
                    String password = "yourpassword";
                    con = DriverManager.getConnection(host, username, password);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }*/

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return con;
    }
}
