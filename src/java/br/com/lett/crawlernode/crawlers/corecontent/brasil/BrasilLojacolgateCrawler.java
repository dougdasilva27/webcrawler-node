package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;
import models.prices.Prices;

public class BrasilLojacolgateCrawler extends Crawler {

   private static final String HOST = "lojacolgate.com.br";

   public BrasilLojacolgateCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
   }

   @Override
   protected Object fetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put("Cookie", "JSESSIONID=F1648205CBA7A84ED93C6D4802CA07E8;");

      Request request = RequestBuilder.create()
            .setUrl(session.getOriginalURL())
            .setHeaders(headers)
            .setProxyservice(Arrays.asList(
                  ProxyCollection.NETNUT_RESIDENTIAL_BR,
                  ProxyCollection.INFATICA_RESIDENTIAL_BR))
            .build();

      return Jsoup.parse(this.dataFetcher.get(session, request).getBody());
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-details .name", true).replace("|", "");
         CategoryCollection categories = scrapCategories(doc);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".image-gallery__image .lazyOwl", Arrays.asList("data-zoom-image", "src"), "https", HOST);
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".image-gallery__image .lazyOwl", Arrays.asList("data-zoom-image", "src"), "https", HOST, primaryImage);
         String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".tabhead", ".tabbody .tab-details"));
         RatingsReviews ratingsReviews = new RatingsReviews();

         Elements variations = doc.select(".product-item-content .js-variant-select > option");

         if (variations.isEmpty()) {

            String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".js-variant-sku", true);
            String internalPid = null;
            Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".js-variant-price", null, false, ',', session);
            Prices prices = scrapPrices(doc, price);
            Integer stock = CrawlerUtils.scrapIntegerFromHtmlAttr(doc, "[id*=productMaxQty-]", "data-max", 0);
            boolean available = doc.selectFirst("#outofstock.hidden") != null;
            List<String> eans = Arrays.asList(internalId);

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
                  .setEans(eans)
                  .setRatingReviews(ratingsReviews)
                  .build();

            products.add(product);

         } else {
            for (Element sku : variations) {

               String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(sku, null, "value");
               String internalPid = null;
               String variationName = sku.hasText() ? name + " - " + sku.text().split("-")[0].trim() : name;
               Float price = CrawlerUtils.scrapFloatPriceFromHtml(sku, null, "data-formatted", false, ',', session);
               Prices prices = scrapVariationPrices(sku, price);
               Integer stock = CrawlerUtils.scrapIntegerFromHtmlAttr(sku, null, "data-maxqty", 0);
               boolean available = !sku.attr("data-stock").equalsIgnoreCase("outOfStock");
               List<String> eans = Arrays.asList(internalId);

               // Creating the product
               Product product = ProductBuilder.create()
                     .setUrl(session.getOriginalURL())
                     .setInternalId(internalId)
                     .setInternalPid(internalPid)
                     .setName(variationName)
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
                     .setEans(eans)
                     .setRatingReviews(ratingsReviews)
                     .build();

               products.add(product);
            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".page-productDetails") != null;
   }

   private CategoryCollection scrapCategories(Document doc) {
      CategoryCollection categoryCollection = new CategoryCollection();
      Elements elementsCategories = doc.select(".breadcrumb > li:not(:first-child) > a");

      for (Element categoryElement : elementsCategories) {

         if (categoryElement.hasAttr("href") && !categoryElement.attr("href").contains("Categories") && !categoryElement.attr("href").contains("AllProducts")) {
            categoryCollection.add(categoryElement.text().trim());
         }
      }

      return categoryCollection;
   }

   private Prices scrapPrices(Document doc, Float price) {
      Prices prices = new Prices();

      if (price != null) {
         prices.setBankTicketPrice(price);
         prices.setPriceFrom(CrawlerUtils.scrapDoublePriceFromHtml(doc, ".js-variant-discount", null, false, ',', session));
      }

      return prices;
   }

   private Prices scrapVariationPrices(Element e, Float price) {
      Prices prices = new Prices();

      if (price != null) {
         prices.setBankTicketPrice(price);
         prices.setPriceFrom(CrawlerUtils.scrapDoublePriceFromHtml(e, null, "data-discount", false, ',', session));
      }

      return prices;
   }
}
