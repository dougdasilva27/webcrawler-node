package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.LinxImpulseRanking;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.Collections;

public class BrasilDrogalCrawler extends LinxImpulseRanking {
   protected String internalPid = "";
   protected String internalId = "";

   public BrasilDrogalCrawler(Session session) {
      super(session);
   }

   // Inverts internalPid and internalId
   @Override
   protected String crawlInternalPid(JSONObject product) {
      this.internalPid = super.crawlInternalPid(product);
      this.internalId = super.crawlInternalId(product, this.internalPid);

      return internalId;
   }

   @Override
   protected String crawlInternalId(JSONObject product, String internalPid) {
      return this.internalPid;
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String url = mountURL();
      JSONObject data = fetchPage(url);
      if (data.optString("queryType").contains("redirect")) {
         url = data.optString("link");
         this.currentDoc = fetchDocument(url);
         Elements products = this.currentDoc.select(".list-products .li");
         if (!products.isEmpty()) {
            for (Element e : products) {
               RankingProduct rankingProduct = createRankingProductFromHtml(e);
               saveDataProduct(rankingProduct);
               if (this.arrayProducts.size() == productsLimit) {
                  break;
               }
            }
         }
      } else if (!data.optString("queryType").contains("redirect")) {
         JSONArray products = data.optJSONArray("products");
         if (products != null && !products.isEmpty()) {
            this.totalProducts = data.optInt("size");
            for (Object object : products) {
               RankingProduct rankingProduct = createRankingProductFromData(object);
               saveDataProduct(rankingProduct);
               if (this.arrayProducts.size() == productsLimit) {
                  break;
               }
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private String crawlInternalPid(Element e) {
      return e.attr("data-sku");
   }
   private String crawlInternalPidAlt(Element e){
         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".item-product", "data-sku");
         if (internalId != null) {
            return internalId;
         }
         return null;
   }

   private RankingProduct createRankingProductFromData(Object object) throws MalformedProductException {
      JSONObject product = (JSONObject) object;
      String productUrl = crawlProductUrl(product);
      String internalPid = crawlInternalPid(product);
      String internalId = crawlInternalId(product, internalPid);
      String name = product.optString("name");
      String image = crawlImage(product);
      int priceInCents = crawlPrice(product);
      boolean isAvailable = crawlAvailability(product);

      return RankingProductBuilder.create()
         .setUrl(productUrl)
         .setInternalId(internalId)
         .setInternalPid(internalPid)
         .setName(name)
         .setImageUrl(image)
         .setPriceInCents(priceInCents)
         .setAvailability(isAvailable)
         .build();
   }

   //todo
   private RankingProduct createRankingProductFromHtml(Element e) throws MalformedProductException {
      String productUrl = CrawlerUtils.scrapUrl(e, ".title a", "href", "https:", "www.drogal.com.br");
      String internalPid = crawlInternalPidAlt(e);
      String name = CrawlerUtils.scrapStringSimpleInfo(e, ".title a", true);
      String image = CrawlerUtils.scrapSimplePrimaryImage(e, ".item-product > a > .img-lazy", Collections.singletonList("data-src"), "https", "www.drogal.com.br");
      int price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".box-prices > div > div > p.sale-price > strong", null, false, ',', session, null);
      boolean isAvailable = price != 0;

      return RankingProductBuilder.create()
         .setUrl(productUrl)
         .setInternalPid(internalPid)
         .setName(name)
         .setPriceInCents(price)
         .setImageUrl(image)
         .setPriceInCents(price)
         .setAvailability(isAvailable)
         .build();
   }
}
