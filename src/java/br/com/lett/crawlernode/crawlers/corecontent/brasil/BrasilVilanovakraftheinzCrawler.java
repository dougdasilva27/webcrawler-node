package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.Vilanova;

public class BrasilVilanovakraftheinzCrawler extends Vilanova {

  public BrasilVilanovakraftheinzCrawler(Session session) {
    super(session);
  }

  @Override
  public String getCNPJ() {
    return "lorenzo.lamas@kraftheinz.com";
  }

  @Override
  public String getPassword() {
    return "24373852";
  }
}
