package org.javarosa.core.model.test;

import org.javarosa.core.test.FormParseInit;
import org.javarosa.xform.parse.XFormParseException;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for cyclic references
 *
 * @author wpride
 */

public class CyclicReferenceTests {
    /**
     * Test that XPath cyclic reference that references parent throws usable error
     */
    @Test
    public void testCyclicReferenceWithGroup() {
        try {
            new FormParseInit("/xform_tests/group_cyclic_reference.xml");
        } catch (XFormParseException e) {
            String detailMessage = e.getMessage();
            // Assert that we're using the shortest cycle algorithm
            assertTrue(detailMessage.contains("Logic is cyclical"));
            // There should only be three newlines since only the three core cyclic references were included
            int newlineCount = detailMessage.length() - detailMessage.replace("\n", "").length();
            assertTrue(newlineCount == 3);
            return;
        }
        fail("Cyclical reference did not throw XFormParseException");
    }

    /**
     * Test that XPath cyclic reference that references parent throws usable error
     */
    @Test
    public void testCyclicalReferenceRegression() {
        testCyclicReferenceForPath("/xform_tests/real_form_with_cycle_errors.xml", 4);
        testCyclicReferenceForPath("/xform_tests/real_form2_with_cycle_errors.xml", 2);

    }

    private void testCyclicReferenceForPath(String formPath, int numberOfCyclicReferences) {
        try {
            new FormParseInit(formPath);
        } catch (XFormParseException e) {
            String detailMessage = e.getMessage();
            // Assert that we're using the shortest cycle algorithm
            assertTrue(detailMessage.contains("Logic is cyclical"));
            // number of newlines should be equal to number of core cyclic references were included
            int newlineCount = detailMessage.length() - detailMessage.replace("\n", "").length();
            assertTrue(newlineCount == numberOfCyclicReferences);
            return;
        }
        fail("Cyclical reference did not throw XFormParseException");
    }
}
