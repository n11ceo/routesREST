package ru.bpc.cm.routes.boot.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ru.bpc.cm.items.routes.OrgItem;
import ru.bpc.cm.routes.boot.service.OrgServiceImpl;
import ru.bpc.cm.utils.db.JdbcUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository("orgDao")
public class OrgDaoImpl implements OrgDao {

    private Logger slf4jLog = LoggerFactory.getLogger(OrgDaoImpl.class);

    private DataSource dataSource;

    @Autowired
    public OrgDaoImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }


    @Override
    public List<OrgItem> getOrgs() {
        slf4jLog.debug("Trying to get list of Orgs from database");
        ArrayList<OrgItem> orgList = new ArrayList<>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            Connection connection = dataSource.getConnection();
            String sql = "SELECT ID, NAME, DESCRIPTION, INST_ID, DEPOT_ID FROM T_CM_ROUTE_ORG";
            pstmt = connection.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                OrgItem item = new OrgItem();
                item.setId(rs.getInt("ID"));
                item.setName(rs.getString("NAME"));
                item.setDescx(rs.getString("DESCRIPTION"));
                item.setInstId(rs.getString("INST_ID"));
                item.setDepotId(String.valueOf(rs.getInt("DEPOT_ID")));
                orgList.add(item);
            }
        } catch (SQLException e) {
            slf4jLog.error("", e);
        } finally {
            JdbcUtils.close(rs);
            JdbcUtils.close(pstmt);
        }
        slf4jLog.debug("Returning list of Orgs");
        return orgList;
    }
}
