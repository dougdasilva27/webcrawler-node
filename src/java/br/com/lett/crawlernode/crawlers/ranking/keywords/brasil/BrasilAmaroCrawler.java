package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilAmaroCrawler extends CrawlerRankingKeywords {
   public BrasilAmaroCrawler(Session session) {
      super(session);
   }

   private final String HOME_PAGE = "https://amaro.com";

   @Override
   protected void processBeforeFetch() {
      Request request = Request.RequestBuilder.create()
         .setUrl("https://amaro.com/br/pt")
         .setProxyservice(List.of(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_ROTATE_BR))
         .setFollowRedirects(true)
         .build();
      Response response = new FetcherDataFetcher().get(session, request);
      this.cookies.addAll(response.getCookies());
   }

   @Override
   public void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 64;
      Integer position = 1;
      this.log("Página " + this.currentPage);

      String url = "https://amaro.com/br/pt/busca/" + this.keywordEncoded + "?order=relevance&page=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("[class*=ProductList_listItem]");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element product : products) {
            Elements variants = product.select("ul[class*=ProductTile_variants] li");

            for (Element variant : variants) {
               String name = CrawlerUtils.scrapStringSimpleInfo(product, "h3[class*=ProductTile_title]", true);
               String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(product, "[class*=ProductTile_image] img", Collections.singletonList("src"), "https", "amaroecp-res.cloudinary.com");
               Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, "[class*=ProductPrice_mainPrice][class*=ProductPrice_hideDesktop]", null, false, ',', session, null);
               boolean isAvailable = price != null;

               String productUrl = HOME_PAGE + CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "figure [class*=ProductTile_link]", "href");

               if (variants.size() > 1) {
                  productUrl = HOME_PAGE + CrawlerUtils.scrapStringSimpleInfoByAttribute(variant, "a[class*=Link_link]", "href");
                  name = crawlVariantName(name, variant);
               }

               String internalId = scrapInternal(productUrl, "/([0-9]*_[0-9]*)/");
               String internalPid = scrapInternal(productUrl, "/(\\d+)_");

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setImageUrl(imgUrl)
                  .setPriceInCents(price)
                  .setAvailability(isAvailable)
                  .setPosition(position)
                  .build();

               saveDataProduct(productRanking);
            }

            position++;

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

   private String scrapInternal(String productUrl, String pattern) {
      Pattern regexPattern = Pattern.compile(pattern);
      Matcher matcher = regexPattern.matcher(productUrl);

      if (matcher.find()) {
         return matcher.group(1);
      }

      return null;
   }

   private String crawlVariantName(String name, Element variant) {
      String variantCode = CrawlerUtils.scrapStringSimpleInfoByAttribute(variant, "a[class*=Link_link] img", "alt");

      if (variantCode != null) {
         return name + " - " + variantCode;
      }

      return name;
   }

   @Override
   protected void setTotalProducts() {
      String totalProducts = CrawlerUtils.scrapStringSimpleInfo(this.currentDoc, "strong[class*=countValue]", true);

      if (totalProducts != null) {
         this.totalProducts = Integer.parseInt(totalProducts);
      }

      this.log("Total da busca: " + this.totalProducts);
   }
}
