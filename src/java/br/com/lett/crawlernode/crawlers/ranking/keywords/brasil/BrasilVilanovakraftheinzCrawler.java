package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BrasilVilanova;

public class BrasilVilanovakraftheinzCrawler extends BrasilVilanova {

  public BrasilVilanovakraftheinzCrawler(Session session) {
    super(session);
  }

  @Override
  protected String getCnpj() {
    return null;
  }

  @Override
  protected String getPassword() {
    return null;
  }
}
