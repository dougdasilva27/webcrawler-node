package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilVilanova;

/**
 * Date: 09/07/2019
 *
 * @author Gabriel Dornelas
 */
public class BrasilVilanovamondelezCrawler extends BrasilVilanova {

  public BrasilVilanovamondelezCrawler(Session session) {
    super(session);
  }

  @Override
  public String getCnpj() {
    return "33033028004090";
  }

  @Override
  public String getPassword() {
    return "521542";
  }
}
