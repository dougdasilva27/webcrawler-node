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
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class BrasilPalacioDasFerramentas extends CrawlerRankingKeywords {
   protected Integer PRODUCTS_PER_PAGE = 24;
   private static final String HOME_PAGE = "https://www.palaciodasferramentas.com.br";

   public BrasilPalacioDasFerramentas(Session session) {
      super(session);
      super.fetchMode = FetchMode.JSOUP;
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.log("Página " + this.currentPage);

      String url = HOME_PAGE + "/load/buscar?q=" + this.keywordEncoded +
         "&page=" + this.currentPage + "&inverter=true";

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("body li");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            this.totalProducts = products.size();
         }

         for (Element e : products) {
            String productUrlPath = scrapUrl(e);
            String productUrl = productUrlPath != null ? HOME_PAGE + productUrlPath : null;
            String internalId = scrapInternalId(productUrlPath);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, "a h1", false);
            String imgUrl = scrapImgUrl(e);
            Integer price = scrapPrice(e);
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setImageUrl(imgUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
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

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");

   }

   private String scrapImgUrl (Element e) {
      String img = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "img", "data-src");

      if (img != null) {
         img = HOME_PAGE + img.replaceAll("\\s+","%20");
      }
      return img;
   }
   private String scrapInternalId(String url) {
      List<String> urlParts = url.isEmpty() ? null : List.of(url.split("/"));

      if (urlParts != null && !urlParts.isEmpty()) {
         return urlParts.get(2);
      }

      return null;
   }

   private String scrapUrl (Element e) {
      String urlPath = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a:nth-child(2n)", "href");

      if (urlPath == null || urlPath.contains("sub-departamento")){
         urlPath = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a:nth-child(3n)", "href");
      }

      return urlPath;
   }
   private Integer scrapPrice(Element e) {
      String priceDescription = CrawlerUtils.scrapStringSimpleInfo(e, "h2", false);

      if (priceDescription != null) {
         priceDescription = priceDescription.replaceAll("[^0-9]", "");
         return Integer.parseInt(priceDescription);
      }

      return null;
   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }

   @Override
   protected Document fetchDocument(String url) {
      Map<String, String> headers = new HashMap<>();

      headers.put("Accept","*/*");
      headers.put("Accept-Encoding","gzip, deflate, br");
      headers.put("Connection","keep-alive");
      headers.put("authority", "www.palaciodasferramentas.com.br");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.LUMINATI_SERVER_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR))
         .setSendUserAgent(false)
         .build();

      Response response = CrawlerUtils.retryRequest(request, session, dataFetcher, true);

      return Jsoup.parse(response.getBody());
   }

}
