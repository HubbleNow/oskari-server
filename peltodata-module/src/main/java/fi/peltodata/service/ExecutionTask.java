package fi.peltodata.service;

import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.peltodata.domain.Farmfield;
import fi.peltodata.domain.FarmfieldExecution;

import java.io.IOException;
import java.nio.file.Path;

public abstract class ExecutionTask implements Runnable {
    private static final Logger LOG = LogFactory.getLogger(CropEstimationTask.class);

    private PeltodataService peltodataService;
    private Farmfield farmfield;
    private Path inputFilePath;
    private Path outputFilePath;
    private String outputType;

    public ExecutionTask(PeltodataService peltodataService, Farmfield farmfield, Path inputFilepath, Path outputFilePath, String outputType) {
        this.peltodataService = peltodataService;
        this.farmfield = farmfield;
        this.inputFilePath = inputFilepath;
        this.outputFilePath = outputFilePath;
        this.outputType = outputType;
    }

    abstract void createOutput() throws Exception;

    @Override
    public void run() {
        LOG.info("Starting execution " + inputFilePath + " " + outputFilePath);

        FarmfieldExecution execution = getPeltodataService().farmfieldExecutionStarted(farmfield, outputType);
        try {
            createOutput();
            getPeltodataService().farmfieldExecutionCompleted(execution);
            LOG.info("output created");
        } catch (Exception e) {
            LOG.error("execution failed", e);
            getPeltodataService().farmfieldExecutionFailed(execution);
            throw new RuntimeException("Failed to copy file", e);
        }

        peltodataService.createFarmfieldGeoserverLayer(farmfield, outputFilePath);
    }

    public PeltodataService getPeltodataService() {
        return peltodataService;
    }

    public Farmfield getFarmfield() {
        return farmfield;
    }

    public Path getInputFilePath() {
        return inputFilePath;
    }

    public Path getOutputFilePath() {
        return outputFilePath;
    }
}
