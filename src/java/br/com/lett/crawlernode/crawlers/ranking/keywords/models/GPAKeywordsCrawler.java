package br.com.lett.crawlernode.crawlers.ranking.keywords.models;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.MathUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;

public class GPAKeywordsCrawler extends CrawlerRankingKeywords {
   private String keyword = this.keywordEncoded;
   protected String storeId = getStoreId();
   protected String store;
   protected String storeShort;
   protected String homePageHttps;

   public GPAKeywordsCrawler(Session session) {
      super(session);
      inferFields();
   }

   public String getStoreId() {
      return session.getOptions().optString("storeId");
   }

   private void inferFields() {
      String className = this.getClass().getSimpleName().toLowerCase();
      if (className.contains("paodeacucar")) {
         this.store = "paodeacucar";
         this.storeShort = "pa";
         this.homePageHttps = "https://www.paodeacucar.com/";
      } else if (className.contains("extra")) {
         this.store = "deliveryextra";
         this.storeShort = "ex";
         this.homePageHttps = "https://www.clubeextra.com.br/";
      }
   }

   @Override
   protected void processBeforeFetch() {
         BasicClientCookie cookie = new BasicClientCookie("STORE_ID", this.storeId);
         cookie.setDomain(homePageHttps.substring(homePageHttps.indexOf("www"), homePageHttps.length() - 1));
         cookie.setPath("/");
         cookie.setExpiryDate(new Date(System.currentTimeMillis() + 604800000L + 604800000L));
         this.cookies.add(cookie);

   }

   @Override
   public void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 0;

      this.log("Página " + this.currentPage);
      JSONObject search = crawlSearchApi();

      if (search.has("results") && search.getJSONArray("results").length() > 0) {
         JSONArray products = search.getJSONArray("results");

         if (this.totalProducts == 0) {
            setTotalProducts(search);
         }

         for (int i = 0; i < products.length(); i++) {
            JSONObject product = products.getJSONObject(i);

            String productUrl = crawlProductUrl(product);
            String internalPid = crawlInternalPid(product);
            String internalId = crawlInternalId(productUrl);
            String imgUrl = crawlProductImgUrl(product);
            String name  = crawlProductName(product);
            Integer price  = crawlProductPrice(product);
            boolean isAvailable = price != 0;

            RankingProduct rankingProducts = RankingProductBuilder.create()
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setUrl(productUrl)
               .setImageUrl(imgUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setIsSponsored(false)
               .setKeyword(this.keywordEncoded)
               .setPosition(this.position)
               .setPageNumber(this.currentPage)
               .build();

            saveDataProduct(rankingProducts);

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

   @Override
   protected boolean hasNextPage() {
      return this.arrayProducts.size() < this.totalProducts;
   }

   protected void setTotalProducts(JSONObject search) {
      if (search.has("result_meta")) {
         JSONObject resultMeta = search.getJSONObject("result_meta");

         if (resultMeta.has("total")) {
            this.totalProducts = resultMeta.getInt("total");
            this.log("Total da busca: " + this.totalProducts);
         }
      }
   }

   private String crawlInternalId(String url) {
      String internalId = null;

      if (url != null) {
         internalId = CommonMethods.getLast(url.split("/"));
      }

      return internalId;
   }

   private Integer crawlProductPrice(JSONObject product) {
      String priceStr = product.optString("price");
      int price = priceStr != null ? MathUtils.parseInt(priceStr) : 0;
      return price;
   }

   private Integer crawlProductMarketId(JSONObject product) {
      Integer marketId = null;

      if (product.has("market_id")) {
         marketId = product.getInt("market_id");
      }

      return marketId;
   }

   private String crawlInternalPid(JSONObject product) {
      String internalPid = null;

      if (product.has("sku")) {
         internalPid = product.getString("sku");
      }

      return internalPid;
   }

   private String crawlProductUrl(JSONObject product) {
      String urlProduct = null;

      if (product.has("url")) {
         urlProduct = product.getString("url");
      }

      return urlProduct;
   }

   private String crawlProductImgUrl(JSONObject product) {
      String imgUrlProduct = null;

      if (product.has("image_url")) {
         imgUrlProduct = product.getString("image_url");
      }

      return imgUrlProduct;
   }

   private String crawlProductName(JSONObject product) {
      String nameProduct = null;

      if (product.has("title")) {
         nameProduct = product.getString("title");
      }

      return nameProduct;
   }
   private JSONObject crawlSearchApi() {
      StringBuilder aux = new StringBuilder();
      aux.append("https://").append(this.store).append(".resultspage.com/search?af=&cnt=36")
         .append("&isort=&lot=json&p=Q&")
         .append("ref=www.").append(this.store).append(".com.br&srt=").append(this.arrayProducts.size())
         .append("&ts=json-full")
         .append("&ua=Mozilla%2F5.0+(X11;+Linux+x86_64)+AppleWebKit%2F537.36+(KHTML,+like+Gecko)+Chrome%2F62.0.3202.62+Safari%2F537.36")
         .append("&w=").append(this.keyword);

      if (this.storeId != null) {
         aux.append("&ep.selected_store=").append(this.storeId);
      }

      String url = aux.toString();

      JSONObject searchApi = fetchJSONObject(url, cookies);

      if (this.currentPage == 1 && searchApi.has("merch")) {
         JSONObject merch = searchApi.getJSONObject("merch");
         if (merch.has("jumpurl")) {
            String jumpurl = merch.get("jumpurl").toString();
            if (jumpurl.contains("especial")) {
               String newKeyword = CommonMethods.getLast(jumpurl.split("\\?")[0].split("/"));
               url = url.replace(this.keyword, newKeyword);
               this.keyword = newKeyword;
               searchApi = fetchJSONObject(url, cookies);
            }
         }
      }

      this.log("Link onde são feitos os crawlers: " + url);

      return searchApi;
   }
}
