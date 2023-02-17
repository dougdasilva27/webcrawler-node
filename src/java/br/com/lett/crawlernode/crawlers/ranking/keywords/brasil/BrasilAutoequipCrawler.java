package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilAutoequipCrawler extends CrawlerRankingKeywords {


   public BrasilAutoequipCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 10;

      this.log("Página " + this.currentPage);

      String url = "https://www.autoequip.com.br/buscar?q=" + this.keywordEncoded + "&pagina=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".listagem-item");

      if (!products.isEmpty()) {
         for (Element e : products) {
            String imgUrl = CrawlerUtils.scrapUrl(e, ".imagem-produto img", "src", "https", "www.autoequip.com.br");
            String productUrl = CrawlerUtils.scrapUrl(e, ".listagem-item a", "href", "https", "www.autoequip.com.br");
            String internalPid = CrawlerUtils.scrapStringSimpleInfo(e, ".produto-sku",true);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".nome-produto", true);
            Integer priceInCents = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".preco-promocional", "data-sell-price", true, '.', session, null);
            boolean availability = e.select(".produto-avise").size() == 0 && priceInCents != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(null)
               .setInternalPid(internalPid)
               .setImageUrl(imgUrl)
               .setName(name)
               .setPriceInCents(priceInCents)
               .setAvailability(availability)
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


   private String crawlInternaId(String imgUrl) {
      String internalId = null;
      if (imgUrl != null) {
         Pattern pattern = Pattern.compile("produto\\/([0-9]*)");
         Matcher matcher = pattern.matcher(imgUrl);
         if (matcher.find()) {
            internalId = matcher.group(1);
         }
      }
      return internalId;
   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }
}
