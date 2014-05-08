package bs.localserve;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;


import com.json.parsers.JSONParser;
import com.json.parsers.JsonParserFactory;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * Python execution handler for dynamic server side content.
 * **** TEST CODE - NOT USED ****
 * @author barclakj
 *
 */
public class PyServeHandler implements HttpHandler {
	private static Logger log = Logger.getLogger(PyServeHandler.class.getCanonicalName());
	private DB database = null;
	private String rootPath = "/py/";
	private Map<String, String> operationMap = new HashMap<String, String>();
	private Map<String, String> redirectMap = new HashMap<String, String>();
	private Map<String, String> methodMap = new HashMap<String, String>();
	
	public PyServeHandler(String _path) throws FileNotFoundException {
		super();
		rootPath = _path;
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
		OutputStream os = exchange.getResponseBody();
		String contentType = "text/plain"; // "application/octet-stream";
		
		log.finest("Operation: " + rootPath);
		String opPath = path.substring(rootPath.length());  
		File f = new File(opPath);
		if (f.exists() && f.getAbsolutePath().toLowerCase().endsWith(".py")) {
			log.finest("Executing Python: " + f.getAbsolutePath());
			Process proc = Runtime.getRuntime().exec("python " + f.getAbsolutePath());
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			BufferedInputStream bis = new BufferedInputStream(proc.getInputStream());
			
			int read = 0;
			boolean init = true;
			data = new byte[64000];
			while ((read=bis.read(data, 0, 64000))>0) {
				baos.write(data, 0, read);
				if (init) {
					init = false;
					String d = new String(data,0,read);
					if (d.indexOf("<body>")>=0 || d.indexOf("<BODY>")>=0) {
						contentType = "text/html";
					} else if (d.indexOf("<?xml ")>=0) {
						contentType = "text/xml";
					}
				}
			}
			bis.close();
			bis = null;
			data = null;
			data = baos.toByteArray();
			
			exchange.getResponseHeaders().set("Content-Type", contentType);
			exchange.sendResponseHeaders(200, data.length);
			
			os.write(data,0, data.length);
			
			data = null;
			baos = null;
		}
		os.close();
		os = null;
		
		long end = System.currentTimeMillis();
		System.out.println((new Date()).toLocaleString() + "|Py|" + path + "|" + rcode + "|" + (end-start) + "ms");
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
