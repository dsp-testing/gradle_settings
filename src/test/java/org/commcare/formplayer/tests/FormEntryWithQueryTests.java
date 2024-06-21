package org.commcare.formplayer.tests;


import static org.commcare.formplayer.util.Constants.TOGGLE_SESSION_ENDPOINTS;
import static org.commcare.formplayer.util.FormplayerPropertyManager.AUTO_ADVANCE_MENU;
import static org.commcare.formplayer.util.FormplayerPropertyManager.YES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.QuestionBean;
import org.commcare.formplayer.beans.menus.CommandListResponseBean;
import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.commcare.formplayer.beans.menus.QueryResponseBean;
import org.commcare.formplayer.objects.QueryData;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.services.FormplayerStorageFactory;
import org.commcare.formplayer.session.FormSession;
import org.commcare.formplayer.utils.FileUtils;
import org.commcare.formplayer.utils.MockRequestUtils;
import org.commcare.formplayer.utils.TestContext;
import org.commcare.formplayer.utils.WithHqUser;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ContextConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

@WebMvcTest
public class FormEntryWithQueryTests extends BaseTestClass {

    @Autowired
    FormplayerStorageFactory storageFactory;

    @Autowired
    CacheManager cacheManager;

    private MockRequestUtils mockRequest;

    @Captor
    ArgumentCaptor<Multimap<String, String>> requestDataCaptor;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("caseclaimdomain", "caseclaimusername");
        cacheManager.getCache("case_search").clear();
        mockRequest = new MockRequestUtils(webClientMock, restoreFactoryMock);
    }

    @Test
    public void testFormEntryWithQuery() throws Exception {
        configureQueryMock();
        // selecting the command containing form with `query` should launch the query screen
        sessionNavigateWithQuery(new String[]{"1"},
                "caseclaimquery",
                null,
                QueryResponseBean.class);

        Hashtable<String, String> inputs = new Hashtable<>();
        QueryData queryData = new QueryData();
        queryData.setInputs("m1_results", inputs);
        queryData.setExecute("m1_results", true);

        sessionNavigateWithQuery(new String[]{"1"},
                "caseclaimquery",
                queryData,
                EntityListResponse.class);

        // Check if form's query was executed
        verify(webClientMock).postFormData(any(), any());
        verifyNoMoreInteractions(webClientMock);

        // Open the form with `query` blocks
        NewFormResponse formResponse = sessionNavigateWithQuery(
                new String[]{"1", "0156fa3e-093e-4136-b95c-01b13dae66c6", "0"},
                "caseclaimquery",
                queryData,
                NewFormResponse.class);

        // verify the second query block to fetch the remote case was executed
        verify(webClientMock, times(2)).postFormData(any(), any());
        verifyNoMoreInteractions(webClientMock);

        // see if the instance is retained into the form session
        checkXpath(
                formResponse.getSessionId(),
                "instance('registry')/results/case[@case_type='case']/case_name",
                "Burt Maclin"
        );

        // make sure that the instance in the form session is using the case template
        SerializableFormSession serializableFormSession = formSessionService.getSessionById(
                formResponse.getSessionId());
        FormSession formSession = getFormSession(serializableFormSession);
        EvaluationContext evaluationContext =
                formSession.getFormEntryModel().getForm().getEvaluationContext();
        ExternalDataInstance registry = (ExternalDataInstance)evaluationContext.getInstance(
                "registry");
        assertTrue(registry.useCaseTemplate());
    }

    @Test
    public void testNavigationToFormEntryWithMultipleQueries() throws Exception {
        configureQueryMock();
        // select module 2
        sessionNavigateWithQuery(new String[]{"2"},
                "caseclaimquery",
                null,
                QueryResponseBean.class);


        Hashtable<String, String> inputs = new Hashtable<>();
        QueryData queryData = new QueryData();
        queryData.setInputs("m2_results", inputs);
        queryData.setExecute("m2_results", true);

        // execute search query
        sessionNavigateWithQuery(new String[]{"2"},
                "caseclaimquery",
                queryData,
                EntityListResponse.class);

        // Check if form's query was executed
        verify(webClientMock).postFormData(any(), any());
        verifyNoMoreInteractions(webClientMock);

        // Select a case
        CommandListResponseBean menuResponse = sessionNavigateWithQuery(
                new String[]{"2", "0156fa3e-093e-4136-b95c-01b13dae66c6"},
                "caseclaimquery",
                queryData,
                CommandListResponseBean.class);

        assertEquals(1, menuResponse.getCommands().length);
        assertEquals("Dedupe Form", menuResponse.getCommands()[0].getDisplayText());

        // verify the second query block to fetch the remote case was executed as well as the 3rd
        // query block
        // to do a custom lookup
        verify(webClientMock, times(3)).postFormData(any(), any());
        verifyNoMoreInteractions(webClientMock);

        // Open the form
        NewFormResponse formResponse = sessionNavigateWithQuery(
                new String[]{"2", "0156fa3e-093e-4136-b95c-01b13dae66c6", "0"},
                "caseclaimquery",
                queryData,
                NewFormResponse.class);

        verify(webClientMock, times(3)).postFormData(any(), any());
        verifyNoMoreInteractions(webClientMock);

        // check we can access the 'registry' instance in the form
        checkXpath(
                formResponse.getSessionId(),
                "instance('registry')/results/case[@case_type='case']/case_name",
                "Burt Maclin"
        );

        // check we can access the 'duplicate' instance in the form
        checkXpath(
                formResponse.getSessionId(),
                "instance('duplicate')/results/case[@case_id='dupe_case_id']/case_name",
                "Duplicate of Burt"
        );
    }

    /**
     * Test that setting "cc-auto-advance-menu" works even when the last screen is a query.
     */
    @Test
    public void testNavigationToFormEntryWithQueriesAutoAdvance() throws Exception {
        configureQueryMock();
        // select module 2
        sessionNavigateWithQuery(new String[]{"2"},
                "caseclaimquery",
                null,
                QueryResponseBean.class);


        Hashtable<String, String> inputs = new Hashtable<>();
        QueryData queryData = new QueryData();
        queryData.setInputs("m2_results", inputs);
        queryData.setExecute("m2_results", true);

        // execute search query
        sessionNavigateWithQuery(new String[]{"2"},
                "caseclaimquery",
                queryData,
                EntityListResponse.class);

        // Check if form's query was executed
        verify(webClientMock, times(1)).postFormData(any(), any());

        // with auto-advance enabled the selection of a case should result in the session
        // being auto-advanced directly to the form (since there is only one form to choose from)
        storageFactory.getPropertyManager().setProperty(AUTO_ADVANCE_MENU, YES);
        NewFormResponse formResponse = sessionNavigateWithQuery(
                new String[]{"2", "0156fa3e-093e-4136-b95c-01b13dae66c6"},
                "caseclaimquery",
                queryData,
                NewFormResponse.class);

        assertEquals(formResponse.getTitle(), "Followup Form");
        verify(webClientMock, times(3)).postFormData(any(), any());
    }

    @Test
    public void testSearchInputInstanceInForm() throws Exception {
        configureQueryMock();
        ArrayList<String> selections = new ArrayList<>();
        selections.add("3");  // m3
        selections.add("0156fa3e-093e-4136-b95c-01b13dae66c6");
        selections.add("0");  // m3-f0

        QueryData queryData = new QueryData();
        queryData.setInputs("m3_results", new Hashtable<String, String>() {{
            put("name", "bob");
        }});

        when(webClientMock.postFormData(any(), any())).thenReturn(
                FileUtils.getFile(this.getClass(), "query_responses/case_claim_response.xml"));
        NewFormResponse formResponse = sessionNavigateWithQuery(selections,
                "caseclaimquery",
                queryData,
                NewFormResponse.class);

        assertEquals("test-search-input", formResponse.getTitle());
        QuestionBean nameLegacyInstanceRef = formResponse.getTree()[0];
        QuestionBean nameNewInstanceRef = formResponse.getTree()[1];
        assertEquals("bob", nameLegacyInstanceRef.getAnswer());
        assertEquals("bob", nameNewInstanceRef.getAnswer());

        // redo the same query to test for instance loading correctly from storage
        NewFormResponse newFormResponse = sessionNavigateWithQuery(selections,
                "caseclaimquery",
                queryData,
                NewFormResponse.class);
        nameLegacyInstanceRef = newFormResponse.getTree()[0];
        nameNewInstanceRef = newFormResponse.getTree()[1];
        assertEquals("bob", nameLegacyInstanceRef.getAnswer());
        assertEquals("bob", nameNewInstanceRef.getAnswer());
    }

    @Test
    @WithHqUser(enabledToggles = {TOGGLE_SESSION_ENDPOINTS})
    public void testEndpointToFormWithQuery_QueryParamsContainsEndpointExtras() throws Exception {
        String selectedCaseId = "94f8d030-c6f9-49e0-bc3f-5e0cdbf10c18";

        // mock case fixture api to return a result containing selectedCaseId
        String caseFixtureUri =
                "http://localhost:8000/a/test-1/phone/case_fixture/dec220eae9974c788654f23320f3a8d3/";
        when(webClientMock.postFormData(eq(caseFixtureUri), any())).thenReturn(
                getFileStream("query_responses/case_search_multi_select_response.xml"));

        // case search to return a result that doesn't contain the selectedCaseId
        String caseSearchUri = "http://localhost:8000/a/test-1/phone/search/dec220eae9974c788654f23320f3a8d3/";
        when(webClientMock.postFormData(eq(caseSearchUri), requestDataCaptor.capture())).thenReturn(
                getFileStream("query_responses/case_search_multi_select_response.xml"));

        HashMap<String, String> endpointArgs = new HashMap<>();
        endpointArgs.put("case_id", selectedCaseId);

        NewFormResponse formResponse = sessionNavigateWithEndpoint("caseclaimquery",
                "m3_f0_endpoint",
                endpointArgs,
                NewFormResponse.class);
        assert formResponse.getTitle().contentEquals("test-search-input");

        Multimap<String, String> requestData = requestDataCaptor.getAllValues().get(0);
        assertTrue(requestData.get("case_id").contains(selectedCaseId),
                "case search request doesn't have selectedCaseId as a query param");

    }

    private String getFileStream(String filePath) {
        return FileUtils.getFile(this.getClass(), filePath);
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/caseclaim.xml";
    }

    private void configureQueryMock() {
        String searchUri =
                "http://localhost:8000/a/test-1/phone/search/dec220eae9974c788654f23320f3a8d3/";
        String searchResponse = "query_responses/case_claim_response.xml";
        ImmutableMultimap<String, String> data = ImmutableMultimap.of("commcare_registry",
                "shubham", "case_type", "case");
        when(webClientMock.postFormData(eq(searchUri), eq(data))).thenReturn(
                FileUtils.getFile(this.getClass(), searchResponse));

        String registryUrl =
                "http://localhost:8000/a/test-1/phone/registry_case"
                        + "/dec220eae9974c788654f23320f3a8d3/";
        ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap.builder();
        builder.putAll(data).put("case_id", "0156fa3e-093e-4136-b95c-01b13dae66c6");
        String firstQueryResponse = "query_responses/case_claim_response.xml";
        when(webClientMock.postFormData(eq(registryUrl), eq(builder.build()))).thenReturn(
                FileUtils.getFile(this.getClass(), firstQueryResponse));

        builder = ImmutableMultimap.builder();
        builder.putAll(data).put("case_id", "dupe_case_id");
        String secondQueryResponse = "query_responses/registry_query_response.xml";
        when(webClientMock.postFormData(eq(registryUrl), eq(builder.build()))).thenReturn(
                FileUtils.getFile(this.getClass(), secondQueryResponse));
    }
}
