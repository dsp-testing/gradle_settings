package org.commcare.formplayer.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import org.apache.commons.io.IOUtils;
import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.SubmitResponseBean;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.session.FormSession;
import org.commcare.formplayer.util.serializer.FormDefStringSerializer;
import org.commcare.formplayer.utils.FileUtils;
import org.javarosa.core.model.FormDef;
import org.javarosa.xform.util.XFormUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpEntity;
import org.springframework.util.MultiValueMap;

import java.io.InputStreamReader;

@WebMvcTest
public class SavedFormDefTest extends BaseTestClass {

    @Captor
    ArgumentCaptor<MultiValueMap<String, HttpEntity<Object>>> requestBodyCaptor;

    @Test
    public void testCleanFormDefSaved() throws Exception {
        String formXml = FileUtils.getFile(this.getClass(), "xforms/hidden_value_form.xml");
        FormDef expectedFormDef = XFormUtils.getFormRaw(
                new InputStreamReader(IOUtils.toInputStream(formXml, "UTF-8")));

        NewFormResponse newSessionResponse = startNewForm("requests/new_form/new_form_3.json",
                "xforms/hidden_value_form.xml");

        SerializableFormSession session = this.formSessionService.getSessionById(
                newSessionResponse.getSessionId());
        FormDef actualFormDef = FormDefStringSerializer.deserialize(session.getFormDefinition().getSerializedFormDef());

        assertEquals(expectedFormDef.getMainInstance().getRoot(), actualFormDef.getMainInstance().getRoot());
    }

    @Test
    public void testIrrelevantDataIsIncludedPriorToSubmission() throws Exception {
        NewFormResponse newSessionResponse = startNewForm("requests/new_form/new_form_3.json",
                "xforms/hidden_value_form.xml");

        String sessionId = newSessionResponse.getSessionId();

        // this answer does not satisfy the condition, and therefore the calculated property is
        // irrelevant
        answerQuestionGetResult("0", "9", sessionId);

        SerializableFormSession session = this.formSessionService.getSessionById(
                newSessionResponse.getSessionId());
        FormSession formSession = new FormSession(session,
                this.restoreFactoryMock,
                null,
                this.storageFactoryMock,
                null,
                this.remoteInstanceFetcherMock,
                this.formDefinitionService
        );
        assertEquals(formSession.getInstanceXml(true), session.getInstanceXml());
    }

    @Test
    public void testIrrelevantDataIsNotIncludedInSubmission() throws Exception {
        NewFormResponse newSessionResponse = startNewForm("requests/new_form/new_form_3.json",
                "xforms/hidden_value_form.xml");

        String sessionId = newSessionResponse.getSessionId();

        // this answer does not satisfy the condition, and therefore the calculated property is
        // irrelevant
        answerQuestionGetResult("0", "9", sessionId);
        SerializableFormSession session = this.formSessionService.getSessionById(
                newSessionResponse.getSessionId());
        FormSession formSession = new FormSession(session,
                this.restoreFactoryMock,
                null,
                this.storageFactoryMock,
                null,
                this.remoteInstanceFetcherMock,
                this.formDefinitionService);
        SubmitResponseBean submitResponseBean = submitForm(
                "requests/submit/submit_hidden_value_form.json", sessionId);
        assertEquals("success", submitResponseBean.getStatus());

        Mockito.verify(this.submitServiceMock).submitForm(requestBodyCaptor.capture(),
                eq(formSession.getPostUrl()));
        assertEquals(requestBodyCaptor.getAllValues().get(0).getFirst("xml_submission_file").getBody(),
                (formSession.getInstanceXml(false)));
    }
}
