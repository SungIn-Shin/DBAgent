package com.hopper.dbagent;

import java.io.PrintWriter;
import java.io.StringWriter;

public class StackTrace {
	public static String toString( Exception ex ) {
		StringWriter errors = new StringWriter();
		ex.printStackTrace(new PrintWriter(errors));
		return errors.toString();
	}
}
