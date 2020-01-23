package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import java.util.ArrayList;
import java.util.Arrays;
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
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXCrawlersUtils;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.YourreviewsRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.AdvancedRatingReview;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

public class BrasilDrogariapachecoCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.drogariaspacheco.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "drogarias pacheco";


   public BrasilDrogariapachecoCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
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

      if (isProductPage(doc, session.getOriginalURL())) {
         VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies, dataFetcher);

         JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

         String internalPid = vtexUtil.crawlInternalPid(skuJson);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb > ul li a");
         String description = crawlDescription(doc, internalPid);
         RatingsReviews ratingReviews = crawlRating(internalPid);

         // sku data in json
         JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

         // ean data in json
         JSONArray arrayEans = CrawlerUtils.scrapEanFromVTEX(doc);

         for (int i = 0; i < arraySkus.length(); i++) {
            JSONObject jsonSku = arraySkus.getJSONObject(i);

            String internalId = vtexUtil.crawlInternalId(jsonSku);
            JSONObject apiJSON = vtexUtil.crawlApi(internalId);
            String name = vtexUtil.crawlName(jsonSku, skuJson, " ");
            Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, true);
            List<String> sellersPacheco = CrawlerUtils.getMainSellers(marketplaceMap, Arrays.asList(MAIN_SELLER_NAME_LOWER));
            Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, sellersPacheco, Arrays.asList(Card.VISA), session);
            boolean available = CrawlerUtils.getAvailabilityFromMarketplaceMap(marketplaceMap, sellersPacheco);
            String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
            String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
            Prices prices = CrawlerUtils.getPrices(marketplaceMap, sellersPacheco);
            Float price = CrawlerUtils.extractPriceFromPrices(prices, Card.VISA);
            Integer stock = vtexUtil.crawlStock(apiJSON);
            String ean = i < arrayEans.length() ? arrayEans.getString(i) : null;


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
                  .setRatingReviews(ratingReviews)
                  .build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private RatingsReviews crawlRating(String internalPid) {
      RatingsReviews ratingReviews = new RatingsReviews();

      YourreviewsRatingCrawler yr =
            new YourreviewsRatingCrawler(session, cookies, logger, "87b2aa32-fdcb-4f1d-a0b9-fd6748df725a", this.dataFetcher);

      Document docRating = yr.crawlPageRatingsFromYourViews(internalPid, "87b2aa32-fdcb-4f1d-a0b9-fd6748df725a", this.dataFetcher);

      Integer totalNumOfEvaluations = getTotalNumOfRatings(docRating);
      Double avgRating = getTotalAvgRating(docRating, totalNumOfEvaluations);
      AdvancedRatingReview advancedRatingReview = yr.getTotalStarsFromEachValue(internalPid);

      ratingReviews.setAdvancedRatingReview(advancedRatingReview);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setDate(session.getDate());

      return ratingReviews;
   }

   /**
    * Average is calculate
    * 
    * @param document
    * @return
    */
   private Double getTotalAvgRating(Document docRating, Integer totalRating) {
      Double avgRating = null;
      Element rating = docRating.select("meta[itemprop=ratingValue]").first();

      if (rating != null) {
         avgRating = Double.parseDouble(rating.attr("content"));
      }

      return avgRating;
   }

   /**
    * Number of ratings appear in rating page
    * 
    * @param docRating
    * @return
    */
   private Integer getTotalNumOfRatings(Document doc) {
      Integer totalRating = null;
      Element totalRatingElement = doc.select("strong[itemprop=ratingCount]").first();

      if (totalRatingElement != null) {
         String totalText = totalRatingElement.ownText().replaceAll("[^0-9]", "").trim();

         if (!totalText.isEmpty()) {
            totalRating = Integer.parseInt(totalText);
         }
      }

      return totalRating;
   }


   private List<String> crawlIdList(JSONObject skuJson) {
      List<String> idList = new ArrayList<>();

      if (skuJson.has("skus")) {
         JSONArray skus = skuJson.getJSONArray("skus");

         for (int i = 0; i < skus.length(); i++) {
            JSONObject sku = skus.getJSONObject(i);

            if (sku.has("sku")) {
               idList.add(Integer.toString(sku.getInt("sku")));
            }
         }
      }

      return idList;
   }


   private boolean isProductPage(Document document, String url) {
      return document.selectFirst(".productName") != null && url.startsWith(HOME_PAGE);
   }

   private String crawlDescription(Document doc, String internalPid) {
      StringBuilder description = new StringBuilder();

      Element elementInformation = doc.select(".productSpecification").first();
      if (elementInformation != null) {

         Element iframe = elementInformation.select("iframe[src]").first();
         if (iframe != null) {
            Request request = RequestBuilder.create().setUrl(iframe.attr("src").trim()).setCookies(cookies).build();
            description.append(this.dataFetcher.get(session, request).getBody());
         }

         description.append(elementInformation.html());
      }

      // Nesse site todo medicamento deve ter a advertencia
      Elements elementCategories = doc.select(".bread-crumb li > a");
      for (int i = 1; i < elementCategories.size(); i++) { // first item is the home page
         String text = elementCategories.get(i).text().trim();

         if (text.equalsIgnoreCase("medicamentos")) {
            description.append("<div class=\"container medicamento-information-component\"><h2>Advertência do Ministério da Saúde</h2><p>" +
                  CrawlerUtils.scrapStringSimpleInfo(doc, ".fn.productName", true)
                  + " É UM MEDICAMENTO. SEU USO PODE TRAZER RISCOS. PROCURE UM MÉDICO OU UM FARMACÊUTICO. LEIA A BULA.</p></div>");
            break;
         }
      }

      Element advert = doc.select(".advertencia").first();
      if (advert != null && !advert.select("#___rc-p-id").isEmpty()) {
         description.append(advert.html());
      }

      String url = "https://www.drogariaspacheco.com.br/api/catalog_system/pub/products/search?fq=productId:" + internalPid;
      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      JSONArray skuInfo = CrawlerUtils.stringToJsonArray(this.dataFetcher.get(session, request).getBody());

      if (skuInfo.length() > 0) {
         JSONObject product = skuInfo.getJSONObject(0);

         description.append(product.optString("description", ""));

         if (product.has("Informações")) {
            JSONArray infos = product.getJSONArray("Informações");

            for (Object o : infos) {
               description.append("<div> <strong>" + o.toString() + ":</strong>");
               JSONArray spec = product.getJSONArray(o.toString());

               for (Object obj : spec) {
                  description.append(obj.toString() + "&nbsp");
               }

               description.append("</div>");
            }
         }

         if (product.has("Especificações")) {
            JSONArray infos = product.getJSONArray("Especificações");

            for (Object o : infos) {
               if (!Arrays.asList("Garantia", "Parte do Corpo", "PREÇO VIVA SAÚDE").contains(o.toString())) {
                  description.append("<div> <strong>" + o.toString() + ":</strong>");
                  JSONArray spec = product.getJSONArray(o.toString());

                  for (Object obj : spec) {
                     description.append(obj.toString() + "&nbsp");
                  }

                  description.append("</div>");
               }
            }
         }

         if (product.has("Página Especial")) {
            JSONArray specialPage = product.getJSONArray("Página Especial");

            if (specialPage.length() > 0) {
               Element iframe = Jsoup.parse(specialPage.get(0).toString()).select("iframe").first();

               if (iframe != null && iframe.hasAttr("src") && !iframe.attr("src").contains("youtube")) {
                  Request requestSpecial = RequestBuilder.create().setUrl(iframe.attr("src").trim()).setCookies(cookies).build();
                  description.append(this.dataFetcher.get(session, requestSpecial).getBody());
               }
            }
         }
      }

      return description.toString();
   }

}
