package bs.localserve;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.json.parsers.JSONParser;
import com.json.parsers.JsonParserFactory;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * SQL handler. On each request the configuration JSON file is read
 * and the named SQL command executed. HTTP parameters are bound to
 * named place-holders in the SQL statements. If the SQL command is an
 * insert/update/delete then a subsequent redirect can return the 
 * user to a new page or SQL command (i.e. chaining them together).
 * @author barclakj
 *
 */
public class SQLServeHandler implements HttpHandler {
	private DB database = null;
	private String rootPath = "/sql/";
	private String configFile = null;
	private Map<String, String> operationMap = new HashMap<String, String>();
	private Map<String, String> redirectMap = new HashMap<String, String>();
	private Map<String, String> methodMap = new HashMap<String, String>();
	
	public SQLServeHandler(String _path, String configFile) throws FileNotFoundException {
		super();
		rootPath = _path;
		this.configFile = configFile;
	}
	
	public String getRootPath() {
		return rootPath;
	}
	
	@SuppressWarnings("unchecked")
	public void handle(HttpExchange exchange) throws IOException {
		long start = System.currentTimeMillis();
		Map<String, Object> params = (Map<String, Object>)exchange.getAttribute("parameters");
		Map<String, String[]> headers = (Map<String,String[]>)exchange.getAttribute("headers");
		String path = exchange.getRequestURI().getPath();
		String result = null;
		String method = exchange.getRequestMethod();
		int rcode = -1;
		byte[] data = null;
		
		this.loadJSON(configFile);
		String opPath = path.substring(rootPath.length());  
		
		if (operationMap.containsKey(opPath) && methodMap.containsKey(opPath) && methodMap.get(opPath).indexOf(method)>=0) { // check valid path and method is supported
			String opStmt = operationMap.get(opPath);
			
			Map<String, String> mergeParams = new HashMap<String, String>();;
			for (String key : params.keySet()) {
				Object v = params.get(key);
				if (v==null) mergeParams.put(key, null);
				else {
					mergeParams.put(key, v.toString());
				}
			}
			
			for (String key : headers.keySet()) {
				if (mergeParams.containsKey(key)) {
					mergeParams.remove(key); // headers override parameters
				}
				String[] v = headers.get(key);
				if (v==null || v.length==0) mergeParams.put(key, null);
				else {
					StringBuffer b = new StringBuffer();
					for(int i=0;i<v.length;i++) {
						if (i!=0) b.append("|");
						b.append(v[i]);
					}
					mergeParams.put(key, b.toString());
					b = null;
				}
			}
			
			try {
				result = SQL2JSON.parseAndExecute(opStmt, mergeParams, database);
			} catch (RuntimeException e) {
				e.printStackTrace();
				result = null;
			}
			if (result!=null) {
				data = result.getBytes();
				
				if (redirectMap.containsKey(opPath)) {
					
					String redirectLocation = "" + redirectMap.get(opPath) + "" + getRedirectParams(params);
					// exchange.getResponseHeaders().set("HTTP/1.1", "302 Temporary Redirect");
					exchange.getResponseHeaders().set("Location", redirectLocation);
					exchange.sendResponseHeaders(302, 0);
					rcode=302;
					result = redirectLocation;
					// System.out.println(redirectLocation);
				} else {
					if (data==null) {
						result = "Internal Server Error";
						exchange.sendResponseHeaders(500, result.length());
						rcode = 500;
						data = result.getBytes();
					} else {
						exchange.sendResponseHeaders(200, data.length);
						rcode = 200;
					}
				}
			} else {
				// check logs for error. Here all I know is a null response aint right...
				result = "Internal Server Error";
				exchange.sendResponseHeaders(500, result.length());
				rcode = 500;
				data = result.getBytes();
			}
		} else {
			result = "404 Not Found";
			exchange.sendResponseHeaders(404, result.length());
			rcode = 404;
			data = result.getBytes();
		}

		if (result!=null) {
			BufferedOutputStream baos = new BufferedOutputStream(exchange.getResponseBody());
			baos.write(data, 0, data.length);
			//baos.flush();
			baos.close();
			baos = null;
		}
		long end = System.currentTimeMillis();
		System.out.println((new Date()).toLocaleString() + "|SQL|" + path + "|" + rcode + "|" + (end-start) + "ms");
		System.out.flush();
	}
	
	private String getRedirectParams(Map<String, Object> params) throws UnsupportedEncodingException {
		StringBuffer qs = new StringBuffer();
		qs.append("?");
		for (String key : params.keySet()) {
			Object v = params.get(key);
			if (v==null) {
				qs.append(key + "=&");
			} else {
				qs.append(key + "=" + URLEncoder.encode(v.toString(), "UTF-8") + "&");
			}
		}
		return qs.toString();
	}

	private void setConnection(String connString, String driver) {
		database = new DB();
		database.init(driver, connString);
	}
	
	private void loadJSON(String filename) throws FileNotFoundException {
		JsonParserFactory factory=JsonParserFactory.getInstance();
		JSONParser parser=factory.newJsonParser();
		Map jsonData=parser.parseJson(new FileInputStream(filename), "UTF-8");
		
		operationMap.clear();
		redirectMap.clear();
		methodMap.clear();
		
		ArrayList operations = (ArrayList)(((Map)jsonData.get("wsm")).get("operations"));
		for(int i=0;i<operations.size();i++) {
			String operationPath = (String)((Map)(operations.get(i))).get("path");
			String operationStatement = (String)((Map)(operations.get(i))).get("statement");
			String redirect = (String)((Map)(operations.get(i))).get("redirect");
			String methods = (String)((Map)(operations.get(i))).get("methods");
			
			operationMap.put(operationPath, operationStatement);
			methodMap.put(operationPath, methods);
			
			if (redirect!=null) {
				redirectMap.put(operationPath, redirect);
			}
		}
		
		this.setConnection( (String)(((Map)jsonData.get("wsm")).get("connString")),
						(String)(((Map)jsonData.get("wsm")).get("driver"))
						);
	}
}
