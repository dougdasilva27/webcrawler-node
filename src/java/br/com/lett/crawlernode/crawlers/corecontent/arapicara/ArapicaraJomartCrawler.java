package br.com.lett.crawlernode.crawlers.corecontent.arapicara;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.LifeappsCrawler;

public class ArapicaraJomartCrawler extends LifeappsCrawler {

   private static final String HOME_PAGE = "https://comprajomart.com.br/jomart-arapiraca/";
   private static final String COMPANY_ID = "e2e4386e-281b-40ef-b3f0-96f57c14a267";    //never changes
   private static final String API_HASH = "3a582d60-bd76-11eb-9c2c-331bdae23698cc64548c0cbad6cf58d4d3bbd433142b"; //can change, but is working since 2019
   private static final String FORMA_PAGAMENTO = "7d3e6114-5195-4f8b-a6f3-6b5f39c056aa";  //never changes
   private static final String SELLER_NAME_LOWER = "jomart";

   public ArapicaraJomartCrawler(Session session) {
      super(session);
   }

   @Override
   public String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   public String getCompanyId() {
      return COMPANY_ID;
   }

   @Override
   public String getApiHash() {
      return API_HASH;
   }

   @Override
   public String getFormaDePagamento() {
      return FORMA_PAGAMENTO;
   }

   @Override
   public String getSellerName() {
      return SELLER_NAME_LOWER;
   }
}
