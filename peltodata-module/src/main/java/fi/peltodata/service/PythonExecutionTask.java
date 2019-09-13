package fi.peltodata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.io.Resources;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.PropertyUtil;
import fi.peltodata.config.PeltodataConfig;
import fi.peltodata.domain.Farmfield;
import fi.peltodata.domain.FarmfieldFile;
import fi.peltodata.domain.FarmfieldFileDataType;

import java.io.*;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PythonExecutionTask extends ExecutionTask {

    String jsonFolder = PropertyUtil.get(PeltodataConfig.PROP_UPLOAD_JSON_DIR_PATH, "." + File.separator + "peltodata_json");

    private static final Logger LOG = LogFactory.getLogger(RawConversionTask.class);
    private String key;

    public PythonExecutionTask(PeltodataService peltodataService, Farmfield farmfield, FarmfieldFile farmfieldFile, Path outputFilePath, FarmfieldFileDataType outputType, User user, String key) {
        super(peltodataService, farmfield, farmfieldFile, outputFilePath, outputType, user);
        this.key = key;
    }

    void copyDevTiff() {
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

    @Override
    void createOutput() throws Exception {
        String pythonFile = PropertyUtil.getNecessary(key);

        if (pythonFile.equals("dev")) {
            copyDevTiff();
            return;
        }

        Path pythonPath = Paths.get(pythonFile);
        if (!Files.exists(pythonPath)) {
            throw new RuntimeException("File missing " + pythonFile);
        }

        String jsonFile = generateJsonFile(getFarmfield(), getFarmfieldFile());

        String[] commandParts = new String[] { "python", pythonFile, jsonFile };
        LOG.info("Trying to execute python script {}, {}", new Object[]{pythonFile, jsonFile});

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

    private String generateJsonFile(Farmfield farmfield, FarmfieldFile farmfieldFile) throws IOException {
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put("id", farmfield.getId());
        jsonData.put("farm_id", farmfield.getFarmId());
        jsonData.put("crop_type", farmfield.getCropType());
        jsonData.put("sowing_date", farmfield.getSowingDate());
        jsonData.put("description", farmfield.getDescription());
        jsonData.put("input_file", farmfieldFile.getFullPath());
        jsonData.put("output_file", getOutputFilePath().toString());
        jsonData.put("file_date", farmfieldFile.getFileDate());

        Path jsonFile = Paths.get(jsonFolder, farmfieldFile.getId() + "_" + new Date().getTime() + ".json");
        Files.createDirectories(jsonFile.getParent());

        objectMapper().writeValue(jsonFile.toFile(), jsonData);

        Path absolutePath = FileSystems.getDefault().getPath(jsonFile.toString()).normalize().toAbsolutePath();
        return absolutePath.toString();
    }

    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
