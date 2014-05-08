package bs.localserve;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class DB {
	private static Logger log = Logger.getLogger(DB.class.getCanonicalName());
	
	private Connection conn = null;
	
	public void disconnect() {
		if (conn!=null) {
			try {
				conn.close();
			} catch (SQLException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			} finally {
				conn = null;
			}
		}
	}
	
	public void init(String classname, String connString) {
		log.finest("Connecting to: " + connString + " with driver: " + classname);
		try {
			Class.forName(classname);
		} catch (ClassNotFoundException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		try {
			conn = DriverManager.getConnection(connString);
			conn.setAutoCommit(true);
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			conn = null;
			System.exit(-1);
		}
	}
	
	public String execute(String statement, List<String> sqlParams) {
		StringBuffer response = null;
		int rowsAffected = 0;
		PreparedStatement stmt = null;
		
		try {
			log.finest("Executing update: " + statement);
			stmt = conn.prepareStatement(statement);
			stmt.clearParameters();
			for (int i=0; i<sqlParams.size();i++) {
				String v = sqlParams.get(i);
				if (v!=null && v.equalsIgnoreCase("")) {
					v = null;
				}
				log.finest("Binding param idx: " + (i+1) + " => " + v);
				if (v!=null) {
					stmt.setString((i+1), v);
				} else {
					stmt.setNull((i+1), java.sql.Types.VARCHAR);
				}
			}
			
			rowsAffected = stmt.executeUpdate();
			log.finest("Updated " + rowsAffected + " rows");
			stmt.close();
			
			response = new StringBuffer();
			response.append("{ \"updates\": " + rowsAffected + " }");
		} catch (SQLException e) {
			log.log(Level.WARNING, "\"" + statement + "\" - lead to: " + e.getMessage(), e);
			response = null;
		} finally {
			stmt = null;
		}
		
		if (response!=null) return response.toString();
		else return null;
	}
	
	public String query(String query, List<String> sqlParams, int page, int pageSize) {
		StringBuffer response = new StringBuffer();
		ResultSet rset = null;
		PreparedStatement stmt = null;
		
		try {
			log.finest("Executing: " + query);
			stmt = conn.prepareStatement(query);
			stmt.clearParameters();
			for (int i=0; i<sqlParams.size();i++) {
				String v = sqlParams.get(i);
				if (v!=null && v.equalsIgnoreCase("")) {
					v = null;
				}
				log.finest("Binding param idx: " + (i+1) + " => " + v);
				if (v!=null) {
					stmt.setString((i+1), v);
				} else {
					stmt.setNull((i+1), java.sql.Types.VARCHAR);
				}
			}

			rset = stmt.executeQuery();
			ResultSetMetaData meta = rset.getMetaData();
			int idx = 0;
			int start = pageSize * page;
			int end = pageSize * (page+1);
			
			response.append("{ \"dataset\": { ");
			response.append(" \"page\": " + page + ",");
			response.append(" \"pageSize\": " + pageSize + ", ");
			response.append(" \"record\": [ ");
			log.finest("Query executed..");
			while(rset.next()) {
				if (idx>=start) {
					if (idx>start) {
						response.append(", ");
					}
					response.append(" { ");
					for(int i=0;i<meta.getColumnCount();i++) {
						String colName = meta.getColumnName((i+1));
						Object val = rset.getObject((i+1));
						if (val!=null) {
							// needs escaping shit here
							response.append("\"" + colName + "\": \"" + jsonify(val.toString()) + "\"");
						} else {
							response.append("\"" + colName + "\": \"\"");
						}
						if (i<(meta.getColumnCount()-1)) {
							response.append(", ");
						}
					}
					response.append("} ");
				}
				if (idx>end) {
					break;
				}
				idx++;
			}
			response.append(" ] } } ");
			
			rset.close();
			stmt.close();

		} catch (SQLException e) {
			log.log(Level.WARNING, "\"" + query + "\" - lead to: " + e.getMessage(), e);
			response = null;
		} finally {
			rset = null;
			stmt = null;
		}
		
		if (response!=null) return response.toString();
		else return null;
	}
	
	public static String jsonify(String t) {
		if (t!=null) {
			String rval = t.replaceAll("\r","\\\\\r").replaceAll("\n","\\\\\n");
			return rval.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\""); // .replaceAll("'", "\\\\'");
		} else return null;
	}
}
