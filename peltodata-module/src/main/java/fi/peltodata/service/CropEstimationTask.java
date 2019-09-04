package fi.peltodata.service;

import com.google.common.io.Files;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.peltodata.domain.Farmfield;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class CropEstimationTask implements Runnable {
    private static final Logger LOG = LogFactory.getLogger(CropEstimationTask.class);

    private PeltodataService peltodataService;
    private Farmfield farmfield;
    private Path inputFilePath;
    private Path outputFilePath;

    public CropEstimationTask(PeltodataService peltodataService, Farmfield farmfield, Path inputFilepath, Path outputFilePath) {
        this.peltodataService = peltodataService;
        this.farmfield = farmfield;
        this.inputFilePath = inputFilepath;
        this.outputFilePath = outputFilePath;
    }

    @Override
    public void run() {
        LOG.info("Starting to create crop estimation from " + inputFilePath);
        try {
            // Fake delay
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        LOG.info("Crop estimation created output={}", outputFilePath.toString());
        try {
            java.nio.file.Files.copy(inputFilePath, outputFilePath);
        } catch (IOException e) {
            LOG.error("Failed to create yield data image");
            throw new RuntimeException("Failed to copy file", e);
        }

        peltodataService.createFarmfieldGeoserverLayer(farmfield, outputFilePath);
    }
}
