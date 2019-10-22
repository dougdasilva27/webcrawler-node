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
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.prices.Prices;

public class BrasilMerceariadoanimalCrawler extends Crawler {
  
  // Ava: https://www.merceariadoanimal.com.br/biscrok-multi-500g/
  // Var: https://www.merceariadoanimal.com.br/bolsa-atenas-1/
  // Var ind: https://www.merceariadoanimal.com.br/fraldas-higienicas-super-secao-macho/
  // Ind: https://www.merceariadoanimal.com.br/oral-care-water-additive/
  
  public BrasilMerceariadoanimalCrawler(Session session) {
    super(session);
  }

  private static final String HOME_PAGE = "https://www.merceariadoanimal.com.br/";

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
        
      String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".VariationProductSKU", true);
      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#productDetailsAddToCartForm > input[name=\"product_id\"]", "value");
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".ProductMain [itemprop=\"name\"]", true) + " - " +
          CrawlerUtils.scrapStringSimpleInfo(doc, ".ProductMain .brand > a", true);
      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".PriceRow [itemprop=\"price\"]", "content", false, '.', session);
      Prices prices = scrapPrices(doc, price);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".Breadcrumb > ul > li[itemprop]", true);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".ProductThumbImage > a", Arrays.asList("href"), "https:", HOME_PAGE);
      String secondaryImages = scrapSecondaryImages(doc, primaryImage);
      String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(
          "#product-tabs ul> li:not(#tab-reviews)", "#product-tabs .tab-content > div:not(#reviews)"));
      Integer stock = null;
      boolean available = doc.selectFirst("#hidden_aviseme") == null;
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
      
      Elements variations = doc.select(".ProductOptionList .VariationSelect option:not([value=\"\"])");
      if(variations.size() > 0) {
        for(Element variation : variations) {
          JSONObject json = getVariationJSON(variation.attr("value"), internalPid);
          
          Product clone = product.clone();
          
          if(json.has("combinationid") && !json.isNull("combinationid")) {
            clone.setInternalId(json.get("combinationid").toString());
          }
          
          clone.setName(product.getName() + ", " + variation.text().replace("(Indisponível)", "").trim());
          
          clone.setPrice(CrawlerUtils.getFloatValueFromJSON(json, "price", false, true));
          clone.setPrices(scrapVariationPrices(prices, json));
         
          if(json.has("instock") && json.get("instock") instanceof Boolean) {
            clone.setAvailable(json.getBoolean("instock"));
          }
          
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
    return doc.selectFirst("#product-main") != null;
  }
  
  private String scrapSecondaryImages(Document doc, String primaryImage) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();
    Elements imagesElement = doc.select(".ProductTinyImageList > ul > li > div > div > a");
    
    for(Element imageElement : imagesElement) {
      if(imageElement.hasAttr("rel")) {
        JSONObject json = JSONUtils.stringToJson(imageElement.attr("rel"));
        
        if(json.has("largeimage") && json.get("largeimage") instanceof String) {
          
          String image = json.getString("largeimage");
          
          if(!image.equals(primaryImage)) {
            secondaryImagesArray.put(image);
          }
        }
      }
    }
    
    if(secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }
    
    return secondaryImages;
  }
  
  private Prices scrapPrices(Document doc, Float price) {
    Prices prices = new Prices();
    
    if(price != null) {
      prices.setBankTicketPrice(CrawlerUtils.scrapFloatPriceFromHtml(doc, ".MsgBoleto strong", null, true, ',', session));
      
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      
      Pair<Integer, Float> installment = CrawlerUtils.crawlSimpleInstallment(".MsgParcelamento", doc, true, "x de", "juros", true);
      if (!installment.isAnyValueNull()) {
        installmentPriceMap.put(installment.getFirst(), installment.getSecond());
      }
      
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
    }
    
    return prices;
  }
  
  private JSONObject getVariationJSON(String variation, String internalPid) {
    JSONObject variationJSON = new JSONObject();
    
    Request request = RequestBuilder.create()
        .setUrl("https://www.merceariadoanimal.com.br/ajax.ecm?w=GetVariationOptions&productId=" + internalPid + "&options=" + variation)
        .build();
    
    variationJSON = JSONUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
    
    return variationJSON;
  }
  
  // TODO: todo
  private Prices scrapVariationPrices(Prices prices, JSONObject json) {
    Prices clone = prices.clone();
    
    return clone;
  }
}
