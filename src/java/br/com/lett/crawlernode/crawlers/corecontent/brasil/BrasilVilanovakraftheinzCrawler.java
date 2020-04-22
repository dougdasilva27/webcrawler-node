package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilVilanova;

public class BrasilVilanovakraftheinzCrawler extends BrasilVilanova {

  public BrasilVilanovakraftheinzCrawler(Session session) {
    super(session);
  }

  @Override
  public String getCnpj() {
    return "";
  }

  @Override
  public String getPassword() {
    return "";
  }
}
