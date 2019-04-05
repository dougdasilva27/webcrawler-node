package br.com.lett.crawlernode.crawlers.ranking.keywords.unitedstates;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.NikeCrawler;

public class UnitedstatesNikeCrawler extends NikeCrawler {
  public UnitedstatesNikeCrawler(Session session) {
    super(session);
  }

  @Override
  protected String buildUrl() {
    String key = this.keywordWithoutAccents.replaceAll(" ", "%20");

    return "https://store.nike.com/html-services/gridwallData?gridwallPath=n%2F1j7&country=US&lang_locale=en_US&pn=2&sl=" + key + "&anchor="
        + (this.currentPage - 1) * this.pageSize + "&vst=" + key;
  }
}
