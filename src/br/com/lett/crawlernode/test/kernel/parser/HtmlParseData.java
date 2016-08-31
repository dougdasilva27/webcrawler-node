
package br.com.lett.crawlernode.test.kernel.parser;

import java.util.Map;

public class HtmlParseData {

	private String html;
	private String text;
	private String title;
	private Map<String, String> metaTags;

	public String getHtml() {
		return html;
	}

	public void setHtml(String html) {
		this.html = html;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Map<String, String> getMetaTags() {
		return metaTags;
	}

	public void setMetaTags(Map<String, String> metaTags) {
		this.metaTags = metaTags;
	}

	@Override
	public String toString() {
		return text;
	}
}