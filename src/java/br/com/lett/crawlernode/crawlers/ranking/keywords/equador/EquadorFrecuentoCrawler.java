package br.com.lett.crawlernode.crawlers.ranking.keywords.equador;

import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Locale;

public class EquadorFrecuentoCrawler extends CrawlerRankingKeywords {
   public EquadorFrecuentoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 32;

      String url = "https://www.frecuento.com/frecuento/es/search?q=" + this.keywordEncoded + "%3Arelevance&page=" + (this.currentPage-1);
      this.currentDoc = fetchDocument(url, new ArrayList<>());

      Elements products = this.currentDoc.select("ul.product__list li");

      if (totalProducts == 0) {
         String totalBuscaTexto = CrawlerUtils.scrapStringSimpleInfo(currentDoc,".pagination-bar-results",true);
         if(totalBuscaTexto!=null) {
            String totalBusca = CommonMethods.substring(totalBuscaTexto.toLowerCase(Locale.ROOT), "de ", " producto", true);
         totalProducts = Integer.parseInt(totalBusca!=null?totalBusca:"0");
         }
      }
      for (Element element : products) {
         String id = CrawlerUtils.scrapStringSimpleInfoByAttribute(element, "input[name=\"productCodePost\"]", "value");
         String Pid ;
         String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(element, ".product__list--name", "href");
         String completeUrl = CrawlerUtils.completeUrl(productUrl, "https:", "www.frecuento.com");
         Integer priceInCents = CrawlerUtils.scrapPriceInCentsFromHtml(element, ".product__listing--price", null, true, '.', session, null);

         if(id==null){
            Pid=CommonMethods.getLast(completeUrl.split("/"));
         }else {
            Pid = id;
         }

         RankingProduct rankingProducts = RankingProductBuilder.create()
            .setInternalId(id)
            .setInternalPid(Pid)
            .setName(CrawlerUtils.scrapStringSimpleInfo(element, ".product__list--name", true))
            .setUrl(completeUrl)
            .setImageUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(element, "img", "data-src"))
            .setAvailability(true)
            .setPriceInCents(priceInCents)
            .setIsSponsored(false)
            .setKeyword(this.keywordEncoded)
            .setPosition(this.position)
            .setPageNumber(this.currentPage)
            .build();

         saveDataProduct(rankingProducts);
      }


   }

   @Override
   protected boolean hasNextPage() {
      return this.position<totalProducts;
   }
}
