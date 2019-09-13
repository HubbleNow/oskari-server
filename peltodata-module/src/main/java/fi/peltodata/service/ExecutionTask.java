package fi.peltodata.service;

import fi.nls.oskari.domain.User;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.peltodata.domain.Farmfield;
import fi.peltodata.domain.FarmfieldExecution;
import fi.peltodata.domain.FarmfieldFile;
import fi.peltodata.domain.FarmfieldFileDataType;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class ExecutionTask implements Runnable {
    private static final Logger LOG = LogFactory.getLogger(ExecutionTask.class);

    private PeltodataService peltodataService;
    private PeltodataOskariLayerService peltodataOskariLayerService;
    private Farmfield farmfield;
    private FarmfieldFile farmfieldFile;
    private Path outputFilePath;
    private FarmfieldFileDataType outputType;
    private User user;

    public ExecutionTask(PeltodataService peltodataService, Farmfield farmfield, FarmfieldFile farmfieldFile, Path outputFilePath, FarmfieldFileDataType outputType, User user) {
        this.peltodataService = peltodataService;
        this.farmfield = farmfield;
        this.farmfieldFile = farmfieldFile;
        this.outputFilePath = outputFilePath;
        this.outputType = outputType;
        this.user = user;
        this.peltodataOskariLayerService = new PeltodataOskariLayerServiceImpl();
    }

    abstract void createOutput() throws Exception;

    @Override
    public void run() {
        LOG.info("Starting execution " + farmfieldFile.getFullPath() + " " + outputFilePath);

        FarmfieldExecution execution = getPeltodataService().farmfieldExecutionStarted(farmfieldFile, outputFilePath, outputType.getTypeId());
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
            peltodataService.addWMSLayerFromGeoserver(farmfield, farmfieldFile, layerName, outputType, user);
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
        return Paths.get(farmfieldFile.getFullPath());
    }

    public Path getOutputFilePath() {
        return outputFilePath;
    }

    public FarmfieldFile getFarmfieldFile() {
        return farmfieldFile;
    }
}
