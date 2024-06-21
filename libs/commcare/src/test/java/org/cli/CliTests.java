package org.cli;

import org.commcare.util.cli.ApplicationHost;
import org.commcare.util.cli.CliCommand;
import org.commcare.util.engine.CommCareConfigEngine;
import org.commcare.util.screen.SessionUtils;
import org.javarosa.core.util.externalizable.LivePrototypeFactory;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import static junit.framework.TestCase.assertTrue;

/**
 *
 * Tests for the CommCare CLI
 *
 * Uses a specific, highly paired format to deal with the CLI's I/O
 *
 * @author wpride
 */

public class CliTests {

    private class CliTestRun<E extends CliTestReader> {

        CliTestRun(String applicationPath,
                String restoreResource,
                CliStepProcessor processor,
                String steps,
                String endpointId,
                String[] endpointArgs,
                boolean debug, SessionUtils sessionUtils) {
            ApplicationHost host = buildApplicationHost(
                    applicationPath, restoreResource, processor, steps, debug, sessionUtils);
            boolean passed = false;
            try {
                host.run(endpointId, endpointArgs);
            } catch (TestPassException e) {
                passed = true;
            }
            assertTrue(passed);
        }

        private ApplicationHost buildApplicationHost(
                String applicationResource,
                String restoreResource,
                CliStepProcessor processor,
                String steps,
                boolean debug,
                SessionUtils sessionUtils) {
            ClassLoader classLoader = getClass().getClassLoader();
            String applicationPath = new File(classLoader.getResource(applicationResource).getFile()).getAbsolutePath();
            PrototypeFactory prototypeFactory = new LivePrototypeFactory();

            CommCareConfigEngine engine = CliCommand.configureApp(applicationPath, prototypeFactory);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream outStream = new PrintStream(baos);

            CliTestReader reader = new CliTestReader(steps, baos, processor);
            reader.setDebug(debug);

            ApplicationHost host = new ApplicationHost(engine, prototypeFactory, reader, outStream);
            host.setUsernamePassword("test", "test");
            if (sessionUtils == null) {
                sessionUtils = new MockSessionUtils();
            }
            host.setSessionUtils(sessionUtils);
            File restoreFile = new File(classLoader.getResource(restoreResource).getFile());
            String restorePath = restoreFile.getAbsolutePath();
            host.setRestoreToLocalFile(restorePath);
            return host;
        }
    }

    @Test
    public void testConstraintsForm() throws Exception {
        // Start a basic form
        new CliTestRun<>("basic_app/basic.ccz",
                "case_create_basic.xml",
                new BasicTestReader(),
                "1 0 \n",
                null,
                null, false, null);
    }

    @Test
    public void testCaseSelection() throws Exception {
        // Perform case selection
        new CliTestRun<>("basic_app/basic.ccz",
                "basic_app/restore.xml",
                new CaseTestReader(),
                "2 1 5 1 \n \n",
                null,
                null, false, null);
    }

    @Test
    public void testSessionEndpoint() throws Exception {
        // Run CLI with session endpoint arg
        new CliTestRun<>("basic_app/basic.ccz",
                "basic_app/restore.xml",
                new SessionEndpointTestReader(),
                "\n",
                "m5_endpoint",
                new String[] {"124938b2-c228-4107-b7e6-31a905c3f4ff"}, false, null);
    }

    @Test
    public void testEntryWithPost_multipleEntriesInMenu() throws Exception {
        // Run CLI with session endpoint arg
        new CliTestRun<>("session-tests-template/profile.ccpr",
                "session-tests-template/user_restore.xml",
                new PostTestReader(),
                "2 0 \n 2",
                null,
                null, false, null);
    }

    @Test
    public void testEntryWithPost_singleEntriesInMenu() throws Exception {
        // Run CLI with session endpoint arg
        new CliTestRun<>("session-tests-template/profile.ccpr",
                "session-tests-template/user_restore.xml",
                new PostTestReader(),
                "3 0 \n 0",
                null,
                null, false, null);
    }

    @Test
    public void testMultiSelectCaseList() throws Exception {
        CliStepProcessor processor = (stepIndex, output) -> {
            switch(stepIndex) {
                case 0:
                    Assert.assertTrue(output.contains("4) Multi select case list"));
                    break;
                case 1:
                    Assert.assertTrue(output.contains("0) Name"));
                    break;
                case 2:
                    Assert.assertTrue(output.contains("0) Jack"));
                    Assert.assertTrue(output.contains("1) Lucy"));
                    break;
                case 3:
                    Assert.assertTrue(output.contains("0) multi-select form with auto-launch case list"));
                    throw new TestPassException();
            }
        };
        MockSessionUtils sessionUtils = new MockSessionUtils(this.getClass().getResourceAsStream("/session-tests-template/query_response.xml"));
        new CliTestRun<>("session-tests-template/profile.ccpr",
                "session-tests-template/user_restore.xml",
                processor,
                "4 name 0,1",
                null,
                null, true, sessionUtils);
    }

    static interface CliStepProcessor {
        void processLine(int stepIndex, String output);
    }


    /**
     * The CliTestReader overrides the Reader (usually System.in) passed into the CLI
     * and so is able to provide input through the readLine() function that the CLI
     * reads from. We are also able to get the output at this point and make assertions
     * about its content.
     */
    static class CliTestReader extends BufferedReader {

        private final CliStepProcessor processor;
        private String[] steps;
        private int stepIndex;
        private ByteArrayOutputStream outStream;

        private boolean debug = false;

        CliTestReader(String steps, ByteArrayOutputStream outStream, CliStepProcessor processor) {
            super(new StringReader("Unused dummy reader"));
            this.steps = steps.split(" ");
            this.outStream = outStream;
            this.processor = processor;
        }

        @Override
        public String readLine() throws IOException {
            String output = new String(outStream.toByteArray(), StandardCharsets.UTF_8);
            if (debug) {
                System.out.println(output);
            }
            processLine(stepIndex, output);
            if (stepIndex >= steps.length) {
                throw new RuntimeException("Insufficient steps");
            }
            String ret = steps[stepIndex++];
            if (debug) {
                System.out.println("Input: " + (ret.equals("\n") ? "[enter]" : ret));
            }
            outStream.reset();
            // Return the next input for the CLI to process
            return ret;
        }

        public void setDebug(boolean debug) {
            this.debug = debug;
        }
        private void processLine(int stepIndex, String output) {
            this.processor.processLine(stepIndex, output);
        }
    }

    static class BasicTestReader implements CliStepProcessor {
        public void processLine(int stepIndex, String output) {
            switch(stepIndex) {
                case 0:
                    Assert.assertTrue(output.contains("Basic Tests"));
                    Assert.assertTrue(output.contains("0) Basic Form Tests"));
                    break;
                case 1:
                    Assert.assertTrue(output.contains("0) Constraints"));
                    break;
                case 2:
                    Assert.assertTrue(output.contains("Press Return to proceed"));
                    break;
                case 3:
                    Assert.assertTrue(output.contains("This form tests different logic constraints."));
                    throw new TestPassException();
                default:
                    throw new RuntimeException(String.format("Did not recognize output %s at stepIndex %s", output, stepIndex));
            }
        }
    }

    static class CaseTestReader implements CliStepProcessor {
        public void processLine(int stepIndex, String output) {
            switch(stepIndex) {
                case 0:
                    Assert.assertTrue(output.contains("Basic Tests"));
                    Assert.assertTrue(output.contains("0) Basic Form Tests"));
                    break;
                case 1:
                    Assert.assertTrue(output.contains("0) Create a Case"));
                    break;
                case 2:
                    // m2_case_short
                    Assert.assertTrue(output.contains("Case | vl1"));
                    Assert.assertTrue(output.contains("Date Opened"));
                    Assert.assertTrue(output.contains("case one"));
                    break;
                case 3:
                    // Tab 0 of m2_case_long
                    Assert.assertTrue(output.contains("Phone Number"));
                    Assert.assertTrue(output.contains("9632580741"));
                    break;
                case 4:
                    // Tab 1 of m2_case_long
                    Assert.assertTrue(output.contains("Geodata"));
                    Assert.assertTrue(output.contains("17.4469641 78.3719456 543.4 24.36"));
                    break;
                case 5:
                    Assert.assertTrue(output.contains("Form Start: Press Return to proceed"));
                    break;
                case 6:
                    Assert.assertTrue(output.contains("This form will allow you to add and update"));
                    throw new TestPassException();
                default:
                    throw new RuntimeException(String.format("Did not recognize output %s at stepIndex %s", output, stepIndex));

            }
        }
    }

    static class SessionEndpointTestReader implements CliStepProcessor {
        public void processLine(int stepIndex, String output) {
            switch(stepIndex) {
                case 0:
                    Assert.assertTrue(output.contains("0) Update a Case"));
                    Assert.assertTrue(output.contains("1) Close a Case"));
                    throw new TestPassException();
                default:
                    throw new RuntimeException(String.format("Did not recognize output %s at stepIndex %s", output, stepIndex));

            }
        }
    }

    static class PostTestReader implements CliStepProcessor {
        public void processLine(int stepIndex, String output) {
            switch (stepIndex) {
                case 0:
                    Assert.assertTrue(output.contains("test [36]"));
                    Assert.assertTrue(output.contains("2) Module 2"));
                    break;
                case 1:
                    // Tab 0 of case_short
                    Assert.assertTrue(output.contains("0) Test Case 1"));
                    break;
                case 2:
                    // Tab 0 of case_long
                    Assert.assertTrue(output.contains("Case | test"));
                    Assert.assertTrue(output.contains("name"));
                    Assert.assertTrue(output.contains("Test Case 1"));
                    break;
                case 3:
                    Assert.assertTrue(output.contains("Module 2 Form 2"));
                    break;
                case 4:
                    Assert.assertTrue(output.contains("Sync complete, press Enter to continue"));
                    throw new TestPassException();
                default:
                    throw new RuntimeException(
                            String.format("Did not recognize output %s at stepIndex %s", output, stepIndex));

            }
        }
    }

    // Because the CLI is a REPL that will loop indefinitely unless certain code paths are
    // reached we need to provide a way for tests to exit early. This exception will be
    // caught at the top level of the CliTestRun and set the tests to pass when thrown.
    private static class TestPassException extends RuntimeException {}

}
