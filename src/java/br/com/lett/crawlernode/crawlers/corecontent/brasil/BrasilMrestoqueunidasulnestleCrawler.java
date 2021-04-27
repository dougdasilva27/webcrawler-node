package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.MrestoqueunidasulCrawler;

/**
 * 27/04/2021
 *
 * @author Thainá Aguiar
 */

public class BrasilMrestoqueunidasulnestleCrawler extends MrestoqueunidasulCrawler {


   public BrasilMrestoqueunidasulnestleCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getPassword() {
      return "Nestle2021";
   }

   @Override
   protected String getLogin() {
      return "thaispadua93@gmail.com";
   }

}

