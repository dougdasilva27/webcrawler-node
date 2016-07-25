package br.com.lett.crawlernode.parser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.apache.tika.language.LanguageIdentifier;
import org.apache.tika.metadata.DublinCore;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.ToHTMLContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.fetcher.PageContent;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;


public class Parser {

	protected static final Logger logger = LoggerFactory.getLogger(Parser.class);

	private final HtmlParser htmlParser;
	private final ParseContext parseContext;

	public Parser() {
		htmlParser = new HtmlParser();
		parseContext = new ParseContext();
	}

	public void parse(PageContent pageContent) {

		/*
		 * binary
		 */
		if (CommonMethods.hasBinaryContent(pageContent.getContentType())) {
			BinaryParseData parseData = new BinaryParseData();
			parseData.setBinaryContent(pageContent.getContentData());

			pageContent.setBinaryParseData(parseData);
			if (parseData.getHtml() == null) {
				Logging.printLogError(logger, "Error parsing binary content [" + pageContent.getUrl() + "]");
			}

		} 

		/*
		 * plain text
		 */
		else if (CommonMethods.hasPlainTextContent(pageContent.getContentType())) {
			try {
				TextParseData parseData = new TextParseData();
				if (pageContent.getContentCharset() == null) {
					parseData.setTextContent(new String(pageContent.getContentData()));
				} else {
					parseData.setTextContent(new String(pageContent.getContentData(), pageContent.getContentCharset()));
				}
				pageContent.setTextParseData(parseData);
			} catch (Exception e) {
				logger.error("{}, while parsing: {}", e.getMessage(), pageContent.getUrl());
			}
		} 

		/*
		 * html
		 */
		else {
			Metadata metadata = new Metadata();
			ToHTMLContentHandler contentHandler = new ToHTMLContentHandler();
			
			try (InputStream inputStream = new ByteArrayInputStream(pageContent.getContentData())) {
				htmlParser.parse(inputStream, contentHandler, metadata, parseContext);
			} catch (Exception e) {
				logger.error("{}, while parsing: {}", e.getMessage(), pageContent.getUrl());
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
				logger.error("error parsing the html: " + pageContent.getUrl(), e);
			}
		}
	}

}
