package br.com.lett.crawlernode.util;

import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Version {

	private static Logger logger = LoggerFactory.getLogger(Version.class);
	
	public synchronized String getVersion() {
	    String version = null;

	    // try to load from maven properties first
	    try {
	        Properties p = new Properties();
	        InputStream is = getClass().getResourceAsStream("/META-INF/maven/history/history/pom.properties");
	        if (is != null) {
	            p.load(is);
	            version = p.getProperty("version", "");
	        }
	    } catch (Exception e) {
	       Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
	    }

	    // fallback to using Java API
	    if (version == null) {
	        Package aPackage = getClass().getPackage();
	        if (aPackage != null) {
	            version = aPackage.getImplementationVersion();
	            if (version == null) {
	                version = aPackage.getSpecificationVersion();
	            }
	        }
	    }

	    if (version == null) {
	        // we could not compute the version so use a blank
	        version = "";
	    }

	    return version;
	}
}
