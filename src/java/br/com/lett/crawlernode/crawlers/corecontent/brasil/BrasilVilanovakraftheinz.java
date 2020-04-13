package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilVilanova;

public class BrasilVilanovakraftheinz extends BrasilVilanova {

  public BrasilVilanovakraftheinz(Session session) {
    super(session);
  }

  @Override
  public String getCnpj() {
    return "lorenzo.lamas@kraftheinz.com";
  }

  @Override
  public String getPassword() {
    return "24373852";
  }
}
