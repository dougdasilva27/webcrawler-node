package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXScraper;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VtexConfig;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VtexConfig.CardsInfo;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VtexConfig.VtexConfigBuilder;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilAgroverdesrCrawler extends VTEXScraper {

   private static final String MAIN_SELLER_NAME = "Agroverde s. r.";
   private static final String HOME_PAGE = "https://www.agroverdesr.com.br/";
   private Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString());


   public BrasilAgroverdesrCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);

      List<CardsInfo> cardsInfo = setListOfCards(cards, null);
      VtexConfig vtexConfig = VtexConfigBuilder.create()
            .setMainSellerNames(Arrays.asList(MAIN_SELLER_NAME))
            .setHomePage(HOME_PAGE)
            .setUsePriceAPI(true)
            .setCards(cardsInfo)
            .setSalesIsCalculated(true)
            .build();

      return extractVtexInformation(doc, vtexConfig);
   }


   @Override
   protected CategoryCollection scrapCategories(Document doc, String internalId) {
      return CrawlerUtils.crawlCategories(doc, ".bread-crumb li" + CrawlerUtils.CSS_SELECTOR_IGNORE_FIRST_CHILD + " > a");
   }

   @Override
   protected String scrapDescription(Document document, JSONObject apiJSON, JSONObject skuJson, JSONObject productJson, String internalId) {
      StringBuilder description = new StringBuilder();
      Element descriptionElement = document.selectFirst(".description-specification");

      if (descriptionElement != null) {
         description.append(descriptionElement.html());
      }

      return description.toString();
   }

}
