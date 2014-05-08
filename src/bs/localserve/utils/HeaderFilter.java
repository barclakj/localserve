package bs.localserve.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

/**
 * Header filter used to obtain headers from http requests.
 * @author barclakj
 *
 */
public class HeaderFilter extends Filter {

    @Override
    public String description() {
        return "Parses the request headers";
    }
    
    /**
	 * Gets the parameters at the end of a HTTP request. Uses a HashMap to keep track of the HTTP request parameters.
	 * @param exchange the HttpExchange you want parameters from
	 * @return a HashMap that contains the keys and their values as String
	 */
	private Map<String,String[]> getHeaders(HttpExchange exchange) {
		Map<String,String[]> hash = new HashMap<String,String[]>();
		Set<String> keySet = exchange.getRequestHeaders().keySet();
		Iterator<String> iter = keySet.iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			List values = exchange.getRequestHeaders().get(key);
			String[] table = new String[values.size()];
			int index = 0;
			for (Iterator i = values.iterator(); i.hasNext();) {
				table[index] = (String)i.next();
				// System.out.println(key + " = " + table[index]);
				index++;
			}
			hash.put(key, table);
		}
		return hash;
	}

    @Override
    public void doFilter(HttpExchange exchange, Chain chain)
        throws IOException {
    	parseHeaderParameters(exchange);
        chain.doFilter(exchange);
    }    

    private void parseHeaderParameters(HttpExchange exchange)
        throws UnsupportedEncodingException {

    	Map<String,String[]> headers = getHeaders(exchange);
        exchange.setAttribute("headers", headers);
    }

 
}
