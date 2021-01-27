package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;

public class ArgentinaMasfarmaciasCrawler extends CrawlerRankingKeywords {

   public ArgentinaMasfarmaciasCrawler(Session session) {
      super(session);
   }

   public Document crawlApi(String url) {

      Map<String, String> headers = new HashMap<>();
      headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36");
      headers.put("X-Requested-With", "XMLHttpRequest");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();

      JSONObject json = CrawlerUtils.stringToJson(this.dataFetcher
         .get(session, request)
         .getBody());
      String page = json.optString("page");
      return Jsoup.parse(page);

   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 24;
      this.log("Página " + this.currentPage);
      String url = "https://www.masfarmacias.com/catalogsearch/result/?q=" + this.keywordEncoded + "&page=1&is_ajax=1&p=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = crawlApi(url);

      Elements products = this.currentDoc.select(".category-products .products-grid li");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts(this.currentDoc);
         }
         for (Element e : products) {
            String internalId = crawlIntenalId(e);
            String productUrl = CrawlerUtils.scrapUrl(e, ".product-details h3.product-name a", "href", "https", "www.masfarmacias.com");

            saveDataProduct(internalId, null, productUrl);

            this.log(
               "Position: " + this.position +
                  " - InternalId: " + internalId +
                  " - InternalPid: " + null +
                  " - Url: " + productUrl);

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");

   }

   private String crawlIntenalId(Element e) {
      String internalIdAttribute = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".special-price .price", "id");

      if (internalIdAttribute == null || internalIdAttribute.isEmpty()) {
         internalIdAttribute = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".price-box .regular-price", "id");

      }

      return split(internalIdAttribute);
   }

   private String split(String internalIdAttribute) {
      String[] internalIdArray = internalIdAttribute.split("-");

      return internalIdArray.length > 1 ? internalIdArray[2] : null;
   }

   private void setTotalProducts(Document doc) {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(doc, ".amountvisible", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }
}
