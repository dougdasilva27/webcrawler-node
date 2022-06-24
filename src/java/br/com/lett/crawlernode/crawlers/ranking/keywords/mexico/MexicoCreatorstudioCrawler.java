package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.fetcher.models.Request;
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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MexicoCreatorstudioCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "https://creatorstudio.com.mx";

   public MexicoCreatorstudioCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 12;
      this.log("Página " + this.currentPage);

      String url;

      if (this.currentPage != 1) {
         url = HOME_PAGE + "/search?page=" + this.currentPage + "&q=" + this.keywordEncoded + "&type=product%2Carticle";
      } else {
         url = HOME_PAGE + "/search?type=product%2Carticle&q=" + this.keywordEncoded;
      }

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".grid.grid--uniform .grid__item");

      if (!products.isEmpty()) {

         for (Element e : products) {
            if (!e.select(".grid-search__page").isEmpty()) {
               continue;
            }
            String imgUrl = scrapImage(e);
            String productUrl = HOME_PAGE + CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a", "href");
            String internalPid = scrapInternalPid(e, productUrl);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".grid-product__title.grid-product__title--body", true);
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".grid-product__price", null, false, ',', session, 0);
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalPid(internalPid)
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

   private String scrapImage(Element e) {
      String split = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".grid__image-ratio.grid__image-ratio--square", "data-bgset");

      if (split != null) {
         return CrawlerUtils.completeUrl(split.replace("\n", "").replace("    //", "").split(",")[0].split(" ")[0], "https", "");
      }

      return null;
   }

   /*
   In same products haven't pid in grid
    */
   private String scrapInternalPidInPageProduct(String productUrl) {
      Request request = Request.RequestBuilder.create()
         .setUrl(productUrl)
         .build();

      Document document = Jsoup.parse(this.dataFetcher.get(session, request).getBody());

      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, ".product-single__sku", "id");

      return internalPid.replace("Sku-", "");

   }

   private String scrapInternalPid(Element e, String productUrl) {
      String internalPid = null;
      List<String> regexs = List.of("colors--([0-9]+)\"\\>", "colors--([0-9]+)\\>");
      for (String regex : regexs) {
         Pattern pattern = Pattern.compile(regex);
         Matcher matcher = pattern.matcher(e.toString());
         if (matcher.find()) {
            internalPid = matcher.group(1);
            break;
         } else {
            continue;
         }
      }

      if (internalPid == null) {
         internalPid = scrapInternalPidInPageProduct(productUrl);
      }

      return internalPid;
   }

   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select(".pagination .next").isEmpty();
   }
}
