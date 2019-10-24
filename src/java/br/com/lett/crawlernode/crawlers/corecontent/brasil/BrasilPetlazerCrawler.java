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
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

public class BrasilPetlazerCrawler extends Crawler {
  
  private static final String HOME_PAGE = "petlazer.com.br";
  
  public BrasilPetlazerCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();
    
    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
      
      JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, ".no-display script", "Product.Config(", ");", false, true);

      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-view [name=\"product\"]", "value");
      String internalPid = internalId;
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name h2", true);
      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, "[id*=product-price]", null, true, ',', session);
      Prices prices = scrapPrices(doc, price);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs > ul > li", true);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product-image > a", Arrays.asList("href"), "https", HOME_PAGE);
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "#galeria a", Arrays.asList("href"), "https", HOME_PAGE, primaryImage);
      String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".box-descricao"));
      Integer stock = null;
      boolean available = doc.selectFirst(".availability") != null && doc.selectFirst(".availability").hasAttr("in-stock");
      Marketplace marketplace = null;
          
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
      
      if(json != null && !json.keySet().isEmpty()) {
        String idAttr = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".outros-lista-atributos [data-idatributo]", "data-idatributo");
        if(idAttr != null) {
          Float basePrice = JSONUtils.getFloatValueFromJSON(json, "basePrice", true);
          
          json = json.has("attributes") && json.get("attributes") instanceof JSONObject ? json.getJSONObject("attributes") : new JSONObject();
          json = json.has(idAttr) && json.get(idAttr) instanceof JSONObject ? json.getJSONObject(idAttr) : new JSONObject();
          
          String code = "";
          if(json.has("code") && json.get("code") instanceof String) {
            code = json.getString("code");
          }
          
          JSONArray variations = json.has("options") && json.get("options") instanceof JSONArray ? json.getJSONArray("options") : new JSONArray();
          for(Object o : variations) {
            if(o instanceof JSONObject) {
              JSONObject variationJson = (JSONObject) o;
              Product clone = product.clone();
              
              if(variationJson.has("label") && variationJson.get("label") instanceof String) {
                clone.setName(product.getName() + " - " + variationJson.getString("label") + " " + code);
              }
              
              if(basePrice != null && variationJson.has("price")) {
                Float priceSum = JSONUtils.getFloatValueFromJSON(variationJson, "price", true);
                priceSum = priceSum != null ? priceSum : 0.0f;
                
                clone.setPrice(basePrice + priceSum);
                clone.setPrices(scrapPrices(doc, clone.getPrice()));
              }
              
              if(variationJson.has("products") && variationJson.get("products") instanceof JSONArray) {
                for(Object obj : variationJson.getJSONArray("products")) {
                  if(obj instanceof String) {
                    Product cloneClone = clone.clone();
                    cloneClone.setInternalId((String) obj);

                    products.add(cloneClone);
                  }
                }
              }
            }
          }
        }
      } else {
        products.add(product);
      } 
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }
    
    return products;
  }
  
  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".catalog-product-view") != null;
  }
  
  private Prices scrapPrices(Document doc, Float price) {
    Prices prices = new Prices();
    
    Element parcelaBloco = doc.selectFirst(".parcelaBloco");
    
    if(price != null) {
      Float discount = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".price-box .priceAvista", "data-desconto", false, '.', session);
      
      if(discount != null) {
        prices.setBankTicketPrice(MathUtils.normalizeTwoDecimalPlaces(price * (1.0 - discount)));
      }
      
      prices.setPriceFrom(CrawlerUtils.scrapDoublePriceFromHtml(doc, ".old-price .price", null, false, ',', session));
      
      Integer maxParcelas = CrawlerUtils.scrapIntegerFromHtmlAttr(parcelaBloco, null, "data-maximo_parcelas", 0);
      Integer maxParcelasSemJuros = CrawlerUtils.scrapIntegerFromHtmlAttr(parcelaBloco, null, "data-maximo_parcelas_sem_juros", 0);
      Float juros = CrawlerUtils.scrapFloatPriceFromHtml(parcelaBloco, null, "data-juros", false, '.', session);
      Float valorMinimo = CrawlerUtils.scrapFloatPriceFromHtml(parcelaBloco, null, "data-valor_minimo", false, '.', session);
      
      // Evitando null pointers
      juros = juros == null ? 0.0f : juros;
      
      if(valorMinimo != null) {
        Integer parcelas = (int)(price / valorMinimo);
        
        Map<Integer, Float> installmentPriceMap = new TreeMap<>();
        for(int i = 1; i <= parcelas && i <= maxParcelas; i++) {
          Float installment = price/i;
          
          if(i > maxParcelasSemJuros) {
            installment += installment * juros;
          }
          
          installmentPriceMap.put(i, MathUtils.normalizeTwoDecimalPlaces(installment));
        }
        
        prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.HIPER.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      }
    }
    
    return prices;
  }
}
