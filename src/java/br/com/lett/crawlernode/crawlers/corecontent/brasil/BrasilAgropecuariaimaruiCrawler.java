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

public class BrasilAgropecuariaimaruiCrawler extends Crawler {
  
  private static final String HOME_PAGE = "https://agropecuariaimarui.com.br/";
  
  public BrasilAgropecuariaimaruiCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();
    
    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
      
      JSONObject skuJson = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"application/ld+json\"]", null, null, false, true);
      JSONArray graphJsonArray = skuJson != null &&
          skuJson.has("@graph") &&
          !skuJson.isNull("@graph")
              ? skuJson.getJSONArray("@graph")
              : new JSONArray();
        
      if(graphJsonArray.length() > 1 && graphJsonArray.get(1) instanceof JSONObject) {
        skuJson = graphJsonArray.getJSONObject(1);
      } else {
        skuJson = null;
      }

      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input#comment_post_ID", "value");
      String internalPid = skuJson != null && skuJson.has("sku") ? skuJson.get("sku").toString() : null;
      String name = skuJson != null && skuJson.has("name") ? skuJson.get("name").toString() : null;
      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".price-wrapper .product-page-price .woocommerce-Price-amount", null, true, ',', session);
      Prices prices = scrapPrices(doc, price);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".product-info .woocommerce-breadcrumb a[href]", true);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".woocommerce-product-gallery figure img", Arrays.asList("src"), "https:", HOME_PAGE);
      String secondaryImages = null;
      String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".product-short-description p", ".product-footer .tab-panels #tab-description"));
      Integer stock = CrawlerUtils.scrapIntegerFromHtmlAttr(doc, ".product-info form.cart input[name=\"gtm4wp_stocklevel\"]", "value", 0);
      boolean available = stock > 0;
      Marketplace marketplace = null;

      Elements variations = doc.select(".variations .value .tawcvs-swatches span");
      Element variationIdsElement = doc.selectFirst(".single_variation_wrap #wc-shipping-simulator");
      String[] variationIds = variationIdsElement != null && variationIdsElement.hasAttr("data-product-ids") 
          ? variationIdsElement.attr("data-product-ids").split(",") 
          : null;
      
      if(!variations.isEmpty() && variationIds != null && variations.size() == variationIds.length) {
        for(int i = 0; i < variations.size(); i++) {
          Element e = variations.get(i);
          
          // Creating the product
          Product product = ProductBuilder.create()
              .setUrl(session.getOriginalURL())
              .setInternalId(internalId + "-" + variationIds[i])
              .setInternalPid(internalPid)
              .setName(name + " - " + e.ownText())
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
              .build();
    
          products.add(product);
          
        }
      } else {
        
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
            .build();
  
        products.add(product);
      }
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }
    
    return products;
  }
  
  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".product-container .product-main") != null;
  }
  
  private Prices scrapPrices(Document doc, Float price) {
    Prices prices = new Prices();
    
    if (price != null) {
      prices.setBankTicketPrice(CrawlerUtils.scrapDoublePriceFromHtml(doc, ".wc-simulador-parcelas-offer .woocommerce-Price-amount", null, true, ',', session));
      
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-wrapper .price del .amount", null, true, ',', session);
      if(priceFrom != null) {
        prices.setPriceFrom(priceFrom);
      }
      
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      
      for(Element e : doc.select(".product-info .wc-simulador-parcelas-payment-options li")) {
        Pair<Integer, Float> installment = CrawlerUtils.crawlSimpleInstallment(null, e, false, "x de", "juros", true);
        
        if (!installment.isAnyValueNull()) {
          installmentPriceMap.put(installment.getFirst(), installment.getSecond());
        }
      }
      
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DISCOVER.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AURA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.UNKNOWN_CARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
    }
    
    return prices;
  }
}
