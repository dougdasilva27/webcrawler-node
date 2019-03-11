package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.DrogariaMinasbrasilNetCrawler;

/**
 * Date: 08/08/2017 - Remade date: 11/03/2019
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilDrogarianetCrawler extends DrogariaMinasbrasilNetCrawler {

  public BrasilDrogarianetCrawler(Session session) {
    super(session);
    super.host = "www.drogarianet.com.br";
  }
}
