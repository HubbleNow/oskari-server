package fi.peltodata.controller;

import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.spring.extension.OskariParam;
import fi.peltodata.controller.request.UserFarmfieldCreateRequest;
import fi.peltodata.controller.request.UserFarmfieldUpdateRequest;
import fi.peltodata.controller.response.UserFarmfieldResponse;
import fi.peltodata.domain.Farmfield;
import fi.peltodata.domain.FarmfieldFileDataType;
import fi.peltodata.service.PeltodataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("peltodata/api")
public class PeltodataController {
    private static final Logger LOG = LogFactory.getLogger(PeltodataController.class);

    @Autowired
    private PeltodataService peltodataService;

    @RequestMapping("info")
    public @ResponseBody String info() {
        return "peltodata-REST-API";
    }

    @RequestMapping(value = "auth-test", method = RequestMethod.GET)
    public String secure(Model model, @OskariParam ActionParameters params) {
        User user = params.getUser();
        boolean isGuest = user.isGuest();
        if (isGuest) {
            return "peltodata_auth_error";
        } else {
            return "peltodata_error";
        }
    }

    @RequestMapping(value = "farms", method = RequestMethod.GET)
    public @ResponseBody List<UserFarmfieldResponse> findAllByUser(@OskariParam ActionParameters params) {
        User user = params.getUser();
        boolean isGuest = user.isGuest();
        if (isGuest) {
            return Collections.emptyList();
        } else if (user.isAdmin()) {
            List<Farmfield> allFarmfields = peltodataService.findAllFarmfields();
            return allFarmfields.stream().map(UserFarmfieldResponse::new).collect(Collectors.toList());
        } else {
            long userId = user.getId();
            List<Farmfield> allByUser = peltodataService.findAllFarmfieldsByUser(userId);
            return allByUser.stream().map(UserFarmfieldResponse::new).collect(Collectors.toList());
        }
    }

    @RequestMapping(value = "farms", method = RequestMethod.POST)
    public @ResponseBody UserFarmfieldResponse addNewFarmField(@RequestBody UserFarmfieldCreateRequest requestData,
                                                               @OskariParam ActionParameters params) {
        User user = params.getUser();
        boolean isGuest = user.isGuest();
        if (isGuest) {
            return null;
        } else if (user.isAdmin()) {
            Farmfield farmfield = new Farmfield();
            if (requestData.getUserId() != null) {
                //admin can create behalf of
                farmfield.setUserId(requestData.getUserId());
            } else {
                farmfield.setUserId(user.getId());
            }
            farmfield.setSowingDate(requestData.getFarmfieldSowingDate());
            farmfield.setCropType(requestData.getFarmfieldCropType());
            farmfield.setDescription(requestData.getFarmfieldDescription());
            peltodataService.insertFarmfield(farmfield);
            return new UserFarmfieldResponse(farmfield);
        } else {
            long userId = user.getId();
            Farmfield farmfield = new Farmfield();
            farmfield.setDescription(requestData.getFarmfieldDescription());
            farmfield.setSowingDate(requestData.getFarmfieldSowingDate());
            farmfield.setCropType(requestData.getFarmfieldCropType());
            farmfield.setUserId(userId);
            peltodataService.insertFarmfield(farmfield);
            return new UserFarmfieldResponse(farmfield);
        }
    }

    @RequestMapping(value = "farms/{id}", method = RequestMethod.POST)
    public @ResponseBody UserFarmfieldResponse updateFarmfield(@PathVariable("id") Long farmFieldId,
                                                               @RequestBody UserFarmfieldUpdateRequest requestData,
                                                               @OskariParam ActionParameters params) {
        User user = params.getUser();
        boolean isGuest = user.isGuest();
        if (isGuest) {
            return null;
        } else if (user.isAdmin()) {
            Farmfield farmfield = new Farmfield();
            farmfield.setId(farmFieldId);
            if (requestData.getUserId() != null) {
                //admin can create behalf of
                farmfield.setUserId(requestData.getUserId());
            } else {
                farmfield.setUserId(user.getId());
            }
            farmfield.setDescription(requestData.getFarmfieldDescription());
            farmfield.setSowingDate(requestData.getFarmfieldSowingDate());
            farmfield.setCropType(requestData.getFarmfieldCropType());
            peltodataService.updateFarmfield(farmfield);
            return new UserFarmfieldResponse(farmfield);
        } else {
            long userId = user.getId();
            Farmfield farmfield = new Farmfield();
            boolean allowed = peltodataService.farmfieldBelongsToUser(farmFieldId, userId);
            if (!allowed) {
                throw new AccessDeniedException("update not allowed");
            }
            farmfield.setId(farmFieldId);
            farmfield.setDescription(requestData.getFarmfieldDescription());
            farmfield.setSowingDate(requestData.getFarmfieldSowingDate());
            farmfield.setCropType(requestData.getFarmfieldCropType());
            farmfield.setUserId(userId);
            peltodataService.updateFarmfield(farmfield);
            return new UserFarmfieldResponse(farmfield);
        }
    }

    @RequestMapping(value = "farms/{id}", method = RequestMethod.GET)
    public @ResponseBody UserFarmfieldResponse getFarmfield(@PathVariable("id") Long farmFieldId,
                                                            @OskariParam ActionParameters params,
                                                            HttpServletResponse response) {
        User user = params.getUser();
        boolean isGuest = user.isGuest();
        if (isGuest) {
            return null;
        } else {
            long userId = user.getId();
            boolean allowed = peltodataService.farmfieldBelongsToUser(farmFieldId, userId);
            if (!allowed && !user.isAdmin()) {
                // 404, do not reveal existing id
                response.setStatus(HttpStatus.NOT_FOUND.value());
            }
            Farmfield farmfield = peltodataService.findFarmfield(farmFieldId);
            if (farmfield == null) {
                return null;
            }
            return new UserFarmfieldResponse(farmfield);
        }
    }

    @RequestMapping(value = "farms/{id}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteFarmfield(@PathVariable("id") Long farmFieldId, @OskariParam ActionParameters params) {
        User user = params.getUser();
        boolean isGuest = user.isGuest();
        if (isGuest) {
            throw new AccessDeniedException("delete not allowed");
        } else {
            long userId = user.getId();
            boolean allowed = peltodataService.farmfieldBelongsToUser(farmFieldId, userId);
            if (!allowed && !user.isAdmin()) {
                throw new AccessDeniedException("delete not allowed");
            }
            peltodataService.deleteFarmfield(farmFieldId);
        }
    }

    @RequestMapping(value = "farms/{id}/file", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public String uploadFarmfieldLayerDataFile(@PathVariable("id") Long farmFieldId,
                                               @RequestParam(value = "type", required = true) String type,
                                               @RequestPart("file") MultipartFile file,
                                               @OskariParam ActionParameters params,
                                               HttpServletRequest request,
                                               HttpServletResponse response) throws IOException {
        User user = params.getUser();
        boolean isGuest = user.isGuest();
        if (isGuest) {
            throw new AccessDeniedException("upload not allowed");
        } else {
            long userId = user.getId();
            boolean allowed = peltodataService.farmfieldBelongsToUser(farmFieldId, userId);
            if (!allowed && !user.isAdmin()) {
                throw new AccessDeniedException("upload not allowed");
            }
            FarmfieldFileDataType dataType = FarmfieldFileDataType.fromString(type);
            if (dataType == null) {
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                return null;
            }
            //for now timestamp from upload moment
            String filename = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "." + dataType.getDataFormat();
            String filePathString = null;
            try (InputStream in = file.getInputStream()){
                filePathString = peltodataService.uploadLayerData(farmFieldId, in, dataType, filename);
            }
            if (filePathString == null) {
                response.setStatus(HttpStatus.BAD_REQUEST.value());
            }
            return filePathString;
        }
    }

    @RequestMapping(value = "farms/{id}/file", method = RequestMethod.GET)
    @ResponseBody
    public List<String> getFarmfieldLayerFiles(@PathVariable("id") Long farmFieldId,
                                               @OskariParam ActionParameters params) {
        User user = params.getUser();
        boolean isGuest = user.isGuest();
        if (isGuest) {
            throw new AccessDeniedException("files fetch not allowed");
        } else {
            long userId = user.getId();
            boolean allowed = peltodataService.farmfieldBelongsToUser(farmFieldId, userId);
            if (!allowed && !user.isAdmin()) {
                throw new AccessDeniedException("files fetch not allowed");
            }
            return peltodataService.findAllFarmfieldFiles(farmFieldId);
        }
    }

    @RequestMapping(value = "farms/{id}/layer", method = RequestMethod.POST)
    @ResponseBody
    public String createFarmfieldOskariLayer(@PathVariable("id") Long farmFieldId,
                                             @OskariParam ActionParameters params,
                                             @RequestParam(value = "filename", required = true) String inputFilename,
                                             @RequestParam(value = "type", required = false) String outputType,
                                             HttpServletResponse response) {
        User user = params.getUser();
        boolean isGuest = user.isGuest();
        if (isGuest) {
            throw new AccessDeniedException("files fetch not allowed");
        } else {
            long userId = user.getId();
            boolean allowed = peltodataService.farmfieldBelongsToUser(farmFieldId, userId);
            if (!allowed && !user.isAdmin() || (inputFilename != null && inputFilename.contains(".."))) {
                LOG.error("files fetch not allowed farmFieldId {}, filename {}", new Object[] { farmFieldId, inputFilename });
                throw new AccessDeniedException("files fetch not allowed");
            }
            FarmfieldFileDataType outputDataType;
            FarmfieldFileDataType inputDataType = FarmfieldFileDataType.fromPathString(inputFilename);
            if (inputDataType == null) {
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                LOG.error("inputDataType was not specified, perhaps no folder in filename??. Given filename {}", new Object[] { inputFilename });
                throw new RuntimeException("inputDataType was not specified");
            }
            if (outputType == null) {
                // if output data is not explicitly defined then it is regarded as self
                outputDataType = inputDataType;
            } else {
                outputDataType = FarmfieldFileDataType.fromString(outputType);
                boolean allowedOutputType = inputDataType.isAllowedOutputTypeId(outputDataType);
                if (!allowedOutputType) {
                    response.setStatus(HttpStatus.BAD_REQUEST.value());
                    LOG.error("outputDataType was not allowed. Given output {}, allowed {}", new Object[] { outputDataType.getTypeId(), inputDataType.getAllowedOutputTypeIds() });
                    throw new RuntimeException("outputDataType was not allowed");
                }
            }
            List<String> allFarmfieldFiles = peltodataService.findAllFarmfieldFiles(farmFieldId);
            Optional<String> match = allFarmfieldFiles.stream().filter(f -> Paths.get(inputFilename).toString().equals(f)).findFirst();
            if (!match.isPresent() || outputDataType == null) {
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                LOG.error("outputDataType null or no existing file found. inputFilename {}, outputDataType == null {}", new Object[] { inputFilename, outputDataType == null });
                throw new RuntimeException("outputDataType null or no existing file found");
            } else {
                String inputFilenamePath = match.get();
                //TODO: rename to createGeoserverLayer
                peltodataService.createFarmfieldLayer(farmFieldId, inputFilenamePath, inputDataType, outputDataType);
                return "";
                //TODO: consider oskarilayer savehandler call in addition???
            }
        }
    }

    @RequestMapping(value = "farms/datatypes", method = RequestMethod.GET)
    @ResponseBody
    public List<String> getFarmfieldLayerFileDataTypes() {
        return EnumSet.allOf(FarmfieldFileDataType.class)
                .stream().sorted().map(t -> t.getTypeId()).collect(Collectors.toList());
    }
}
