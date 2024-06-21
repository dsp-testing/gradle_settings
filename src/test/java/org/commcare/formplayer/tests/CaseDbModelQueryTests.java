package org.commcare.formplayer.tests;

import static org.commcare.formplayer.utils.DbTestUtils.evaluate;

import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.commcare.formplayer.utils.TestContext;
import org.commcare.formplayer.utils.TestStorageUtils;
import org.javarosa.core.model.condition.EvaluationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author wspride
 */
@WebMvcTest
public class CaseDbModelQueryTests extends BaseTestClass {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("synctestdomain", "synctestusername");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/dbtests/case_db_model_query.xml";
    }

    /**
     * Tests for basic common case database queries
     */
    @Test
    public void testDbModelQueryLookups() throws Exception {
        syncDb();
        UserSqlSandbox sandbox = getRestoreSandbox();
        EvaluationContext ec = TestStorageUtils.getEvaluationContextWithoutSession(sandbox);
        ec.setDebugModeOn();
        evaluate(
                "join(',',instance('casedb')/casedb/case[@case_type='unit_test_child_child"
                        + "'][@status='open'][true() and "
                        +
                        "instance('casedb')/casedb/case[@case_id = instance('casedb')"
                        + "/casedb/case[@case_id=current()/index/parent]/index/parent]/test = "
                        + "'true']/@case_id)",
                "child_ptwo_one_one,child_one_one", ec);
        evaluate(
                "join(',',instance('casedb')/casedb/case[@case_type='unit_test_child'][@status"
                        + "='open'][true() and "
                        +
                        "count(instance('casedb')/casedb/case[index/parent = instance('casedb')"
                        + "/casedb/case[@case_id=current()/@case_id]/index/parent][false = "
                        + "'true']) > 0]/@case_id)",
                "", ec);

    }
}
