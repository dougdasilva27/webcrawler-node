package br.com.lett.crawlernode.crawlers.corecontent.florianopolis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
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
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.prices.Prices;

public class FlorianopolisAngeloniCrawler extends Crawler {
  
  public FlorianopolisAngeloniCrawler(Session session) {
    super(session);
  }
  
  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    
    boolean shouldVisit = false;
    
    shouldVisit = !FILTERS.matcher(href).matches() && (href.startsWith("http://www.angeloni.com.br/super/"));
    
    return shouldVisit;
  }
  
  
  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();
    
    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, "Product page identified: " + this.session.getOriginalURL());
      
      String internalId = crawlInternalId(doc);
      String internalPid = internalId;
      String newUrl = internalId != null ? CrawlerUtils.crawlFinalUrl(session.getOriginalURL(), session) : session.getOriginalURL();
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcumb > a");
      String name = crawlName(doc);
      Float price = crawlPrice(doc);
      boolean available = price != null;
      String primaryImage = crawlPrimaryImage(doc);
      String secondaryImages = crawlSecondaryImages(doc);
      Integer stock = null;
      Marketplace marketplace = new Marketplace();
      String description = crawlDescription(doc);
      Prices prices = crawlPrices(doc, price);
      
      Product product = ProductBuilder.create().setUrl(newUrl).setInternalId(internalId).setInternalPid(internalPid).setName(name).setPrice(price)
          .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setStock(stock).setMarketplace(marketplace).build();
      
      products.add(product);
      
    } else {
      Logging.printLogDebug(logger, session, "Not a product page.");
    }
    
    return products;
  }
  
  /*******************************
   * Product page identification *
   *******************************/
  
  private boolean isProductPage(Document doc) {
    return !doc.select(".container__body-detalhe-produto").isEmpty();
  }
  
  private String crawlInternalId(Document doc) {
    Element elementInternalId = doc.select("[itemprop=sku]").first();
    if (elementInternalId != null) {
      return elementInternalId.attr("content").trim();
    }
    return null;
  }
  
  private String crawlName(Document doc) {
    Element elementName = doc.select(".box-desc-prod__titulo-produto").first();
    if (elementName != null) {
      return elementName.text().trim();
    }
    return null;
  }
  
  private Float crawlPrice(Document doc) {
    Float price = null;
    
    Element elementPrice = doc.selectFirst(".content__desc-prod__box-valores");
    if (elementPrice != null) {
      price = Float.parseFloat(elementPrice.attr("content"));
    }
    
    return price;
  }
  
  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();
    
    Elements descs = doc.select(".prod-info .div__box-info-produto");
    for (Element e : descs) {
      description.append(e.html());
    }
    
    return description.toString();
  }
  
  
  private String crawlPrimaryImage(Document document) {
    String primaryImage = null;
    Element primaryImageElement = document.selectFirst(".box-galeria img");
    
    if (primaryImageElement != null) {
      primaryImage = primaryImageElement.attr("data-zoom-image").trim();
      
      if (!primaryImage.startsWith("http")) {
        primaryImage = "https:" + primaryImage;
      }
    }
    
    return primaryImage;
  }
  
  private String crawlSecondaryImages(Document document) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();
    
    Elements imagesElement = document.select(".box-galeria img" + CrawlerUtils.CSS_SELECTOR_IGNORE_FIRST_CHILD);// first index is the primary image
    
    for (Element e : imagesElement) {
      String image = e.attr("data-zoom-image").trim();
      
      if (!image.startsWith("http")) {
        image = "https:" + image;
      }
      secondaryImagesArray.put(image);
    }
    
    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }
    
    return secondaryImages;
  }
  
  
  /**
   * Each card has your owns installments Showcase price is price sight
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Document doc, Float price) {
    Prices prices = new Prices();
    
    if (price != null) {
      Element priceFrom = doc.select(".d-block box-produto__texto-tachado").first();
      if (priceFrom != null) {
        prices.setPriceFrom(MathUtils.parseDouble(priceFrom.ownText()));
      }
      
      Map<Integer, Float> installmentsPriceMap = new HashMap<>();
      installmentsPriceMap.put(1, price);
      
      Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(".box-produto__parcelamento", doc, false);
      if (!pair.isAnyValueNull()) {
        installmentsPriceMap.put(pair.getFirst(), pair.getSecond());
      }
      
      prices.insertCardInstallment(Card.AMEX.toString(), installmentsPriceMap);
      prices.insertCardInstallment(Card.VISA.toString(), installmentsPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentsPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentsPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentsPriceMap);
    }
    
    return prices;
  }
}
