package hpr.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Except {
	public static String getStackTrace( Exception ex ) {
		
		StringWriter errors = new StringWriter();
		ex.printStackTrace(new PrintWriter(errors));
		return errors.toString();
	}
}