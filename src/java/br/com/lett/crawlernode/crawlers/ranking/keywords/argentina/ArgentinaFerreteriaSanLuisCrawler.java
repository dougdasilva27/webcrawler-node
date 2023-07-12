package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
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
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArgentinaFerreteriaSanLuisCrawler extends CrawlerRankingKeywords {

   public ArgentinaFerreteriaSanLuisCrawler(Session session) {
      super(session);
   }

   @Override
   protected Document fetchDocument(String url) {
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setProxyservice(List.of(
            ProxyCollection.SMART_PROXY_AR_HAPROXY,
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
            ProxyCollection.LUMINATI_SERVER_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_DE_HAPROXY
         ))
         .build();
      Response response = CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher());
      return Jsoup.parse(response.getBody());
   }

   private String getHashMd5() {
      MessageDigest digest = null;
      try {
         digest = MessageDigest.getInstance("MD5");
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException(e);
      }
      digest.update(this.keywordWithoutAccents.getBytes(), 0, this.keywordWithoutAccents.length());
      return new BigInteger(1, digest.digest()).toString(16);
   }

   Integer auxNum = 0;

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 24;

      String url = "https://www.ferreterasanluis.com/s/" + getHashMd5() + "/0/id_ASC/" + this.auxNum;
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".tm-item");
      this.auxNum += this.pageSize;

      if (products != null && !products.isEmpty()) {
         if (this.totalProducts == 0) setTotalProducts();
         for (Element e : products) {
            String productName = CrawlerUtils.scrapStringSimpleInfo(e, ".tm-name", true);
            String productUrl = CrawlerUtils.scrapUrl(e, ".js-link a", "href", "https", "www.ferreterasanluis.com/");
            String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".tm-img img", Collections.singletonList("src"), "https", "www.ferreterasanluis.com");
            String internalPid = getInternalPid(imageUrl);
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".tm-final-price", null, false, ',', session, 0);
            boolean available = price > 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalPid(internalPid)
               .setName(productName)
               .setPriceInCents(price)
               .setAvailability(available)
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
      this.log("Finalizando Crawler de produtos da página: " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private String getInternalPid(String image_path) {
      if (image_path != null && !image_path.isEmpty()) {
         String id_str = CommonMethods.getLast(List.of(image_path.split("/")));
         if (id_str != null) {
            return CommonMethods.substring(id_str, "", ".", false);
         }
      }
      return null;
   }

   @Override
   protected void setTotalProducts() {
      String regex = " ([0-9]+)";
      Element string = this.currentDoc.selectFirst(".uk-width-1-1 h1 span");

      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      Matcher matcher = pattern.matcher(String.valueOf(string));
      if (matcher.find()) {
         String totalElement = matcher.group(1);
         this.totalProducts = Integer.parseInt(String.valueOf(totalElement));
      }

      this.log("Total da busca: " + this.totalProducts);
   }
}
