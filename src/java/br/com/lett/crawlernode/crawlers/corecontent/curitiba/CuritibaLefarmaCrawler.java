package br.com.lett.crawlernode.crawlers.corecontent.curitiba;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.prices.Prices;

public class CuritibaLefarmaCrawler extends Crawler {

  public CuritibaLefarmaCrawler(Session session) {
    super(session);
  }
  
  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();
    
    List<String> selectors = new ArrayList<>();
    selectors.add(".descricao_texto");
    
    Elements options = doc.select("#ddlVarDef1 option:not(:first-child)");
    String internalPid = crawlInternalPid(doc);
    Float price = CrawlerUtils.scrapSimplePriceFloat(doc, "span[itemprop=price]", false);
    Prices prices = crawlPrices(price, doc);
    String description = CrawlerUtils.scrapSimpleDescription(doc, selectors);
    CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".migalha > a", true);
    boolean available = crawlAvailability(doc);
    
    if (isProductPage(doc)) {
      for (Element e : options) {
      
        Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
        
        String name = crawlName(e, doc);
        String internalId = crawlInternalId(e, doc);
        String primaryImage = crawlPrimaryImage(e, doc);
  
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(null).setDescription(description)
            .setMarketplace(null).build();
  
        products.add(product);
  
      } 
      
    }
    else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;

  }


  private String crawlInternalPid(Element e) {
    String internalPid = null;
    Element spanInternalPid = e.selectFirst("#lblCodigoProduto");
    
    if(spanInternalPid != null) {
      internalPid = spanInternalPid.text().trim();
    }
    
    return internalPid;
  }

  private String crawlInternalId(Element e, Document doc) {
    String internalId = null;
    String optionInternalId = e.val().trim();
    
    if(!optionInternalId.isEmpty()) {
      internalId = crawlInternalPid(doc) + "-" + optionInternalId;
    }
    
    return internalId;
  }


  private String crawlPrimaryImage(Element e, Document doc) {
   String url = null;
   String val = e.val().trim();
   
   JSONObject script = CrawlerUtils.selectJsonFromHtml(doc, "script", "dataLayer[0]['product'] = ", ";", false, true);
   
   if(script.has("id")) {
     if(!val.isEmpty()) {
       url = "https://www.lefarma.com.br/ImagensProduto/CodVariante/"+ val +"/produto_id/"+script.get("id")+"/exibicao/produto/t/10";
       Document docImg = DataFetcher.fetchDocument("POST_REQUEST", session, url, null, cookies);
     }
   }
   
    return null;
  }

  private boolean crawlAvailability(Element e) {
    return e.selectFirst("#condicaobotao_comprar") != null;
  }

  private Prices crawlPrices(Float price, Element e) {
    return null;
  }

  private String crawlName(Element e, Document doc) {
    String name = null;
    Element spanName = doc.selectFirst("#lblNome");
    String variableName = e.text().trim();
    
    if(spanName != null) {
      name = spanName.text().trim();
      
      if(variableName.contains("Indisponível")) {
       name += " " + variableName.substring(0, variableName.indexOf("Indisponível")-1);
      } else {        
        name += " " +  variableName;
      }
      
    }
    
    return name;
  }


  private boolean isProductPage(Element e) {
    return e.selectFirst(".produto_comprar") != null;
  }
}
