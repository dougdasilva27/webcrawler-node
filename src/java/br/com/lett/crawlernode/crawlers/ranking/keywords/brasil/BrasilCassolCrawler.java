package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

public class BrasilCassolCrawler extends CrawlerRankingKeywords {

   public BrasilCassolCrawler(Session session) {
      super(session);
   }

   private String HOME_PAGE = "https://www.cassol.com.br/";
   private Document doc;


   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String url = HOME_PAGE + this.keywordEncoded + "?_q=" + this.keywordEncoded + "&map=ft&page=" + this.currentPage;
      this.doc = fetchDocument(url);
      String currentDoc = fetchProduct();
      JSONObject obj = JSONUtils.stringToJson(currentDoc);
      JSONArray queryData = obj.optJSONArray("queryData");
      //fiz vários testes e melhor forma foi realmente pegar na posição 0
      JSONObject dataJson = queryData.optJSONObject(0);
      String dataString = dataJson.optString("data");
      JSONObject productObj = JSONUtils.stringToJson(dataString);
      JSONArray productsArr = JSONUtils.getValueRecursive(productObj, "productSearch.products", JSONArray.class);
      Elements products = this.doc.select(".vtex-search-result-3-x-gallery .vtex-search-result-3-x-galleryItem");
      if (!products.isEmpty() && !productsArr.isEmpty()) {
         if (totalProducts == 0) {
            setTotalProducts();
         }
         for (Object elementObj : productsArr) {
            String elementString = elementObj.toString();
            JSONObject elementJson = JSONUtils.stringToJson(elementString);
            String productUrl = HOME_PAGE + elementJson.optString("linkText") + "/p";
            String internalPid = elementJson.optString("productId");
            String name = elementJson.optString("productName");
            String imgUrl = JSONUtils.getValueRecursive(elementJson, "items.0.images.0.imageUrl", String.class);
            JSONObject priceRange = JSONUtils.getValueRecursive(elementJson, "priceRange.sellingPrice", JSONObject.class);
            Integer price = JSONUtils.getPriceInCents(priceRange, "lowPrice");
            boolean isAvailable = price != 0;
            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalPid(internalPid)
               .setName(name)
               .setImageUrl(imgUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }

         }

      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }
   }

   private String scrapInternalId(Element e) {
      return CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".price-box.price-final_price", "data-product-id");
   }

   private String fetchProduct() {

      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.cassol.com.br/" + this.keywordEncoded + "?page=" + this.currentPage + "&map=ft&__pickRuntime=page,queryData,contentResponse,route")
         .setSendUserAgent(false)
         .build();

      Response res = this.dataFetcher.get(session, request);

      return res.getBody();

   }


   @Override
   protected void setTotalProducts() {
      String totalProduct = CrawlerUtils.scrapStringSimpleInfo(this.doc, ".vtex-search-result-3-x-totalProducts--layout", false);
      String[] arrProduct = totalProduct.split(" ");
      this.totalProducts = arrProduct.length == 2 ? Integer.parseInt(arrProduct[(arrProduct.length) - 2]) : 0;
      this.log("Total: " + this.totalProducts);
   }


}
