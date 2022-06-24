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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class BrasilDDMaquinasCrawler extends CrawlerRankingKeywords {
   public BrasilDDMaquinasCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      pageSize = 24;

      currentDoc = fetchDocument("https://ddmaquinas.com.br/busca?search=" + keywordEncoded + "&page=" + currentPage);

      Elements products = currentDoc.select("#shelf-list-product  div.product-layout");
      for (Element element : products) {

         String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(element, "a", "href");
         String productName = CrawlerUtils.scrapStringSimpleInfoByAttribute(element, "a[title]", "title");
         String productImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(element, ".image a img", "src");
         Integer productPrice = CrawlerUtils.scrapPriceInCentsFromHtml(element, ".avista_price", null, true, ',', session, null);

         String regex = "-([0-9]*)$";
         Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
         Matcher matcher = pattern.matcher(productUrl);
         if (matcher.find()) {
            String internalPid = matcher.group(1);
            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalPid(internalPid)
               .setName(productName)
               .setPriceInCents(productPrice)
               .setAvailability(true)
               .setImageUrl(productImage)
               .build();

            saveDataProduct(productRanking);
         }
      }
   }
}
