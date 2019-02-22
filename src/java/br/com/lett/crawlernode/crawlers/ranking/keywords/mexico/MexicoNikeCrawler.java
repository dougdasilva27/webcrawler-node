package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.NikeCrawler;

public class MexicoNikeCrawler extends NikeCrawler {

  public MexicoNikeCrawler(Session session) {
    super(session);
  }

  @Override
  protected String buildUrl() {
    return "https://store.nike.com/html-services/gridwallData?country=MX&lang_locale=es_LA&gridwallPath=n/1j7&pn=2&anchor="
        + (this.currentPage - 1) * this.pageSize + "&sl=" + this.keywordEncoded;
  }
}
