package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.Arrays;
import java.util.List;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.CNOVANewCrawler;

public class SaopauloPontofrioCrawler extends CNOVANewCrawler {

   private static final String STORE = "pontofrio";
   private static final String INITIALS = "PF";
   private static final List<String> SELLER_NAMES = Arrays.asList("Pontofrio", "pontofrio.com");

   public SaopauloPontofrioCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStore() {
      return STORE;
   }

   @Override
   protected List<String> getSellerName() {
      return SELLER_NAMES;
   }

   @Override
   protected String getInitials() {
      return INITIALS;
   }
}
