package fi.peltodata.service;

import fi.nls.oskari.domain.User;
import fi.peltodata.domain.Farmfield;
import fi.peltodata.domain.FarmfieldFileDataType;

import java.nio.file.Path;

public class CropEstimationTask extends ExecutionTask {
    public CropEstimationTask(PeltodataService peltodataService, Farmfield farmfield, Path inputFilepath, Path outputFilePath, FarmfieldFileDataType outputType, User user) {
        super(peltodataService, farmfield, inputFilepath, outputFilePath, outputType, user);
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
