package fi.peltodata.service;

import com.google.common.io.Resources;
import fi.peltodata.domain.Farmfield;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;

public class YieldImageTask extends ExecutionTask {
    public YieldImageTask(PeltodataService peltodataService, Farmfield farmfield, Path inputFilepath, Path outputFilePath, String outputType) {
        super(peltodataService, farmfield, inputFilepath, outputFilePath, outputType);
    }

    @Override
    void createOutput() throws Exception {
        try {
            // Fake delay
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try(OutputStream os = java.nio.file.Files.newOutputStream(getOutputFilePath())) {
            URL url = Resources.getResource("fi/peltodata/service/bogota.tif");
            Resources.copy(url, os);
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy file", e);
        }
    }
}
