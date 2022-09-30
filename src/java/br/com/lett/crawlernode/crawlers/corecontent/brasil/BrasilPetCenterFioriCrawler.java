package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import com.google.common.collect.Sets;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BrasilPetCenterFioriCrawler extends Crawler {

   public BrasilPetCenterFioriCrawler(Session session) {
      super(session);
   }
   private static final String HOME_PAGE="https://www.petcenterfiore.com.br/";
   private static final String SELLER_NAME = "PetCenterFiore";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AMEX.toString(),Card.DINERS.toString(), Card.AURA.toString(),
      Card.ELO.toString(), Card.HIPER.toString(), Card.HIPERCARD.toString(), Card.DISCOVER.toString());

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      Element product = doc.selectFirst(".js-has-new-shipping.js-product-detail.js-product-container.js-shipping-calculator-container");
      if( product != null) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product,".js-product-form input", "value");
            String dataVariants = CrawlerUtils.scrapStringSimpleInfoByAttribute(product,".js-has-new-shipping.js-product-detail.js-product-container.js-shipping-calculator-container","data-variants");
            Integer spotlitePrice = CrawlerUtils.scrapPriceInCentsFromHtml(product,".js-price-display.text-primary",
               null,false,',',session,0);
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product,".js-compare-price-display.price-compare.font-weight-normal",null,false,',',
               session, 0);
           // String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(product,"")
      }
      return products;
   }
}
