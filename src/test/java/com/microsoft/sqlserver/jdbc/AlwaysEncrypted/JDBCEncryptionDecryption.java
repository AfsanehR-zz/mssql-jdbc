/*
 * Microsoft JDBC Driver for SQL Server
 * 
 * Copyright(c) Microsoft Corporation All rights reserved.
 * 
 * This program is made available under the terms of the MIT License. See the LICENSE file in the project root for more information.
 */
package com.microsoft.sqlserver.jdbc.AlwaysEncrypted;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;



@RunWith(JUnitPlatform.class)
public class JDBCEncryptionDecryption extends AESetup {
    private static Connection conn = null;
    private static Statement stmt = null;
    /**
     * Connect to specified server and close the connection
     * 
     * @throws SQLException
     */
    @Test
    @DisplayName("test connection")
    public void testConnection() throws SQLException {
        try {
            conn = DriverManager.getConnection(connectionString);
            conn.close();
        }
        finally {
        }
    }
}
