package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class BrasilEspacoprimeCrawler extends CrawlerRankingKeywords {

   public BrasilEspacoprimeCrawler(Session session) {
      super(session);
   }

   final private String HOME_PAGE = "https://www.espacoprime.com.br";
   final private String loja = "686651";

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 36;

      String url = HOME_PAGE + "/loja/busca.php?loja=" + loja + "&palavra_busca=" + this.keywordEncoded + "&pg=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      this.log("Página " + this.currentPage);

      JSONObject pageJson = crawlPageJson(this.currentDoc);

      if (this.currentPage == 1) {
         this.totalProducts = pageJson.optInt("siteSearchResults", 0);
      }

      JSONArray products = pageJson.optJSONArray("listProducts");

      if (products != null && !products.isEmpty()) {
         for (Object o : products) {
            JSONObject product = (JSONObject) o;
            String internalId = product.optString("idProduct");
            String internalPid = internalId;
            String productUrl = product.optString("urlProduct");
            String name = product.optString("nameProduct");
            String imageUrl = product.optString("urlImage");
            Double price = JSONUtils.getDoubleValueFromJSON(product,"sellPrice", true);
            Integer priceInCents = price != null ? (int) Math.round((price * 100)) : 0;
            int stock = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, "form[data-id=\"" + internalId + "\"] .qntdProd", true, 0);
            boolean isAvailable = stock > 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPriceInCents(priceInCents)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
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

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private JSONObject crawlPageJson(Document doc) {
      Element dataScript = doc.selectFirst("script:containsData(dataLayer = [)");
      String jsonDataString = dataScript != null ? dataScript.data().substring(13, dataScript.data().length() - 1) : null; // removing "dataLayer = [" and the last "}"
      return CrawlerUtils.stringToJson(jsonDataString);
   }
}
