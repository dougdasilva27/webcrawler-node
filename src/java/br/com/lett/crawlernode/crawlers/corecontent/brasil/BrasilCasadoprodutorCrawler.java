package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
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
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.prices.Prices;

/**
 * 
 * @author Paula
 *
 */

public class BrasilCasadoprodutorCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.casadoprodutor.com.br/";

  public BrasilCasadoprodutorCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    JSONObject dataLayer = CrawlerUtils.selectJsonFromHtml(doc, "script", "dataLayer[0]['product'] =", ";", false, true);

    if (dataLayer.has("id")) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalIds = getMainProductId(dataLayer);// mudar o metodo usado 
      String internalPid = crawlInternalPid(doc);
      //System.err.println(internalPid);
      //System.err.println(internalId);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".respiro_conteudo .migalha a");
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList( ".descricao_texto .descricao_texto_conteudo"));
      String mainProductId = getMainProductId(dataLayer);

      // sku data in json
      JSONArray arraySkus = dataLayer != null && dataLayer.has("variants") ? dataLayer.getJSONArray("variants") : new JSONArray();
/*
      boolean a = internalPid instanceof String;
      float v = (float) 6;
      
      Object o;
      
      o instanceof JSONObject;
      json = (JSONObject) o;
  */    
      for (int i = 0; i < arraySkus.length(); i++) { 
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String name = crawlName(jsonSku);
        String internalId = getMainProductId(jsonSku);

        Document productAPI = captureImageAndPricesInfo(internalIds, internalPid, mainProductId, doc);
        
        Float price = JSONUtils.getFloatValueFromJSON(jsonSku, "payInFullPrice", true);
        //Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".preco_por_titulo span", null, false, ',', session);
        boolean available = crawlAvailability(price, jsonSku);
        Prices prices = available ? crawlPrices(price, jsonSku) : new Prices();
        String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(productAPI, ".produto_foto #divImagemPrincipalZoom > a", Arrays.asList("href"),
            "https:", "www.casadoprodutor.com.br");
        String secondaryImages = crawlSecondaryImages(doc);

        String ean = crawlEan(jsonSku);
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
        		.setMarketplace(new Marketplace())
        		.setEans(eans)
        		.build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  /*******************
   * General methods *
   *******************/

  private String getMainProductId(JSONObject json) {
    String mainProductId = null;

    if (json.has("id")) {
      mainProductId = json.get("id").toString();
    }

    return mainProductId;
  }

  private String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has("variants")) {
      internalId = json.get("id").toString();
    }

    return internalId;
  }

  private String crawlInternalPid(Document doc) {
    String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name .produto_codigo span", false);
    
   
    
    if(internalPid.contains("-")) {
    	int finalInternalPid = internalPid.indexOf("-");
    	internalPid = internalPid.substring(0, finalInternalPid);
    }
    return internalPid;
  }

  private String crawlName(JSONObject skuJson) {
    StringBuilder name = new StringBuilder();

    if (skuJson.has("name")) {
      name.append(skuJson.getString("name"));
    }

    return name.toString();
  }


  private boolean crawlAvailability(Float price, JSONObject jsonSku) {
    return jsonSku.has("available") && jsonSku.get("available") instanceof Boolean && jsonSku.getBoolean("available") && price != null;
  }

  private String crawlSecondaryImages(Document doc) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements images = doc.select("#thumblist li:not(:first-child) > a");
    for (Element e : images) {
      JSONObject rel = CrawlerUtils.stringToJson(e.attr("rel"));

      if (rel.has("largeimage")) {
        secondaryImagesArray.put(rel.get("largeimage"));
      } else if (rel.has("smallimage")) {
        secondaryImagesArray.put(rel.get("smallimage"));
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  /**
   * 
   * Access prices and images api's
   * 
   * @param internalId
   * @param internalPid
   * @param mainProductId
   * @param mainPage
   * @return
   */
  private Document captureImageAndPricesInfo(String internalId, String internalPid, String mainProductId, Document mainPage) {
    Document doc = mainPage;

    if (mainProductId != null && !mainProductId.equals(internalId)) {
      doc = new Document("");
      String imagesUrl =
          "https://www.cassol.com.br/ImagensProduto/CodVariante/" + internalId + "/produto_id/" + internalPid + "/exibicao/produto/t/32";
      String pricesUrl = "https://www.cassol.com.br/ParcelamentoVariante/CodVariante/" + internalId + "/produto_id/" + internalPid + "/t/32";

      Request requestImages = RequestBuilder.create().setUrl(imagesUrl).setCookies(cookies).build();
      doc.append(Jsoup.parse(this.dataFetcher.get(session, requestImages).getBody()).toString());

      Request requestPrices = RequestBuilder.create().setUrl(pricesUrl).setCookies(cookies).build();
      doc.append(Jsoup.parse(this.dataFetcher.get(session, requestPrices).getBody()).toString());
    }

    return doc;
  }

  /**
   * 
   * @param internalId
   * @param price
   * @return
   */
  
  private Prices crawlPrices(Float price, JSONObject json) {
	    Prices prices = new Prices();

	    if (price != null) {
	      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
	      Integer quantityInstallment = JSONUtils.getIntegerValueFromJSON(json, "quantityOfInstallmentsNoInterest", null);
	      Float installment = JSONUtils.getFloatValueFromJSON(json, "valueOfInstallmentsNoInterest", true);
	      installmentPriceMap.put(1, price);
	      installmentPriceMap.put(quantityInstallment, installment);
	      prices.setBankTicketPrice(price);

	      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
	      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
	      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
	      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
	      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
	    }

	    return prices;
	  }

  private String crawlEan(JSONObject json) {
    String ean = null;

    if (json.has("ean")) {
      Object obj = json.get("ean");

      if (obj != null) {
        ean = obj.toString();
      }
    }

    return ean;
  }
}
