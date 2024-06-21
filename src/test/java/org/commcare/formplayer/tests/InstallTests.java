package org.commcare.formplayer.tests;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.menus.CommandListResponseBean;
import org.commcare.formplayer.sandbox.SqlSandboxUtils;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * Created by willpride on 1/14/16.
 */
@WebMvcTest
public class InstallTests extends BaseTestClass {

    Log log = LogFactory.getLog(InstallTests.class);

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("casetestdomain", "casetestuser");
    }

    @Test
    public void testCaseCreate() throws Exception {
        SqlSandboxUtils.deleteDatabaseFolder("dbs");
        // setup files

        CommandListResponseBean menuResponseBean =
                doInstall("requests/install/install.json");

        NewFormResponse menuResponseObject =
                sessionNavigate(new String[]{"2", "0"}, "case", NewFormResponse.class);
        SqlSandboxUtils.deleteDatabaseFolder("dbs");
    }


    @Test
    public void testNewForm() throws Exception {
        // setup files
        CommandListResponseBean menuResponseBean =
                doInstall("requests/install/install.json");
        assert menuResponseBean.getCommands().length == 12;
        assert menuResponseBean.getTitle().equals("Basic Tests");
        assert menuResponseBean.getCommands()[0].getDisplayText().equals("Basic Form Tests");

        NewFormResponse newFormResponse =
                sessionNavigate(new String[]{"0", "0"}, "case", NewFormResponse.class);

    }

    @Test
    public void testCaseSelect() throws Exception {
        SqlSandboxUtils.deleteDatabaseFolder("dbs");
        NewFormResponse formSessionResponse =
                sessionNavigate(new String[]{"2", "1", "1a8ca44cb5dc4ce9995a71ea8929d4c3"},
                        "case", NewFormResponse.class);
        assert formSessionResponse.getTitle().equals("Update a Case");
        assert formSessionResponse.getTree().length == 7;

        SqlSandboxUtils.deleteDatabaseFolder("dbs");
    }

}
