package br.com.lett.crawlernode.crawlers.corecontent.taubate;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ComprebemCrawler;

public class TaubateComprebemCrawler extends ComprebemCrawler {

   private final String HOME_PAGE = "delivery.comprebem.com.br";
   private final String MAIN_SELLER_NAME = "compre bem";
   private final String CEP = "12010-600";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected String getMainSellerName() {
      return MAIN_SELLER_NAME;
   }

   @Override
   protected String getCep() {
      return CEP;
   }

   public TaubateComprebemCrawler(Session session) {
      super(session);
   }

}
