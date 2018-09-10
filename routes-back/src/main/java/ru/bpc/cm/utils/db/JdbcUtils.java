package ru.bpc.cm.utils.db;

import org.apache.commons.lang3.StringUtils;
import ru.bpc.cm.utils.CollectionUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class JdbcUtils {

	public static final String ORACLE_DATE_TIME_PATTERN = "YYYY/MM/DD HH24:MI:SS";
	public static final String JAVA_DATE_TIME_PATTERN = "yyyy/MM/dd HH:mm:ss";

	/**
	 * max quantity of parameters for use with IN keywords.
	 * select * from T where a IN (?)
	 */
	public static final int MAX_PARAMS_FOR_IN_CONDITION = 999;
	
	public static final int ORACLE_DUP_VAL_ERROR_CODE = 1;
	public static final int DB2_DUP_VAL_ERROR_CODE = 1;
	public static final int POSTGRES_DUP_VAL_ERROR_CODE = 1;

	public static final String NUMBER_TABLE_NAME = "t_api_tmp_number";
	public static final String VALUE_SET_TABLE_NAME = "t_tmp_value_sets";
	public static final String VALUE_SET_COLUMN_SET_ID_NAME = "set_id";
	public static final String VALUE_SET_COLUMN_STRING_NAME = "string_value";
	public static final String VALUE_SET_COLUMN_NUMBER_NAME = "number_value";
	public static final String VALUE_SET_COLUMN_DATE_NAME = "date_value";

	public static final int MAX_COUNT_BINDING_PARAM_FOR_BATCH_MODE = 32000;

	public static void close(Connection conn) {
		if (conn != null) {
			try {
				conn.close();
			} catch (Exception e) {
			}
		}
	}

	public final static void close(Statement stmt) {
		if (stmt != null) {
			try {
				stmt.close();
			} catch (Exception e) {
			}
		}
	}

	public final static void close(ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (Exception e) {
			}
		}
	}

	public static Byte getByte(ResultSet rs, String columnName) throws SQLException {
		byte val = rs.getByte(columnName);
		if (rs.wasNull()) {
			return null;
		} else {
			return Byte.valueOf(val);
		}
	}

	public static Byte getByte(ResultSet rs, int columnIndex) throws SQLException {
		byte val = rs.getByte(columnIndex);
		if (rs.wasNull()) {
			return null;
		} else {
			return Byte.valueOf(val);
		}
	}

	public static Byte getByte(CallableStatement cs, int columnIndex) throws SQLException {
		byte val = cs.getByte(columnIndex);
		if (cs.wasNull()) {
			return null;
		} else {
			return Byte.valueOf(val);
		}
	}

	public static Short getShort(ResultSet rs, String columnName) throws SQLException {
		short val = rs.getShort(columnName);
		if (rs.wasNull()) {
			return null;
		} else {
			return Short.valueOf(val);
		}
	}

	public static Short getShort(ResultSet rs, int columnIndex) throws SQLException {
		short val = rs.getShort(columnIndex);
		if (rs.wasNull()) {
			return null;
		} else {
			return Short.valueOf(val);
		}
	}

	public static Short getShort(CallableStatement cs, int columnIndex) throws SQLException {
		short val = cs.getShort(columnIndex);
		if (cs.wasNull()) {
			return null;
		} else {
			return Short.valueOf(val);
		}
	}

	public static Integer getInteger(ResultSet rs, int columnIndex) throws SQLException {
		int val = rs.getInt(columnIndex);
		if (rs.wasNull()) {
			return null;
		} else {
			return Integer.valueOf(val);
		}
	}

	public static Integer getInteger(CallableStatement cs, int columnIndex) throws SQLException {
		int val = cs.getInt(columnIndex);
		if (cs.wasNull()) {
			return null;
		} else {
			return Integer.valueOf(val);
		}
	}

	public static Integer getInteger(ResultSet rs, String columnName) throws SQLException {
		int val = rs.getInt(columnName);
		if (rs.wasNull()) {
			return null;
		} else {
			return Integer.valueOf(val);
		}
	}

	public static Boolean getBoolean(ResultSet rs, String columnName) throws SQLException {
		int val = rs.getInt(columnName);
		if (rs.wasNull()) {
			return null;
		}
		return val == 1;
	}

	public static Boolean getBoolean(ResultSet rs, int columnIndex) throws SQLException {
		int val = rs.getInt(columnIndex);
		if (rs.wasNull()) {
			return null;
		}
		return val == 1;
	}

	public static Long getLong(ResultSet rs, int columnIndex) throws SQLException {
		long val = rs.getLong(columnIndex);
		if (rs.wasNull()) {
			return null;
		} else {
			return Long.valueOf(val);
		}
	}

	public static Long getLong(ResultSet rs, String columnName) throws SQLException {
		long val = rs.getLong(columnName);
		if (rs.wasNull()) {
			return null;
		} else {
			return Long.valueOf(val);
		}
	}

	public static Long getLong(CallableStatement cs, int columnIndex) throws SQLException {
		long val = cs.getLong(columnIndex);
		if (cs.wasNull()) {
			return null;
		} else {
			return Long.valueOf(val);
		}
	}

	public static Double getDouble(ResultSet rs, int columnIndex) throws SQLException {
		double val = rs.getDouble(columnIndex);
		if (rs.wasNull()) {
			return null;
		} else {
			return Double.valueOf(val);
		}
	}

	public static Double getDouble(ResultSet rs, String columnName) throws SQLException {
		double val = rs.getDouble(columnName);
		if (rs.wasNull()) {
			return null;
		} else {
			return Double.valueOf(val);
		}
	}

	public static Double getDouble(CallableStatement cs, int columnIndex) throws SQLException {
		double val = cs.getDouble(columnIndex);
		if (cs.wasNull()) {
			return null;
		} else {
			return Double.valueOf(val);
		}
	}

	public static Float getFloat(ResultSet rs, String columnName) throws SQLException {
		float val = rs.getFloat(columnName);
		if (rs.wasNull()) {
			return null;
		} else {
			return val;
		}
	}

	public static Float getFloat(ResultSet rs, int columnIndex) throws SQLException {
		float val = rs.getFloat(columnIndex);
		if (rs.wasNull()) {
			return null;
		} else {
			return val;
		}
	}

	public static Float getFloat(CallableStatement cs, int columnIndex) throws SQLException {
		float val = cs.getFloat(columnIndex);
		if (cs.wasNull()) {
			return null;
		} else {
			return val;
		}
	}

	public static Date getDate(ResultSet rs, String columnName) throws SQLException {
		return getDate(rs.getDate(columnName));
	}

	public static Date getDate(ResultSet rs, int columnIndex) throws SQLException {
		return getDate(rs.getDate(columnIndex));
	}

	public static Date getTime(ResultSet rs, String columnName) throws SQLException {
		return getTime(rs.getTime(columnName));
	}

	public static Date getTime(ResultSet rs, int columnIndex) throws SQLException {
		return getTime(rs.getTime(columnIndex));
	}

	public static Date getTimestamp(ResultSet rs, String columnName) throws SQLException {
		return getTimestamp(rs.getTimestamp(columnName));
	}

	public static Date getTimestamp(ResultSet rs, int columnIndex) throws SQLException {
		return getTimestamp(rs.getTimestamp(columnIndex));
	}

	public static Date getDate(java.sql.Date date) {
		return (date == null) ? null : new Date(date.getTime());
	}

	public static Date getTime(java.sql.Time time) {
		return (time == null) ? null : new Date(time.getTime());
	}

	public static Date getTimestamp(java.sql.Timestamp timestamp) {
		return (timestamp == null) ? null : new Date(timestamp.getTime());
	}

	public static java.sql.Date getSqlDate(Date date) {
		return (date == null) ? null : new java.sql.Date(date.getTime());
	}

	public static java.sql.Time getSqlTime(Date time) {
		return (time == null) ? null : new java.sql.Time(time.getTime());
	}

	public static java.sql.Timestamp getSqlTimestamp(Date timestamp) {
		return (timestamp == null) ? null : new java.sql.Timestamp(timestamp.getTime());
	}

	public static String getNotNull(ResultSet rs, int columnIndex) throws SQLException {
		String s = rs.getString(columnIndex);
		return (s == null) ? "" : s;
	}

	public static double secondsToDays(long seconds) {
		return 1d / 24 / 60 / 60 * seconds;
	}

	public static Long getNextSequenceValue(DataSource dataSource, String sequence) {
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
			return getNextSequenceValue(conn, sequence);
		} catch (SQLException e) {
			throw new RuntimeException("next sequence: " + sequence, e);
		} finally {
			JdbcUtils.close(conn);
		}
	}

	public static Long getNextSequenceValue(Connection conn, String sequence) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		String sql = "SELECT " + getNextSequence(conn, sequence) +" "+getFromDummyExpression(conn);
		try {
			ps = conn.prepareStatement(sql);
			rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getLong(1);
			} else {
				throw new SQLException("No value obtained from sequence with name \"" + sequence + "\"");
			}
		} finally {
			JdbcUtils.close(rs);
			JdbcUtils.close(ps);
		}
	}

	public static String generateInCondition(String column, int count) {
		return generateInCondition(column, Collections.nCopies(count, "?"));
	}

	public static <T extends Number> String generateInConditionNumber(String column, Collection<T> items) {
		return generateInCondition(column, items);
	}

	private static <T> String generateInCondition(String column, Collection<T> items) {
		if (items == null) {
			return null;
		}

		StringBuilder sb = null;

		Iterator<T> iter = items.iterator();

		while (iter.hasNext()) {

			if (sb == null) {
				sb = new StringBuilder();
				sb.append("(");
			} else {
				sb.append(" OR ");
			}

			sb.append(String.format("%s IN (", column));
			int index = 0;
			while (iter.hasNext() && index <= MAX_PARAMS_FOR_IN_CONDITION) {
				if (index > 0) {
					sb.append(",");
				}
				sb.append(iter.next().toString());
				index++;
			}
			sb.append(")");
		}

		if (sb != null) {
			sb.append(")");
		}

		return sb == null ? "" : sb.toString();
	}

	public static void setBoolean(PreparedStatement ps, int index, Boolean value) throws SQLException {
		if (value == null) {
			ps.setNull(index, java.sql.Types.NUMERIC);
		} else {
			ps.setInt(index, value ? 1 : 0);
		}
	}

	public static void setNumber(PreparedStatement ps, int index, Number value)
		throws SQLException
	{
		if (value == null) {
			ps.setNull(index, java.sql.Types.NUMERIC);
		} else if (value instanceof Double) {
			ps.setDouble(index, value.doubleValue());
		} else if (value instanceof Long) {
			ps.setLong(index, value.longValue());
		} else if (value instanceof Integer) {
			ps.setInt(index, value.intValue());
		} else if (value instanceof Short) {
			ps.setShort(index, value.shortValue());
		} else if (value instanceof Byte) {
			ps.setByte(index, value.byteValue());
		} else if (value instanceof BigDecimal) {
			ps.setBigDecimal(index, (BigDecimal) value);
		} else {
			ps.setObject(index, value);
		}
	}

	public static void setObject(PreparedStatement ps, int index, Object value) throws SQLException {
		setObject(ps, index, value, false);
	}

	public static void setObject(PreparedStatement ps, int index, Object value, boolean isDateAsString) throws SQLException {
		if (value == null) {
			ps.setString(index, null);
		} else if (value instanceof Date) {
			if (isDateAsString) {
				JdbcUtils.setDateAsString(ps, index, (Date) value);
			} else {
				JdbcUtils.setDate(ps, index, (Date) value);
			}
		} else if (value instanceof Number) {
			JdbcUtils.setNumber(ps, index, (Number) value);
		} else if (value instanceof Boolean) {
			JdbcUtils.setBoolean(ps, index, (Boolean) value);
		} else {
			ps.setString(index, value.toString());
		}
	}

	public static void setString(PreparedStatement ps, int index, String value) throws SQLException {
		if (value == null || value.isEmpty()) {
			ps.setNull(index, java.sql.Types.VARCHAR);
		} else {
			ps.setString(index, value);
		}
	}

	public static void setDate(PreparedStatement ps, int index, Date value) throws SQLException {
		if (value == null) {
			ps.setNull(index, java.sql.Types.DATE);
		} else {
			ps.setTimestamp(index, new java.sql.Timestamp(value.getTime()));
		}
	}

	public static void setDateAsString(PreparedStatement ps, int index, Date value) throws SQLException {
		setString(ps, index, value == null ? null : new SimpleDateFormat(JAVA_DATE_TIME_PATTERN).format(value));
	}

	@Deprecated
	public static <N extends Number> void fillNumberTable(Connection conn, Collection<N> values) throws SQLException {
		fillNumberTable(conn, NUMBER_TABLE_NAME, values);
	}

	@Deprecated
	public static <N extends Number> void fillNumberTable(Connection conn, String tableName, Collection<N> values) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(String.format("insert into %s values (?)", tableName));
			int index = 0;
			for (N value : values) {
				JdbcUtils.setNumber(ps, 1, value);
				ps.addBatch();
				index++;
				if (index % MAX_COUNT_BINDING_PARAM_FOR_BATCH_MODE == 0 || index == values.size()) {
					ps.executeBatch();
				}
			}
		} finally {
			JdbcUtils.close(ps);
		}
	}

	public static <V extends Number> void addNumberSet(Connection conn, Iterable<V> values) throws SQLException {
		addNumberSet(conn, null, values);
	}

	public static void addStringSet(Connection conn, Iterable<String> values) throws SQLException {
		addStringSet(conn, null, values);
	}

	public static <V extends Date> void addDateSet(Connection conn, Iterable<V> values) throws SQLException {
		addDateSet(conn, null, values);
	}

	public static <V extends Number> void addNumberSet(Connection conn, String setId, Iterable<V> values) throws SQLException {
		addValueSet(conn, buildAddValueSetQuery(VALUE_SET_COLUMN_NUMBER_NAME), setId, values);
	}

	public static void addStringSet(Connection conn, String setId, Iterable<?> values) throws SQLException {
		addValueSet(conn, buildAddValueSetQuery(VALUE_SET_COLUMN_STRING_NAME), setId, values);
	}

	private static String buildAddValueSetQuery(String valueColumnName) {
		return MessageFormat.format("INSERT INTO {0} ({1}, {2}) VALUES (?, ?)", VALUE_SET_TABLE_NAME, VALUE_SET_COLUMN_SET_ID_NAME,
				valueColumnName);
	}

	public static <V extends Date> void addDateSet(Connection conn, String setId, Iterable<V> values) throws SQLException {
		addValueSet(conn, buildAddValueSetQuery(VALUE_SET_COLUMN_DATE_NAME), setId, values);
	}

	private static <V> void addValueSet(Connection conn, String query, String setId, Iterable<V> values) throws SQLException {
		PreparedStatement ps = null;
		if (values == null || !values.iterator().hasNext()) {
			return;
		}
		try {
			ps = conn.prepareStatement(query);
			JdbcUtils.setString(ps, 1, setId);
			int index = 0;

			for (Iterator<V> iterator = values.iterator(); iterator.hasNext();) {
				V value = iterator.next();
				JdbcUtils.setObject(ps, 2, value, false);
				ps.addBatch();
				index++;
				if (index % (MAX_COUNT_BINDING_PARAM_FOR_BATCH_MODE / 2) == 0 || !iterator.hasNext()) {
					ps.executeBatch();
				}
			}
		} finally {
			JdbcUtils.close(ps);
		}
	}

	public static void clearValueSets(Connection conn) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement("DELETE FROM " + VALUE_SET_TABLE_NAME);
			ps.executeUpdate();
		} finally {
			JdbcUtils.close(ps);
		}
	}

	public static <V extends Number> void addNumberSet(DataSource dataSource, String setId, Iterable<V> values) {
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
			addNumberSet(conn, setId, values);
		} catch (SQLException e) {
			throw new RuntimeException("addNumberSet", e);
		} finally {
			close(conn);
		}
	}

	public static void addStringSet(DataSource dataSource, String setId, Iterable<?> values) {
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
			addStringSet(conn, setId, values);
		} catch (SQLException e) {
			throw new RuntimeException("addStringSet", e);
		} finally {
			close(conn);
		}
	}

	public static <V extends Date> void addDateSet(DataSource dataSource, String setId, Iterable<V> values) {
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
			addDateSet(conn, setId, values);
		} catch (SQLException e) {
			throw new RuntimeException("addDateSet", e);
		} finally {
			close(conn);
		}
	}

	public static <V extends Number> void addNumberSet(DataSource dataSource, Iterable<V> values) {
		addNumberSet(dataSource, null, values);
	}

	public static void addStringSet(DataSource dataSource, Iterable<?> values) {
		addStringSet(dataSource, null, values);
	}

	public static <V extends Date> void addDateSet(DataSource dataSource, Iterable<V> values) {
		addDateSet(dataSource, null, values);
	}

	public static String getBindingExpressionForValue(Object value) {
		if (value instanceof Date) {
			return String.format("TO_DATE(?, '%s')", ORACLE_DATE_TIME_PATTERN);
		} else if (value instanceof Iterable<?>) {
			Iterator<?> iterator = ((Iterable<?>) value).iterator();
			String columnName = VALUE_SET_COLUMN_STRING_NAME;
			if (iterator.hasNext()) {
				Object element = iterator.next();
				if (element instanceof Date) {
					columnName = VALUE_SET_COLUMN_DATE_NAME;
				} else if (element instanceof Number) {
					columnName = VALUE_SET_COLUMN_NUMBER_NAME;
				}
			}
			return MessageFormat.format("(SELECT {0} FROM {1} WHERE {2} = ?)", columnName, VALUE_SET_TABLE_NAME, VALUE_SET_COLUMN_SET_ID_NAME);
		}
		return "?";
	}
	
	public static String getNextSequence(Connection con, String seqName) throws SQLException{
		String DbName = getDbName(con);
		if (DbName=="Oracle" || DbName=="DB2"){
			return seqName+".nextval";
		} else if(DbName=="PostgreSQL"){
			return "nextval('"+seqName+"')";
		} else {
			throw new SQLException();
		}
	}
	
	public static String getCurrentSequence(Connection con, String seqName) throws SQLException{
		String DbName = getDbName(con);
		if (DbName=="Oracle" || DbName=="DB2"){
			return seqName+".currval";
		} else if(DbName=="PostgreSQL"){
			return "currval('"+seqName+"')";
		} else {
			throw new SQLException();
		}
	}
	
	public static String getFromDummyExpression(Connection con) throws SQLException{
		String DbName = getDbName(con);
		if (DbName=="Oracle" ){
			return " from DUAL";
		} else if(DbName=="DB2"){
			return " from SYSIBM.SYSDUMMY1";
		} else if(DbName=="PostgreSQL"){
			return "";
		} else {
			throw new SQLException();
		}
	}
	
	public static String getTruncateTableUnrecoverable(Connection con, String tableName) throws SQLException{
		String DbName = getDbName(con);
		if (DbName=="Oracle" ){
			//return " DELETE FROM "+tableName+" UNRECOVERABLE ";
			return " TRUNCATE TABLE "+tableName+" DROP STORAGE ";
		} else if(DbName=="DB2"){
			return " DELETE FROM "+tableName;
		} else if(DbName=="PostgreSQL"){
			return "TRUNCATE "+tableName;
		} else {
			throw new SQLException();
		}
	}
	
	public static String getDeleteFromTableFieldInConditional(Connection con, String tableName, String columnName, List<Integer> inClauseList) throws SQLException{
		String query="";
		if (inClauseList!=null && !inClauseList.isEmpty()) {
			String DbName = getDbName(con);
			boolean splitFlag=false;
			if (DbName=="Oracle" ){
				query=" DELETE FROM "+tableName+" where ";
				} else if(DbName=="DB2"){
					query=" DELETE FROM "+tableName+" where ";
				} else if(DbName=="PostgreSQL"){
					query="TRUNCATE "+tableName+" where ";
				} else {
					throw new SQLException();
				}
			for (List<Integer> inList : CollectionUtils.splitListBySizeView(inClauseList, MAX_PARAMS_FOR_IN_CONDITION)){
				if (splitFlag){
					query+=" or ";
				}
				query+=columnName+" in ("+StringUtils.join(inList, ",")+") ";
				splitFlag=true;
			}
		}
		
		return query;
	}
	
	public static void createTemporaryTableIfNotExists(Connection con, String tableName) throws SQLException{
		String DbName = getDbName(con);
		String tempSql;
		String signature="";
		if (DbName=="PostgreSQL"){
			if (tableName.equalsIgnoreCase("t_cm_temp_atm_group_list")){
				signature="id NUMERIC(9)";
			} else if(tableName.equalsIgnoreCase("t_cm_temp_atm_list")){
				signature="id NUMERIC(9)";
			} else if(tableName.equalsIgnoreCase("t_cm_temp_enc_plan_curr")){
				signature="enc_plan_id NUMERIC(6) NOT NULL,"+
							" curr_code NUMERIC(3) NOT NULL,"+
							" curr_summ NUMERIC(15) NOT NULL";
			} else if(tableName.equalsIgnoreCase("t_cm_temp_enc_plan_denom")){
				signature="enc_plan_id NUMERIC(6) NOT NULL,"+
						" denom_value NUMERIC(6) NOT NULL,"+
						" denom_count NUMERIC(4) NOT NULL,"+
						" denom_curr NUMERIC(3) NOT NULL";
				
			} else if(tableName.equalsIgnoreCase("t_cm_temp_enc_plan")){
				signature="enc_plan_id NUMERIC(6) NOT NULL,"+
						" atm_id NUMERIC(9) NOT NULL,"+
						" date_forthcoming_encashment TIMESTAMP";
			} else if(tableName.equalsIgnoreCase("t_cm_temp_enc_report")){
				signature="remaining NUMERIC(15) NOT NULL,"+
							" curr_code VARCHAR(7) NOT NULL,"+
							" stat_date TIMESTAMP,"+
							" end_of_stats_date NUMERIC(1)";
			}
			tempSql  = "CREATE TEMPORARY TABLE IF NOT EXISTS "+tableName+
						" ("+signature+") "+
						" ON COMMIT DELETE ROWS";
			Statement stmt = con.createStatement();
			stmt.executeUpdate(tempSql);
		}
	}
	
	public static int getDuplicateValueErrorCode(Connection con) throws SQLException{
		String DbName = getDbName(con);
		if (DbName=="Oracle" ){
			return ORACLE_DUP_VAL_ERROR_CODE;
		} else if(DbName=="DB2"){
			return DB2_DUP_VAL_ERROR_CODE;
		} else if(DbName=="PostgreSQL"){
			return POSTGRES_DUP_VAL_ERROR_CODE;
		} else {
			throw new SQLException();
		}
	}
	
	public static String getDoubleTypeExpression(Connection con){
		String DbName = getDbName(con);
		if (DbName=="Oracle" ){
			return "NUMBER";
		} else if(DbName=="DB2"){
			return "DOUBLE";
		} else if(DbName=="PostgreSQL"){
			return "double precision";
		} else {
			return "NUMBER";
		}
	}
	
	public static String getLimitRowsExpression(Connection con, int limitNumber){
		String DbName = getDbName(con);
		if (DbName=="Oracle" ){
			return " where rownum < "+limitNumber;
		} else if(DbName=="DB2"){
			return " where rownum < "+limitNumber;
		} else if(DbName=="PostgreSQL"){
			return " limit "+limitNumber;
		} else {
			return " where rownum < "+limitNumber;
		}
	}
	
	public static String getLimitRowsAndExpression(Connection con, int limitNumber){
		String DbName = getDbName(con);
		if (DbName=="Oracle" ){
			return " and rownum < "+limitNumber;
		} else if(DbName=="DB2"){
			return " and rownum < "+limitNumber;
		} else if(DbName=="PostgreSQL"){
			return " limit "+limitNumber;
		} else {
			return " and rownum < "+limitNumber;
		}
	}
	
	public static String getLimitToFirstRowExpression(Connection con){
		String DbName = getDbName(con);
		if (DbName=="Oracle" ){
			return " where rownum = 1 ";
		} else if(DbName=="DB2"){
			return " where rownum = 1 ";
		} else if(DbName=="PostgreSQL"){
			return " limit 1 ";
		} else {
			return " where rownum = 1 ";
		}
	}
	
	public static String getLimitToFirstRowAndExpression(Connection con){
		String DbName = getDbName(con);
		if (DbName=="Oracle" ){
			return " and rownum = 1 ";
		} else if(DbName=="DB2"){
			return " and rownum = 1 ";
		} else if(DbName=="PostgreSQL"){
			return " limit 1 ";
		} else {
			return " and rownum = 1 ";
		}
	}
	
	public static String getDbName(Connection con){
		String DbName;
		try {
			DbName = con.getMetaData().getDatabaseProductName();
			if (DbName.startsWith("DB2") && !DbName.isEmpty()){
				DbName="DB2";
			}
		} catch (SQLException e) {
			DbName  ="Undefined";
		}
		return DbName;
	}

}
