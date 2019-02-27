package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilDellCrawler extends CrawlerRankingKeywords {

  public BrasilDellCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 8;

    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página
    String url = "https://pilot.search.dell.com/queryunderstandingapi/4/br/pt/19/search-v/products?term=" + this.keywordEncoded;
    String payload = createRequestPayload(this.currentPage, pageSize);

    Map<String, String> headers = new HashMap<>();
    headers.put("content-type", "application/json");

    this.log("Link onde são feitos os crawlers: " + url);

    // chama função de pegar o html
    JSONObject products = new JSONObject(fetchPostFetcher(url, payload, headers, null));
    // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
    if (products.length() >= 1) {
      // se o total de busca não foi setado ainda, chama a função para setar
      if (this.totalProducts == 0) {
        setTotalBusca(products);
      }

      Map<String, String> productsMap = crawlSkuInformations(products);

      if (productsMap.size() >= 1) {

        for (Entry<String, String> product : productsMap.entrySet()) {

          // InternalPid
          String internalPid = null;

          // InternalId
          String internalId = product.getKey();

          // Url do produto
          String productUrl = CrawlerUtils.completeUrl(product.getValue(), "https", "www.dell.com");

          saveDataProduct(internalId, internalPid, productUrl);

          this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);

          if (this.arrayProducts.size() == productsLimit) {
            break;
          }
        }
      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado na página atual!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }

  @Override
  protected boolean hasNextPage() {
    // se elemeno page obtiver algum resultado
    if (this.arrayProducts.size() < this.totalProducts) {
      // tem próxima página
      return true;
    }

    return false;
  }

  protected void setTotalBusca(JSONObject products) {
    if (products.has("AdditionalInformation")) {
      JSONObject info = products.getJSONObject("AdditionalInformation");

      if (info.has("ResultCount-Products")) {
        try {
          this.totalProducts = Integer.parseInt(info.getString("ResultCount-Products").trim());
        } catch (Exception e) {
          this.logError(CommonMethods.getStackTraceString(e));
        }

        this.log("Total da busca: " + this.totalProducts);
      }
    }
  }

  /**
   * Mapa de id e url
   * 
   * @param products
   * @return
   */
  private Map<String, String> crawlSkuInformations(JSONObject products) {
    Map<String, String> productMap = new HashMap<>();

    if (products.has("Results")) {
      JSONArray arrayProducts = products.getJSONArray("Results");

      for (int i = 0; i < arrayProducts.length(); i++) {
        JSONObject product = arrayProducts.getJSONObject(i);

        if (product.has("Data")) {
          JSONObject data = product.getJSONObject("Data");

          if (data.has("Id") && data.has("MoreDetailsLink") && !data.get("Id").toString().trim().isEmpty()) {
            productMap.put(data.getString("ProductId"), data.getString("MoreDetailsLink"));
          }
        }
      }
    }

    return productMap;
  }

  private String createRequestPayload(int page, int pageSize) {
    JSONObject jsonPayload = new JSONObject();

    jsonPayload.put("IncludeRefiners", true);
    jsonPayload.put("IncludeCategoryTree", false);

    JSONObject virtualAssistantData = new JSONObject();

    virtualAssistantData.put("TemplateName", "AutoSuggest");
    virtualAssistantData.put("TemplateId", JSONObject.NULL);// The Dell API still reads the null value inside the String.
    virtualAssistantData.put("Purpose", JSONObject.NULL);
    virtualAssistantData.put("Data", new JSONObject());

    jsonPayload.put("VirtualAssistantData", virtualAssistantData);
    jsonPayload.put("FiltersUpdatedByUser", false);
    jsonPayload.put("PreviousTerm", this.location);
    jsonPayload.put("Categories", new JSONArray());

    JSONObject options = new JSONObject();
    JSONArray resultOptions = new JSONArray();

    options.put("withqueryunderstandingenabled", true);
    options.put("UrlReferrer", "https://www.dell.com/pt-br");
    options.put("WithNoTrackingEnabled", false);
    options.put("ResultOptions", resultOptions);
    options.put("IncludeGraph", false);
    options.put("IncludeSignals", false);
    options.put("IncludeTimings", false);

    jsonPayload.put("Options", options);
    jsonPayload.put("Categories", JSONObject.NULL);

    JSONObject profile = new JSONObject();

    profile.put("Segment", "gen");
    profile.put("CustomerSet", "");
    profile.put("Language", "pt");
    profile.put("Country", "br");
    profile.put("STPShopEnabled", false);

    jsonPayload.put("Profile", profile);
    jsonPayload.put("Products", new JSONArray().put(new JSONObject().put("Code", "")));
    jsonPayload.put("PreviousCategories", new JSONArray());
    jsonPayload.put("FiltersUpdatedByUser", false);
    jsonPayload.put("OverrideTerm", false);

    jsonPayload.put("PagingOptions", new JSONObject().put("Take", this.pageSize).put("Skip", this.arrayProducts.size()));

    return jsonPayload.toString();
  }
}
