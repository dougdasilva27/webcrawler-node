package br.com.lett.crawlernode.crawlers.corecontent.curitiba;

import java.util.ArrayList;
import java.util.List;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
    
    Elements options = doc.select("#ddlVarDef1 option");
    String internalPid = crawlInternalPid(doc);
    Float price = CrawlerUtils.scrapSimplePriceFloat(doc, "span[itemprop=price]", false);
    Prices prices = crawlPrices(price, doc);
    String description = crawlDescription(doc);
    CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".migalha > a", true);
    boolean available = crawlAvailability(doc);
    
    if (isProductPage(doc)) {
      for (Element e : options) {
      
        Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
        
        String internalId = crawlInternalId(e);
        String name = crawlName(e);
        String primaryImage = crawlPrimaryImage(e);
  
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

  private String crawlInternalId(Element e) {
    return null;
  }

  private String crawlDescription(Element e) {
    String description = null;
    Element divDescription =  e.selectFirst(".descricao_texto");
    
    if(divDescription != null) {
      description = divDescription.text();
    }
    
    return description;
  }


  private String crawlPrimaryImage(Element e) {
    return null;
  }

  private boolean crawlAvailability(Element e) {
    return e.selectFirst("#condicaobotao_comprar") != null;
  }

  private Prices crawlPrices(Float price, Element e) {
    return null;
  }

  private String crawlName(Element e) {
    String name = null;
    Element spanName = e.selectFirst("#lblNome");
    Elements selectName = e.select("#ddlVarDef1 option");
    
    if(spanName != null && selectName != null) {
      name = spanName.text().trim();
      for (Element option : selectName) {
        if(option.hasAttr("selected")) {
          name = name + " " +  option.text().trim();
        }
      }
    }
    
    return name;
  }


  private boolean isProductPage(Element e) {
    return e.selectFirst(".produto_comprar") != null;
  }
}
