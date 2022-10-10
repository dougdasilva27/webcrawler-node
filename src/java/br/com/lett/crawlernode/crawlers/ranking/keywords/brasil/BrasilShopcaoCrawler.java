package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
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

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

public class BrasilShopcaoCrawler extends CrawlerRankingKeywords {
   public BrasilShopcaoCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.JSOUP;
   }

   @Override
   protected Document fetchDocument(String url) {
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
            ProxyCollection.BONANZA,
            ProxyCollection.NETNUT_RESIDENTIAL_ES,
            ProxyCollection.NETNUT_RESIDENTIAL_BR
         ))
         .build();

      String response = dataFetcher.get(session, request).getBody();

      return Jsoup.parse(response);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String url = "https://shopcao.com.br/search?options%5Bprefix%5D=none&options%5Bunavailable_products%5D=last&page=" + this.currentPage + "&q=" + this.keywordEncoded + "%2A&type=product";
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("div.products.nt_products_holder.row > div");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) setTotalProducts();
         for (Element product : products) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "div.jdgm-widget.jdgm-preview-badge", "data-id");
            String productUrl = CrawlerUtils.scrapUrl(product, "div.product-info.mt__15 > h3 > a", "href", "https", "www.shopcao.com.br");
            String productName = CrawlerUtils.scrapStringSimpleInfo(product, "h3 > a", true);
            String imageUrl = captureImage(product);
            Integer price = getPrice(product);

            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create().setUrl(productUrl).setInternalPid(internalPid).setName(productName).setPriceInCents(price).setAvailability(isAvailable).setImageUrl(imageUrl).build();

            saveDataProduct(productRanking);
            if (this.arrayProducts.size() == productsLimit) break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }
      this.log("Finalizando Crawler de produtos da página: " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select("div.products-footer.tc.mt__40.mb__60 > nav > ul > li > a").isEmpty();
   }

   private Integer getPrice(Element element) {
      String price = CrawlerUtils.scrapStringSimpleInfo(element, ".price.dib.mb__5", true);

      if (price.isEmpty()) {
         Integer specialPrice = CrawlerUtils.scrapPriceInCentsFromHtml(element, ".price ins", null, true, ',', session, null);
         return specialPrice;
      }

      return priceVariation(price);
   }

   private Integer priceVariation(String price) {
      String priceString = "";
      if (priceString != null && !priceString.isEmpty()) {
         String arrayPrice[] = price.split(" ");
         if (arrayPrice[1] != null && !arrayPrice[1].isEmpty()) {
            priceString = arrayPrice[1];
            return CommonMethods.stringPriceToIntegerPrice(priceString, ',', null);
         }
      }
      return null;
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, " div.col-12.nt_pr__ > h4", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }

   private String captureImage(Element element) {
      String urlImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(element, ".product-image.pr.oh.lazyload", "data-include");
      if (urlImage != null && !urlImage.isEmpty()) {
         urlImage = "https://shopcao.com.br" + urlImage;
         Request request = Request.RequestBuilder.create()
            .setUrl(urlImage)
            .setSendUserAgent(true)
            .build();

         Response response = this.dataFetcher.get(session, request);

         Document document = Jsoup.parse(response.getBody());
         String imageString = CrawlerUtils.scrapSimplePrimaryImage(document, ".pr_lazy_img.main-img.nt_img_ratio.lazyload.nt_bg_lz", List.of("data-bgset"), "https", "");

         if (imageString != null && !imageString.isEmpty()) {
            return imageString.replace("_1x1", "");
         }
      }
      return null;
   }
}
