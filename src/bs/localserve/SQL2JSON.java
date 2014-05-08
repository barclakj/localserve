package bs.localserve;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SQL2JSON {
	private static final Logger log = Logger.getLogger(SQL2JSON.class.getCanonicalName());
	
	public static String parseAndExecute(String statement, Map<String, String> parameters, DB db) {
		String result = null;
		List<String> sqlParams = new ArrayList<String>();
		StringBuffer sb = new StringBuffer();
		
		int prevIdx = 0;
		int idx = -1;
		while(((idx=statement.indexOf("{", idx+1)))>=0) {
			log.finest(sb.toString() + " " + idx + " " + prevIdx + " " + statement);
			sb.append(statement.substring(prevIdx, idx));
			
			String paramName = statement.substring(idx+1, statement.indexOf("}", idx));
			log.finest("Adding: " + paramName + " as " + parameters.get(paramName));
			if (parameters.containsKey(paramName)) {
				sqlParams.add(parameters.get(paramName));
			} else {
				sqlParams.add(""); // we'll assume zero length string is a null - sql needs to accomodate this
			}
			sb.append("?"); // replace {NAME} with ?
			prevIdx = statement.indexOf("}", idx)+1;
		}
		sb.append( statement.substring(prevIdx) );
		
		if (statement.toLowerCase().startsWith("select ")) {
			int pageSize = 20;
			int page = 0;
			try {
				if (parameters.containsKey("PAGE")) {
					page = Integer.parseInt(parameters.get("PAGE"));
				}
				if (parameters.containsKey("PAGE_SIZE")) {
					pageSize = Integer.parseInt(parameters.get("PAGE_SIZE"));
				}
			} catch (NumberFormatException nfe) {
				pageSize = 20;
				page = 0;
			}
			// avoid daft values
			if (page<0) page=0;
			if (pageSize<0) pageSize=0;
			if (pageSize>32768) pageSize=32768; // big but should be sufficient...
			
			result = db.query(sb.toString(), sqlParams, page, pageSize);
		} else {
			result = db.execute(sb.toString(), sqlParams);
		}
		// System.out.println(result);
		
		return result;
	}
}
