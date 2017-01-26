package br.com.lett.crawlernode.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import br.com.lett.crawlernode.database.DatabaseDataFetcher;

public class TestHtmlBuilder {
	
	private static final Logger logger = LoggerFactory.getLogger(TestHtmlBuilder.class);
	
	public void buildProductHtml(JSONObject productJson, String path) {
		MustacheFactory mustacheFactory = new DefaultMustacheFactory();
		File file = new File("/home/gabriel/Desktop/htmls/crawler_frontend.html");
		
		Mustache mustache = null;
		try {
			mustache = mustacheFactory.compile(new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8")), file.getName());
		} catch (FileNotFoundException e) {
			Logging.printLogError(logger, CommonMethods.getStackTrace(e));
		}
		
		if(mustache != null) {
			
		}
	}
	
	private String crawlInternalId(JSONObject productJson) {
		String internalId = null;
		
		return internalId;
	}
}
