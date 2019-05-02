package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SaopauloB2WCrawlersUtils {

  /**
   * B2W has two types of crawler
   * 
   * Old Way: Shoptime New Way: Americanas and Submarino
   * 
   * this happen because americanas and submarino have changed their sites, but shoptime no
   */

  public static final String AMERICANAS = "americanas";
  public static final String SUBMARINO = "submarino";
  public static final String SHOPTIME = "shoptime";

  /**
   * New way
   */

  /**
   * 
   * @param doc
   * @return
   */
  public static JSONObject getDataLayer(Document doc) {
    JSONObject skus = new JSONObject();
    Elements scripts = doc.select("script");

    for (Element e : scripts) {
      String json = e.outerHtml();

      if ((json.contains("__PRELOADED_STATE__") || json.contains("__INITIAL_STATE__")) && json.contains("};")) {

        int x = json.indexOf("_ =") + 3;
        int y = json.indexOf("};", x);

        json = json.substring(x, y + 1);

        skus = new JSONObject(json);

        break;
      }
    }

    return skus;
  }

  /**
   * Nesse novo site da americanas todas as principais informações dos skus estão em um json no html,
   * esse json é muito grande, por isso pego somente o que preciso e coloco em outro json para
   * facilitar a captura de informações
   * 
   * { internalPid = '51612', skus:[ { internal_id: '546', variationName: '110v' } ], images:{
   * primaryImage: '123.jpg'. secondaryImages: [ '1.jpg', '2.jpg' ] }, categories:[ { id: '123', name:
   * 'cafeteira' } ], prices:{ 546:{ stock: 1706 bankTicket: 59.86 installments: [ { quantity: 1,
   * value: 54.20 } ] } }
   * 
   * }
   */

  public static JSONObject assembleJsonProductWithNewWay(JSONObject initialJson) {
    JSONObject jsonProduct = new JSONObject();

    if (initialJson.has("product")) {
      JSONObject productJson = initialJson.getJSONObject("product");

      JSONArray skus = getJSONSkus(initialJson);
      jsonProduct.put("skus", skus);

      String internalPid = null;

      if (productJson.has("id")) {
        internalPid = productJson.getString("id");
        jsonProduct.put("internalPid", internalPid);
      }

      if (productJson.has("name")) {
        jsonProduct.put("name", productJson.get("name"));
      }

      JSONObject jsonForPrices;
      if (productJson.has("offers")) {
        jsonForPrices = productJson;
      } else {
        jsonForPrices = initialJson;
      }

      JSONObject jsonPrices = getJsonPrices(jsonForPrices, internalPid);
      jsonProduct.put("prices", jsonPrices);

      JSONObject jsonImages = getJSONImages(productJson);
      jsonProduct.put("images", jsonImages);

      JSONArray jsonCategories = getJSONCategories(productJson);
      jsonProduct.put("categories", jsonCategories);
    }

    return jsonProduct;

  }

  /**
   * Para pegar todos os preços acessamos uma api que retorna um json com todos preços de todos os
   * marketplaces, depois pegamos somente as parcelas e o preço no boleto do shoptime. Em seguida
   * coloco somente o id do produto com seu preço no boleto e as parcelas no cartão, também coloco as
   * parcelas do produto com maior quantidade de parcelas, pois foi verificado que produtos com
   * variações, a segunda variação está vindo com apenas uma parcela no json da api. Vale lembrar que
   * pegamos as parcelas do Cartão VISA. ex:
   * 
   * Endereço api:
   * http://product-v3.shoptime.com.br/product?q=itemId:(125628846)&responseGroups=medium&limit=5&offer.condition=ALL&paymentOptionIds=CARTAO_VISA,BOLETO
   * 
   * Parse Json: http://json.parser.online.fr/
   * 
   * { 125628854":{ "installments":[ { "interestRate":0, "total":1699, "quantity":1,
   * "interestAmount":0, "value":1699, "annualCET":0 }, {...}, {...} ], "bankTicket":1699, "stock":72
   * }, "moreQuantityOfInstallments":[ { "interestRate":0, "total":1699, "quantity":1,
   * "interestAmount":0, "value":1699, "annualCET":0 } }
   */
  public static JSONObject assembleJsonProductOldWay(JSONObject initialJson) {
    JSONObject jsonProduct = new JSONObject();

    if (initialJson.has("products") && initialJson.getJSONArray("products").length() > 0) {
      JSONObject productJson = initialJson.getJSONArray("products").getJSONObject(0);

      JSONArray skus = getJSONSkus(productJson);
      jsonProduct.put("skus", skus);

      String internalPid = null;

      if (productJson.has("id")) {
        internalPid = productJson.getString("id");
        jsonProduct.put("internalPid", internalPid);
      }

      if (productJson.has("name")) {
        jsonProduct.put("name", productJson.get("name"));
      }

      JSONObject jsonPrices = getJsonPrices(productJson, internalPid);
      jsonProduct.put("prices", jsonPrices);

      JSONObject jsonImages = getJSONImages(productJson);
      jsonProduct.put("images", jsonImages);

      JSONArray jsonCategories = getJSONCategories(productJson);
      jsonProduct.put("categories", jsonCategories);
    }

    return jsonProduct;

  }

  private static JSONArray getJSONSkus(JSONObject initialJson) {
    JSONArray skus = new JSONArray();

    if (initialJson.has("skus")) {
      JSONArray skusJson = initialJson.getJSONArray("skus");

      for (int i = 0; i < skusJson.length(); i++) {
        JSONObject skuJson = skusJson.getJSONObject(i);
        JSONObject sku = new JSONObject();

        if (skuJson.has("id")) {
          sku.put("internalId", skuJson.getString("id"));

          if (skuJson.has("name")) {
            StringBuilder name = new StringBuilder();
            sku.put("name", skuJson.getString("name"));

            if (skuJson.has("diffs")) {
              JSONArray diffs = skuJson.getJSONArray("diffs");

              for (int j = 0; j < diffs.length(); j++) {
                JSONObject variation = diffs.getJSONObject(j);

                if (variation.has("value")) {
                  name.append(" " + variation.getString("value").trim());
                }
              }

              sku.put("variationName", name.toString().trim());
            }
          }
        }
        skus.put(sku);
      }
    }

    return skus;
  }

  private static JSONArray getJSONCategories(JSONObject productJson) {
    JSONArray jsonCategories = new JSONArray();

    if (productJson.has("category")) {
      JSONObject category = productJson.getJSONObject("category");

      if (category.has("breadcrumb")) {
        jsonCategories = category.getJSONArray("breadcrumb");
      }
    }

    return jsonCategories;
  }

  private static JSONObject getJSONImages(JSONObject productJson) {
    JSONObject jsonImages = new JSONObject();

    if (productJson.has("images")) {
      JSONArray imagesArray = productJson.getJSONArray("images");
      JSONArray secondaryImages = new JSONArray();

      for (int i = 0; i < imagesArray.length(); i++) {
        JSONObject images = imagesArray.getJSONObject(i);
        String image = null;

        if (images.has("extraLarge")) {
          image = images.getString("extraLarge");
        } else if (images.has("large")) {
          image = images.getString("large");
        } else if (images.has("big")) {
          image = images.getString("big");
        } else if (images.has("medium")) {
          image = images.getString("medium");
        }

        if (i == 0) {
          jsonImages.put("primaryImage", image);
        } else {
          secondaryImages.put(image);
        }
      }

      jsonImages.put("secondaryImages", secondaryImages);
    }

    return jsonImages;
  }

  private static JSONObject getJsonPrices(JSONObject initialJson, String internalPid) {
    JSONObject jsonPrices = new JSONObject();

    JSONArray offersJsonArray = new JSONArray();

    if (initialJson.has("offers") && initialJson.get("offers") instanceof JSONArray) {
      offersJsonArray = initialJson.getJSONArray("offers");
    } else if (initialJson.has("entities")) {
      JSONObject entities = initialJson.getJSONObject("entities");

      if (entities.has("offers")) {
        JSONObject offerObject = entities.getJSONObject("offers");

        if (offerObject.has(internalPid)) {
          offersJsonArray = offerObject.getJSONArray(internalPid);
        }
      }
    }

    if (offersJsonArray.length() > 0) {
      JSONArray moreQuantityOfInstallments = new JSONArray();

      for (int i = 0; i < offersJsonArray.length(); i++) {
        JSONObject jsonOffer = offersJsonArray.getJSONObject(i);
        JSONObject jsonSeller = new JSONObject();

        String idProduct = crawlIdProduct(jsonOffer);

        if (jsonOffer.has("listPrice")) {
          jsonSeller.put("priceFrom", jsonOffer.get("listPrice"));
        }

        if (idProduct != null) {
          manageEmbedded(jsonOffer, jsonSeller, moreQuantityOfInstallments);

          if (jsonOffer.has("availability")) {
            JSONObject availability = jsonOffer.getJSONObject("availability");

            if (availability.has("_embedded")) {
              JSONObject embedded = availability.getJSONObject("_embedded");

              if (embedded.has("stock")) {
                JSONObject stock = embedded.getJSONObject("stock");

                if (stock.has("quantity")) {
                  jsonSeller.put("stock", stock.getInt("quantity"));
                }
              }
            }
          }

          if (jsonOffer.has("paymentOptions")) {
            JSONObject payment = jsonOffer.getJSONObject("paymentOptions");

            setBoleto(payment, jsonSeller);
            setCard(payment, jsonSeller, moreQuantityOfInstallments);
          }

          if (jsonPrices.has(idProduct)) {
            JSONArray installments = jsonPrices.getJSONArray(idProduct);
            installments.put(jsonSeller);

            jsonPrices.put(idProduct, installments);
          } else {
            JSONArray installments = new JSONArray();
            installments.put(jsonSeller);

            jsonPrices.put(idProduct, installments);
          }
        }
      }

      jsonPrices.put("moreQuantityOfInstallments", moreQuantityOfInstallments);
    }

    return jsonPrices;
  }

  private static String crawlIdProduct(JSONObject jsonOffer) {
    String idProduct = null;

    if (jsonOffer.has("_links")) {
      JSONObject links = jsonOffer.getJSONObject("_links");

      if (links.has("sku")) {
        JSONObject sku = links.getJSONObject("sku");

        if (sku.has("id")) {
          idProduct = sku.getString("id");
        }
      }
    }

    return idProduct;
  }

  private static void manageEmbedded(JSONObject jsonOffer, JSONObject jsonSeller, JSONArray moreQuantityOfInstallments) {
    if (jsonOffer.has("_embedded")) {
      JSONObject embedded = jsonOffer.getJSONObject("_embedded");

      if (embedded.has("seller")) {
        JSONObject seller = embedded.getJSONObject("seller");

        setStock(seller, jsonSeller);

        if (seller.has("name")) {
          jsonSeller.put("sellerName", seller.get("name").toString().toLowerCase());
        }
      }
    }
  }

  private static void setBoleto(JSONObject payment, JSONObject jsonSeller) {
    if (payment.has("BOLETO")) {
      JSONObject boleto = payment.getJSONObject("BOLETO");

      if (boleto.has("price")) {
        jsonSeller.put("bankTicket", boleto.getDouble("price"));
      }
    }
  }

  private static void setCard(JSONObject payment, JSONObject jsonSeller, JSONArray moreQuantityOfInstallments) {
    if (payment.has("CARTAO_VISA")) {
      JSONObject visa = payment.getJSONObject("CARTAO_VISA");

      if (visa.has("installments")) {
        JSONArray installments = visa.getJSONArray("installments");
        jsonSeller.put("installments", installments);

        if (installments.length() > moreQuantityOfInstallments.length()) {
          moreQuantityOfInstallments = installments;
        }
      }

      if (visa.has("price")) {
        jsonSeller.put("defaultPrice", visa.get("price"));
      }
    }

    if (payment.has("CARTAO_SUBA_MASTERCARD")) {
      JSONObject shopCard = payment.getJSONObject("CARTAO_SUBA_MASTERCARD");

      if (shopCard.has("installments")) {
        JSONArray installments = shopCard.getJSONArray("installments");
        jsonSeller.put("installmentsShopCard", installments);

        if (installments.length() > moreQuantityOfInstallments.length()) {
          moreQuantityOfInstallments = installments;
        }
      }
    }

    if (payment.has("CARTAO_ACOM_MASTERCARD")) {
      JSONObject shopCard = payment.getJSONObject("CARTAO_ACOM_MASTERCARD");

      if (shopCard.has("installments")) {
        JSONArray installments = shopCard.getJSONArray("installments");
        jsonSeller.put("installmentsShopCard", installments);

        if (installments.length() > moreQuantityOfInstallments.length()) {
          moreQuantityOfInstallments = installments;
        }
      }
    }

    if (payment.has("CARTAO_SHOP_MASTERCARD")) {
      JSONObject shopCard = payment.getJSONObject("CARTAO_SHOP_MASTERCARD");

      if (shopCard.has("installments")) {
        JSONArray installments = shopCard.getJSONArray("installments");
        jsonSeller.put("installmentsShopCard", installments);

        if (installments.length() > moreQuantityOfInstallments.length()) {
          moreQuantityOfInstallments = installments;
        }
      }
    }
  }

  private static void setStock(JSONObject seller, JSONObject jsonSeller) {
    if (seller.has("availability")) {
      JSONObject availability = seller.getJSONObject("availability");

      if (availability.has("_embedded")) {
        JSONObject embeddedStock = availability.getJSONObject("_embedded");

        if (embeddedStock.has("stock")) {
          JSONObject jsonStock = embeddedStock.getJSONObject("stock");

          if (jsonStock.has("quantity")) {
            jsonSeller.put("stock", jsonStock.getInt("quantity"));
          }
        }
      }
    }
  }

  public static String crawlInternalPidShoptime(Document doc) {
    String internalID = null;

    Element elementInternalID = doc.select(".p-name#main-product-name .p-code").first();
    if (elementInternalID != null) {
      internalID = elementInternalID.text().split(" ")[1].replace(")", " ").trim();
    }

    return internalID;
  }
}
