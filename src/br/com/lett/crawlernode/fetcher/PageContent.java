package br.com.lett.crawlernode.fetcher;

import java.nio.charset.Charset;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;

public class PageContent {
	
	/**
	 * The url
	 */
	private String url;

	/**
	 * The content of this page in binary format.
	 */
	private byte[] contentData;

	/**
	 * The ContentType of this page.
	 * For example: "text/html; charset=UTF-8"
	 */
	private String contentType;

	/**
	 * The encoding of the content.
	 * For example: "gzip"
	 */
	private String contentEncoding;

	/**
	 * The charset of the content.
	 * For example: "UTF-8"
	 */
	private String contentCharset;
	
	/**
	 * Language of content
	 */
	private String language;
	
	private HttpEntity entity;
	

	public PageContent(HttpEntity entity) {
		this.entity = entity;
	}

	public void load() throws Exception {

		setContentType(null);
		Header type = entity.getContentType();
		if (type != null) {
			setContentType(type.getValue());
		}

		setContentEncoding(null);
		Header encoding = entity.getContentEncoding();
		if (encoding != null) {
			setContentEncoding(encoding.getValue());
		}

		Charset charset = ContentType.getOrDefault(entity).getCharset();
		if (charset != null) {
			setContentCharset(charset.displayName());
		}

		setContentData(EntityUtils.toByteArray(entity));
	}


	public byte[] getContentData() {
		return contentData;
	}


	public void setContentData(byte[] contentData) {
		this.contentData = contentData;
	}


	public String getContentType() {
		return contentType;
	}


	public void setContentType(String contentType) {
		this.contentType = contentType;
	}


	public String getContentEncoding() {
		return contentEncoding;
	}


	public void setContentEncoding(String contentEncoding) {
		this.contentEncoding = contentEncoding;
	}


	public String getContentCharset() {
		return contentCharset;
	}


	public void setContentCharset(String contentCharset) {
		this.contentCharset = contentCharset;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

}
