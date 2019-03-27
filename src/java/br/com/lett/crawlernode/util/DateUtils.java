package br.com.lett.crawlernode.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.joda.time.DateTimeZone;

public class DateUtils {

  public static final DateTimeZone timeZone = DateTimeZone.forID("America/Sao_Paulo");
  
  	/**
	 * Generates a timestamp in UTC timezone
	 * @return
	 */
	public static String newTimestamp() {
		ZonedDateTime zdt = ZonedDateTime.now(ZoneId.of("UTC"));
		return zdt.toInstant().toString(); // to instant represents a timestamp
	}

}
