package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class ArgentinaFravegaCrawler extends CrawlerRankingKeywords {

  public ArgentinaFravegaCrawler(Session session) {
    super(session);
  }

   protected String getPostalCode() {
      return session.getOptions().optString("postal_code");
   }

   @Override
   public void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException, OfferException, MalformedPricingException {
      this.pageSize = 24;
      Integer offsetPage = (this.currentPage - 1) * 15;
      this.log("Página " + this.currentPage);
      String url = "https://www.fravega.com/l/?keyword=" + this.keywordWithoutAccents.replaceAll(" ", "+") + "&page=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetch(url);

      JSONObject pageJson = CrawlerUtils.selectJsonFromHtml(this.currentDoc, "#__NEXT_DATA__", null, null, false, false);
      JSONArray products = JSONUtils.getValueRecursive(pageJson, "props.pageProps.__APOLLO_STATE__.ROOT_QUERY.items({\"filtering\":{\"active\":true,\"availableStock\":{\"includeThoseWithNoAvailableStockButListable\":true,\"postalCodes\":\"" + getPostalCode() + "\"},\"keywords\":{\"query\":\"" + this.keywordWithoutAccents + "\"},\"salesChannels\":[\"fravega-ecommerce\"]}}).results({\"buckets\":[{\"customSorted\":true,\"offset\":"+ offsetPage +",\"sorting\":\"TOTAL_SALES_IN_LAST_30_DAYS\"}],\"size\":15})", JSONArray.class);
      JSONObject items = JSONUtils.getValueRecursive(pageJson, "props.pageProps.__APOLLO_STATE__.ROOT_QUERY.items({\"filtering\":{\"active\":true,\"availableStock\":{\"includeThoseWithNoAvailableStockButListable\":true,\"postalCodes\":\"" + getPostalCode() + "\"},\"keywords\":{\"query\":\"" + this.keywordWithoutAccents + "\"},\"salesChannels\":[\"fravega-ecommerce\"]}})", JSONObject.class);

      if (products != null) {
         if (this.totalProducts == 0) {
            this.totalProducts = JSONUtils.getValueRecursive(items, "total", Integer.class);
         }
         for (Object o : products) {
            JSONObject product = (JSONObject) o;
            String internalPid = product.optString("id");
            String internalId = JSONUtils.getValueRecursive(product.optJSONObject("skus"), "results.0.code", String.class);
            String productUrl = formatUrl(product.optString("slug"), internalId);
            String name = product.optString("title");
            JSONObject priceObj = JSONUtils.getValueRecursive(product, "salePrice.amounts.0", JSONObject.class);;
            Integer price = null;

            if(priceObj != null && !priceObj.isEmpty()) {
               price = JSONUtils.getPriceInCents(priceObj, "min");
            }

            String image = JSONUtils.getValueRecursive(product, "images.0", String.class);

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setImageUrl(formatImage(image))
               .setPriceInCents(price)
               .setAvailability(price != null)
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
   }

   private String formatUrl(String slug, String sku) {
     return "https://www.fravega.com/p/" + slug + "-" + sku + "/";
   }

   private String formatImage(String image) throws OfferException, MalformedPricingException {
      String formatedImage = "https://images.fravega.com/f500/" + image;
      return formatedImage;
   }

   private Document fetch(String url) {
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setFollowRedirects(true)
         .build();

      Response response = dataFetcher.get(session, request);

      return Jsoup.parse(response.getBody());
   }

  @Override
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.selectFirst(".resultado-busca-numero > span.value");

    if (totalElement != null) {
      String text = totalElement.text().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }

  private String crawlInternalPid(Element e) {
    String internalPid = null;

    if (e != null) {
      String id = e.attr("id");

      if (id.contains("_")) {
        internalPid = CommonMethods.getLast(id.split("_"));
      }
    }
    return internalPid;
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;
    Element urlElement = e.selectFirst("a");

    if (urlElement != null) {
      productUrl = urlElement.attr("href");

      if (!productUrl.contains("fravega.com")) {
        productUrl = ("https://www.fravega.com/" + urlElement.attr("href")).replace(".com//", ".com/");
      }
    }

    return productUrl;
  }
}
