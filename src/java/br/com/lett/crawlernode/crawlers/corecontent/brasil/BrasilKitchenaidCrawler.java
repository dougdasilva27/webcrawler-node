package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.Arrays;
import java.util.List;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXNewScraper;

public class BrasilKitchenaidCrawler extends VTEXNewScraper {

   private static final String HOME_PAGE = "https://www.kitchenaid.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "kitchenaid";

   public BrasilKitchenaidCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
   }

   @Override
   protected Product extractProduct(String internalPid, CategoryCollection categories, String description, JSONObject jsonSku) throws Exception {
      Product product = super.extractProduct(internalPid, categories, description, jsonSku);
      return product;
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Arrays.asList(MAIN_SELLER_NAME_LOWER);
   }

   @Override
   protected List<Card> getCards() {
      return Arrays.asList(Card.AMEX, Card.SHOP_CARD);
   }

}
