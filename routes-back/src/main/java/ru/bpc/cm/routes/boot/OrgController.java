package ru.bpc.cm.routes.boot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.bpc.cm.items.routes.OrgItem;
import ru.bpc.cm.routes.boot.service.OrgService;

import java.util.List;

@RestController
@RequestMapping("/routes")
public class OrgController {

    private final OrgService orgService;

    @Autowired
    public OrgController(OrgService orgService) {
        this.orgService = orgService;
    }

    @RequestMapping(path="/carriers")
    public List<OrgItem> getCarriers() {
        return orgService.getOrgs();
    }

}
