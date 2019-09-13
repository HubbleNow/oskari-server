package fi.peltodata.service;

import fi.nls.oskari.domain.User;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.PropertyUtil;
import fi.peltodata.domain.Farmfield;
import fi.peltodata.domain.FarmfieldFileDataType;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class RawConversionTask extends ExecutionTask {
    private static final Logger LOG = LogFactory.getLogger(RawConversionTask.class);
    private static final String PROP_GDAL_COMMAND = "peltodata.gdal.convert_raw";

    public RawConversionTask(PeltodataService peltodataService, Farmfield farmfield, Path inputFilepath, Path outputFilePath, FarmfieldFileDataType outputType, User user) {
        super(peltodataService, farmfield, inputFilepath, outputFilePath, outputType, user);
    }


    private String getCommand() {
        String defaultCommand;
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        if (isWindows) {
            // Default for windows expects windows subsystem for linux and gdal-bin package installed
            defaultCommand = String.format("wsl gdal_translate -co NBITS=8 -co COMPRESS=JPEG -co TILED=yes `wslpath -a \"%s\"` `wslpath -a \"%s\"` -ot Byte -scale -a_nodata 0",
                    this.getInputFilePath().toString(), this.getOutputFilePath().toString());
        } else {
            // Linux expects only gdal-bin ofc
            defaultCommand = String.format("gdal_translate -co NBITS=8 -co COMPRESS=JPEG -co TILED=yes \"%s\" \"%s\" -ot Byte -scale -a_nodata 0",
                    this.getInputFilePath().toString(), this.getOutputFilePath().toString());
        }

        LOG.info("executing command ", defaultCommand);

        // Otherwise command can be configured in props
        return PropertyUtil.get(PROP_GDAL_COMMAND, defaultCommand);
    }

    @Override
    void createOutput() throws Exception {
        String command = this.getCommand();
        String[] commandParts = command.split(" ");

        ProcessBuilder processBuilder = new ProcessBuilder(commandParts);
        Process process = processBuilder.start();
        StringBuilder stringBuilder = new StringBuilder();
        StreamGobbler inputStreamGobbler = new StreamGobbler(process.getInputStream(), stringBuilder::append);
        StreamGobbler errorStreamGobbler = new StreamGobbler(process.getErrorStream(), stringBuilder::append);
        ExecutorService e = Executors.newSingleThreadExecutor();
        e.submit(inputStreamGobbler);
        e.submit(errorStreamGobbler);

        int exitCode = process.waitFor();
        e.shutdown();
        String resultOutput = stringBuilder.toString();
        if (exitCode != 0) {
            throw new RuntimeException("Failed to execute conversion task exitCode=" + exitCode + " output:" + resultOutput);
        }
    }

    private static class StreamGobbler implements Runnable {
        private InputStream inputStream;
        private Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines()
                    .forEach(consumer);
        }
    }
}
