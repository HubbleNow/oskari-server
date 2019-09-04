package fi.peltodata.service;

import com.google.common.io.Files;
import com.google.common.io.Resources;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.peltodata.domain.Farmfield;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class YieldImageTask implements Runnable {
    private static final Logger LOG = LogFactory.getLogger(YieldImageTask.class);

    private final PeltodataService peltodataService;
    private final Farmfield farmfield;
    private final Path inputFilePath;
    private Path outputFilePath;

    public YieldImageTask(PeltodataService peltodataService, Farmfield farmfield, Path inputFilePath, Path outputFilePath) {
        this.peltodataService = peltodataService;
        this.farmfield = farmfield;
        this.inputFilePath = inputFilePath;
        this.outputFilePath = outputFilePath;
    }

    @Override
    public void run() {
        // This is a fake implementation of javaclass
        LOG.info("Starting to yield layer from " + inputFilePath);
        try {
            // Fake delay
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        LOG.info("yield layer created output={}", outputFilePath.toString());

        try(OutputStream os = java.nio.file.Files.newOutputStream(outputFilePath)) {
            URL url = Resources.getResource("fi/peltodata/service/bogota.tif");
            Resources.copy(url, os);
        } catch (IOException e) {
            LOG.error("Failed to create yield data image", e);
            throw new RuntimeException("Failed to copy file", e);
        }

        peltodataService.createFarmfieldGeoserverLayer(farmfield, outputFilePath);
    }
}
