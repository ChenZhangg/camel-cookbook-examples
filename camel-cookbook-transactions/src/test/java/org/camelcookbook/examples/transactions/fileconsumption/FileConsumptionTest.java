package org.camelcookbook.examples.transactions.fileconsumption;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * @author jkorab
 */
public class FileConsumptionTest extends CamelTestSupport {
    private static Logger log = LoggerFactory.getLogger(FileConsumptionTest.class);

    public static final String TARGET_TEMP = "target/temp/";
    public static final String TARGET_IN = "target/in/";
    public static final String TARGET_OUT = "target/out/";
    public static final String TARGET_ERRORS = "target/errors/";

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        createTargetDirectories();
        return new FileConsumptionRouteBuilder(TARGET_IN, TARGET_OUT, TARGET_ERRORS);
    }


    @Test
    public void testFileLocationOnError() throws IOException, InterruptedException {
        String text = "This message will explode";

        MockEndpoint mockExplosion = getMockEndpoint("mock:explosion");
        mockExplosion.expectedMessageCount(1);
        mockExplosion.message(0).body().isEqualTo(text);

        String fileName = "expectedToFail.txt";
        safelyWriteFile(fileName, text);
        Thread.sleep(2000); // give the route a bit of time to work

        assertMockEndpointsSatisfied();

        // check that the message never got to the output directory, and is in the errors directory
        assertTrue(!new File(TARGET_OUT + fileName).exists());
        assertTrue(new File(TARGET_ERRORS + fileName).exists());
    }

    @Test
    public void testFileLocationOnSuccess() throws IOException, InterruptedException {
        String fileName = "expectedToPass.txt";
        String text = "This message should be written out with no problems";

        MockEndpoint mockExplosion = getMockEndpoint("mock:explosion");
        mockExplosion.expectedMessageCount(0);

        safelyWriteFile(fileName, text);
        Thread.sleep(2000); // give the route a bit of time to work

        assertMockEndpointsSatisfied();

        // check that the message got to the output directory, and is not in the errors directory
        assertTrue(new File(TARGET_OUT + fileName).exists());
        assertTrue(!new File(TARGET_ERRORS + fileName).exists());
    }

    private void createTargetDirectories() {
        createIfNotExists(TARGET_TEMP);
        createIfNotExists(TARGET_IN);
        createIfNotExists(TARGET_OUT);
        createIfNotExists(TARGET_ERRORS);
    }

    private void createIfNotExists(String location) {
        File file = new File(location);
        if (file.exists()) {
            // delete it to make sure that any files from a previous run have been destroyed
            log.info("Deleting {}", file.getAbsolutePath());
            delete(file);
        }

        if (!file.mkdirs()) {
            throw new IllegalStateException("Could not create " + file.getAbsolutePath() + ". Check your directory permissions.");
        }
    }

    private void delete(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                delete(child);
            }
        }
        if (!file.delete()) {
            throw new IllegalStateException("Failed to delete file: " + file);
        }
    }

    private void safelyWriteFile(String fileName, String text) throws IOException {
        File outputFile = new File(TARGET_TEMP + fileName);
        log.info("Writing temporary file: {}", outputFile.getAbsolutePath());

        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
        writer.append(text);
        writer.close();

        // move the file - the Camel file consumer doesn't like files being written at the same time
        File destination = new File(TARGET_IN + fileName);
        log.info("Moving temporary file to: {}", destination.getAbsolutePath());
        outputFile.renameTo(destination);
    }

}
