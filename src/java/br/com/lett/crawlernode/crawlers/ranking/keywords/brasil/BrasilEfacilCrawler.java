package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class BrasilEfacilCrawler extends CrawlerRankingKeywords {

  public BrasilEfacilCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 48;

    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página
    String url = "https://busca.efacil.com.br/busca?q=" + this.keywordEncoded + "&page="
        + this.currentPage + "&results_per_page=48";
    this.log("Link onde são feitos os crawlers: " + url);

    // chama função de pegar o html
    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".nm-product-item");

    // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
    if (!products.isEmpty()) {
      // se o total de busca não foi setado ainda, chama a função para setar
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {

        // InternalPid
        String internalPid = crawlInternalPid(e);

        // InternalId
        String internalId = crawlInternalId(e);

        // Url do produto
         String productUrl = null;
         try {
            productUrl = crawlProductUrl(e);
         } catch (UnsupportedEncodingException unsupportedEncodingException) {
            unsupportedEncodingException.printStackTrace();
         }

         saveDataProduct(internalId, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: "
            + internalPid + " - Url: " + productUrl);
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

  @Override
  protected boolean hasNextPage() {
    // se elemeno page obtiver algum resultado
      // tem próxima página
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

  private String crawlInternalId(Element e) {

     return CrawlerUtils.scrapStringSimpleInfoByAttribute(e, null, "catentry");

  }

  private String crawlInternalPid(Element e) {
    String internalPid = null;

    String comparedProducts = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".compare label input", "onclick");
    String[] formattedProducts = comparedProducts != null ? comparedProducts.split(",") : null;

    if(formattedProducts != null){
       internalPid = formattedProducts.length > 0 ? formattedProducts[formattedProducts.length -1]: null;
       internalPid = internalPid != null ? internalPid.replaceAll("[^0-9]", "").trim() : null;
    }


    return internalPid;
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
