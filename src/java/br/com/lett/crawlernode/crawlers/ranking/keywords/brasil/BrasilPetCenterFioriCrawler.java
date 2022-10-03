package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.util.Collections;

public class BrasilPetCenterFioriCrawler extends CrawlerRankingKeywords {
   public BrasilPetCenterFioriCrawler(Session session) {
      super(session);
   }

   private String getImage(String values) {
      // String i[]= CommonMethods.getLast(values.split(" "));
      String imgs[] = values.split(",");
      Integer ult = imgs.length - 1;
      String pathImg[] = imgs[ult].split(" ");
      if (pathImg[1].contains("https://")) {
         return pathImg[1];
      }
      return "https:" + pathImg[1];
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      // Quando site acrrega os produtos se apresentam de uma forma e quando se atualiaza ou se scrolla a página ele altera a ordem dos produtos
      String url = "https://www.petcenterfiore.com.br/search/?q=" + this.keywordEncoded +
         "&page=" + this.currentPage;
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".js-item-product");

      if (!products.isEmpty()) {
         for (Element product : products) {
            String name = CrawlerUtils.scrapStringSimpleInfo(product, ".js-item-name.item-name", false);
            Integer spotlitePrice = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".js-price-display.item-price",
               null, false, ',', session, 0);
            String productUrl = CrawlerUtils.scrapUrl(product, ".item-link", "href",
               "https", "https://www.petcenterfiore.com.br");
            boolean isAvaliable = checkIsAvaliable(product);
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".js-product-form input", "value");
            String pId = internalId;
            String imgUrl = getImage(CrawlerUtils.scrapSimplePrimaryImage(product, ".js-item-image", Collections.singletonList("data-srcset"),
               "https", ""));
            RankingProduct rankingProduct = RankingProductBuilder.create().
               setInternalId(internalId)
               .setInternalPid(pId)
               .setName(name)
               .setImageUrl(imgUrl)
               .setAvailability(isAvaliable)
               .setPriceInCents(spotlitePrice)
               .setUrl(productUrl)
               .build();

            saveDataProduct(rankingProduct);
            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }

      }
   }
   private Boolean checkIsAvaliable(Element doc) {
      String avaliable = CrawlerUtils.scrapStringSimpleInfo(doc,".label.label-default",false);
      if (avaliable !=  null) {
         return !avaliable.contains("Esgotado");
      };
      return true;
   }
   @Override
   protected boolean hasNextPage() {
      Elements textsPages = this.currentDoc.select(".mt-4.page-header.category-body.container");
      for(Element textPage: textsPages) {
         String text = CrawlerUtils.scrapStringSimpleInfo(textPage,".text-center",false);
         if(text.contains("Não há resultados para a sua pesquisa")) {
            return false;
         }
      }
      return true;
   }
}
