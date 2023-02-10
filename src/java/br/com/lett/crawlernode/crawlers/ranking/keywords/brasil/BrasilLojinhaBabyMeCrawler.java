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
import java.util.Objects;

public class BrasilLojinhaBabyMeCrawler extends CrawlerRankingKeywords {
   public BrasilLojinhaBabyMeCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String url = "https://www.lojinhababyandme.com.br/catalogsearch/result/?q=" + this.keywordEncoded;
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".product-item-info.product-item-details");
      pageSize = products.size();

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) setTotalProducts();
         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-item-photo", "data-id");
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-item-photo", "href");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product-item-name a", true);
            String image = CrawlerUtils.scrapSimplePrimaryImage(e, ".product-image-wrapper img", Collections.singletonList("src"), "https", "www.lojinhababyandme.com.br").replace("4b21a695c360ef29dc1a3e8e073c7d35","fa0b1991bb146595e0635a88fa579466");
            boolean available = e.selectFirst("[data-event=\"addToCart\"]") != null;
            Integer priceInCents = available ? scrapPrice(e) : null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalId)
               .setImageUrl(image)
               .setName(name)
               .setPriceInCents(priceInCents)
               .setAvailability(available)
               .build();

            saveDataProduct(productRanking);
         }
      }
      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }

   private Integer scrapPrice(Element e) {
      Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, "[id*=product-price] .price", null, true, ',', session, null);

      if (price == null){
         price = CrawlerUtils.scrapPriceInCentsFromHtml(e, "[data-price-type=\"maxPrice\"] span", null, true, ',', session, null);
      }

      return price;
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = Integer.parseInt(Objects.requireNonNull(CrawlerUtils.scrapStringSimpleInfo(this.currentDoc, "#toolbar-amount span", true)));
      this.log("Total da busca: " + this.totalProducts);
   }
}
