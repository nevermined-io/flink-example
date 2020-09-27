package io.keyko.nevermined.examples;

import io.keyko.nevermined.models.DID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TestDemoIT {

    @Test
    public void testDemoEndToEnd(@TempDir Path tempDir) throws Exception {
        Demo demo = new Demo();

        String executionId = demo.run();
        assertNotNull(executionId);

        DID didOutput = demo.monitorComputeJob(executionId);
        assertNotNull(didOutput);

        demo.download(didOutput, tempDir.toAbsolutePath().toString());
        String expectedDestinationPath = tempDir.toAbsolutePath().toString() + File.separator + "datafile."
                + didOutput.getHash() + ".0" + File.separator + "result.csv";
        assertTrue(new File(expectedDestinationPath).exists());
    }
}