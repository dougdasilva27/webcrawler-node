package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

public class BrasilMadeiramadeiraCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.madeiramadeira.com.br/";

   public BrasilMadeiramadeiraCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      if (isProductPage(doc)) {

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".buy-box-wrapper div[data-product-sku]", "data-product-sku");
         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "data[data-product-id]", "value");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-title", false);
         Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".txt-incash-value", null, false, ',', session);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product-image .product-featured-image", Arrays.asList(
               "data-product-image-zoom"),
               "https",
               "images.madeiramadeira.com.br");
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "#product-images-desktop .product-slider-thumbs div[data-image-zoom]",
               Arrays.asList("data-image-zoom"), "https", "images.madeiramadeira.com.br",
               primaryImage);

         Prices prices = scrapPrices(doc, price);
         RatingsReviews ratingsReviews = scrapRating(internalId, doc);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#product-attributes-tab-information .product-description"));
         Map<String, Prices> marketplaceMap = assembleMarketplaceMap(doc);
         Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, Arrays.asList("MadeiraMadeira"), Card.VISA, session);
         boolean available = CrawlerUtils.getAvailabilityFromMarketplaceMap(marketplaceMap, Arrays.asList("MadeiraMadeira"));

         // Creating the product
         Product product = ProductBuilder.create()
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
               .setMarketplace(marketplace)
               .setRatingReviews(ratingsReviews)
               .setEans(new ArrayList<String>())
               .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private Map<String, Prices> assembleMarketplaceMap(Document doc) {
      String url = session.getOriginalURL().replace(HOME_PAGE, HOME_PAGE + "/parceiros/");
      Request request = RequestBuilder.create().setCookies(cookies).setUrl(url).build();
      Response response = dataFetcher.get(session, request);
      Map<String, Prices> marketplaceMap = new TreeMap<>();
      Map<Integer, Float> installmentMap = new TreeMap<>();
      Document sellers = Jsoup.parse(response.getBody());
      Prices prices = new Prices();
      Elements markets = sellers.select(".buybox-list div[data-product-container]");

      for (Element element : markets) {
         Element nameElement = element.selectFirst(".name");
         Element priceElement = element.selectFirst(".price");

         if (nameElement != null) {
            String name = nameElement.text();

            if (priceElement != null) {

               Float price = MathUtils.parseFloatWithComma(priceElement.ownText());
               installmentMap.put(1, price);
               prices.insertCardInstallment(Card.ELO.toString(), installmentMap);
               prices.insertCardInstallment(Card.VISA.toString(), installmentMap);
               prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentMap);
               prices.setBankTicketPrice(price);

               marketplaceMap.put(name, prices);
            }
         }
      }

      return marketplaceMap;
   }

   private Prices scrapPrices(Document doc, Float price) {
      Prices prices = new Prices();

      if (price != null) {
         Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div.section-price > p.text > del", null, false, ',', session);
         Map<Integer, Float> installmentPriceMap = new TreeMap<>();
         Pair<Integer, Float> pairInstallment = CrawlerUtils.crawlSimpleInstallment(".installment-payment-info-installments", doc, false, "x");

         installmentPriceMap.put(1, price);

         if (!pairInstallment.isAnyValueNull()) {
            installmentPriceMap.put(pairInstallment.getFirst(), pairInstallment.getSecond());
         }
         prices.setBankTicketPrice(price);
         prices.setPriceFrom(priceFrom);
         prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);

      }

      return prices;
   }

   private boolean isProductPage(Document doc) {
      return doc.select(".section product__header").isEmpty();
   }

   private RatingsReviews scrapRating(String internalId, Document doc) {
      TrustvoxRatingCrawler trustVox = new TrustvoxRatingCrawler(session, "85050", logger);
      return trustVox.extractRatingAndReviews(internalId, doc, dataFetcher);
   }
}
