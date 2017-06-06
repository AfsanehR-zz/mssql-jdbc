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
    static String keyPath = "LocalMachine/My";
    
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
        createCMK();
        createCEK();
    }
    
    @AfterAll
    static void dropAll() throws SQLServerException, SQLException{
        dropCEK();
        dropCMK();     
    }
    
    private static void readCertificateFromFile(){
        try {
            File f = new File(filePath + inputFile);

            BufferedReader b = new BufferedReader(new FileReader(f));

            String readLine = "";
            String[] linecontents;
            System.out.println("Reading file using Buffered Reader");

            while ((readLine = b.readLine()) != null) {
                if (readLine.contains("CN=testcert.petri.com ")){
                    linecontents = readLine.split(" ");
                    thumbprint = linecontents[0];
                    break;
                }
            }
            keyPath += thumbprint.trim();

        } catch (IOException e) {
            e.printStackTrace();
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
        String encryptedValue = "0x016E000001630075007200720065006E00740075007300650072002F006D0079002F006500380037003000650062006200620030006200340030006400390032006500610039003500360038006300650039006500300063006100650037003400630062003100310061003400660031006600B9AE7EBD7079C21F8C6219ACFDA9D0DB386ACD5AB953036CF9432BAF9164BB23C1989F828325B15E421A3E2519ED78B8CA90635E54EAF1BFC88E4D2D1A96D56E62C906516BA0C73139AE98B6999E3F21FFFAECDF2EFE3E75876638E36AB57B416932DFDFBAEC1DCB2A8C2CF1BA2A9869978E7E1037DB3B5F8CC4FEA0B66CD7DEACC8877CD52ED2CCE4B9303F9A79979D9E4B4BB1E317F7E21F2A551D3266B43A5CE3BF7F4823AAB40ACE2EB228ABA80FB7C692A0B9C8EB3075118B05126C74D7B5A3287A1B16707FBDD37257B88BF4F1E6EDA8FDB0AFD86EE534B6241C799C39D83D6DEB655B0F78C3D925F8A39503F58329951DF6695BDE2756855008D0AA3E0B6DA1DE39B6EB08B558D881C41315A14371ABEB1D0E92728C7CFA02448506F1A05622A79189FCEBC7EBD6989A6BAACCC4D51CA855093578D22F5ADB8FDE557DCC13F0E66A294C8636BA2E2F350EF81806E5C4C0482DFCF9861F184B81696266A992E66D3C4B28AAEF6E503222648B5418D2416E730E0D6ED98CC567221CD7DF8675FFA26CC75FD6CEF3FDF689934935A9B5A4910543782D7F79DC1F02E03392F96FFD03FE828D43551BBA6BDDE38303DBAD450A1E961C5C622160AE75895D8952A36817CDD0710EAE8F473B316007CCCD29C7C5C8AC52F7F0FF567C7482F5AEC8185819A99E00525E3472A054CDAA7A032E9FC2083F924A9534F50FABE320DA";
        String sql = "CREATE COLUMN ENCRYPTION KEY " + cekName + " WITH VALUES " + "(COLUMN_MASTER_KEY = " + cmkName
                + ", ALGORITHM = 'RSA_OAEP', ENCRYPTED_VALUE = " + encryptedValue + ")" + ";";
        stmt.execute(sql);
    }
    
    private static void dropCEK() throws SQLServerException, SQLException {
        String cekSql = " if exists (SELECT name from sys.column_encryption_keys where name='" + cekName + "')"
                + " begin" + " drop column encryption key " + cekName + " end";
        stmt.execute(cekSql);
    }
    
    private static void dropCMK() throws SQLServerException, SQLException {
        String cekSql = " if exists (SELECT name from sys.column_master_keys where name='" + cmkName + "')"
                + " begin"
                + " drop column master key " + cmkName
                + " end";
        stmt.execute(cekSql);
    }
}
