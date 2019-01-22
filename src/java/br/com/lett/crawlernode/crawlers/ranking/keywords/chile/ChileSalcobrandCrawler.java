package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class ChileSalcobrandCrawler extends CrawlerRankingKeywords {

  public ChileSalcobrandCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);
    
    this.pageSize = 15;

    String url = "https://salcobrand.cl/chaordic_api/search?resultsPerPage=15&page=" + this.currentPage + "&terms=" + this.keywordEncoded + "&sortBy=relevance&filter[]=";
    this.log("Link onde são feitos os crawlers: " + url);

    JSONObject searchedJson = DataFetcher.fetchJSONObject(DataFetcher.GET_REQUEST, session, url, null, cookies);
    
    JSONArray products = searchedJson.has("products") ? searchedJson.getJSONArray("products") : new JSONArray();
    
    if (products.length() > 0) {
     
      if (this.totalProducts == 0) {
    
        setTotalProducts(searchedJson);
      
      }
      
      for (Object object : products) {
        
        JSONObject product = (JSONObject) object;
        
        String internalId = crawlInternalId(product);
        String productUrl = crawlProductUrl(product);
        
        saveDataProduct(internalId, null, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit)
          break;

      }

    } else {
      
      this.result = false;
      this.log("Keyword sem resultado!");
    
    }
    
    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

  }
  
  private String crawlInternalId(JSONObject product) {
    String internalId = null;
    
    if(product.has("sku")) {
      internalId = product.getString("sku");
    }
    
    return internalId;
  }
  
  private String crawlProductUrl(JSONObject product) {
    String productUrl = null;
    
    if(product.has("url")) {
    
      productUrl = product.getString("url");
    
    }
    
    return CrawlerUtils.completeUrl(productUrl, "https:", "salcobrand.cl");
  }
  
  
  protected void setTotalProducts(JSONObject searchedJson) {
    
    if(searchedJson.has("size")) {
      
      if(searchedJson.get("size") instanceof Integer) {
      
        this.totalProducts = searchedJson.getInt("size");
        this.log("Total da busca: " + this.totalProducts);

      }
      
    }
    
  }
  
}
