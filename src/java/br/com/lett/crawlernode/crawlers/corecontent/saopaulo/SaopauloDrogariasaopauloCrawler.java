package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXCrawlersUtils;
import br.com.lett.crawlernode.crawlers.extractionutils.core.YourreviewsRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import models.AdvancedRatingReview;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SaopauloDrogariasaopauloCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.drogariasaopaulo.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "drogaria são paulo";


   public SaopauloDrogariasaopauloCrawler(Session session) {
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

      if (isProductPage(doc, session.getOriginalURL())) {
         VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies, dataFetcher);

         JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

         String internalPid = vtexUtil.crawlInternalPid(skuJson);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb li:not(:first-child) > a");
         String description = crawlDescription(doc, internalPid);
         String primaryImage = null;
         List<String> secondaryImages = null;

         // sku data in json
         JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

         // ean data in json
         JSONArray arrayEan = CrawlerUtils.scrapEanFromVTEX(doc);

         for (int i = 0; i < arraySkus.length(); i++) {
            JSONObject jsonSku = arraySkus.getJSONObject(i);

            String internalId = vtexUtil.crawlInternalId(jsonSku);
            JSONObject apiJSON = vtexUtil.crawlApi(internalId);
            String name = vtexUtil.crawlName(jsonSku, skuJson, " ");
            Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, true);
            List<String> dpspSellers = CrawlerUtils.getMainSellers(marketplaceMap, Arrays.asList(MAIN_SELLER_NAME_LOWER));
            Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, dpspSellers, Arrays.asList(Card.VISA), session);
            boolean available = CrawlerUtils.getAvailabilityFromMarketplaceMap(marketplaceMap, dpspSellers);
            primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#image-main", Arrays.asList("src"), "http://", "https://drogariasp.vteximg.com.br/");
            secondaryImages = scrapSecondaryImages(doc, primaryImage);
            Prices prices = CrawlerUtils.getPrices(marketplaceMap, dpspSellers);
            Float price = vtexUtil.crawlMainPagePrice(prices);
            Integer stock = vtexUtil.crawlStock(apiJSON);
            String descriptionV = description + CrawlerUtils.scrapLettHtml(internalId, session, session.getMarket().getNumber());
            String ean = i < arrayEan.length() ? arrayEan.getString(i) : null;
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
               .setDescription(descriptionV)
               .setStock(stock)
               .setMarketplace(marketplace)
               .setEans(eans)
               .build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   /*******************************
    * Product page identification *
    *******************************/

   private boolean isProductPage(Document document, String url) {
      return document.selectFirst("#___rc-p-sku-ids") != null && url.startsWith(HOME_PAGE);
   }

   /*******************
    * General methods *
    *******************/


   private String crawlDescription(Document doc, String internalPid) {
      StringBuilder description = new StringBuilder();

      description.append(CrawlerUtils.scrapSimpleDescription(doc,
         Arrays.asList(".tab-information-component", "#root-ministerio-saude")));

      Element shortDescription = doc.select(".productDescription").first();
      if (shortDescription != null) {
         description.append(shortDescription.html());
      }

      Element elementInformation = doc.select(".productSpecification").first();
      if (elementInformation != null) {

         Element iframe = elementInformation.select("iframe[src]").first();
         if (iframe != null) {
            Request request = RequestBuilder.create().setUrl(iframe.attr("src")).setCookies(cookies).build();
            description.append(this.dataFetcher.get(session, request).getBody());
         }

         description.append(elementInformation.html());
      }

      Element advert = doc.select(".advertencia").first();
      if (advert != null && !advert.select("#___rc-p-id").isEmpty()) {
         description.append(advert.html());
      }

      String url = "https://www.drogariasaopaulo.com.br/api/catalog_system/pub/products/search?fq=productId:" + internalPid;
      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      JSONArray skuInfo = CrawlerUtils.stringToJsonArray(this.dataFetcher.get(session, request).getBody());

      if (skuInfo.length() > 0) {
         JSONObject product = skuInfo.getJSONObject(0);

         String metaDescription = JSONUtils.getStringValue(product, "description");
         if (metaDescription != null) {
            description.append(metaDescription + "\n");
         }

         if (product.has("allSpecifications")) {
            JSONArray infos = product.getJSONArray("allSpecifications");

            for (Object o : infos) {
               if (!Arrays.asList("Garantia", "Parte do Corpo", "Gênero").contains(o.toString().trim())) {
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
                  Request requestFrame = RequestBuilder.create().setUrl(iframe.attr("src")).setCookies(cookies).build();
                  description.append(this.dataFetcher.get(session, requestFrame).getBody());
               }
            }
         }
      }

      description.append(CrawlerUtils.scrapLettHtml(internalPid, session, 102));

      return description.toString();
   }


   private List<String> scrapSecondaryImages(Document doc, String primaryImage){
      List<String> secondaryImage = new ArrayList<>();

      Elements elements = doc.select(".thumbs li a");

      for(Element e: elements){
         String secondary =  e.attr("rel");

         if(!secondary.equals(primaryImage)) {
            secondaryImage.add(secondary);
            if (secondaryImage.size() == 3) { // Isso é necessario porque esse market só mostra as 4 primeiras imagens.
               break;
            }
         }
      }
      return  secondaryImage;
   }
}
