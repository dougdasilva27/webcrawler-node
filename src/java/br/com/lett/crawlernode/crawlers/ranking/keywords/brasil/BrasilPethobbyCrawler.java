package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;

import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;

public class BrasilPethobbyCrawler extends CrawlerRankingKeywords {
  
  private static final String HOME_PAGE = "https://www.pethobby.com.br/";
  private String encodedPath = null;

  public BrasilPethobbyCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 24;
    this.log("Página " + this.currentPage);
    
    String url = "";

    if(this.currentPage > 1) {
      if(this.encodedPath == null) {
    	this.logError("Não foi possivel montar o caminho de busca");
        return;
      }
      
      url = HOME_PAGE + "buscapagina?fq=" + this.encodedPath + "&PS=24&sl=ee3ee0fc-5cb7-4d70-a0f4-b006076bb312&cc=4&sm=0&PageNumber=" + this.currentPage;
    } else {
      url = "https://www.pethobby.com.br/" + this.keywordEncoded + "?utmi_pc=BuscaFullText&utmi_cp=" + this.keywordEncoded;
    }
    
    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    
    Elements products = this.currentDoc.select(":not([id*=menu]) > [class*=prateleira] > ul > li:not(.helperComplement)");
    Elements helper = this.currentDoc.select(":not([id*=menu]) > [class*=prateleira] > ul > li.helperComplement");
    
    if(this.currentPage == 1) {
      scrapEncodedPath();
    }

    if (!products.isEmpty()) {
      if (this.totalProducts == 0 && this.currentPage == 1) {
        setTotalProducts();
      }
      
      for(int i = 0; i < products.size() && i < helper.size(); i++) {
        Element prod = products.get(i);
        Element help = helper.get(i);
        
        String internalPid = scrapInternalPid(help);
        String productUrl = CrawlerUtils.scrapUrl(prod, "a.productImage", Arrays.asList("href"), "https", HOME_PAGE);

        saveDataProduct(null, internalPid, productUrl);
        
        this.log(
            "Position: " + this.position + 
            " - InternalId: " + null +
            " - InternalPid: " + internalPid + 
            " - Url: " + productUrl);
        
        if (this.arrayProducts.size() == productsLimit)
          break;
      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
        + this.arrayProducts.size() + " produtos crawleados");
  }
  
  private String scrapInternalPid(Element doc) {
    String internalPid = null;
    
    if(doc != null && doc.id().contains("helperComplement_")) {
      internalPid = doc.id().replace("helperComplement_", "").trim();
    }
    
    return internalPid;        
  }
  
  private void scrapEncodedPath() {
    JSONObject json = CrawlerUtils.selectJsonFromHtml(this.currentDoc, "script", "vtex.events.addData(", ");", false, true);
    Integer catId = JSONUtils.getIntegerValueFromJSON(json, "categoryId", -1);
        
    if(catId == -1) {
      return;
    }
    
    String path = "C:/" + catId + "/";
    
    try {
      this.encodedPath = URLEncoder.encode(path, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      this.encodedPath = null;
    }
  }
  
  @Override
  protected void setTotalProducts() {
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, 
        ".resultado-busca-numero .value", "", "", true, true, 0);

    this.log("Total da busca: " + this.totalProducts);
  }
}