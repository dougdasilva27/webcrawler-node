package br.com.lett.crawlernode.crawlers.corecontent.chile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
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
import models.Marketplace;
import models.prices.Prices;

public class ChileSalcobrandCrawler extends Crawler{

  public ChileSalcobrandCrawler(Session session) {
    super(session);
  }
  
  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();
    
    JSONObject json  = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"text/javascript\"]", "window.chaordic_meta = ", ";", false, false);
    JSONObject productJson = new JSONObject();
    JSONObject categoriesJson = new JSONObject();
    
    if(json.has("product")) {
      productJson = (JSONObject) json.get("product");
    }

    if(json.has("page")) {
      categoriesJson = (JSONObject) json.get("page");
    }    
    
    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
      int index = 0;

      
      for (Object obj: productJson.getJSONArray("skus")) {
        
        JSONObject sku =  (JSONObject) obj;
        
        String internalId   = crawlInternalId(sku);
        String internalPid  =  crawlInternalPid(sku);
        String name         = crawlName(doc, index);
        boolean available = crawlAvailability(sku);
        
        Float priceUnique = CrawlerUtils.scrapSimplePriceFloat(doc, ".pricened", false);
        Float price = priceUnique == null ? 
            CrawlerUtils.scrapSimplePriceFloat(doc, "div .fraccionado_columns td[valign=bottom]:not(.container_gray_fracc) .ahora", false) : priceUnique;
        
        CategoryCollection categories = crawlCategories(doc);
        Prices prices = crawlPrices(price, doc);
        
        String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#gallery img", Arrays.asList("src"), "https:", "www.larebajavirtual.com");
        
        String secondaryImages =
            CrawlerUtils.scrapSimpleSecondaryImages(doc, ".ad-thumb-list li a img", Arrays.asList("src"), "https:", "www.larebajavirtual.com", primaryImage);
 
            Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name).setPrice(price)
                .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
                .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(null)
                .setMarketplace(new Marketplace()).build();

            products.add(product);
            index++;
      }


    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;

  }
  


  private String crawlName(Document doc, int index) {
    Element nameElement = doc.selectFirst(".product-content .info");
    Elements selectElement = doc.select("#variant_id option");
    String name = null;
    
    if(nameElement != null && selectElement != null) {
      name = nameElement.text() + " " + selectElement.get(index).text().trim();
    }
    
    return name;
  }

  private Prices crawlPrices(Float price, Document doc) {
    
    return null;
  }

  private CategoryCollection crawlCategories(Document doc) {
    // TODO Auto-generated method stub
    return null;
  }

  private boolean crawlAvailability(JSONObject sku) {
    boolean availability = false;
    
    if(sku.has("status")) {
     availability = sku.getString("status") == "available" ? true : false;  
    }
    
    return availability;
  }

  private String crawlInternalPid(JSONObject sku) {
    String id = null;
    JSONArray skus = new JSONArray();
    
    if(sku.has("id")) {
      id = sku.getString("id");
    }
    
    return id;

  }

  private String crawlInternalId(JSONObject sku) {
    String id = null;
    JSONArray skus = new JSONArray();
    
    if(sku.has("sku")) {
      id = sku.getString("sku");
    }
    
    return id;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".big-product-container") != null;
  }

}
