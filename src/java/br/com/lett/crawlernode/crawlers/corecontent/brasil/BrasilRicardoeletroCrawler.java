package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.Pair;
import exceptions.OfferException;
import models.Marketplace;
import models.Offer;
import models.Offer.OfferBuilder;
import models.Offers;
import models.prices.Prices;

public class BrasilRicardoeletroCrawler extends Crawler {

   private static final String SELLER_NAME = "ricardo eletro";

   public BrasilRicardoeletroCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      JSONArray jsonArray = CrawlerUtils.selectJsonArrayFromHtml(doc, "script", "dataLayer = ", ";", false, true);
      JSONObject productJSON = jsonArray.length() > 0 && jsonArray.get(0) instanceof JSONObject ? jsonArray.getJSONObject(0) : new JSONObject();

      if (productJSON.has("productSKUList")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = scrapInternalPid(productJSON);
         String mainId = scrapMainId(productJSON);
         String ean = JSONUtils.getStringValue(productJSON, "productEAN13");
         List<String> eans = ean != null && !ean.isEmpty() ? Arrays.asList(ean) : null;

         JSONArray skusArray = JSONUtils.getJSONArrayValue(productJSON, "productSKUList");

         for (Object obj : skusArray) {
            JSONObject skuJson = obj instanceof JSONObject ? (JSONObject) obj : new JSONObject();

            String name = scrapName(skuJson);
            String internalId = scrapInternalId(skuJson);
            String newUrl = JSONUtils.getStringValue(skuJson, "url");
            String url = newUrl != null ? newUrl : session.getOriginalURL();

            Document docVariation = mainId != null && internalId != null && !mainId.equalsIgnoreCase(internalId) ? scrapVariationHTML(url) : doc;

            String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(docVariation, ".product-info-images .swiper-slide img", Arrays.asList("src"),
                  "https", "www.imgeletro.com.br");
            String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(docVariation, ".product-info-images .swiper-slide img", Arrays.asList("src"),
                  "https", "www.imgeletro.com.br", primaryImage);
            CategoryCollection categories = CrawlerUtils.crawlCategories(docVariation, "#Breadcrumbs a");
            String description = scrapDescription(docVariation);

            List<Document> sellersHtmls = scrapProductPagePerSeller(doc);
            Offers offers = scrapOffers(sellersHtmls);
            Map<String, Prices> marketplaceMap = scrapMarketplaceMap(sellersHtmls);
            Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, Arrays.asList(SELLER_NAME), Card.VISA, session);
            boolean available = CrawlerUtils.getAvailabilityFromMarketplaceMap(marketplaceMap, Arrays.asList(SELLER_NAME));
            Prices prices = CrawlerUtils.getPrices(marketplaceMap, Arrays.asList(SELLER_NAME));
            Float price = CrawlerUtils.extractPriceFromPrices(prices, Card.VISA);
            Integer stock = JSONUtils.getIntegerValueFromJSON(skuJson, "stock", 0);

            Product product = ProductBuilder.create()
                  .setUrl(url)
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
                  .setStock(stock)
                  .setEans(eans)
                  .setOffers(offers)
                  .build();

            products.add(product);

         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private String scrapInternalPid(JSONObject productJSON) {
      String internalPid = null;

      if (productJSON.has("productID") && !productJSON.isNull("productID")) {
         internalPid = productJSON.get("productID").toString();
      }

      return internalPid;
   }

   private String scrapMainId(JSONObject productJSON) {
      String internalPid = null;

      if (productJSON.has("productSKU") && !productJSON.isNull("productSKU")) {
         internalPid = productJSON.get("productSKU").toString();
      }

      return internalPid;
   }

   private Document scrapVariationHTML(String url) {
      Request request = RequestBuilder.create()
            .setUrl(url)
            .setCookies(cookies)
            .build();

      return Jsoup.parse(this.dataFetcher.get(session, request).getBody());
   }

   private String scrapName(JSONObject obj) {
      StringBuilder name = new StringBuilder();
      name.append(JSONUtils.getStringValue(obj, "name"));

      JSONObject specs = JSONUtils.getJSONValue(obj, "specs");
      for (String key : specs.keySet()) {
         name.append(" ").append(specs.get(key));
      }

      return name.toString();
   }

   private String scrapInternalId(JSONObject obj) {
      String internalId = null;

      if (obj.has("sku") && !obj.isNull("sku")) {
         internalId = obj.get("sku").toString();
      }

      return internalId;
   }

   private String scrapDescription(Document doc) {
      StringBuilder description = new StringBuilder();

      Element principalDescription = doc.selectFirst("#product-description");
      if (principalDescription != null) {
         String iframeUrl = CrawlerUtils.scrapUrl(principalDescription, "iframe", Arrays.asList("src"), "https", "conteudo.maquinadevendas.com.br");

         if (iframeUrl != null) {
            Request request = RequestBuilder.create().setUrl(iframeUrl).build();
            description.append(Jsoup.parse(this.dataFetcher.get(session, request).getBody()));

            principalDescription.select("iframe").remove();
         }

         description.append(principalDescription.html());
      }

      description.append(CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#product-features", "#product-list", "#product-provider")));

      return description.toString();
   }

   private List<Document> scrapProductPagePerSeller(Document doc) {
      List<Document> htmls = new ArrayList<>();
      htmls.add(doc);

      Elements sellers = doc.select(".product-price-marketplace .product-price-card a[href]");
      for (Element seller : sellers) {
         Float price = CrawlerUtils.scrapFloatPriceFromHtml(seller, ".product-price-card-price", null, true, ',', session);

         // if price is not null this seller have a offer and we need to access another url to scrap
         // installments
         if (price != null) {
            String sellerUrl = CrawlerUtils.completeUrl(seller.attr("href"), "https", "www.ricardoeletro.com.br");
            Request request = RequestBuilder.create().setUrl(sellerUrl).build();

            htmls.add(Jsoup.parse(this.dataFetcher.get(session, request).getBody()));
         }
      }

      return htmls;
   }

   private Offers scrapOffers(List<Document> htmls) {
      Offers offers = new Offers();

      int position = 1;
      for (Document doc : htmls) {
         String sellerName = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-price-details .product-price-details-sold-by b", true);
         String sellerId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#ExibeFormasPagamento[data-siteid]", "data-siteid");
         Double price = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-price-details .product-price-details-new-price", null, false, ',', session);

         try {
            Offer offer = new OfferBuilder()
                  .setSellerFullName(sellerName)
                  .setSlugSellerName(CrawlerUtils.toSlug(sellerName))
                  .setInternalSellerId(sellerId)
                  .setMainPagePosition(position)
                  .setIsBuybox(htmls.size() > 1)
                  .setMainPrice(price)
                  .build();
            offers.add(offer);
         } catch (OfferException e) {
            Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
         }

         position++;
      }

      return offers;
   }

   private Map<String, Prices> scrapMarketplaceMap(List<Document> htmls) {
      Map<String, Prices> marketplaceMap = new HashMap<>();

      for (Document doc : htmls) {
         String sellerName = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-price-details .product-price-details-sold-by b", true).toLowerCase();
         Prices prices = scrapPrices(doc);

         marketplaceMap.put(sellerName, prices);
      }

      return marketplaceMap;
   }

   private Prices scrapPrices(Document doc) {
      Prices prices = new Prices();

      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".product-price-details .product-price-details-new-price", null, false, ',', session);
      if (price != null) {
         Map<Integer, Float> installments = new HashMap<>();
         installments.put(1, price);

         prices.setBankTicketPrice(price);
         prices.setPriceFrom(CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-price-details .product-price-details-old-price", null, false, ',', session));

         Elements installmentsElements = doc.select(".product-price-details  .product-price-details-installments:not(:last-child) b");
         for (Element e : installmentsElements) {
            Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(null, e, true);
            if (!pair.isAnyValueNull()) {
               installments.put(pair.getFirst(), pair.getSecond());
            }
         }

         prices.insertCardInstallment(Card.VISA.toString(), installments);
         prices.insertCardInstallment(Card.DINERS.toString(), installments);
         prices.insertCardInstallment(Card.MASTERCARD.toString(), installments);
         prices.insertCardInstallment(Card.HIPERCARD.toString(), installments);
         prices.insertCardInstallment(Card.AMEX.toString(), installments);
      }

      return prices;
   }
}
