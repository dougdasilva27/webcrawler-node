package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

public class BrasilEfacilCrawler extends Crawler {

  public BrasilEfacilCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && href.startsWith("https://www.efacil.com.br/");
  }


  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (session.getOriginalURL().startsWith("https://www.efacil.com.br/loja/produto/")
        || session.getOriginalURL().startsWith("http://www.efacil.com.br/loja/produto/")) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      Element variationSelector = doc.select(".options_attributes").first();

      // Nome
      Element elementName = doc.select("h1.product-name").first();
      String name = null;
      if (elementName != null) {
        name = elementName.text().trim();
      }

      // Categorias
      Elements elementsCategories = doc.select("#widget_breadcrumb ul li a");
      String category1 = "";
      String category2 = "";
      String category3 = "";
      for (int i = 1; i < elementsCategories.size(); i++) {
        if (category1.isEmpty()) {
          category1 = elementsCategories.get(i).text().trim();
        } else if (category2.isEmpty()) {
          category2 = elementsCategories.get(i).text().trim();
        } else if (category3.isEmpty()) {
          category3 = elementsCategories.get(i).text().trim();
        }
      }

      // Imagem primária
      Element elementPrimaryImage = doc.select("div.product-photo a").first();
      String primaryImage = null;
      if (elementPrimaryImage != null) {
        primaryImage = "http:" + elementPrimaryImage.attr("href").trim();
      }

      // Imagem secundária
      Elements elementImages = doc.select("div.thumbnails a");
      JSONArray secondaryImagesArray = new JSONArray();
      String secondaryImages = null;
      if (elementImages.size() > 1) {
        for (int i = 1; i < elementImages.size(); i++) { // primeira imagem eh primaria
          String image = elementImages.get(i).attr("data-original").trim();

          if (image.isEmpty()) {
            image = elementImages.get(i).attr("href").trim();
          }

          if (!image.isEmpty()) {
            if (!image.startsWith("http")) {
              image = "http:" + image;
            }

            secondaryImagesArray.put(image);
          }
        }
      }
      if (secondaryImagesArray.length() > 0) {
        secondaryImages = secondaryImagesArray.toString();
      }

      // Descrição
      String description = "";
      Element elementSpecs = doc.select("#especificacoes").first();
      Element elementTabContainer = doc.select("#tabContainer").first();
      if (elementTabContainer != null)
        description += elementTabContainer.html();
      if (elementSpecs != null)
        description = description + elementSpecs.html();

      // Filtragem
      boolean mustInsert = true;

      // Estoque
      Integer stock = null;

      // Marketplace
      Marketplace marketplace = new Marketplace();

      if (variationSelector == null) { // sem variações

        // InternalPid
        String internalPid = null;
        Element elementInternalPid = doc.select("input#productId").first();
        if (elementInternalPid != null) {
          internalPid = elementInternalPid.attr("value").trim();
        } else {
          elementInternalPid = doc.select("input[name=productId]").first();
          if (elementInternalPid != null) {
            internalPid = elementInternalPid.attr("value").trim();
          }
        }

        // ID interno
        String internalId = null;
        Element internalIdElement = doc.select("#entitledItem_" + internalPid).first();
        JSONObject info = new JSONObject();
        if (internalIdElement != null) {
          JSONArray infoArray = new JSONArray(internalIdElement.text().trim());
          info = infoArray.getJSONObject(0);

          internalId = info.getString("catentry_id");
        }

        // Disponibilidade
        boolean available = true;
        Element elementAvailable = doc.select("#disponibilidade-estoque").first();
        if (elementAvailable == null) {
          available = false;
        }

        // Prices
        Prices prices = new Prices();

        // Price
        Float price = null;

        if (available) {
          // Price BankTicket 1x
          Float priceBank = null;

          Element priceElement = doc.select(".priceby span[itemprop=price]").first();

          if (priceElement != null) {
            priceBank = MathUtils.parseFloat(priceElement.ownText());
          }


          // Prices Json
          JSONArray jsonPrices = crawlPriceFromApi(internalId, priceBank);

          // Prices
          prices = crawlPrices(info, priceBank, jsonPrices);

          // Price
          price = crawlPrice(jsonPrices);
        }

        if (mustInsert) {

          Product product = new Product();
          product.setUrl(session.getOriginalURL());
          product.setInternalId(internalId);
          product.setInternalPid(internalPid);
          product.setName(name);
          product.setAvailable(available);
          product.setPrice(price);
          product.setPrices(prices);
          product.setCategory1(category1);
          product.setCategory2(category2);
          product.setCategory3(category3);
          product.setPrimaryImage(primaryImage);
          product.setSecondaryImages(secondaryImages);
          product.setDescription(description);
          product.setStock(stock);
          product.setMarketplace(marketplace);

          products.add(product);
        }

      }

      else { // múltiplas variações

        Element tmpIdElement = doc.select("input[id=productId]").first();
        String tmpId = null;
        if (tmpIdElement != null) {
          tmpId = tmpIdElement.attr("value").trim();
        }

        try {

          JSONArray variationsJsonInfo = new JSONArray(doc.select("#entitledItem_" + tmpId).text().trim());

          for (int i = 0; i < variationsJsonInfo.length(); i++) {

            JSONObject variationJsonObject = variationsJsonInfo.getJSONObject(i);

            // ID interno
            String internalId = variationJsonObject.getString("catentry_id").trim();

            // InternalPid
            String internalPid = null;
            Element elementInternalPid = doc.select("input#productId").first();
            if (elementInternalPid != null) {
              internalPid = elementInternalPid.attr("value").trim();
            }

            // Nome
            JSONObject attributes = variationJsonObject.getJSONObject("Attributes");
            String variationName = null;
            if (attributes.has("Voltagem_110V")) {
              if (name.contains("110V")) {
                variationName = name;
              } else if (name.contains("220V")) {
                variationName = name.replace("220V", "110V");
              } else {
                variationName = name + " 110V";
              }
            } else if (attributes.has("Voltagem_220V")) {
              if (name.contains("220V")) {
                variationName = name;
              } else if (name.contains("110V")) {
                variationName = name.replace("110V", "220V");
              } else {
                variationName = name + " 220V";
              }
            } else {
              variationName = name;
            }

            // Disponibilidade
            boolean available = true;
            if (variationJsonObject.getString("hasInventory").equals("false")) {
              available = false;
            }

            // Prices
            Prices prices = new Prices();

            // Preço
            Float price = null;

            if (available) {
              // Price BankTicket 1x
              Float priceBank =
                  Float.parseFloat(variationJsonObject.getString("offerPrice").replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));

              // Prices Json
              JSONArray jsonPrices = crawlPriceFromApi(internalId, priceBank);

              // Prices
              prices = crawlPrices(variationJsonObject, priceBank, jsonPrices);

              // Price
              price = crawlPrice(jsonPrices);

            }

            if (mustInsert) {

              Product product = new Product();
              product.setUrl(session.getOriginalURL());
              product.setInternalId(internalId);
              product.setInternalPid(internalPid);
              product.setName(variationName);
              product.setAvailable(available);
              product.setPrice(price);
              product.setPrices(prices);
              product.setCategory1(category1);
              product.setCategory2(category2);
              product.setCategory3(category3);
              product.setPrimaryImage(primaryImage);
              product.setSecondaryImages(secondaryImages);
              product.setDescription(description);
              product.setStock(stock);
              product.setMarketplace(marketplace);

              products.add(product);
            }

          }
        } catch (Exception e) {
          Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
        }
      } // fim do caso de múltiplas variacoes

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }

  /**
   * Foi definido que o preço principal seria a menor parcela a prazo Logo tento sempre pegar a 2
   * parcela do produto caso esse produto tenha somente uma parcela, o preço principal será o preço da
   * primeira parcela.
   * 
   * @param jsonPrices
   * @return
   */
  private Float crawlPrice(JSONArray jsonPrices) {
    Float price = null;

    for (int i = 0; i < jsonPrices.length(); i++) {
      JSONObject json = jsonPrices.getJSONObject(i);

      if (json.has("installmentOptions")) {
        JSONArray installments = json.getJSONArray("installmentOptions");

        for (int j = 0; j < installments.length(); j++) {
          JSONObject installmentJSON = installments.getJSONObject(j);

          if (installmentJSON.has("option")) {
            String installment = installmentJSON.getString("option");

            // Vai rodando os cartões até achar a segunda parcela
            // se achar para o loop
            if (installment.contains("2")) {
              Float valueInstallment = Float.parseFloat(installmentJSON.getString("amount"));
              Float result = valueInstallment * 2;

              price = MathUtils.normalizeTwoDecimalPlaces(result);

              break;

            } else if (installment.contains("1")) { // se não achar o preço será o da primeira parcela
              Float valueInstallment = Float.parseFloat(installmentJSON.getString("amount"));
              price = MathUtils.normalizeTwoDecimalPlaces(valueInstallment);
            }
          }
        }
      }
    }



    return price;
  }

  private Prices crawlPrices(JSONObject info, Float price, JSONArray jsonPrices) {
    Prices prices = new Prices();

    if (price != null) {

      prices.setBankTicketPrice(price);

      try {
        for (int i = 0; i < jsonPrices.length(); i++) {
          JSONObject json = jsonPrices.getJSONObject(i);
          Map<Integer, Float> installmentPriceMap = new HashMap<>();

          if (json.has("paymentMethodName")) {
            String cardName = json.getString("paymentMethodName").replaceAll(" ", "").toLowerCase().trim();

            if (json.has("installmentOptions")) {
              JSONArray installments = json.getJSONArray("installmentOptions");

              for (int j = 0; j < installments.length(); j++) {
                JSONObject installmentJSON = installments.getJSONObject(j);

                if (installmentJSON.has("option")) {
                  String text = installmentJSON.getString("option").toLowerCase();
                  int x = text.indexOf("x");

                  Integer installment = Integer.parseInt(text.substring(0, x));

                  if (installmentJSON.has("amount")) {
                    Float priceBig = Float.parseFloat(installmentJSON.getString("amount"));
                    Float value = MathUtils.normalizeTwoDecimalPlaces(priceBig);

                    installmentPriceMap.put(installment, value);
                  }
                }
              }

              if (cardName.equals("amex")) {
                prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);

              } else if (cardName.equals("visa")) {
                prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);

              } else if (cardName.equals("mastercard")) {
                prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);

              } else if (cardName.equals("diners")) {
                prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);

              } else if (cardName.equals("americanexpress")) {
                prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);

              } else if (cardName.equals("elo")) {
                prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
              }
            }
          }
        }
      } catch (Exception e) {

      }
    }

    return prices;
  }

  /**
   * Pega o JSON filho que possui informações sobre as parecelas.
   * 
   * @param internalId
   * @param price
   * @return
   */
  private JSONArray crawlPriceFromApi(String internalId, Float price) {
    String url = "http://www.efacil.com.br/webapp/wcs/stores/servlet/GetCatalogEntryInstallmentPrice?storeId=10154&langId=-6&catalogId=10051"
        + "&catalogEntryId=" + internalId + "&nonInstallmentPrice=" + price;

    String json = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, cookies);

    int x = json.indexOf("/*");
    int y = json.indexOf("*/", x + 2);

    json = json.substring(x + 2, y);


    JSONArray jsonPrice;
    try {
      jsonPrice = new JSONArray(json);
    } catch (Exception e) {
      jsonPrice = new JSONArray();
      e.printStackTrace();
    }


    return jsonPrice;
  }
}
