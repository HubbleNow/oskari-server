package fi.peltodata.controller;

import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.spring.extension.OskariParam;
import fi.peltodata.controller.response.UserFarmfieldsResponse;
import fi.peltodata.domain.Farmfield;
import fi.peltodata.service.PeltodataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("peltodata/api")
public class PeltodataController {

    @Autowired
    private PeltodataService peltodataService;

    @RequestMapping("info")
    public @ResponseBody
    String info() {
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
    public @ResponseBody List<UserFarmfieldsResponse> findAllByUser(@OskariParam ActionParameters params) {
        User user = params.getUser();
        boolean isGuest = user.isGuest();
        if (isGuest) {
            return Collections.emptyList();
        } else if (user.isAdmin()) {
            List<Farmfield> allFarmfields = peltodataService.findAll();
            return allFarmfields.stream().map(UserFarmfieldsResponse::new).collect(Collectors.toList());
        } else {
            long userId = user.getId();
            List<Farmfield> allByUser = peltodataService.findAllByUser(userId);
            return allByUser.stream().map(UserFarmfieldsResponse::new).collect(Collectors.toList());
        }
    }
}
