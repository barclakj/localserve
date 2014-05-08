package test.bobsoft.wsm;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import bs.localserve.DB;
import bs.localserve.SQL2JSON;

public class TestMerge {

	@Test
	public void test() {
		DB db = new DB();
		db.init("org.sqlite.JDBC", "jdbc:sqlite:/home/barclakj/Documents/petl/test.db");
		
		String query = "select id, name from bob where id = {ID}";
		
		Map<String, String> params =  new HashMap<String, String>();
		
		params.put("ID", "2");
		params.put("PAGE", "0");
		params.put("PAGE_SIZE", "20");
		
		System.out.println(SQL2JSON.parseAndExecute(query, params, db));
	}

}
