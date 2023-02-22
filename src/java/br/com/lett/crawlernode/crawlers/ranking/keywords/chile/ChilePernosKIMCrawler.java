package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

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

public class ChilePernosKIMCrawler extends CrawlerRankingKeywords {
   public ChilePernosKIMCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String url = "https://ferreteria.cl/busquedas/" + this.keywordEncoded + "/0/0/0/0?page=" + this.currentPage;
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("div.prod");
      pageSize = products.size();

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) setTotalProducts();
         for (Element e : products) {
            String productUrl = "https://ferreteria.cl/" + CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a.img", "href");
            String internalId = scrapInternalId(productUrl);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".nombre", true);
            String image = CrawlerUtils.scrapSimplePrimaryImage(e, "a.img img", Collections.singletonList("src"), "https", "ferreteria.cl");
            boolean available = false;
            Integer price = available ? CrawlerUtils.scrapPriceInCentsFromHtml(e, ".precio", null, true, ',', session, null) : null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalId)
               .setImageUrl(image)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(available)
               .build();

            saveDataProduct(productRanking);
         }
      }
      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }

   private String scrapInternalId(String url) {
      String regex = "ficha/(.*)/";

      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      Matcher matcher = pattern.matcher(url);

      if (matcher.find()) {
         return matcher.group(1);
      }
      return null;
   }

   @Override
   protected void setTotalProducts() {
      String regex = "\\(([0-9]+) Productos\\)";
      String qntProducts = CrawlerUtils.scrapStringSimpleInfo(this.currentDoc, ".contenedor h3 small", true);

      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      Matcher matcher = pattern.matcher(qntProducts);

      if (matcher.find()) {
         this.totalProducts = Integer.parseInt(matcher.group(1));
      }

      this.log("Total da busca: " + this.totalProducts);
   }
}
