package fi.peltodata.service;

import fi.nls.oskari.util.PropertyUtil;
import fi.peltodata.config.PeltodataConfig;
import fi.peltodata.domain.FarmfieldFileDataType;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class FileService {
    public static String getUploadRootPath() {
        String uploadRootDir = PropertyUtil.get(PeltodataConfig.PROP_UPLOAD_ROOT_DIR_PATH, "." + File.separator + "geoserver_data");
        uploadRootDir += File.separator + "farms";
        return uploadRootDir;
    }
    public static String getFarmUploadRootPath(long farmfieldId) {
        String farmUploadPath = getUploadRootPath()
                + File.separator + farmfieldId
                + File.separator;
        return farmUploadPath;
    }

    public static String getUploadPath(int year, long farmfieldId, FarmfieldFileDataType dataType) {
        String farmUploadPath = getFarmUploadRootPath(farmfieldId);
        String uploadPath = farmUploadPath
                + year
                + File.separator;
        return uploadPath;
    }

    public static String getRelativeFarmfieldFilePath(Path fullPath) {
        return fullPath.getParent().getParent().toFile().getName()
                + File.separator + fullPath.getParent().toFile().getName()
                + File.separator + fullPath.getFileName();
    }

    public static Path getFullPathForInputFile(String inputFilepath) {
        String fullPath = getUploadRootPath() + File.separator + inputFilepath;
        Path absolutePath = FileSystems.getDefault().getPath(fullPath).normalize().toAbsolutePath();
        return absolutePath;
    }

    public static boolean fileExists(String filePathString) {
        File f = new File(filePathString);
        return f.exists() && !f.isDirectory();
    }

    public static boolean fileExists(long farmfieldId, FarmfieldFileDataType dataType, String filename) {
        String fileTimestamp = filename.split(".")[0];
        int year = LocalDateTime.parse(fileTimestamp).getYear();
        String uploadPath = FileService.getUploadPath(year, farmfieldId, dataType);
        String filePathString = uploadPath + filename;
        return fileExists(filePathString);
    }
}
