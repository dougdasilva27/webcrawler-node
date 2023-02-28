package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilMafachaCrawler extends CrawlerRankingKeywords {
   public BrasilMafachaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {

      String url = "https://www.mafacha.com.ar/module/iqitsearch/searchiqit?s=" + this.keywordEncoded + "&page=" + this.currentPage;
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".products.row.products-list .js-product-miniature-wrapper.mto-mayorista.col-12");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0)
            setTotalProducts();

         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-miniature.product-miniature-default.product-miniature-list.js-product-miniature.mto-articulo", "data-reference");
            String productUrl = CrawlerUtils.completeUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".thumbnail-container a", "href"), "", "");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".h3.product-title a", false);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "article > div > div.col-12.col-sm-3 > div > a > img", "data-src");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".mto-contador-resultado span", null, false, ',', session, null);
            boolean isAvailable = price != null && price != 0;

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

   protected Document fetchDocument(String url) {
      this.currentDoc = new Document(url);
      Request request = Request.RequestBuilder.create().setUrl(url).build();
      Response response = dataFetcher.get(session, request);

      return Jsoup.parse(response.getBody());
   }

   @Override
   protected void setTotalProducts() {
      String totalProduct = CrawlerUtils.scrapStringSimpleInfo(this.currentDoc, ".showing.hidden-sm-down", false);
      if (totalProduct != null) {
         Pattern pattern = Pattern.compile("(?<=de )\\d+(?= artículo\\(s\\))");
         Matcher matcher = pattern.matcher(totalProduct);

         if (matcher.find()) {
            this.totalProducts = Integer.parseInt(matcher.group());
         }

      } else {
         this.totalProducts = 0;
      }

      this.log("Total: " + this.totalProducts);
   }
}
