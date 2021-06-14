package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.session.Session;

public class MercadolivreCoreNewImpl extends MercadolivreCrawler{

   public MercadolivreCoreNewImpl(Session session) {
      super(session);
      super.setHomePage(session.getOptions().optString("HomePage"));
      super.setMainSellerNameLower(session.getOptions().optString("Seller"));
   }

}
