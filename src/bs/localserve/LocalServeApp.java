package bs.localserve;

import java.net.InetSocketAddress;

import bs.localserve.utils.HeaderFilter;
import bs.localserve.utils.ParameterFilter;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;

public class LocalServeApp {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// util vars for params
		String p1 = null;
		String p2 = null;
		if (args.length>0) {
			p1 = args[0];
		}
		if (args.length>1) {
			p2 = args[1];
		}
		
		// determine which is the port number (if either).
		// default to 8765 if neither of them are valid numbers
		boolean portFound = false;
		int port = 8765;
		try {
			port = Integer.parseInt(p1);
			portFound = true;
		} catch (NumberFormatException e) { }
		if (p2!=null && !portFound) {
			try {
				port = Integer.parseInt(p2);
				portFound = true;
			} catch (NumberFormatException e) { }
		}
		if (!portFound) {
			port = 8765;
		}
		
		// determine which config file to use. note that p2 will be used if multiple config files are passed in
		String configFile = null;
		if (p1!=null && p1.toLowerCase().endsWith(".json")) {
			configFile = p1;
		}
		if (p2!=null && p2.toLowerCase().endsWith(".json")) {
			configFile = p2;
		}
		
		// establish SQL handler. Only use if config file provided.
		SQLServeHandler sqlHandler = null;
		if (configFile!=null) {
			sqlHandler = new SQLServeHandler("/sql/", configFile); // "/home/barclakj/Documents/wsm/test.json");
		}

		// bind - 4 threads - each client should have max of two connections
		// therefore sufficient for x2 concurrent clients since this should
		// not be used in any prod like environment this should be more than 
		// sufficient.
		HttpServer server = HttpServer.create(new InetSocketAddress(port), 4);
		// create and assign a file handler - for current directory.
		// if a sql handler is defined then any requests starting /sql/ will
		// be sent to the sql handler instead.
		RootServeHandler rootHandler = new RootServeHandler("/", sqlHandler);
		server.createContext(rootHandler.getRootPath(), rootHandler);
		
		// if sql handler defined (config file) then create context and 
		// add filters for headers and parameter handling
		if (sqlHandler!=null) {
			HttpContext context = server.createContext(sqlHandler.getRootPath(), sqlHandler);
			context.getFilters().add(new HeaderFilter());
	        context.getFilters().add(new ParameterFilter());
		}

		// start serving!
        server.setExecutor(null);
        server.start();
	}

}
