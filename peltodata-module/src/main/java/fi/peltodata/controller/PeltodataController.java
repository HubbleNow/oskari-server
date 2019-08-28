package fi.peltodata.controller;

import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.spring.extension.OskariParam;
import fi.peltodata.controller.request.UserFarmfieldCreateRequest;
import fi.peltodata.controller.request.UserFarmfieldUpdateRequest;
import fi.peltodata.controller.response.UserFarmfieldResponse;
import fi.peltodata.domain.Farmfield;
import fi.peltodata.service.PeltodataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("peltodata/api")
public class PeltodataController {

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
            farmfield.setDescription(requestData.getFarmfieldDescription());
            peltodataService.insertFarmfield(farmfield);
            return new UserFarmfieldResponse(farmfield);
        } else {
            long userId = user.getId();
            Farmfield farmfield = new Farmfield();
            farmfield.setDescription(requestData.getFarmfieldDescription());
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
}
