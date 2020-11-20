package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.Arrays;
import java.util.List;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CNOVANewCrawler;

public class SaopauloCasasbahiaCrawler extends CNOVANewCrawler {

   private static final String STORE = "casasbahia";
   private static final String INITIALS = "CB";
   private static final List<String> SELLER_NAMES = Arrays.asList("Casas Bahia", "casasbahia.com.br");

   public SaopauloCasasbahiaCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
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
