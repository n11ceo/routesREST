package ru.bpc.cm.routes.boot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.bpc.cm.items.routes.OrgItem;
import ru.bpc.cm.routes.boot.dao.OrgDao;
import ru.bpc.cm.utils.db.JdbcUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service("orgService")
public class OrgServiceImpl implements OrgService {

    private Logger slf4jLog = LoggerFactory.getLogger(OrgServiceImpl.class);

    private final OrgDao orgDao;

    @Autowired
    public OrgServiceImpl(OrgDao orgDao) {
        this.orgDao = orgDao;
    }

    public List<OrgItem> getOrgs() {
        return orgDao.getOrgs();
    }
}
