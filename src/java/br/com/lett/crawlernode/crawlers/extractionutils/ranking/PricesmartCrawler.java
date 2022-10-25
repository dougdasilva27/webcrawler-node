package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;

public class PricesmartCrawler extends CrawlerRankingKeywords {


   public PricesmartCrawler(Session session) {
      super(session);
   }

   @Override
   protected Document fetchDocument(String url) {
      String club_id = session.getOptions().optString("club_id");
      String country = session.getOptions().optString("country");


      Map<String, String> headers = new HashMap<>();
      headers.put("Cookie", "userPreferences=country=" + country + "&selectedClub=" + club_id + "&lang=es");

      Request request = Request.RequestBuilder.create().setUrl(url).setHeaders(headers).build();
      String response = this.dataFetcher.get(session, request).getBody();

      return Jsoup.parse(response);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 30;
      this.log("Página " + this.currentPage);

      String url = "https://www.pricesmart.com/site/" + session.getOptions().optString("country") + "/es/busqueda?_sq=" + this.keywordEncoded + "&" + session.getOptions().optString("location_pagination") + ":page=" + this.currentPage + "&" + session.getOptions().optString("location_pagination");
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".search-product-box");


      if (!products.isEmpty()) {

         for (Element e : products) {

            String scrapInternalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".search-product-box a", "id");
            String internalPid = CommonMethods.getLast(scrapInternalId.split("result-"));
            String internalId = internalPid;
            String productUrl = "https://www.pricesmart.com" + CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".search-product-box a", "href");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, "#product-name", false);
            int price = CrawlerUtils.scrapIntegerFromHtml(e, "#product-price", false, 0);
            String avalilable = CrawlerUtils.scrapStringSimpleInfo(e, ".far.fa-times-circle", true);
            boolean isAvailable = checkIsAvailibale(price, avalilable);

            //New way to send products to save data product
            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);
            log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
            if (arrayProducts.size() == productsLimit) {
               break;
            }

         }
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }

   private boolean checkIsAvailibale(int price, String available) {
      if (available != null && !available.isEmpty()) {
         return price != 0 && available.contains("Disponible");
      }
      return price != 0;
   }

   @Override
   protected boolean hasNextPage() {
      return this.currentDoc.selectFirst(".pagination .page-item:not(.active)") != null;
   }

}
