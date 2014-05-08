package bs.localserve;

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
import java.util.Hashtable;
import java.util.Map;


import com.json.parsers.JSONParser;
import com.json.parsers.JsonParserFactory;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class RootServeHandler implements HttpHandler {
	private String rootPath = "/";
	private HttpHandler sqlHandler = null;
	
	public RootServeHandler(String _path, HttpHandler sql) throws FileNotFoundException {
		super();
		rootPath = _path;
		sqlHandler = sql;
	}
	
	public String getRootPath() {
		return rootPath;
	}
	
	@SuppressWarnings("unchecked")
	public void handle(HttpExchange exchange) throws IOException {
		long start = System.currentTimeMillis();
		int rcode = -1;
		String path = exchange.getRequestURI().getPath();
		
		if (path.startsWith("/sql/") && sqlHandler!=null) {
			sqlHandler.handle(exchange);
		} else {
			// if no file specified for dir assume index page
			if (path.endsWith("/")) {
				path = path + "index.html";
			}
			String opPath = path.substring(rootPath.length());  
			OutputStream os = exchange.getResponseBody();
			
			File f = new File(opPath);
			if (f.exists()) {
				FileInputStream fis = new FileInputStream(f);
				String mimeType = getMimeType(opPath);
				
				exchange.getResponseHeaders().set("Content-Type", mimeType);
				exchange.sendResponseHeaders(200, f.length());
				
				byte[] data = new byte[64000];
				int r = 0;
				while ((r=fis.read(data, 0, 64000))>0) {
					os.write(data, 0, r);				
				}
				fis.close();
				fis = null;
				rcode = 200;
			} else {
				String result = "404 Not Found";
				exchange.sendResponseHeaders(404, result.length());
				os.write(result.getBytes());
				rcode = 404;
			}
			
			os.flush();
			os.close();
			os = null;
			long end = System.currentTimeMillis();
			System.out.println((new Date()).toLocaleString() + "|Root|" + path + "|" + rcode + "|" + (end-start) + "ms");
			System.out.flush();
		}
	}

	public final static String DEFAULT_MIME_TYPE = "application/octet-stream";
	private static Hashtable<String, String> mimeTypes = new Hashtable<String, String>();
	
	static {
		mimeTypes.put("jpeg","image/jpeg");
		mimeTypes.put("jpg","image/jpeg");
		mimeTypes.put("jpe","image/jpeg");
		mimeTypes.put("png","image/png");
		mimeTypes.put("bmp","image/bmp");
		mimeTypes.put("gif","image/gif");
		mimeTypes.put("tiff","image/tiff");
		mimeTypes.put("tif","image/tiff");
		mimeTypes.put("pdf","application/pdf");
		mimeTypes.put("doc","application/doc");
		mimeTypes.put("xls","application/xls");
		mimeTypes.put("ppt","application/ppt");
		mimeTypes.put("csv","application/csv");
		mimeTypes.put("zip","application/zip");
		mimeTypes.put("txt","text/plain");
		mimeTypes.put("xml","text/xml");
		mimeTypes.put("xsl","text/xml");
		mimeTypes.put("xsd","text/xml");
		mimeTypes.put("txt","text/plain");
		mimeTypes.put("sh","text/plain");
		mimeTypes.put("css","text/css");
		mimeTypes.put("log","text/plain");
		mimeTypes.put("html","text/html");
		mimeTypes.put("htm","text/html");
		mimeTypes.put("qt","video/quicktime");
		mimeTypes.put("mov","video/quicktime");
		mimeTypes.put("mpeg","video/mpeg");
		mimeTypes.put("mpg","video/mpeg");
		mimeTypes.put("mpe","video/mpeg");
		mimeTypes.put("avi","video/x-msvideo");
		mimeTypes.put("js","application/javascript");
	}
	
	public static String getMimeType(String path) {
		String mimeType = DEFAULT_MIME_TYPE;
		
		if (path!=null) {
		
			String extension = path.substring(path.lastIndexOf(".")+1);
		
			if (extension!=null) {
				extension = extension.toLowerCase();
			
				if (mimeTypes.get(extension)!=null) {
					mimeType = (String)mimeTypes.get(extension);
				}
			}
		}
		
		return mimeType;
	}
	
	public static String getRootMimeType(String path) {
		String mimeType = getMimeType(path);
		
		return mimeType.substring(0, mimeType.indexOf("/"));
	}
}
