package br.com.lett.crawlernode.kernel.parser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.apache.tika.language.LanguageIdentifier;
import org.apache.tika.metadata.DublinCore;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.BodyContentHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.kernel.fetcher.PageContent;
import br.com.lett.crawlernode.kernel.task.CrawlerSession;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

/**
 * This class is used to parse the content fetched from a web page. It can all the necessary cases
 * for the needs of the crawler node.
 * 
 * @author Samir Leao
 *
 */

public class Parser {
	protected static final Logger logger = LoggerFactory.getLogger(Parser.class);

	private final HtmlParser htmlParser;
	private final ParseContext parseContext;
	private CrawlerSession session;

	public Parser(CrawlerSession session) {
		htmlParser = new HtmlParser();
		parseContext = new ParseContext();
		this.session = session;
	}
	
	/**
	 * Parses the content fetched. This content is passed through a PageContent object,
	 * that was populated with a http entity and a http response, used in DataFetcher class.
	 * This method can handle the situations where we only want to get the html from a webpage
	 * or the case where we are expecting a JSONObject or a JSONArray as reponse from an API
	 * request. In this last case it parses it as a plain text. The PageContent passed, will be populated
	 * with the parsed data.
	 * @param pageContent an object containing the content that will be parsed
	 */
	public void parse(PageContent pageContent) {

		/*
		 * plain text - JSONObjects, JSONArrays from APIs and any other text content
		 */
		 if (CommonMethods.hasPlainTextContent(pageContent.getContentType()) || CommonMethods.hasBinaryContent(pageContent.getContentType())) {
			try {
				TextParseData parseData = new TextParseData();
				if (pageContent.getContentCharset() == null) {
					parseData.setTextContent(new String(pageContent.getContentData()));
				} else {
					parseData.setTextContent(new String(pageContent.getContentData(), pageContent.getContentCharset()));
				}
				pageContent.setTextParseData(parseData);
			} catch (Exception e) {
				Logging.printLogError(logger, session, "Error while parsing plain text [" + pageContent.getUrl() + "]"  + " " + e.getMessage());
				Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
			}
		} 

		/*
		 * html
		 */
		else {
			Metadata metadata = new Metadata();
			BodyContentHandler contentHandler = new BodyContentHandler(-1);
			
			try (InputStream inputStream = new ByteArrayInputStream(pageContent.getContentData())) {
				htmlParser.parse(inputStream, contentHandler, metadata, parseContext);
			} catch (Exception e) {
				Logging.printLogError(logger, session, "Error while parsing html [" + pageContent.getUrl() + "]"  + " " + e.getMessage());
				Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
			}

			if (pageContent.getContentCharset() == null) {
				pageContent.setContentCharset(metadata.get("Content-Encoding"));
			}

			HtmlParseData parseData = new HtmlParseData();
			parseData.setText(contentHandler.toString().trim());
			parseData.setTitle(metadata.get(DublinCore.TITLE));
			
			// Please note that identifying language takes less than 10 milliseconds
			LanguageIdentifier languageIdentifier = new LanguageIdentifier(parseData.getText());
			pageContent.setLanguage(languageIdentifier.getLanguage());

			try {
				if (pageContent.getContentCharset() == null) {
					parseData.setHtml(new String(pageContent.getContentData()));
				} else {
					parseData.setHtml(new String(pageContent.getContentData(), pageContent.getContentCharset()));
				}

				pageContent.setHtmlParseData(parseData);
				
			} catch (UnsupportedEncodingException e) {
				Logging.printLogError(logger, session, "error parsing the html: " + pageContent.getUrl() + e.getMessage());
				Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
			}
		}
	}

}
