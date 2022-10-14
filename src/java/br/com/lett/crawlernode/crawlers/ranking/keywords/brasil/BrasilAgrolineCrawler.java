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

import java.io.UnsupportedEncodingException;

public class BrasilAgrolineCrawler extends CrawlerRankingKeywords {

   private final String HOME_PAGE = "https://www.agroline.com.br/";

   public BrasilAgrolineCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String url = HOME_PAGE + "busca?busca=" + this.keywordWithoutAccents + "&pagina=" + this.currentPage;
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".spotContent");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts(this.currentDoc);
         }
         for (Element product : products) {
            String productName = CrawlerUtils.scrapStringSimpleInfo(product, ".spotTitle", true);
            String productUrl = CrawlerUtils.scrapUrl(product, ".spot-parte-um", "href", "https", "www.agroline.com.br");
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".jsImgSpot.imagem-primaria", "src");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".fbits-valor", null, false, ',', session, null);
            String internalPid = CommonMethods.getLast(productUrl.split("-"));
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalPid(internalPid)
               .setName(productName)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
               .build();

            saveDataProduct(productRanking);
            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }
      this.log("Finalizando Crawler de produtos da página: " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private void setTotalProducts(Document doc) {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(doc, ".fbits-qtd-produtos-pagina", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }
}
