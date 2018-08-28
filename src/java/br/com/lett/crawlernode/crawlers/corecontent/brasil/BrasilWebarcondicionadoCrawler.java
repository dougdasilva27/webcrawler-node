package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.Seller;
import models.Util;
import models.prices.Prices;

/**
 * Date: 14/06/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilWebarcondicionadoCrawler extends Crawler {
  
  private static final String HOME_PAGE = "http://www.webarcondicionado.com.br/";
  
  public BrasilWebarcondicionadoCrawler(Session session) {
    super(session);
  }
  
  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }
  
  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();
    
    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
      
      String internalId = crawlInternalId(doc);
      String name = crawlName(doc);
      Float price = null;
      Prices prices = new Prices();
      boolean available = false;
      CategoryCollection categories = crawlCategories(doc);
      String primaryImage = crawlPrimaryImage(doc);
      String description = crawlDescription(doc);
      Integer stock = null;
      Marketplace marketplace = crawlMarketplace(doc);
      
      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setName(name).setPrice(price)
          .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setDescription(description).setStock(stock)
          .setMarketplace(marketplace).build();
      
      products.add(product);
      
    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }
    
    return products;
    
  }
  
  private boolean isProductPage(Document doc) {
    return !doc.select("#product").isEmpty();
  }
  
  private String crawlInternalId(Document doc) {
    String internalId = null;
    
    String token = "project.advertising.product=";
    Elements scripts = doc.select("script[type=\"text/javascript\"]");
    
    for (Element e : scripts) {
      String script = e.html().toLowerCase().replace(" ", "");
      
      if (script.contains(token)) {
        int x = script.indexOf(token) + token.length();
        int y = script.indexOf(';', x);
        internalId = script.substring(x, y).trim();
        break;
      }
      
    }
    
    return internalId;
  }
  
  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select(".h1-product-name").first();
    
    if (nameElement != null) {
      name = nameElement.text().trim();
    }
    
    return name;
  }
  
  private Marketplace crawlMarketplace(Document doc) {
    Marketplace marketplace = new Marketplace();
    Elements sellers = doc.select("#product-store tr");
    
    for (Element e : sellers) {
      Element name = e.select(".name > a").first();
      Element price = e.select(".price").first();
      
      if (name != null && price != null) {
        Prices prices = crawlPrices(price);
        
        JSONObject sellerJSON = new JSONObject();
        sellerJSON.put("name", CommonMethods.getLast(name.attr("data-label").split("/")));
        sellerJSON.put("prices", prices.toJSON());
        
        if (prices.getCardPaymentOptions(Card.VISA.toString()).containsKey(1)) {
          Double priced = prices.getCardPaymentOptions(Card.VISA.toString()).get(1);
          Float priceFloat = MathUtils.normalizeTwoDecimalPlaces(priced.floatValue());
          
          sellerJSON.put("price", priceFloat);
        }
        
        try {
          Seller seller = new Seller(sellerJSON);
          marketplace.add(seller);
        } catch (Exception ex) {
          Logging.printLogError(logger, session, Util.getStackTraceString(ex));
        }
      }
    }
    
    
    return marketplace;
  }
  
  private String crawlPrimaryImage(Document doc) {
    String primaryImage = null;
    
    Element image = doc.select(".product-image img").first();
    if (image != null) {
      primaryImage = image.attr("src");
    }
    
    return primaryImage;
  }
  
  /**
   * @param document
   * @return
   */
  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select("#breadcrumb a:not([data-category])");
    
    for (Element e : elementCategories) {
      String cat = e.ownText().trim();
      
      if (!cat.isEmpty()) {
        categories.add(cat);
      }
    }
    
    return categories;
  }
  
  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();
    
    Element elementDescription = doc.selectFirst("#product-feature");
    
    if (elementDescription != null) {
      description.append(elementDescription.html());
    }
    
    Element elementExtraDescription = doc.selectFirst("#product-content");
    
    if (elementExtraDescription != null) {
      description.append(elementExtraDescription.html());
    }
    
    return description.toString();
  }
  
  /**
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Element price) {
    Prices prices = new Prices();
    
    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      
      
      Element price1x = price.select("span").first();
      if (price1x != null) {
        installmentPriceMap.put(1, MathUtils.parseFloat(price1x.ownText()));
      }
      
      Element installmentsElement = price.select("span").last();
      
      if (installmentsElement != null) {
        String textInstallment = installmentsElement.ownText();
        
        if (textInstallment.contains("de")) {
          int x = textInstallment.indexOf("de") + 2;
          
          String installment = textInstallment.substring(0, x).replaceAll("[^0-9]", "").trim();
          Float value = MathUtils.parseFloat(textInstallment.substring(x));
          
          if (!installment.isEmpty() && value != null) {
            installmentPriceMap.put(Integer.parseInt(installment), value);
          }
        }
      }
      
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
    }
    
    return prices;
  }
  
}
