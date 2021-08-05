package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class BrasilKalungaCrawler extends CrawlerRankingKeywords {

   public BrasilKalungaCrawler(Session session) {
      super(session);
   }

   public JSONObject crawlApi(String url) {

      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/x-www-form-urlencoded, application/json");

      String payload = null;
      try {
         payload = URLEncoder.encode("{\"termo\":\"" + this.keywordEncoded + "\",\"pagina\":" + this.currentPage + ",\"ordenacao\":1,\"fitroBusca\":[[]],\"classificacao\":null,\"grupo\":null}", "UTF-8");
         payload = "json_str=" + payload;
      } catch (UnsupportedEncodingException e) {
         e.printStackTrace();
      }

      Request request = RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .build();

      String content = this.dataFetcher
         .post(session, request)
         .getBody();

      return CrawlerUtils.stringToJson(content);

   }

   private Document getHtml(String templateProdutos) {
      Document html = null;
      if (templateProdutos != null) {

         html = Jsoup.parse(templateProdutos.replace("\"", ""));
      }

      return html;
   }


   @Override
   protected void extractProductsFromCurrentPage() {

      this.log("Página " + this.currentPage);

      String url = "https://www.kalunga.com.br/api/getBusca";

      JSONObject json = crawlApi(url);

      String templateProdutos = json.optString("templateProdutos");

      this.currentDoc = templateProdutos != null ? getHtml(templateProdutos) : null;
      Elements products = this.currentDoc != null ? this.currentDoc.select(".blocoproduto__row") : null;

      if (products != null && !products.isEmpty()) {
         if (this.totalProducts == 0)
            setTotalProducts(json);

         for (Element e : products) {
            String productUrl = crawlProductUrl(e);

            String internalId = CommonMethods.getLast(productUrl.split("/"));


            saveDataProduct(internalId, null, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultados!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }


   private String crawlProductUrl(Element e) {
      String href = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".blocoproduto__link", "href");

      return CrawlerUtils.completeUrl(href, "https", "www.kalunga.com.br");

   }


   protected void setTotalProducts(JSONObject json) {

      this.totalProducts = json.optInt("quantidadeRegistros");
      this.log("Total da busca: " + this.totalProducts);
   }

}
