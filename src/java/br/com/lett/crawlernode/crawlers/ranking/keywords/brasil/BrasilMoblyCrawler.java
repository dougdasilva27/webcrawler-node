package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;

public class BrasilMoblyCrawler extends CrawlerRankingKeywords {

  public BrasilMoblyCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
    this.pageSize = 60;

    this.log("Página " + this.currentPage);

    String url = "https://www.mobly.com.br/catalog/?terms=" + this.keywordEncoded + "&page=" + this.currentPage
        + "&api=true&partner=Neemu&bucketTest=A";

    Map<String, String> headers = new HashMap<>();
    headers.put("x-requested-with", "XMLHttpRequest");

    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).build();

    JSONObject productsInfo = JSONUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
    JSONObject products = JSONUtils.getJSONValue(productsInfo, "products");

    if (products.length() > 0) {
      if (totalProducts == 0) {
        this.totalProducts = CrawlerUtils.getIntegerValueFromJSON(productsInfo, "total", 0);
        this.log("Total: " + this.totalProducts);
      }

      for (String internalPid : products.keySet()) {
        JSONObject product = products.getJSONObject(internalPid);

        String productUrl = scrapUrl(product);

         String name = product.optString("name");
         String imgUrl = getImage(product);
         String priceString = product.optString("finalPrice") ;
         priceString =  priceString.replaceAll(",", "");
         Integer price = Integer.parseInt(priceString);
         boolean  isAvailable  =  product.optBoolean("stockAvailable");

         RankingProduct productRanking = RankingProductBuilder.create()
            .setUrl(productUrl)
            .setInternalPid(internalPid)
            .setName(name)
            .setImageUrl(imgUrl)
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
     https://staticmobly.akamaized.net/r/222x222/r/222x222/p/Cimol--Mesa-de-Jantar-Retangular-Grace-Nature-e-Chumbo-130-cm-4166-964018-1.jpg
    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }

   private String getImage(JSONObject product){
      try {
         return "htpps:" + product.optQuery("/productImage/optionTwo/main").toString() + ".jpg";
      }
      catch(NullPointerException e)
      {
         return "NullPointerException caught";
      }
   }

  private String scrapUrl(JSONObject product) {
    String url = null;

    String productUrl = JSONUtils.getStringValue(product, "url");
    if (productUrl != null) {
      url = CrawlerUtils.completeUrl(productUrl.split("#")[0], "https", "www.mobly.com.br");
    }

    return url;
  }
}
