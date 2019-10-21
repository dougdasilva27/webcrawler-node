package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
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
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.prices.Prices;

public class BrasilBreedsCrawler extends Crawler {
  
  private static final String HOME_PAGE = "www.breeds.com.br";
  
  public BrasilBreedsCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();
   
    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
      
      JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, ".product script", "dataLayer.push(", ");", false, true);
      
      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "head [name=\"itemId\"]", "content");
      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#info-product #productInternalCode", "content");
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "#info-product .brandNameProduct", true) + " " +
          CrawlerUtils.scrapStringSimpleInfo(doc, "#info-product .name", true);
      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, "#infoPrices [itemprop=\"price\"]", "content", false, '.', session);
      Prices prices = scrapPrices(doc, price);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "#breadcrumbs > span > a > span", true);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".images .photo #Zoom1", Arrays.asList("href"), "https:", HOME_PAGE);
      String secondaryImages = scrapSecondaryImages(doc, primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#description", "#panCaracteristica"));
      Integer stock = null;
      boolean available = scrapAvailability(doc);
      Marketplace marketplace = null;
      String ean = json.has("RKProductEan13") && !json.isNull("RKProductEan13") ? json.get("RKProductEan13").toString() : null;

      List<String> eans = new ArrayList<>();
      eans.add(ean);
      
      // Creating the product
      Product product = ProductBuilder.create()
          .setUrl(session.getOriginalURL())
          .setInternalId(internalId)
          .setInternalPid(internalPid)
          .setName(name)
          .setPrice(price)
          .setPrices(prices)
          .setAvailable(available)
          .setCategory1(categories.getCategory(0))
          .setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2))
          .setPrimaryImage(primaryImage)
          .setSecondaryImages(secondaryImages)
          .setDescription(description)
          .setStock(stock)
          .setMarketplace(marketplace)
          .setEans(eans)
          .build();
      
      products.add(product);
      
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }
    
    return products;
  }
  
  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".product") != null;
  }
  
  private String scrapSecondaryImages(Document doc, String primaryImage) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();
    
    Elements images = doc.select(".images > ul > li:not([style=\"display:none\"])");    
    for(int i = 1; i <= images.size(); i++) {
      secondaryImagesArray.put(primaryImage.replace("/Ampliada/", "/Ampliada" + i + "/"));
    }
    
    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }
    
    return secondaryImages;
  }
  
  private Prices scrapPrices(Document doc, Float price) {
    Prices prices = new Prices();
    
    if (price != null) {
      prices.setBankTicketPrice(CrawlerUtils.scrapDoublePriceFromHtml(doc, ".preco-avista .value-sight", null, true, ',', session));
      
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
        
      Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment("#lblParcelamento", 
          doc, false, "x de", "juros", false);
      
      if (!pair.isAnyValueNull()) {
        installmentPriceMap.put(pair.getFirst(), pair.getSecond());
      }
      
      pair = CrawlerUtils.crawlSimpleInstallment("#lblParcelamento #lblOutroParc", 
          doc, false, "x de", "juros", false);
      
      if (!pair.isAnyValueNull()) {
        installmentPriceMap.put(pair.getFirst(), pair.getSecond());
      }
      
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
    }
    
    return prices;
  }
  
  private boolean scrapAvailability(Element doc) {
    boolean available = false;
    String cssSelector = "#infoPrices [itemprop=\"availability\"]";

    Element infoElement = doc.selectFirst(cssSelector);
    if (infoElement != null && infoElement.hasAttr("content")) {
      available = infoElement.attr("content").equals("in_stock");
    }

    return available;
  }
}
