package hpr.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class DateUtil {

	public static Date strToDate14( final String strDate ) {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
			return sdf.parse( strDate );
		} catch (ParseException e) {
			return new Date();
		}
	}
	
	public static String dateToStr14( final Date date ) {
		if( null == date )
			return "";
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		return sdf.format( date );
	}

	public static String dateToStr14( final Date date, final String dafault ) {
		if( null == date ) {
			return dafault;
		}
		return dateToStr14(date);
	}
	
	public static Date strToDate12( final String strDate ) {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
			return sdf.parse( strDate );
		} catch (ParseException e) {
			return new Date();
		}
	}
	
	public static String dateToStr12( final Date date ) {
		if( null == date )
			return "";
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		return sdf.format( date );
	}
	
	public static String dateToStr12( final Date date, final String dafault ) {
		if( null == date ) {
			return dafault;
		}
		return dateToStr12(date);
	}
	

	public static String getNextMonth( String yyyymm ) {
		Calendar calendar = new GregorianCalendar(Integer.parseInt(yyyymm.substring(0, 4))
				, Integer.parseInt(yyyymm.substring(4))-1
				, 1);
		calendar.add(Calendar.MONTH, 1);

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMM");

		return  dateFormat.format(calendar.getTime());
	}	
	
	public static String getPrevMonth( String yyyymm ) {
		Calendar calendar = new GregorianCalendar(Integer.parseInt(yyyymm.substring(0, 4))
				, Integer.parseInt(yyyymm.substring(4))-1
				, 1);
		calendar.add(Calendar.MONTH, -1);

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMM");

		return  dateFormat.format(calendar.getTime());
	}
	
	public static String getEndDayOfMonth( String yyyymm ) {
		Calendar calendar = new GregorianCalendar(Integer.parseInt(yyyymm.substring(0, 4))
				, Integer.parseInt(yyyymm.substring(4))-1
				, 1);
		
		calendar.add(Calendar.MONTH, 1);
		calendar.add(Calendar.DATE, -1);

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

		return  dateFormat.format(calendar.getTime());
		}
		
	
	// public static void main(String[] args) {

	// 	System.out.println( strToDate14("1201sssds"));
	// 	System.out.println( strToDate14("201205041100000"));
	// 	System.out.println( dateToStr14(new Date()));
	// 	System.out.println( getEndDayOfMonth("201302") );
	// }
	
}
