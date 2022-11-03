package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BrasilMadridSupermercados extends CrawlerRankingKeywords {
   public BrasilMadridSupermercados(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      String url = "https://www.madrid.com.br/busca/" + this.keywordEncoded + "/?pagina=" + this.currentPage;
      this.pageSize = 28;
      this.currentDoc = fetchDocument(url);
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, "#ctl00_ContentPlaceHolder1_dvResultados h1", true, 0);
      Elements elements = this.currentDoc.select(".prods>ul li");
      if (elements != null) {
         for (Element e : elements) {
            String internalId = getInternalId(CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".lazy", "data-src"));
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a", "href");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".info", true);
            int price = CrawlerUtils.scrapIntegerFromHtml(e, ".valor", true, 0);
            String image = CrawlerUtils.scrapSimplePrimaryImage(e, ".lazy", Arrays.asList("data-src"), "", "");
            RankingProduct rankingProduct = RankingProductBuilder.create()
               .setInternalId(internalId)
               .setInternalPid(internalId)
               .setUrl(productUrl)
               .setName(name)
               .setPriceInCents(price)
               .setImageUrl(image)
               .setAvailability(true)
               .build();
            saveDataProduct(rankingProduct);
            if (arrayProducts.size() == productsLimit) {
               break;
            }
         }
      }

   }

   private String getInternalId(String id) {
      if (id != null && !id.isEmpty()) {
         String skuWithPoint = CommonMethods.getLast(id.split("/"));
         if (skuWithPoint != null && !id.isEmpty()) {
            List<String> idReturn = List.of(skuWithPoint.split("\\."));
            return idReturn.get(0);
         }
      }
      return "";
   }
}
