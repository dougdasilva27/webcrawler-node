package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilBelatintasCrawler extends CrawlerRankingKeywords {


   public BrasilBelatintasCrawler(Session session) {
      super(session);
   }

   private static final String HOST = "www.belatintas.com.br";

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {

      this.pageSize = 24;
      this.log("Página " + this.currentPage);

      String url = "https://" + HOST + "/busca?busca=" + this.keywordEncoded + "&pagina=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".spot");

      if (!products.isEmpty()) {
         for (Element e : products) {


            String internalId = scrapId(e);
            String productUrl = CrawlerUtils.scrapUrl(e, ".spotContent > a", "href", "https", HOST);

            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".spotTitle", true);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".imagem-primaria", "data-original");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".fbits-valor", null, true, ',', session, 0);
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
               .build();

            saveDataProduct(productRanking);

            this.log(
                  "Position: " + this.position +
                        " - InternalId: " + internalId +
                        " - Url: " + productUrl
            );

            if (this.arrayProducts.size() == productsLimit)
               break;
         }

      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");


   }


   private String scrapId(Element e) {
      String pid = null;

      String att = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".spotContent a", "href");
      if (att != null) {
         pid = CommonMethods.getLast(att.split("-"));
      }

      return pid;
   }

   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select("div #btnVerMais").isEmpty();
   }


}
