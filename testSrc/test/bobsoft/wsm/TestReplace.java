package test.bobsoft.wsm;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestReplace {

	@Test
	public void test() {
		String t = "this \" is 'a \\simple \" test";
		System.out.println(jsonify(t));
		
		
	}

	public static String jsonify(String t) {
		if (t!=null) {
			return t.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"").replaceAll("'", "\\\\'");
		} else return null;
	}
}
