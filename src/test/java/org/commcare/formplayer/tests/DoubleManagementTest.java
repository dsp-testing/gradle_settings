package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.SubmitResponseBean;
import org.commcare.formplayer.beans.menus.CommandListResponseBean;
import org.commcare.formplayer.beans.menus.DisplayElement;
import org.commcare.formplayer.beans.menus.EntityDetailListResponse;
import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * Created by willpride on 4/14/16.
 */
@WebMvcTest
public class DoubleManagementTest extends BaseTestClass {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("doublemgmtdomain", "doublemgmtusername");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/parent_child.xml";
    }

    @Test
    public void testDoubleForm() throws Exception {
        // setup files
        CommandListResponseBean menuResponseBean =
                doInstall("requests/install/double_mgmt_install.json");
        assert menuResponseBean.getCommands().length == 3;
        assert menuResponseBean.getTitle().equals("Parent Child");
        assert menuResponseBean.getCommands()[0].getDisplayText().equals("Parent");
        assert menuResponseBean.getCommands()[1].getDisplayText().equals("Child");
        assert menuResponseBean.getCommands()[2].getDisplayText().equals("Parent (2)");

        EntityListResponse entityListResponse =
                sessionNavigate(new String[]{"2"}, "doublemgmt", EntityListResponse.class);

        assert entityListResponse.getEntities().length == 2;
        assert entityListResponse.getTitle().equals("Parent (2)");
        assert entityListResponse.getActions() != null;
        assert entityListResponse.getActions()[0].getText().equals("New Parent");

        NewFormResponse newFormResponse =
                sessionNavigate(new String[]{"2", "action 0"}, "doublemgmt", NewFormResponse.class);

        assert newFormResponse.getTitle().equals("Register Parent");
        assert newFormResponse.getTree().length == 2;

        // ok, test end of form nav
        SubmitResponseBean submitResponseBean = submitForm(
                "requests/submit/submit_double_mgmt.json",
                newFormResponse.getSessionId());
        assert submitResponseBean.getNextScreen() != null;
        CommandListResponseBean commandListResponseBean = mapper.readValue(
                mapper.writeValueAsString(submitResponseBean.getNextScreen()),
                CommandListResponseBean.class);
        assert commandListResponseBean.getCommands().length == 2;
        assert commandListResponseBean.getCommands()[0].getDisplayText().equals("Update Parent");
        assert commandListResponseBean.getSelections().length == 2;
        String[] selections = commandListResponseBean.getSelections();
        String firstCommand = selections[0];
        String secondCommand = selections[1];
        String[] newSteps = new String[]{firstCommand, secondCommand, "0"};
        NewFormResponse followupFormResponse = sessionNavigate(newSteps, "doublemgmt",
                NewFormResponse.class);
        assert followupFormResponse.getTree().length == 2;
        assert followupFormResponse.getTree()[0].getAnswer().equals("David Ortiz");
        assert followupFormResponse.getTree()[1].getAnswer().equals(40);
    }

    @Test
    public void testDoubleCaseSelect() throws Exception {
        // setup files
        CommandListResponseBean menuResponseBean =
                doInstall("requests/install/double_mgmt_install.json");
        assert menuResponseBean.getCommands().length == 3;
        assert menuResponseBean.getTitle().equals("Parent Child");
        assert menuResponseBean.getCommands()[0].getDisplayText().equals("Parent");
        assert menuResponseBean.getCommands()[1].getDisplayText().equals("Child");
        assert menuResponseBean.getCommands()[2].getDisplayText().equals("Parent (2)");

        EntityListResponse entityListResponse =
                sessionNavigate(new String[]{"2"}, "doublemgmt", EntityListResponse.class);

        assert entityListResponse.getEntities().length == 2;
        assert entityListResponse.getTitle().equals("Parent (2)");
        assert entityListResponse.getActions() != null;
        assert entityListResponse.getActions()[0].getText().equals("New Parent");

        EntityDetailListResponse detailListResponse =
                getDetails(new String[]{"2", "a9fde9ae-24ee-4d70-9cb4-20f266a62ef8"}, "doublemgmt",
                        EntityDetailListResponse.class);

        assert detailListResponse.getEntityDetailList()[0].getTitle().equals("Cases");
        assert detailListResponse.getEntityDetailList().length == 1;
    }

    @Test
    public void testNavigator() throws Exception {
        NewFormResponse newFormSessionResponse =
                sessionNavigate("requests/navigators/navigator_0.json", NewFormResponse.class);
        assert newFormSessionResponse.getTitle().equals("Update Parent");
        assert newFormSessionResponse.getTree().length == 2;
    }

    @Test
    public void testAllPermutations() throws Exception {
        NewFormResponse newFormResponse =
                sessionNavigate(new String[]{"0", "0"}, "doublemgmt", NewFormResponse.class);
        assert newFormResponse.getTitle().equals("Register Parent");
        assert newFormResponse.getTree().length == 2;

        newFormResponse =
                sessionNavigate(new String[]{"1", "0"}, "doublemgmt", NewFormResponse.class);
        assert newFormResponse.getTitle().equals("Child Registration");
        assert newFormResponse.getTree().length == 2;

        EntityListResponse entityListResponse =
                sessionNavigate(new String[]{"2"}, "doublemgmt", EntityListResponse.class);
        assert entityListResponse.getTitle().equals("Parent (2)");
        assert entityListResponse.getEntities().length == 2;
        assert entityListResponse.getActions() != null;
        DisplayElement action = entityListResponse.getActions()[0];
        assert action.getText().equals("New Parent");

        CommandListResponseBean commandListResponse =
                sessionNavigate(new String[]{"2", "4d1831ab-abfe-4086-bce7-16d325d9ca3a"},
                        "doublemgmt", CommandListResponseBean.class);
        assert commandListResponse.getTitle().equals("Parent (2)");
        assert commandListResponse.getCommands().length == 2;
        assert commandListResponse.getCommands()[0].getDisplayText().equals("Update Parent");
        assert commandListResponse.getCommands()[1].getDisplayText().equals(
                "Parent Register Child");

        newFormResponse =
                sessionNavigate(new String[]{"2", "4d1831ab-abfe-4086-bce7-16d325d9ca3a", "0"},
                        "doublemgmt", NewFormResponse.class);
        assert newFormResponse.getTitle().equals("Update Parent");
        assert newFormResponse.getTree().length == 2;

    }

    @Test
    public void testMenuMedia() throws Exception {
        CommandListResponseBean menuResponseBean =
                doInstall("requests/install/case_media.json");
        assert menuResponseBean.getCommands().length == 2;
        assert menuResponseBean.getCommands()[0].getDisplayText().equals("Registration");
        assert menuResponseBean.getCommands()[0].getAudioUri().equals(
                "jr://file/commcare/audio/module0_form0_en.mp3");
        assert menuResponseBean.getCommands()[0].getImageUri().equals(
                "jr://file/commcare/image/module0_form0_en.png");
        assert menuResponseBean.getCommands()[1].getDisplayText().equals("Follow Up");
        assert menuResponseBean.getCommands()[1].getAudioUri().equals(
                "jr://file/commcare/audio/module1_en.mp3");
        assert menuResponseBean.getCommands()[1].getImageUri().equals(
                "jr://file/commcare/image/module1_en.png");
    }

    @Test
    public void testEndOfFormNavigation() throws Exception {
        CommandListResponseBean response0 =
                sessionNavigate(new String[]{"0"}, "formnav", CommandListResponseBean.class);
        assert response0.getCommands().length == 2;
        assert response0.getCommands()[0].getDisplayText().equals("Link to Module 1");
        assert response0.getCommands()[1].getDisplayText().equals("Link to Module Menu");

        NewFormResponse newFormResponse =
                sessionNavigate(new String[]{"0", "0"}, "formnav", NewFormResponse.class);
        assert newFormResponse.getTitle().equals("Link to Module 1");
        assert newFormResponse.getTree().length == 4;
    }
}
