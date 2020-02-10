package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 30/10/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class MexicoHebCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.heb.com.mx/";
   private static final String SELLER_NAME_LOWER = "heb";

   public MexicoHebCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = crawlInternalPid(doc);
         String internalId = internalPid;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".page-title-wrapper .page-title", false);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs li:not(.home):not(.product)");
         boolean available = !doc.select(".action.tocart.primary").isEmpty();
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".gallery-placeholder img", Arrays.asList("src"),
               "https", "www.heb.com.mx/");

         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".product-image-gallery img:not(#image-main):not(#image-0)",
               Arrays.asList("data-zoom-image", "src"), "https:", "www.heb.com.mx/", primaryImage);
         String description = crawlDescription(doc);

         String ean = scrapEan(doc, ".extra-info span span[data-upc]");
         List<String> eans = new ArrayList<>();
         eans.add(ean);
         Map<String, Prices> marketplaceMap = available ? crawlMarketplace(doc, new JSONObject(), internalPid) : new HashMap<>();
         Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, Arrays.asList(SELLER_NAME_LOWER), Card.AMEX, session);
         Prices prices = marketplaceMap.containsKey(SELLER_NAME_LOWER) ? marketplaceMap.get(SELLER_NAME_LOWER) : new Prices();
         Float price = CrawlerUtils.extractPriceFromPrices(prices, Arrays.asList(Card.AMEX));

         // Creating the product
         Product product = ProductBuilder
               .create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPrice(price)
               .setPrices(prices)
               .setAvailable(available)
               .setCategory1(categories.getCategory(0))
               .setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2))
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setStock(null)
               .setMarketplace(marketplace)
               .setEans(eans).build();

         products.add(product);

      }

      else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return !doc.select("input[name=product]").isEmpty();
   }


   private Map<String, Prices> crawlMarketplace(Document doc, JSONObject jsonSku, String internalPid) {
      Map<String, Prices> marketplace = new HashMap<>();

      String sellerName;

      Element seller = doc.selectFirst(".infoProduct strong a.btn");
      if (seller != null) {
         sellerName = seller.ownText().toLowerCase().trim();
      } else {
         sellerName = SELLER_NAME_LOWER;
      }

      marketplace.put(sellerName, crawlPrices(doc, jsonSku, internalPid));

      return marketplace;
   }

   private String crawlInternalPid(Document doc) {
      String internalPid = null;

      Element pid = doc.selectFirst("input[name=product]");
      if (pid != null) {
         internalPid = pid.val();
      }

      return internalPid;
   }


   private String crawlDescription(Document doc) {
      StringBuilder description = new StringBuilder();

      Elements descriptions = doc.select(".product-collateral dd.tab-container");
      for (Element e : descriptions) {
         if (e.select("#customer-reviews").isEmpty()) {
            description.append(e.html());
         }
      }

      return description.toString();
   }

   private Prices crawlPrices(Document doc, JSONObject jsonSku, String internalPid) {
      Prices prices = new Prices();

      Float price = CrawlerUtils.scrapSimplePriceFloatWithDots(doc, "#product-price-" + internalPid, false);
      if (price == null) {
         price = CrawlerUtils.scrapSimplePriceFloatWithDots(doc, ".price-info .special-price .price", false);
      }

      if (jsonSku.has("price")) {
         price = MathUtils.parseFloatWithDots(jsonSku.get("price").toString());
      }

      prices.setPriceFrom(CrawlerUtils.scrapSimplePriceDoubleWithDots(doc, "#old-price-" + internalPid, false));
      if (jsonSku.has("oldPrice")) {
         prices.setPriceFrom(MathUtils.parseDoubleWithDot(jsonSku.get("oldPrice").toString()));
      }

      if (price != null) {
         Map<Integer, Float> installmentPriceMap = new TreeMap<>();
         installmentPriceMap.put(1, price);

         prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      }

      return prices;
   }

   private String scrapEan(Document doc, String selector) {
      String ean = null;
      Element e = doc.selectFirst(selector);

      if (e != null) {
         String aux = e.attr("data-upc");
         ean = aux.length() == 12 ? "0" + aux : aux;
      }

      return ean;
   }
}
