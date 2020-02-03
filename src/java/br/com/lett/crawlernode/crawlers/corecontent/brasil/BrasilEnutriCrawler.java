package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

/**
 * Date: 20/08/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilEnutriCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.enutri.com.br/";

   public BrasilEnutriCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public void handleCookiesBeforeFetch() {
      Logging.printLogDebug(logger, session, "Adding cookie...");

      BasicClientCookie cookie = new BasicClientCookie("loja", "base");
      cookie.setDomain(".www.enutri.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = crawlInternalId(doc);
         String internalPid = crawlInternalPid(doc);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".prod__name h1", false);
         Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".prod__shop:last-child .price span", null, false, ',', session);
         Prices prices = crawlPrices(price, doc);
         System.err.println(prices);
         boolean available = crawlAvailability(doc);
         CategoryCollection categories = crawlCategories(doc);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product-image-gallery img", Arrays.asList("src"), "https://", "www.enutri.com.br");
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".product-image-thumbs li:not(:first-child) a img", Arrays.asList("src"), "https://", "www.enutri.com.br", primaryImage);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".tabs__content .std"));
         RatingsReviews ratingReviews = crawlRating(internalId, doc);
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
               .setMarketplace(new Marketplace())
               .setRatingReviews(ratingReviews)
               .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private boolean isProductPage(Document doc) {
      return !doc.select(".add-to-cart-buttons").isEmpty();
   }

   private String crawlInternalId(Document doc) {
      String internalId = null;

      Element internalIdElement = doc.select("input[name=product]").first();
      if (internalIdElement != null) {
         internalId = internalIdElement.val();
      }

      return internalId;
   }

   private String crawlInternalPid(Document document) {
      String internalPid = null;
      Element codElement = document.select("#display_product_name span").first();

      if (codElement != null) {
         internalPid = CommonMethods.getLast(codElement.ownText().replace("(", "").replace(")", "").split("\\.")).trim();
      }

      return internalPid;
   }

   private CategoryCollection crawlCategories(Document document) {
      CategoryCollection categories = new CategoryCollection();
      Elements elementCategories = document.select(".breadcrumb ul li span");

      for (Element e : elementCategories) {
         String cat = e.ownText().trim();

         if (!cat.isEmpty()) {
            categories.add(cat);
         }
      }

      return categories;
   }

   private boolean crawlAvailability(Document doc) {
      return !doc.select(".add-to-cart-buttons").isEmpty();
   }

   /**
    * 
    * @param doc
    * @param price
    * @return
    */
   private Prices crawlPrices(Float price, Document doc) {
      Prices prices = new Prices();

      if (price != null) {
         Map<Integer, Float> installments = new HashMap<>();
         installments.put(1, price);
         prices.setBankTicketPrice(CrawlerUtils.scrapFloatPriceFromHtml(doc, ".col2 .price-box-avista .price span", null, false, ',', session));
         prices.setPriceFrom(CrawlerUtils.scrapDoublePriceFromHtml(doc, ".prod__shop:last-child .price span", null, true, ',', session));

         Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(".col2 .price-box-parcelado .preco-parcelado span", doc, true, "x", "sem", false, ',');

         if (!pair.isAnyValueNull()) {
            installments.put(pair.getFirst(), pair.getSecond());
         }

         prices.insertCardInstallment(Card.VISA.toString(), installments);
         prices.insertCardInstallment(Card.MASTERCARD.toString(), installments);
         prices.insertCardInstallment(Card.DINERS.toString(), installments);
         prices.insertCardInstallment(Card.HIPERCARD.toString(), installments);
         prices.insertCardInstallment(Card.AMEX.toString(), installments);
         prices.insertCardInstallment(Card.ELO.toString(), installments);
      }

      return prices;
   }

   private RatingsReviews crawlRating(String internalId, Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      ratingReviews.setInternalId(internalId);
      ratingReviews.setAverageOverallRating(getAverageOverallRating(doc));
      ratingReviews.setTotalWrittenReviews(getTotalRating(doc));

      return ratingReviews;
   }

   private Double scrapDoubleFromAttr(Element element) {
      Double number = 0d;

      if (element.hasAttr("style")) {
         number = MathUtils.parseDoubleWithComma(element.attr("style"));

      }

      return number;
   }

   private Double getAverageOverallRating(Document document) {

      Element ratingElement = document.selectFirst(".rating");
      Double avg = 0d;

      if (ratingElement != null) {
         avg = (scrapDoubleFromAttr(ratingElement) / 100) * 5;
      }

      return avg;
   }

   private Integer getTotalRating(Document document) {
      return CrawlerUtils.scrapIntegerFromHtml(document, ".rating-links", false, 0);
   }
}
