package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ElsalvadorSuperselectosCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "https://www.superselectos.com/";
   protected String sucursalSelectos = session.getOptions().getString("sucursalSelectos");
   protected int productsQuantity;

   public ElsalvadorSuperselectosCrawler(Session session) {
      super(session);
   }

   @Override
   protected void processBeforeFetch() {
      BasicClientCookie cookie = new BasicClientCookie("sucursalSelectos", sucursalSelectos);
      cookie.setDomain("www.superselectos.com");
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 20;
      this.log("Página " + this.currentPage);

      String url = HOME_PAGE + "Tienda/Buscador?page=" + this.currentPage + "&keyword=" + this.keywordEncoded;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select("ul.productList li.elementoProd");
      productsQuantity = products.size();

      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".add-control button", "data-cod");
            String productUrl = HOME_PAGE + "Tienda/_DetalleProducto?idProducto=" + internalId;
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".desc", false);
            String imgUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".pic a img", "src");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e,".precio",null,false,'.',session,null);
            Boolean isAvailable = price !=null;


            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setImageUrl(imgUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();
            saveDataProduct(productRanking);

            if (this.arrayProducts.size() == productsLimit)
               break;
         }

      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return productsQuantity == 20;
   }
}
