package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilNutrimaisvidaCrawler extends CrawlerRankingKeywords {
   public BrasilNutrimaisvidaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 16;

      this.log("Página " + this.currentPage);

      String url = "https://nutrimaisvida.com.br/apps/omega-search/?&index=products&limit=16" + "&p=" + this.currentPage + "&q=" + this.keywordEncoded + "&view=grid";
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".hits-component > div > div > div > ol > li");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalId = crawlInternalId(e);
            String productUrl = CrawlerUtils.scrapUrl(e, "li > div > .os-e.os-image", "href", "https:", "nutrimaisvida.com.br/");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, "li > div > div > .os-e.os-name", true);
            String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(e, "li > div > a > div > img", Collections.singletonList("src"), "https", "//nutrimaisvida.com.br/");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, "li > div > div > .os-e.os-price", null, true, ',', session, null);
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

   protected void setTotalProducts() {
      Element totalElement = this.currentDoc.select(".os-e.os-toolbar-amount").first();

      if (totalElement != null) {
         String text = totalElement.ownText();

         if (text.contains("de")) {
            String total = text.split("de")[1].replaceAll("[^0-9]", "").trim();

            if (!total.isEmpty()) {
               this.totalProducts = Integer.parseInt(total);
            }
         }

         this.log("Total da busca: " + this.totalProducts);
      }
   }

   private String crawlInternalId(Element e) {
      String internalid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "li>div", "class");
      final String regex = "(\\d+)";
      final String string = internalid;

      final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      final Matcher matcher = pattern.matcher(string);

      while (matcher.find()) {
         return matcher.group(1);
      }
      return null;
   }

   @Override
   protected boolean hasNextPage() {
      Element page = this.currentDoc.selectFirst("#os-content > div > div > span > span > ul > li");
      return page != null;
   }
}
