package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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

public class BrasilPethereCrawler extends Crawler {
  
  private static final String CDN_URL = "cdn.awsli.com.br";
  
  public BrasilPethereCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();
    
    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".codigo-produto [itemprop=\"sku\"]", true);
      String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, ".codigo-produto span[itemprop=\"sku\"]", true);
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".nome-produto", false) + " - " + 
          CrawlerUtils.scrapStringSimpleInfo(doc, "[itemprop=\"brand\"] > a", true);
      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".principal .preco-promocional", null, true, ',', session);
      Prices prices = scrapPrices(doc, price);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs > ul > li", true);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#carouselImagem > ul > li img", 
          Arrays.asList("data-largeimg", "src", "data-mediumimg"), "https", CDN_URL);
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "#carouselImagem > ul > li img", 
          Arrays.asList("data-largeimg", "src", "data-mediumimg"), "https", CDN_URL, primaryImage);
      String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList("#descricao"));
      Integer stock = null;
      boolean available = doc.selectFirst(".comprar > .botao-comprar") != null;
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
      
      Elements variations = doc.select(".atributo-comum > ul > li > a");
      if(variations != null && !variations.isEmpty()) {
        for(Element variation : variations) {
          String variationId = variation.hasAttr("data-variacao-id") ? variation.attr("data-variacao-id") : null;
          if(variationId == null) {
             continue;
          }
          
          Product clone = product.clone();
          
          String variationName = CrawlerUtils.scrapStringSimpleInfo(variation, null, false);
          if(variationName != null) {
            clone.setName(product.getName() + " - " + variationName);
          }
          
          Float priceFrom = null;
          
          Elements subElements = doc.select(".acoes-produto[data-variacao-id=\"" + variationId + "\"]");
          for(Element subElement : subElements) {
            if(subElement.hasAttr("data-produto-id")) {
              clone.setInternalId(subElement.attr("data-produto-id"));
            }
            
            Float clonePrice = CrawlerUtils.scrapFloatPriceFromHtml(subElement, "[itemprop=\"price\"]", "content", false, '.', session);
            if(clonePrice != null) {
              clone.setPrice(clonePrice);
            }
            
            String availabilityString = CrawlerUtils.scrapStringSimpleInfoByAttribute(subElement, "[itemprop=\"availability\"]", "content");
            if(availabilityString != null) {
              clone.setAvailable(availabilityString.toLowerCase().contains("instock"));
            }
            
            priceFrom = CrawlerUtils.scrapFloatPriceFromHtml(subElement, ".preco-venda", null, true, ',', session);
          }
          
          clone.setPrices(scrapVariationPrices(doc, clone.getPrice(), clone.getInternalId(), priceFrom));
          
          products.add(clone);
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
    return doc.selectFirst(".pagina-produto") != null;
  }
  
  private Prices scrapPrices(Document doc, Float price) {
    Prices prices = new Prices();
    
    if(price != null) {
      
      prices.setBankTicketPrice(price);
      
      Float priceFrom = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".principal .preco-venda", null, true, ',', session);
      prices.setPriceFrom(priceFrom != null ? MathUtils.normalizeTwoDecimalPlaces(priceFrom.doubleValue()) : null);
      
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      
      Elements elements = doc.select("[id*=mercadopago] ul > li");
      for(Element e : elements) {
        Pair<Integer, Float> installment = CrawlerUtils.crawlSimpleInstallment(null, e, false, "x", "juros", true);
        
        if (!installment.isAnyValueNull()) {
          installmentPriceMap.put(installment.getFirst(), installment.getSecond());
        }
      }
      
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
    }
    
    return prices;
  }
  
  private Prices scrapVariationPrices(Document doc, Float price, String productId, Float priceFrom) {
    Prices prices = new Prices();
    Element installmentElement = doc.selectFirst(".parcelas-produto[data-produto-id=\"" + productId + "\"]");
    
    if(price != null) {
      prices.setBankTicketPrice(price);
      
      if(priceFrom != null) {
        prices.setPriceFrom(MathUtils.normalizeTwoDecimalPlaces(priceFrom.doubleValue()));
      }
      
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      
      Elements elements = installmentElement.select("[id*=mercadopago] ul > li");
      for(Element e : elements) {
        Pair<Integer, Float> installment = CrawlerUtils.crawlSimpleInstallment(null, e, false, "x", "juros", true);
        
        if (!installment.isAnyValueNull()) {
          installmentPriceMap.put(installment.getFirst(), installment.getSecond());
        }
      }
      
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
    }
    
    return prices;
  }
}
