package org.commcare.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.commcare.suite.model.QueryData;
import org.commcare.suite.model.QueryPrompt;
import org.commcare.suite.model.QueryGroup;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;

/**
 * Low level tests for query data parsing
 */
public class QueryDataParserTest {

    @Test
    public void testParseValueData() throws InvalidStructureException, XmlPullParserException, IOException {
        String query = "<data key=\"device_id\" ref=\"instance('session')/session/case_id\"/>";
        QueryDataParser parser = ParserTestUtils.buildParser(query, QueryDataParser.class);
        QueryData queryData = parser.parse();
        assertEquals("device_id", queryData.getKey());

        EvaluationContext evalContext = new EvaluationContext(null, TestInstances.getInstances());
        assertEquals(Collections.singletonList("bang"), queryData.getValues(evalContext));
    }

    @Test
    public void testParseValueData_withExclude() throws InvalidStructureException, XmlPullParserException,
            IOException {
        String query = "<data key=\"device_id\" ref=\"instance('session')/session/case_id\""
                + "exclude=\"true()\"/>";
        QueryDataParser parser = ParserTestUtils.buildParser(query, QueryDataParser.class);
        QueryData queryData = parser.parse();
        assertEquals("device_id", queryData.getKey());

        EvaluationContext evalContext = new EvaluationContext(null, TestInstances.getInstances());
        assertEquals(Collections.emptyList(), queryData.getValues(evalContext));
    }

    @Test
    public void testParseValueData_withRequiredAttribute()
            throws InvalidStructureException, XmlPullParserException,
            IOException, UnfullfilledRequirementsException {
        String query = "<data key=\"device_id\" ref=\"instance('session')/session/case_id\""
                + "required=\"true()\"/>";
        QueryPromptParser parser = ParserTestUtils.buildParser(query, QueryPromptParser.class);
        QueryPrompt queryData = parser.parse();

        EvaluationContext evalContext = new EvaluationContext(null, TestInstances.getInstances());
        assertTrue((boolean)queryData.getRequired().getTest().eval(evalContext));
        assertNull(queryData.getRequired().getMessage());
    }

    @Test
    public void testParseValueData_withRequiredNode() throws InvalidStructureException, XmlPullParserException,
            IOException, UnfullfilledRequirementsException {
        String query = "<prompt key=\"name\">"
                + "          <required test=\"true()\">"
                + "            <text>This field can't be empty</text>"
                + "          </required>"
                + "</prompt>";
        QueryPromptParser parser = ParserTestUtils.buildParser(query, QueryPromptParser.class);
        QueryPrompt queryData = parser.parse();
        EvaluationContext evalContext = new EvaluationContext(null, TestInstances.getInstances());
        assertTrue((boolean)queryData.getRequired().getTest().eval(evalContext));
        assertTrue(queryData.getRequiredMessage(evalContext).contentEquals("This field can't be empty"));
    }

    @Test(expected = InvalidStructureException.class)
    public void testParseValueData_withRequiredAttributeAndNode()
            throws InvalidStructureException, XmlPullParserException,
            IOException, UnfullfilledRequirementsException {
        String query = "<prompt key=\"name\" required=\"true()\">"
                + "          <required test=\"true()\">"
                + "            <text>This field can't be empty</text>"
                + "          </required>"
                + "</prompt>";
        QueryPromptParser parser = ParserTestUtils.buildParser(query, QueryPromptParser.class);
        parser.parse();
    }

    @Test
    public void testParseListData() throws InvalidStructureException, XmlPullParserException, IOException {
        String query = "<data key=\"case_id_list\""
                + "nodeset=\"instance('selected-cases')/session-data/value\""
                + "exclude=\"count(instance('casedb')/casedb/case[@case_id = current()/.]) = 1\""
                + "ref=\".\"/>";
        QueryDataParser parser = ParserTestUtils.buildParser(query, QueryDataParser.class);
        QueryData queryData = parser.parse();
        assertEquals("case_id_list", queryData.getKey());

        Hashtable<String, DataInstance> instances = TestInstances.getInstances();
        EvaluationContext evalContext = new EvaluationContext(null, instances);
        assertEquals(Arrays.asList("456", "789"), queryData.getValues(evalContext));
    }

    @Test
    public void testParseListData_noExclude() throws InvalidStructureException, XmlPullParserException,
            IOException {
        String query = "<data key=\"case_id_list\""
                + "nodeset=\"instance('selected-cases')/session-data/value\""
                + "ref=\".\"/>";
        QueryDataParser parser = ParserTestUtils.buildParser(query, QueryDataParser.class);
        QueryData queryData = parser.parse();
        assertEquals("case_id_list", queryData.getKey());

        Hashtable<String, DataInstance> instances = TestInstances.getInstances();
        EvaluationContext evalContext = new EvaluationContext(null, instances);
        assertEquals(Arrays.asList("123", "456", "789"), queryData.getValues(evalContext));
    }

    @Test
    public void testParseQueryData_noRef() throws XmlPullParserException, IOException {
        String query = "<data key=\"case_id_list\"></data>";
        QueryDataParser parser = ParserTestUtils.buildParser(query, QueryDataParser.class);
        try {
            parser.parse();
            fail("Expected InvalidStructureException");
        } catch (InvalidStructureException ignored) {
        }
    }

    @Test
    public void testParseQueryData_badNesting() throws XmlPullParserException, IOException {
        String query = "<data key=\"case_id_list\" ref=\"true()\">"
                + "<data key=\"device_id\" ref=\"instance('session')/session/case_id\"/>"
                + "</data>";
        QueryDataParser parser = ParserTestUtils.buildParser(query, QueryDataParser.class);
        try {
            parser.parse();
            fail("Expected InvalidStructureException");
        } catch (InvalidStructureException ignored) {
        }
    }

    @Test
    public void testParseValueData_withGroupKeyAttribute()
            throws InvalidStructureException, XmlPullParserException,
            IOException, UnfullfilledRequirementsException {
        String query = "<prompt key=\"name\" group_key=\"group_header_1\"></prompt>";
        QueryPromptParser parser = ParserTestUtils.buildParser(query, QueryPromptParser.class);
        QueryPrompt queryData = parser.parse();
        assertEquals("group_header_1", queryData.getGroupKey());
    }

    @Test
    public void testParseValueData_withGroup()
            throws InvalidStructureException, XmlPullParserException,
            IOException, UnfullfilledRequirementsException {
        String query = "<group key=\"group_header_0\">"
                + "<display><text><locale id=\"search_property.m0.group_header_0\"/></text></display>"
                + "</group>";
        QueryGroupParser parser = ParserTestUtils.buildParser(query, QueryGroupParser.class);
        QueryGroup queryData = parser.parse();
        assertEquals("group_header_0", queryData.getKey());
    }
}
