package fi.peltodata.service;

import fi.peltodata.domain.Farmfield;

import java.nio.file.Path;

public class CropEstimationTask extends ExecutionTask {

    public CropEstimationTask(PeltodataService peltodataService, Farmfield farmfield, Path inputFilepath, Path outputFilePath, String outputType) {
        super(peltodataService, farmfield, inputFilepath, outputFilePath, outputType);
    }

    @Override
    void createOutput() throws Exception {
        try {
            // Fake delay
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        java.nio.file.Files.copy(getInputFilePath(), getOutputFilePath());
    }
}
