package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilCobasiCrawler extends CrawlerRankingKeywords {

   public BrasilCobasiCrawler(Session session) {
      super(session);
   }

   @Override
   protected Document fetchDocument(String url) {
      Request request = Request.RequestBuilder.create().setCookies(cookies).setUrl(url).build();
      Response response = new FetcherDataFetcher().get(session, request);

      return Jsoup.parse(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException, UnsupportedEncodingException {
      this.pageSize = 20;

      this.log("Página " + this.currentPage);

      String url = "https://www.cobasi.com.br/pesquisa?terms=" + this.keywordEncoded + "&p=are&ranking=1&typeclick=1&ac_pos=header&page=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("div.ProductListItem");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapUrl(e, "div.ProductListItem a", "href", "https", "www.cobasi.com.br");
            String internalPid = crawlInternalPid(productUrl);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, "h3[class*=styles__Title]", false);
            String imgUrl = crawlImage(e);
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, "div[class*=styles__PriceBox] span.card-price", null, true, ',', session, null);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(null)
               .setInternalPid(internalPid)
               .setImageUrl(imgUrl)
               .setName(name)
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

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private String crawlImage(Element e) throws UnsupportedEncodingException {
      String attImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "div[class*=styles__ImageBlock] img", "src");
      if (attImage != null) {
         String decodedUrl = URLDecoder.decode(attImage, StandardCharsets.UTF_8);
         if (decodedUrl.contains("=")) {
            String urlImage = decodedUrl.substring(decodedUrl.indexOf("=") + 1);
            return urlImage;
         }
      }

      return null;
   }

   @Override
   protected void setTotalProducts() {
      Element totalElement = this.currentDoc.select("div[class*=TotalDescription]").first();

      if (totalElement != null) {
         String total = totalElement.ownText().replaceAll("[^0-9]", "").trim();

         if (!total.isEmpty()) {
            this.totalProducts = Integer.parseInt(total);
         }

         this.log("Total da busca: " + this.totalProducts);
      }
   }

   private String crawlInternalPid(String url) {
      String id = null;
      Pattern pattern = Pattern.compile("-([0-9]+)\\/p");
      Matcher matcher = pattern.matcher(url);
      if (matcher.find()) {
         id = matcher.group(1);
      }
      return id;
   }
}
