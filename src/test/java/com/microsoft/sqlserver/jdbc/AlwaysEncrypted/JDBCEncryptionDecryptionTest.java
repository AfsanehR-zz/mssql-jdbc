/*
 * Microsoft JDBC Driver for SQL Server
 * 
 * Copyright(c) Microsoft Corporation All rights reserved.
 * 
 * This program is made available under the terms of the MIT License. See the LICENSE file in the project root for more information.
 */
package com.microsoft.sqlserver.jdbc.AlwaysEncrypted;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.microsoft.sqlserver.jdbc.SQLServerConnection;
import com.microsoft.sqlserver.jdbc.SQLServerException;
import com.microsoft.sqlserver.jdbc.SQLServerPreparedStatement;
import com.microsoft.sqlserver.jdbc.SQLServerStatement;
import com.microsoft.sqlserver.jdbc.SQLServerStatementColumnEncryptionSetting;
import com.microsoft.sqlserver.testframework.Utils;



@RunWith(JUnitPlatform.class)
public class JDBCEncryptionDecryptionTest extends AESetup {
    private static SQLServerConnection conn = null;
    private static SQLServerStatement stmt = null;
    private static SQLServerPreparedStatement pstmt = null;
    String [] values = {"10"};

    /**
     * Connect to specified server and close the connection
     * 
     * @throws SQLException
     */
    @Test
    @DisplayName("test connection")
    public void testNumeric() throws SQLException {
        try {
            System.out.println("inside test:" + keyPath);
            Properties info = new Properties();
            info.setProperty("ColumnEncryptionSetting", "Enabled");
            info.setProperty("keyStoreAuthentication", "JavaKeyStorePassword");
            info.setProperty("keyStoreLocation", keyPath);
            info.setProperty("keyStoreSecret", secretstrJks);
            conn = (SQLServerConnection) DriverManager.getConnection(connectionString, info);
            stmt =  (SQLServerStatement) conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, connection.getHoldability(), SQLServerStatementColumnEncryptionSetting.Enabled);

            createCEK(storeProvider, certStore);   
            createNumericTable();
            populateNumeric(values);
            verifyResults();
            Utils.dropTableIfExists(numericTable, stmt);     
        }
        finally {
        }
    }
    
    @AfterAll
    static void dropAll() throws SQLServerException, SQLException{
        Utils.dropTableIfExists(numericTable, stmt);
        dropCEK();
        dropCMK();     
        stmt.close();
        con.close();
    }
    
    private void populateNumeric(String[] values) throws SQLException {
        String sql = "insert into " + numericTable + " values( " 
                + "?,?,?" 
                + ")";

        pstmt = (SQLServerPreparedStatement) conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, connection.getHoldability(), SQLServerStatementColumnEncryptionSetting.Enabled);

        
        for (int i = 1; i <= 3; i++) {
                pstmt.setShort(i,  Short.valueOf(values[0]));
        }
        pstmt.execute();
    }
    
    private void verifyResults() throws NumberFormatException, SQLException{
        String sql = "select * from " + numericTable;
        pstmt = (SQLServerPreparedStatement) connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, connection.getHoldability(), SQLServerStatementColumnEncryptionSetting.Enabled);
        ResultSet rs = null;
     
        rs = pstmt.executeQuery();

        while (rs.next()) {         
           assertEquals(Short.valueOf(values[0]), rs.getObject(1));
           assertEquals(Short.valueOf(values[0]), rs.getObject(2));
           assertEquals(Short.valueOf(values[0]), rs.getObject(3));
        }

        if (null != rs) {
            rs.close();
        }
    }
    
}
