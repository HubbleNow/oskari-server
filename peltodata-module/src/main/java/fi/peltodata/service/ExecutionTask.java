package fi.peltodata.service;

import fi.nls.oskari.domain.User;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.peltodata.domain.Farmfield;
import fi.peltodata.domain.FarmfieldExecution;
import fi.peltodata.domain.FarmfieldFileDataType;

import java.io.IOException;
import java.nio.file.Path;

public abstract class ExecutionTask implements Runnable {
    private static final Logger LOG = LogFactory.getLogger(CropEstimationTask.class);

    private PeltodataService peltodataService;
    private PeltodataOskariLayerService peltodataOskariLayerService;
    private Farmfield farmfield;
    private Path inputFilePath;
    private Path outputFilePath;
    private FarmfieldFileDataType outputType;
    private User user;

    public ExecutionTask(PeltodataService peltodataService, Farmfield farmfield, Path inputFilepath, Path outputFilePath, FarmfieldFileDataType outputType, User user) {
        this.peltodataService = peltodataService;
        this.farmfield = farmfield;
        this.inputFilePath = inputFilepath;
        this.outputFilePath = outputFilePath;
        this.outputType = outputType;
        this.user = user;
        this.peltodataOskariLayerService = new PeltodataOskariLayerServiceImpl();
    }

    abstract void createOutput() throws Exception;

    @Override
    public void run() {
        LOG.info("Starting execution " + inputFilePath + " " + outputFilePath);

        FarmfieldExecution execution = getPeltodataService().farmfieldExecutionStarted(farmfield, outputType.getTypeId());
        try {
            createOutput();
            getPeltodataService().farmfieldExecutionCompleted(execution);
            LOG.info("output created");
        } catch (Exception e) {
            LOG.error("execution failed", e);
            getPeltodataService().farmfieldExecutionFailed(execution);
            throw new RuntimeException("Failed to copy file", e);
        }

        try {
            String layerName = peltodataService.createFarmfieldGeoserverLayer(farmfield, outputFilePath, outputType.getTypeId());
            peltodataService.addWMSLayerFromGeoserver(farmfield, layerName, outputType, user);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
