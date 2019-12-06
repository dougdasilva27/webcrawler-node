package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
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
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.prices.Prices;

public class BrasilNutrineCrawler extends Crawler {
  private static final String HOME_PAGE = "https://www.nutrine.com.br/";

  public BrasilNutrineCrawler(Session session) {
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

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"text/javascript\"]", "produto = ", ",\n    urano = ", false, false);

      String name = JSONUtils.getStringValue(json, "nome");
      String description = getDescription(json);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".detalheHeader a.nivel1:not(:first-child)");
      String primaryImage = scrapPrimaryImage(doc, "#product .halfLeft .thumbs .cloudzoom-gallery");
      String secondaryImages = scrapSecondaryImage(doc, "#product .halfLeft .thumbs .cloudzoom-gallery", primaryImage);

      if (json.has("variacoes")) {
        JSONArray variations = json.getJSONArray("variacoes");

        if (variations.length() > 0) {
          for (int i = 0; i < variations.length(); i++) {
            JSONObject variation = variations.getJSONObject(i);

            String varName = name + getVariationName(variation);
            String internalId = variation.has("idVariacao") && !variation.isNull("idVariacao") ? variation.get("idVariacao").toString() : null;
            String internalPid = JSONUtils.getStringValue(json, "sku");
            Integer stock = JSONUtils.getIntegerValueFromJSON(variation, "quantidadeEstoque", 0);
            boolean available = variation.has("disponivel") && variation.get("disponivel") instanceof Boolean ? variation.getBoolean("disponivel") : false;
            Float price = JSONUtils.getFloatValueFromJSON(json, "precoAtual", true);
            Prices prices = scrapPrices(price, variation, doc);

            // Creating the product
            Product product = ProductBuilder.create()
                .setUrl(session.getOriginalURL())
                .setInternalId(internalId)
                .setInternalPid(internalPid)
                .setName(varName)
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
                .build();

            products.add(product);
          }
        } else {

          String internalId = json.has("id") && !json.isNull("id") ? json.get("id").toString() : null;
          String internalPid = JSONUtils.getStringValue(json, "sku");
          Integer stock = JSONUtils.getIntegerValueFromJSON(json, "quantidadeEstoque", 0);
          Float price = JSONUtils.getFloatValueFromJSON(json, "precoAtual", true);
          Prices prices = scrapPrices(price, json, doc);
          boolean available = json.has("disponivel") && json.get("disponivel") instanceof Boolean ? json.getBoolean("disponivel") : false;

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
              .build();

          products.add(product);
        }
      }
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".produtoDetalhes") != null;
  }

  private String getVariationName(JSONObject json) {
    String name = "";
    JSONArray opcoes = JSONUtils.getJSONArrayValue(json, "opcoes");
    
    for(Object obj : opcoes) {
      if(obj instanceof JSONObject) {
        JSONObject subObj = (JSONObject) obj;
  
        name += (subObj.has("nome") && !subObj.isNull("nome") ? subObj.get("nome") + " " : "");
      }
    }

    return name;
  }

  private String getDescription(JSONObject json) {
    StringBuilder sb = new StringBuilder();
    JSONArray arr = json.has("descricoes") ? json.getJSONArray("descricoes") : new JSONArray();
    
    for(Object obj : arr) {
      if(obj instanceof JSONObject) {
        JSONObject subObj = (JSONObject) obj;
  
        if (subObj.has("titulo") && subObj.get("titulo") instanceof String) {
          sb.append(subObj.getString("titulo"));
        }
  
        if (subObj.has("conteudo") && subObj.get("conteudo") instanceof String) {
          sb.append(subObj.getString("conteudo"));
        }
      }
    }

    return sb.toString();
  }

  private String scrapPrimaryImage(Document doc, String selector) {
    Element e = doc.selectFirst(selector);
    String imageUrl = null;

    if (e != null) {
      JSONObject json = CrawlerUtils.stringToJson(e.attr("data-cloudzoom"));

      if (json.has("zoomImage") && json.get("zoomImage") instanceof String) {
        imageUrl = CrawlerUtils.completeUrl(json.getString("zoomImage"), "https", "cdn.nutrine.com.br");
      }
    }


    return imageUrl;
  }

  private String scrapSecondaryImage(Document doc, String selector, String primaryImage) {
    Elements elmnts = doc.select(selector);
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    for (Element e : elmnts) {
      if (e != null) {
        JSONObject json = CrawlerUtils.stringToJson(e.attr("data-cloudzoom"));

        if (json.has("zoomImage") && json.get("zoomImage") instanceof String) {
          String img = CrawlerUtils.completeUrl(json.getString("zoomImage"), "https", "cdn.nutrine.com.br");

          if ((primaryImage == null || !primaryImage.equals(img)) && img != null) {
            secondaryImagesArray.put(img);
          }
        }
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private Prices scrapPrices(Float price, JSONObject json, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);


      JSONObject descontoPrice = CrawlerUtils.selectJsonFromHtml(doc, "script", "porcentagemDescontoVista=", "},", true, false);
      if (descontoPrice.has("valorComDesconto")) {
        prices.setBankTicketPrice(
            MathUtils.normalizeTwoDecimalPlaces(CrawlerUtils.getDoubleValueFromJSON(descontoPrice, "valorComDesconto", true, false)));
      } else if (json.has("precoAtual") && json.get("precoAtual") instanceof Double) {
        prices.setBankTicketPrice(json.getDouble("precoAtual"));
      }

      if (json.has("parcelaSemJuros") && json.get("parcelaSemJuros") instanceof JSONObject) {
        JSONObject o = json.getJSONObject("parcelaSemJuros");

        if (o.has("valor") && o.get("valor") instanceof Integer && o.has("quantidade") && (o.get("quantidade") instanceof Float || o.get("quantidade") instanceof Double)) {
          installmentPriceMap.put(o.getInt("quantidade"), o.getFloat("valor"));
        }
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
    }

    return prices;
  }
}
