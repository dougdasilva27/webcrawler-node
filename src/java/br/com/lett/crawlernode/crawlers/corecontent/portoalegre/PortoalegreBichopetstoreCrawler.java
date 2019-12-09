package br.com.lett.crawlernode.crawlers.corecontent.portoalegre;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import models.Marketplace;
import models.prices.Prices;

public class PortoalegreBichopetstoreCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.bichopetstore.com.br/";

   public PortoalegreBichopetstoreCrawler(Session session) {
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

         Elements variations = doc.select("#product .otp-option > li");

         String internalPid = scrapInternalPid(doc);
         String internalId = internalPid;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "#content .row > div > h1", true);
         Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, "#otp-price > li", null, false, ',', session);
         Prices prices = null;
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".container > .breadcrumb > li:not(:last-child) > a", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".thumbnails > li.image-additional > a.thumbnail",
               Arrays.asList("href"), "https:", HOME_PAGE);
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".thumbnails > li.image-additional > a.thumbnail",
               Arrays.asList("href"), "https:", HOME_PAGE, primaryImage);
         String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList("#content p"));
         boolean available = doc.selectFirst("#content #otp-stock") != null && !doc.selectFirst("#content #otp-stock").text().contains(" a ");
         Integer stock = null;
         Marketplace marketplace = null;

         String ean = null;
         List<String> eans = new ArrayList<>();
         eans.add(ean);

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
               .setStock(stock)
               .setMarketplace(marketplace)
               .setEans(eans)
               .build();

         if (!variations.isEmpty()) {
            for (Element variation : variations) {
               Product variationProduct = product.clone();

               String variationId = CrawlerUtils.scrapStringSimpleInfoByAttribute(variation, null, "value");
               Prices variationPrices = scrapVariationPrices(doc, price, variationId);

               variationProduct.setInternalId(internalId + "-" + variationId);
               variationProduct.setName(name + " - " + CrawlerUtils.scrapStringSimpleInfoByAttribute(variation, null, "title"));
               variationProduct.setPrices(variationPrices);
               variationProduct.setPrice(CrawlerUtils.extractPriceFromPrices(variationPrices, Arrays.asList(Card.MASTERCARD, Card.VISA, Card.MAESTRO)));

               products.add(variationProduct);
            }

         } else {
            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document document) {
      return document.selectFirst("body[class*=\"product-product\"]") != null;
   }

   private String scrapInternalPid(Document doc) {
      String internalPid = null;

      String onClick = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#content .btn-default[onclick]", "onclick");
      if (onClick != null && onClick.contains("(") && onClick.contains(")")) {
         int firstIndex = onClick.indexOf('(') + 1;
         int lastIndex = onClick.indexOf(')', firstIndex);

         internalPid = onClick.substring(firstIndex, lastIndex).replace("'", "");
      }

      return internalPid;
   }

   private Prices scrapVariationPrices(Document doc, Float price, String variationId) {
      Prices prices = new Prices();

      if (price != null) {
         Map<Integer, Float> installmentPriceMap = new HashMap<>();
         installmentPriceMap.put(1, price);

         prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.MAESTRO.toString(), installmentPriceMap);
      }

      return prices;
   }
}
