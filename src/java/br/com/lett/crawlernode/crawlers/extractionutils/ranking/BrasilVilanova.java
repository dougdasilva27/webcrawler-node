package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.crawlers.extractionutils.generals.BrasilVilaNovaUtils;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class BrasilVilanova extends CrawlerRankingKeywords {

   public BrasilVilanova(Session session) {
      super(session);
   }

   private final BrasilVilaNovaUtils brasilVilaNovaUtils = new BrasilVilaNovaUtils(session);

   @Override
   protected void processBeforeFetch() {
      brasilVilaNovaUtils.login(this.dataFetcher, this.cookies);
   }

   @Override
   protected Document fetchDocument(String url) {
      try {
         Request request = Request.RequestBuilder.create()
            .setUrl(url)
            .setProxy(
               brasilVilaNovaUtils.getFixedIp()
            )
            .setCookies(this.cookies)
            .setFollowRedirects(true)
            .build();
         Response response = this.dataFetcher.get(session, request);
         return Jsoup.parse(response.getBody());
      } catch (IOException e) {
         e.printStackTrace();
      }
      return Jsoup.parse(new Response().getBody());
   }

   int alternativePosition = 1;

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.log("Página " + this.currentPage);

      this.pageSize = 12;
      String url = "https://www.vilanova.com.br/catalogsearch/result/index/?p="
         + this.currentPage
         + "&q="
         + this.keywordEncoded;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".product-item");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element product : products) {
            String internalPid = getId(product);
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".product-item-link", "href");
            String baseName = CrawlerUtils.scrapStringSimpleInfo(product, ".product.details.product-item-details strong", false);
            String imageUrl = scrapImage(product);
            JSONObject productJson = brasilVilaNovaUtils.getJsonConfig(product, ".wrp-swatch script", internalPid);
            if (!productJson.isEmpty()) {
               JSONArray variationsJson = brasilVilaNovaUtils.getAttributes(productJson, "variant_embalagem");
               JSONObject objMarket = brasilVilaNovaUtils.getObjectMarket(productJson);
               for (Object v : variationsJson) {
                  JSONObject variation = (JSONObject) v;
                  String label = variation.optString("label");
                  String variationName = baseName + " " + label;
                  String internalId = brasilVilaNovaUtils.getInternalId(internalPid, label);
                  String id = brasilVilaNovaUtils.findId(variation, objMarket);
                  Integer price = getPrice(id, productJson);
                  boolean available = price != null;

                  RankingProduct productRanking = RankingProductBuilder.create()
                     .setUrl(productUrl)
                     .setInternalId(internalId)
                     .setInternalPid(internalPid)
                     .setImageUrl(imageUrl)
                     .setName(variationName)
                     .setPriceInCents(price)
                     .setAvailability(available)
                     .setPosition(alternativePosition)
                     .build();

                  saveDataProduct(productRanking);
               }
            } else {
               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalPid)
                  .setInternalPid(internalPid)
                  .setImageUrl(imageUrl)
                  .setName(baseName)
                  .setPriceInCents(null)
                  .setAvailability(false)
                  .setPosition(alternativePosition)
                  .build();

               saveDataProduct(productRanking);
            }
            alternativePosition++;
            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private String getId(Element element) {
      String infoId = CrawlerUtils.scrapStringSimpleInfoByAttribute(element, ".product-item-info", "id");
      int index = infoId.indexOf("_");
      return infoId.substring(index + 1);
   }

   @Override
   protected void setTotalProducts() {
      String divTotalProducts = CrawlerUtils.scrapStringSimpleInfo(this.currentDoc, ".toolbar-amount", false);
      if (divTotalProducts != null && !divTotalProducts.isEmpty()) {
         String totalStr = divTotalProducts.contains("de ") ? CommonMethods.getLast(divTotalProducts.split(" ")) : divTotalProducts.split(" ")[0];
         this.totalProducts = Integer.parseInt(totalStr);
      }
   }

   private Integer getPrice(String id, JSONObject json) {
      JSONObject objectPrices = JSONUtils.getValueRecursive(json, "optionPrices." + id, JSONObject.class, new JSONObject());
      JSONObject price = objectPrices.optJSONObject("finalPrice");
      if (price != null) {
         Double priceDouble = JSONUtils.getDoubleValueFromJSON(price, "amount", false);
         return CommonMethods.doublePriceToIntegerPrice(priceDouble, null);
      }
      return null;
   }

   private String scrapImage(Element product) {
      String imageFullUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".product-image-photo", "src");
      return brasilVilaNovaUtils.getSanitizedUrl(imageFullUrl);
   }

}
