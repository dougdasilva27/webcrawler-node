package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.TrayCommerceCrawler;

public class BrasilAnimalshowstoreCrawler extends TrayCommerceCrawler {

  @Override
  protected String setStoreId() {
    return "753303";
  }

  public BrasilAnimalshowstoreCrawler(Session session) {
    super(session);
  }

  @Override
  protected String setHomePage() {
    return "https://www.animalshowstore.com.br/";
  }
}
