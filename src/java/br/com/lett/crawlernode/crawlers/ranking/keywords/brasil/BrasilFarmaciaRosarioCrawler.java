package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.cookie.Cookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrasilFarmaciaRosarioCrawler extends CrawlerRankingKeywords {
   public BrasilFarmaciaRosarioCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String url = "https://www.farmaciarosario.com.br/"+this.keywordEncoded+"/?p="+ this.currentPage;
      String cookie = "zipcode="+ session.getOptions().getString("zipcode");
      this.currentDoc = fetchDocument(url, cookie);

      Elements products = this.currentDoc.select(".col-12.col-lg-9.pl-lg-0 > div > ul.list-products.page-content > li");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0)
            setTotalProducts();

         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".item-product", "data-sku");
            String productUrl = CrawlerUtils.completeUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".item-image", "href"), "https:", "www.farmaciarosario.com.br");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".title a", false);
            String imageUrl = "https:"+ CrawlerUtils.scrapStringSimpleInfoByAttribute(e, " a > img", "data-src");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, "div.box-prices > div > div > p.sale-price > strong", null, false, ',', session, null);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
               .build();

            saveDataProduct(productRanking);
            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   protected Document fetchDocument(String url, String cookies) {
      this.currentDoc = new Document(url);
      Map<String, String> headers = new HashMap<>();
      headers.put("Cookie", cookies);
      Request request = Request.RequestBuilder.create().setHeaders(headers).setUrl(url).build();
      Response response = dataFetcher.get(session, request);

      return Jsoup.parse(response.getBody());
   }
   @Override
   protected void setTotalProducts(){
      String totalProduct = CrawlerUtils.scrapStringSimpleInfo(this.currentDoc,"#main-wrapper > div.content > div.container > div > div.col-12.col-lg-9.pl-lg-0 > div > div:nth-child(2)", false);
      String [] arrProduct = totalProduct.split(" ");
      this.totalProducts = Integer.parseInt(arrProduct[0]);
      this.log("Total: " + this.totalProducts);
   }
}
