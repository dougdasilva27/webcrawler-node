
package br.com.lett.crawlernode.core.parser;

public class TextParseData {

  private String textContent;

  public String getTextContent() {
    return textContent;
  }

  public void setTextContent(String textContent) {
    this.textContent = textContent;
  }

  @Override
  public String toString() {
    return textContent;
  }
}