package org.commcare.formplayer.tests.sandbox;

import static org.junit.jupiter.api.Assertions.fail;

import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.model.Case;
import org.commcare.formplayer.sandbox.SqlHelper;
import org.commcare.formplayer.sandbox.SqlSandboxUtils;
import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

public class CaseAPITests {

    private Case a, b, c, d, e;

    private Ledger l;

    private String owner;
    private String groupOwner;
    private Vector<String> groupOwned;
    private Vector<String> userOwned;

    private Connection connection = null;
    private PreparedStatement preparedStatement = null;
    private ResultSet resultSet = null;
    private final String databaseName = ("test.db");


    @BeforeEach
    public void setUp() throws Exception {

        owner = "owner";
        groupOwner = "groupowned";

        userOwned = new Vector<>();
        userOwned.addElement(owner);

        groupOwned = new Vector<>();
        groupOwned.addElement(owner);
        groupOwned.addElement(groupOwner);

        a = new Case("123", "a");
        a.setCaseId("a");
        a.setUserId(owner);
        a.setID(12345);
        b = new Case("b", "b");
        b.setCaseId("b");
        b.setUserId(owner);
        c = new Case("c", "c");
        c.setCaseId("c");
        c.setUserId(owner);
        d = new Case("d", "d");
        d.setCaseId("d");
        d.setUserId(owner);
        e = new Case("e", "e");
        e.setCaseId("e");
        e.setUserId(groupOwner);

        l = new Ledger("ledger_entity_id");
        l.setID(12345);
        l.setEntry("test_section_id", "test_entry_id", 2345);
    }

    @Test
    public void testOwnerPurge() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseName);

            SqlHelper.dropTable(connection, "TFLedger");

            SqlHelper.createTable(connection, "TFLedger", new Ledger());

            SqlHelper.insertToTable(connection, "TFLedger", l);

            preparedStatement = SqlHelper.prepareTableSelectStatement(connection, "TFLedger",
                    new String[]{"entity-id"},
                    new String[]{"ledger_entity_id"});
            if (preparedStatement == null) {
                fail("failed to prepare table select query");
            }
            resultSet = preparedStatement.executeQuery();
            byte[] caseBytes = resultSet.getBytes("commcare_sql_record");
            DataInputStream is = new DataInputStream(new ByteArrayInputStream(caseBytes));

            Ledger readLedger = new Ledger();
            PrototypeFactory lPrototypeFactory = new PrototypeFactory();
            lPrototypeFactory.addClass(Ledger.class);
            readLedger.readExternal(is, lPrototypeFactory);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception " + e.getMessage());
        }
    }

    @AfterEach
    public void tearDown() {
        try {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (connection != null) {
                connection.close();
            }
            if (resultSet != null) {
                resultSet.close();
            }
            File databaseFile = new File(databaseName);
            if (databaseFile.exists()) {
                databaseFile.delete();
            }
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
        SqlSandboxUtils.deleteDatabaseFolder(UserSqlSandbox.DEFAULT_DATBASE_PATH);
    }

}
