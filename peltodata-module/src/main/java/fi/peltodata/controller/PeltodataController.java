package fi.peltodata.controller;

import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.spring.extension.OskariParam;
import fi.peltodata.controller.request.FarmfieldFileDateUpdateRequest;
import fi.peltodata.controller.request.UserFarmfieldCreateRequest;
import fi.peltodata.controller.request.UserFarmfieldUpdateRequest;
import fi.peltodata.controller.response.UserFarmfieldResponse;
import fi.peltodata.domain.Farmfield;
import fi.peltodata.domain.FarmfieldExecution;
import fi.peltodata.domain.FarmfieldFile;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
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
        Farmfield farmfield = new Farmfield();
        farmfield.setSowingDate(requestData.getFarmfieldSowingDate());
        farmfield.setCropType(requestData.getFarmfieldCropType());
        farmfield.setDescription(requestData.getFarmfieldDescription());
        farmfield.setFarmId(requestData.getFarmfieldId());
        if (isGuest) {
            return null;
        } else if (user.isAdmin()) {
            if (requestData.getUserId() != null) {
                //admin can create behalf of
                farmfield.setUserId(requestData.getUserId());
            } else {
                farmfield.setUserId(user.getId());
            }

            peltodataService.insertFarmfield(farmfield);
            return new UserFarmfieldResponse(farmfield);
        } else {
            long userId = user.getId();
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
        Farmfield farmfield = new Farmfield();
        farmfield.setId(farmFieldId);
        farmfield.setDescription(requestData.getFarmfieldDescription());
        farmfield.setSowingDate(requestData.getFarmfieldSowingDate());
        farmfield.setCropType(requestData.getFarmfieldCropType());
        farmfield.setFarmId(requestData.getFarmfieldId());
        if (isGuest) {
            return null;
        } else if (user.isAdmin()) {

            if (requestData.getUserId() != null) {
                //admin can create behalf of
                farmfield.setUserId(requestData.getUserId());
            } else {
                farmfield.setUserId(user.getId());
            }

            peltodataService.updateFarmfield(farmfield);
            return new UserFarmfieldResponse(farmfield);
        } else {
            long userId = user.getId();
            boolean allowed = peltodataService.farmfieldBelongsToUser(farmFieldId, userId);
            if (!allowed) {
                throw new AccessDeniedException("update not allowed");
            }
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
    @ResponseBody
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
    public FarmfieldFile uploadFarmfieldLayerDataFile(@PathVariable("id") Long farmFieldId,
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
            String filename = peltodataService.getInputFilename(dataType);
            try (InputStream in = file.getInputStream()){
                FarmfieldFile farmfieldFile = peltodataService.uploadLayerData(farmFieldId, in, dataType, file.getOriginalFilename(), filename);
                return farmfieldFile;
            }
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
                                             @RequestParam(value = "file_id", required = true) Long fileId,
                                             @RequestParam(value = "type", required = false) String outputType,
                                             HttpServletResponse response) {
        User user = params.getUser();
        boolean isGuest = user.isGuest();
        if (isGuest) {
            throw new AccessDeniedException("files fetch not allowed");
        } else {
            long userId = user.getId();
            boolean allowed = peltodataService.farmfieldBelongsToUser(farmFieldId, userId) && peltodataService.farmfieldFileBelongsToFarmAndUser(fileId, farmFieldId, user);
            if (!allowed && !user.isAdmin()) {
                LOG.error("files fetch not allowed farmFieldId {}, filename {}", new Object[] { farmFieldId });
                throw new AccessDeniedException("files fetch not allowed");
            }

            FarmfieldFile farmfieldFile = peltodataService.findFarmfieldFile(fileId);
            if (farmfieldFile == null) {
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                LOG.error("invalid farmfieldFileId {}", new Object[] { fileId });
                throw new RuntimeException("invalid farmfieldFileId");
            }

            FarmfieldFileDataType outputDataType;
            FarmfieldFileDataType inputDataType = FarmfieldFileDataType.fromPathString(farmfieldFile.getFullPath());

            if (inputDataType == null) {
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                LOG.error("inputDataType was not specified, perhaps no folder in filename??. Given filename {}", new Object[] { farmfieldFile.getFullPath() });
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
            peltodataService.createFarmfieldLayer(farmFieldId, farmfieldFile.getId(), inputDataType, outputDataType, user);
            return "";
        }
    }

    @RequestMapping(value = "farms/datatypes", method = RequestMethod.GET)
    @ResponseBody
    public List<String> getFarmfieldLayerFileDataTypes() {
        return EnumSet.allOf(FarmfieldFileDataType.class)
                .stream().sorted().map(FarmfieldFileDataType::getTypeId).collect(Collectors.toList());
    }

    @RequestMapping(value = "farms/executions", method = RequestMethod.GET)
    @ResponseBody
    public List<FarmfieldExecution> getFarmfieldExecutions(@OskariParam ActionParameters params) {
        User user = params.getUser();
        if (user.isGuest()) {
            throw new AccessDeniedException("getFarmfieldExecutions not allowed for guest");
        } else if (user.isAdmin()) {
            return peltodataService.findAllFarmfieldExecutions();
        }
        return peltodataService.findAllFarmfieldExecutionsForUser(user.getId());
    }


    @RequestMapping(value = "farms/{farmId}/file/{fileId}", method = RequestMethod.POST)
    @ResponseBody
    public String updateFileDate(@PathVariable("farmId") Long farmFieldId,
                                   @PathVariable("fileId") Long farmfieldFileId,
                                   @RequestBody FarmfieldFileDateUpdateRequest requestData,
                                   @OskariParam ActionParameters params) {
        User user = params.getUser();
        boolean isGuest = user.isGuest();
        if (isGuest) {
            return null;
        }

        boolean allowed = peltodataService.farmfieldFileBelongsToFarmAndUser(farmfieldFileId, farmFieldId, user);
        if (!allowed && !user.isAdmin()) {
            LOG.error("updateFileDAte not allowed for user {}, farmid={}, fileid={}", new Object[] { user.getScreenname(), farmFieldId, farmfieldFileId });
            throw new RuntimeException("operation not allowed");
        }

        Farmfield farmfield = peltodataService.findFarmfield(farmFieldId);
        FarmfieldFile file = peltodataService.findFarmfieldFile(farmfieldFileId);

        file.setFileDate(requestData.getDate());
        peltodataService.updateFarmfieldFile(file);

        return "";
    }
}
