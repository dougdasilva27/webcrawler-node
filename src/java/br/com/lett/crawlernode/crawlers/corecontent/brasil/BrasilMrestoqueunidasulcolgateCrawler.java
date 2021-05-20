package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.MrestoqueunidasulCrawler;

public class BrasilMrestoqueunidasulcolgateCrawler extends MrestoqueunidasulCrawler {

   public BrasilMrestoqueunidasulcolgateCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getPassword() {
      return "123456";
   }

   @Override
   protected String getLogin() {
      return "teste@mrparceiro.com.br";
   }
}
