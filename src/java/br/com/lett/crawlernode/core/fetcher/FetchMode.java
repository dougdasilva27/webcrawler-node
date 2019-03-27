package br.com.lett.crawlernode.core.fetcher;

public enum FetchMode {

  STATIC, WEBDRIVER, FETCHER, JAVANET;

  @Override
  public String toString() {
    switch (this) {
      case STATIC:
        return STATIC.name();
      case WEBDRIVER:
        return WEBDRIVER.name();
      case FETCHER:
        return FETCHER.name();
      case JAVANET:
        return JAVANET.name();
      default:
        return "";
    }
  }

}
