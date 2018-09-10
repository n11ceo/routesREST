package ru.bpc.cm.routes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.bpc.cm.utils.db.JdbcUtils;

import java.sql.*;
import java.util.*;

public class OrgController {
	private static final Logger logger = LoggerFactory.getLogger("CASH_MANAGEMENT");
	
	public static List<AtmGroupAttributeItem> getAttributeListForOrg(Connection connection, int orgId){
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		List<AtmGroupAttributeItem> attributeListForOrg = new ArrayList<AtmGroupAttributeItem>();
		try{
			String sql =
				"SELECT agad.ID,agad.REQUIRED,aga.VALUE,COALESCE(aga.ATTR_IS_USED,1) as ATTR_IS_USED,agad.DESCX "+
                "FROM T_CM_ROUTE_ORG ag "+
                    "join T_CM_ROUTE_ORG_ATTR_DESC agad on(1=1) "+
                    "left outer join T_CM_ROUTE_ORG_ATTR aga "+
                    	"on (aga.ATTR_ID = agad.ID and aga.ORG_ID = ag.ID) "+
                "where ag.ID = ? "+
                    "order by agad.ID";
			pstmt = connection.prepareStatement(sql);
			pstmt.setInt(1, orgId);
			rs = pstmt.executeQuery();

			while (rs.next()) {
				AtmGroupAttributeItem item = new AtmGroupAttributeItem();
				item.setAttributeID(rs.getInt("ID"));
				item.setValue(rs.getString("VALUE"));
				item.setDescx(rs.getString("DESCX"));
				item.setRequired(rs.getBoolean("REQUIRED"));
				if(item.getValue() == null && !item.isRequired() ){
					item.setUsed(false);
				} else {
					item.setUsed(rs.getBoolean("ATTR_IS_USED"));
				}
				item.setValueType(OrgAttribute.getAtmAttributeValueType(item.getAttributeID()));
				item.setInputType(OrgAttribute.getAtmAttributeInputType(item.getAttributeID()));
				attributeListForOrg.add(item);
			}
		}catch (SQLException e) {
			logger.error("", e);
		} finally {
			JdbcUtils.close(rs);
			JdbcUtils.close(pstmt);
		}
		return attributeListForOrg;
	}

}
