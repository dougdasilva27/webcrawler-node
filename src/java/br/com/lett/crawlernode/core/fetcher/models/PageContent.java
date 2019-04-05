package br.com.lett.crawlernode.core.fetcher.models;

import java.io.IOException;
import java.nio.charset.Charset;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import br.com.lett.crawlernode.core.parser.HtmlParseData;
import br.com.lett.crawlernode.core.parser.TextParseData;

/**
 * This class contains the content fetched from a http request and also it's parsed content.
 * 
 * @author Samir Leão
 *
 */
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
   * The ContentType of this page. For example: "text/html; charset=UTF-8"
   */
  private String contentType;

  /**
   * The encoding of the content. For example: "gzip"
   */
  private String contentEncoding;

  /**
   * The charset of the content. For example: "UTF-8"
   */
  private String contentCharset;

  /**
   * Language of content
   */
  private String language;

  /**
   * Status code
   */
  private int statusCode;

  /**
   * Content parsed as html
   */
  private HtmlParseData htmlParseData;

  /**
   * Content parsed as plain text
   */
  private TextParseData textParseData;

  public PageContent(HttpEntity entity) throws IOException {

    // setting content type
    setContentType(null);
    Header type = entity.getContentType();
    if (type != null) {
      setContentType(type.getValue());
    }

    // setting content encoding
    setContentEncoding(null);
    Header encoding = entity.getContentEncoding();
    if (encoding != null) {
      setContentEncoding(encoding.getValue());
    }

    String contentTypeString = type != null ? type.getValue() : null;

    if (contentTypeString != null && !contentTypeString.contains("charset=none")) {
      // setting charset
      Charset charset = ContentType.getOrDefault(entity).getCharset();
      if (charset != null) {
        setContentCharset(charset.displayName());
      }
    }

    // setting content data
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

  public int getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(int statusCode) {
    this.statusCode = statusCode;
  }

  public TextParseData getTextParseData() {
    return textParseData;
  }

  public void setTextParseData(TextParseData textParseData) {
    this.textParseData = textParseData;
  }

  public HtmlParseData getHtmlParseData() {
    return htmlParseData;
  }

  public void setHtmlParseData(HtmlParseData htmlParseData) {
    this.htmlParseData = htmlParseData;
  }

}
