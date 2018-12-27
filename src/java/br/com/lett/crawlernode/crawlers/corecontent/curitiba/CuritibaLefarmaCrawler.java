package br.com.lett.crawlernode.crawlers.corecontent.curitiba;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
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
    JSONObject fetchedJson = null;
    List<Product> products = new ArrayList<>();
    
    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
      
      JSONObject script = CrawlerUtils.selectJsonFromHtml(doc, "script", "dataLayer[0]['product'] = ", ";", false, true);
      if(script.has("id")) {       
        String url = "https://www.lefarma.com.br/public_api/v1/products/" + script.get("id");
        fetchedJson = DataFetcher.fetchJSONObject(DataFetcher.GET_REQUEST, session, url, null, cookies);
      }
      
      
      
      List<String> selectors = new ArrayList<>();
      selectors.add(".descricao_texto");
      
      String internalPid = crawlInternalPid(fetchedJson);
      Float price = CrawlerUtils.scrapSimplePriceFloat(doc, "span[itemprop=price]", false);
      String description = crawlDescription(fetchedJson);
      CategoryCollection categories = crawlCategories(fetchedJson);
      
      for (Object obj : fetchedJson.getJSONArray("variants")) {
        
        JSONObject jsonObject =  (JSONObject) obj;

        String name = crawlName(jsonObject);
        String internalId = crawlInternalId(jsonObject);
        String primaryImage = crawlPrimaryImage(jsonObject);
        boolean available = crawlAvailability(jsonObject);
        Prices prices = crawlPrices(jsonObject);

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

  private String crawlDescription(JSONObject obj) {
    String descprition = null;
    
    if(obj.has("htmlDescriptions")) {
      for (Object objDescriptions : obj.getJSONArray("htmlDescriptions")) {
        JSONObject jsonObject =  (JSONObject) objDescriptions;
        descprition = jsonObject.getString("description");  
      }   
    }
    
    return descprition;
  }

  private CategoryCollection crawlCategories(JSONObject obj) {
    CategoryCollection categories = new CategoryCollection();
    if(obj.has("breadcrumbs")) {
      for (Object category : obj.getJSONArray("breadcrumbs")) {
        JSONObject jsonObjCategory = (JSONObject) category;
        categories.add(jsonObjCategory.getString("name"));
      }
    }
    return categories;
  }

  private String crawlName(JSONObject obj) {
    String name = null;
    if(obj.has("definition1Value")) {
      name = obj.getString("definition1Value");
    }
    return name;
  }
  
  private String crawlInternalId(JSONObject obj) {
    String internalId = null;
    
    if(obj.has("id")) {
      internalId = obj.get("id").toString();
    }
    
    return internalId;
  }

  private String crawlInternalPid(JSONObject obj) {
    String internalPid = null;
    
    if(obj.has("erpId")) {
      internalPid = obj.getString("erpId");
    }
    
    return internalPid;
  }


  private String crawlPrimaryImage(JSONObject obj) {
    String primaryImage = null;
    
    if(obj.has("mainImage")) {
      primaryImage = obj.getString("mainImage");
    }
    
    return primaryImage;
  }

  private boolean crawlAvailability(JSONObject obj) {
    return obj.has("available") ? obj.getBoolean("available") : null;
  }

  private Prices crawlPrices(JSONObject obj) {
    Prices prices = new Prices();
    Map<Integer, Float> installmentPriceMap = new TreeMap<>();
    
    if(!obj.isNull("discountValue")) {
      if(obj.has("promotionPrice")) {        
        prices.setBankTicketPrice(obj.getDouble("promotionPrice"));  
        prices.setPriceFrom(obj.getDouble("price"));
      }
    } else {
      prices.setBankTicketPrice(obj.getDouble("price"));
    }
    
    if(obj.has("hasInstallmentsWithInterest") && obj.getBoolean("hasInstallmentsWithInterest")) {
      
      if(obj.has("quantityOfInstallmentsWithInterest") && !obj.isNull("quantityOfInstallmentsWithInterest")) {
        installmentPriceMap.put(obj.getInt("quantityOfInstallmentsWithInterest"),(float) obj.getDouble("valueOfInstallmentsWithInterest"));
      }
      
    } else {
      
      if(obj.has("quantityOfInstallmentsNoInterest") && !obj.isNull("quantityOfInstallmentsNoInterest")) {
        installmentPriceMap.put(obj.getInt("quantityOfInstallmentsNoInterest"),(float) obj.getDouble("valueOfInstallmentsNoInterest"));
      }
      
    }
    prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);
   
    return prices;
  }




  private boolean isProductPage(Element e) {
    return e.selectFirst(".produto_comprar") != null;
  }
}
