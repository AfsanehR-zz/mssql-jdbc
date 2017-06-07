/*
 * Microsoft JDBC Driver for SQL Server
 * 
 * Copyright(c) Microsoft Corporation All rights reserved.
 * 
 * This program is made available under the terms of the MIT License. See the LICENSE file in the project root for more information.
 */
package com.microsoft.sqlserver.jdbc.AlwaysEncrypted;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import javax.xml.bind.DatatypeConverter;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.microsoft.sqlserver.jdbc.SQLServerException;
import com.microsoft.sqlserver.jdbc.SQLServerStatementColumnEncryptionSetting;
import com.microsoft.sqlserver.testframework.AbstractTest;
import com.microsoft.sqlserver.testframework.DBConnection;
import com.microsoft.sqlserver.testframework.DBStatement;
import com.microsoft.sqlserver.testframework.Utils;

@RunWith(JUnitPlatform.class)
public class AESetup extends AbstractTest {

    static String inputFile = "certificate.txt";
    static String filePath = null;
    static String thumbprint = null;
    static Connection con = null;
    static Statement stmt = null;
    static String cmkName = "JDBC_CMK";
    static String cekName = "JDBC_CEK";
    static String keyStoreName = "MSSQL_CERTIFICATE_STORE";
    static String keyPath = "CurrentUser/My/";
    static String numericTable = "numericTable";
    
    /**
     * Create connection, statement and generate path of resource file
     * @throws SQLException 
     */
    @BeforeAll
    static void setUpConnection() throws SQLException {
        filePath =   Utils.getCurrentClassPath();     
        readCertificateFromFile();
        con =  DriverManager.getConnection(connectionString);
        stmt = con.createStatement(); 
        Utils.dropTableIfExists(numericTable, stmt);
        dropCEK();
        dropCMK();    
        createCMK();
        createCEK();             
    }
    
    
    private static void readCertificateFromFile(){
        try {
            File f = new File(filePath + inputFile);

            BufferedReader b = new BufferedReader(new FileReader(f));

            String readLine = "";
            String[] linecontents;
            System.out.println("Reading file using Buffered Reader");

            while ((readLine = b.readLine()) != null) {
                System.out.println(readLine);
                if (readLine.contains("CN=AlwaysEncryptedCert")){
                    linecontents = readLine.split(" ");
                    thumbprint = linecontents[0];
                    break;
                }
            }
            
            keyPath += thumbprint;
            
            System.out.println("keypath:" + keyPath);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    
    static void createNumericTable(){
        String sql = "create table " + numericTable 
                + " ("             
                + "PlainSmallint smallint null,"
                + "RandomizedSmallint smallint ENCRYPTED WITH (ENCRYPTION_TYPE = RANDOMIZED, ALGORITHM = 'AEAD_AES_256_CBC_HMAC_SHA_256', COLUMN_ENCRYPTION_KEY = "
                + cekName + ") NULL,"
                + "DeterministicSmallint smallint ENCRYPTED WITH (ENCRYPTION_TYPE = DETERMINISTIC, ALGORITHM = 'AEAD_AES_256_CBC_HMAC_SHA_256', COLUMN_ENCRYPTION_KEY = "
                + cekName + ") NULL"
                + ");";

        try {
            stmt.execute(sql);
            System.out.println("Table created!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    
    private static void createCMK() throws SQLException{
        String sql = " if not exists (SELECT name from sys.column_master_keys where name='" + cmkName + "')"
                + " begin"
                + " CREATE COLUMN MASTER KEY " + cmkName+ " WITH (KEY_STORE_PROVIDER_NAME = '" + keyStoreName 
                + "', KEY_PATH = '" + keyPath + "')"
                + " end";
        stmt.execute(sql);
    }
    
    private static void createCEK() throws SQLException{
        String encryptedValue = "0x016E000001630075007200720065006E00740075007300650072002F006D0079002F006500390035003100350065003500330031003500390036003500660033006400620031003900350038003200370032003000660065003300330032003400390035003100300031006400610064003400A7871B8D8F510AA88374823EAED916A7319DB0DC83C6E646FA7AF9F6B734C4356D66A116E36EB5F4A9544B32D4230C7BF5D582EF5401D955BB566F5A5CC14A976F74501C4B2CF2AEEF24589A0A2BABBFFBF558A09EA82EE76A4C0958BA4190AAB37084E9846954D3B68F9AAEB46FD28827110E5254318F24C1634A0259E89D0970B2CF4D6ACDF442185C3762CBDA63BE80104FB53821610CCB1E56C25571AC8F4C19C36DC68E8FF161C0B66605A9F661BB4DFA73D996A6E1DC8D265D2DB1061B46DA27719288830C17F955E0242A76DAB7D09F750919AEBDCEA1F4BA1F1846CD08099DDE11E3FE5E4C40435853A00EB3C0B2C8704B85B0C761FA2BB6944631293512F8D81386A1CBB641873F1E4F452BC5AE07955B506F3C158FA6ED5BACD7C199C825A0220136EBB75FD46B54FB3199C1E6E17897B437C3389D4931AA6982133E42DBED71C233B4C25260C9B78960020C513C6F12B52728C89918BFC4506FB0B1CF162FED01DDE3B6CA0BDA09DE39901D18ED554CB5E7F3224E837CC484C98CA2D00D448330E556504CE6DBB17531FE895781B57F182A74DDD42B0356F4B06B502088FC80F90F285A9F19DEB30F3EA20FE29B21ADF0B65B900A055B7FDA2403D23915C046BA7FC4AFE1153AEFB19F9D35B967AEAB19508BCBE49E7E90B84A22583BBFDE59258DC4081D2B6E0EFE3FC0BCBBCCB5584424CEA7A50A122AC20EE4";
        String sql = "CREATE COLUMN ENCRYPTION KEY " + cekName + " WITH VALUES " + "(COLUMN_MASTER_KEY = " + cmkName
                + ", ALGORITHM = 'RSA_OAEP', ENCRYPTED_VALUE = " + encryptedValue + ")" + ";";
        stmt.execute(sql);
    }
    
    static void dropCEK() throws SQLServerException, SQLException {
        String cekSql = " if exists (SELECT name from sys.column_encryption_keys where name='" + cekName + "')"
                + " begin" + " drop column encryption key " + cekName + " end";
        stmt.execute(cekSql);
    }
    
   static void dropCMK() throws SQLServerException, SQLException {
        String cekSql = " if exists (SELECT name from sys.column_master_keys where name='" + cmkName + "')"
                + " begin"
                + " drop column master key " + cmkName
                + " end";
        stmt.execute(cekSql);
    }
}
