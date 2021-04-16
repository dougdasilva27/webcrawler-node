package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;

/**
 * 15/04/2021
 *
 * @author Thainá Aguiar
 * <p>
 * In this crawler, for a specific keyword "base" (which I found) the site loads the information from the html,
 * making a redirect, no matter what keyword is called, always redirects to "https://www.maccosmetics.com.br/products/13847/produtos/maquiagem/rosto/base"
 * so i created a function that check inside the html if it contains a selector with the keyword, but as there may be some other keyword that captures through the html;
 * <p>
 * The other way to capture the information is through an api, all the other keywords that I tested work by calling the api;
 * <p>
 * Within the "extractProductsFromCurrentPage" method, it checks whether the information will be extracted from the api or from the html,
 * if neither method works, it returns that the keyword has no result.
 */

public class BrasilMaccosmeticosCrawler extends CrawlerRankingKeywords {


   public BrasilMaccosmeticosCrawler(Session session) {
      super(session);
   }


   protected Document fetchDocument() {

      String url = "https://www.maccosmetics.com.br/products/13847/produtos/" + this.keywordEncoded;


      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .build();
      String content = this.dataFetcher
         .get(session, request)
         .getBody();

      return Jsoup.parse(content);
   }


   public JSONObject crawlApi() {

      String apiUrl = "https://www.maccosmetics.com.br/enrpc/JSONControllerServlet.do?M=host%3Alocalhost%7Cport%3A16016%7Crecs_per_page%3A300&D=" + this.keywordEncoded + "&Dx=mode+matchallpartial&Ntt=" + this.keywordEncoded + "&Ntk=all&Ntx=mode+matchallpartial&Nao=0&Nu=p_PRODUCT_ID&N=";
      Map<String, String> headers = new HashMap<>();
      headers.put("authority", "www.maccosmetics.com.br");
      headers.put("referer", "https://www.maccosmetics.com.br/esearch?form_id=perlgem_search_form&search=" + this.keywordEncoded);
      headers.put("accept", "*/*");
      headers.put("x-requested-with", "XMLHttpRequest");
      headers.put("Accept-Encoding", "gzip, deflate, br");
      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.90 Safari/537.36");

      Request request = Request.RequestBuilder.create()
         .setUrl(apiUrl)
         .setHeaders(headers)
         .build();
      String content = this.dataFetcher
         .get(session, request)
         .getBody();

      return CrawlerUtils.stringToJson(content);

   }

   @Override
   protected void extractProductsFromCurrentPage() {

      this.log("Página " + this.currentPage);

      this.currentDoc = fetchDocument();

      Elements products = verificationOfKeyword() ? this.currentDoc.select(".grid--mpp__item") : null;

      if (products != null && !products.isEmpty()) {
         extract(products);

      } else {
         JSONObject json = crawlApi();
         JSONArray productsArray = json.optJSONArray("AggrRecords");
         if (productsArray != null && !productsArray.isEmpty()) {
            extractFromApi(productsArray);
         } else {

            this.result = false;
            this.log("Keyword sem resultado!");
         }

         this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
      }
   }

   private void extractFromApi(JSONArray productsArray) {


      for (Object e : productsArray) {

         JSONObject product = (JSONObject) e;

         JSONObject attributes = product.optJSONObject("Properties");
         if (attributes != null) {
            String internalId = attributes.optString("s_PRODUCT_ID");
            String internalPid = attributes.optString("s_SKU_ID");
            String urlProduct = getUrl(attributes);

            saveDataProduct(internalId, internalPid, urlProduct);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      }

   }

   private boolean verificationOfKeyword() {
      String keyword = CrawlerUtils.scrapStringSimpleInfo(this.currentDoc, ".sec-nav__title-category", true);
      return keyword != null && keyword.equalsIgnoreCase(this.keywordEncoded);

   }

   private void extract(Elements products) {

      for (Element product : products) {
         String data = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".product", "data-product");
         JSONObject jsonObject = CrawlerUtils.stringToJson(data);
         String internalPid = jsonObject.optString("sku");
         String internalId = jsonObject.optString("prodid");
         String productUrl = CrawlerUtils.scrapUrl(product, ".product_header_details a", "href", "https", "https://www.maccosmetics.com.br/");
         saveDataProduct(internalId, internalPid, productUrl);

         log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
         if (arrayProducts.size() == productsLimit) {
            break;
         }
      }


   }

   private String getUrl(JSONObject attributes) {
      String urlProductIncomplete = attributes.optString("p_url");
      String specificProduct = attributes.optString("s_SHADENAME").replace(" ", "_");
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append("https://www.maccosmetics.com.br/");

      if (urlProductIncomplete != null) {
         stringBuilder.append(urlProductIncomplete);
         if (specificProduct != null && !specificProduct.isEmpty()) {
            stringBuilder.append("/shade/").append(specificProduct);
         }

      }

      return stringBuilder.toString();
   }

   @Override
   protected boolean hasNextPage() {
      return false;
   }

   protected void setTotalProducts(JSONObject resultsList) {
      String totalProduct = "totalRecNum";
      if (resultsList.has(totalProduct) && resultsList.get(totalProduct) instanceof Integer) {
         this.totalProducts = CrawlerUtils.getIntegerValueFromJSON(resultsList, totalProduct, 0);
         this.log("Total da busca: " + this.totalProducts);
      }
   }
}


