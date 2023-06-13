package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilEfacilCrawler extends CrawlerRankingKeywords {

  public BrasilEfacilCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() throws MalformedProductException {
    this.pageSize = 12;
    this.log("Página " + this.currentPage);

    String url = "https://busca.efacil.com.br/busca?q=" + this.keywordEncoded + "&page="
        + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".nm-product-item");
    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalPid = crawlInternalPid(e);
        String productUrl = null;
         try {
            productUrl = crawlProductUrl(e);
         } catch (UnsupportedEncodingException unsupportedEncodingException) {
            unsupportedEncodingException.printStackTrace();
         }
         String name = CrawlerUtils.scrapStringSimpleInfo(e, ".nm-product-name", false);
         String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".nm-product-img", Collections.singletonList("src"), "https", "efacil.com.br");
         Integer price = crawlPrice(e);
         boolean isAvailable = price != 0;

         RankingProduct productRanking = RankingProductBuilder.create()
            .setUrl(productUrl)
            .setInternalPid(internalPid)
            .setImageUrl(imgUrl)
            .setName(name)
            .setPriceInCents(price)
            .setAvailability(isAvailable)
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

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
        + this.arrayProducts.size() + " produtos crawleados");
  }

   private Integer crawlPrice(Element e) {
     Integer price = 0;
      Element productScript = e.selectFirst("> script");
      List<String> prices = CrawlerUtils.getPropertyFromJSONInScript(productScript.html(), "price");
      if(!prices.isEmpty()) {
         price = CommonMethods.stringPriceToIntegerPrice(prices.get(0), '.', 0);
      }
      return price;
   }

   @Override
  protected boolean hasNextPage() {
    return this.arrayProducts.size() < this.totalProducts;
  }

  @Override
  protected void setTotalProducts() {
     String totalElement = CrawlerUtils.scrapStringSimpleInfo(this.currentDoc, ".neemu-total-products-container", true);
     int totalProducts = 0;
     if(totalElement != null){
        String totalStr = totalElement.replaceAll("[^0-9]", "").trim();
        totalProducts = !totalStr.isEmpty() ? Integer.parseInt(totalStr) : 0;
     }

     this.totalProducts = totalProducts;
     this.log("Total da busca: " + this.totalProducts);

  }

  private String crawlInternalPid(Element e) {
     return CrawlerUtils.scrapStringSimpleInfoByAttribute(e, null, "catentry");
  }

  private String crawlProductUrl(Element e) throws UnsupportedEncodingException {
     String productUrl = e.select(".nm-product-img-container img").attr("alt");
     String[] separatedUrl = productUrl.split("/");
     String lastPart = null;
     if(separatedUrl.length > 0){
        lastPart = separatedUrl[separatedUrl.length - 1];
     }
     String encodedPart = lastPart != null ? URLEncoder.encode(lastPart,"utf-8") : null;
     return "https://www.efacil.com.br/loja/produto/" + encodedPart;
  }
}
