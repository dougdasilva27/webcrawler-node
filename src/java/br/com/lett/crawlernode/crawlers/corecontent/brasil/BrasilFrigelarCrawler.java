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
import br.com.lett.crawlernode.core.fetcher.DataFetcherNO;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.Seller;
import models.Util;
import models.prices.Prices;

public class BrasilFrigelarCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.frigelar.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "frigelar";

  public BrasilFrigelarCrawler(Session session) {
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

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

      String internalPid = crawlInternalPid(skuJson);
      CategoryCollection categories = crawlCategories(doc);
      String description = crawlDescription(doc, internalPid);

      // sku data in json
      JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

      // ean data in json
      JSONArray arrayEans = CrawlerUtils.scrapEanFromVTEX(doc);

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = crawlInternalId(jsonSku);
        String name = crawlName(jsonSku, skuJson);
        Map<String, Float> marketplaceMap = crawlMarketplace(jsonSku);
        Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap, internalId, jsonSku, doc);
        boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER);
        Float price = crawlMainPagePrice(marketplaceMap);

        JSONObject jsonProduct = crawlApi(internalId);
        String primaryImage = crawlPrimaryImage(jsonProduct);
        String secondaryImages = crawlSecondaryImages(jsonProduct);
        Prices prices = crawlPrices(internalId, price, jsonSku, doc);
        Integer stock = crawlStock(jsonProduct);
        String ean = i < arrayEans.length() ? arrayEans.getString(i) : null;

        List<String> eans = new ArrayList<>();
        eans.add(ean);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
            .setStock(stock).setMarketplace(marketplace).setEans(eans).build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(Document document) {
    return document.select("#___rc-p-dv-id").first() != null;
  }

  /*******************
   * General methods *
   *******************/

  private String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has("sku")) {
      internalId = Integer.toString(json.getInt("sku")).trim();
    }

    return internalId;
  }

  private String crawlInternalPid(JSONObject skuJson) {
    String internalPid = null;

    if (skuJson.has("productId")) {
      internalPid = skuJson.get("productId").toString();
    }

    return internalPid;
  }

  private String crawlName(JSONObject jsonSku, JSONObject skuJson) {
    String name = null;

    String nameVariation = jsonSku.getString("skuname");

    if (skuJson.has("name")) {
      name = skuJson.getString("name");

      if (name.length() > nameVariation.length()) {
        name += " " + nameVariation;
      } else {
        name = nameVariation;
      }
    }

    return name;
  }

  /**
   * Price "de"
   * 
   * @param jsonSku
   * @return
   */
  private Double crawlPriceFrom(JSONObject jsonSku) {
    Double priceFrom = null;

    if (jsonSku.has("listPriceFormated")) {
      Float price = MathUtils.parseFloatWithComma(jsonSku.get("listPriceFormated").toString());
      priceFrom = MathUtils.normalizeTwoDecimalPlaces(price.doubleValue());
    }

    return priceFrom;
  }

  private Float crawlMainPagePrice(Map<String, Float> marketplace) {
    Float price = null;

    if (marketplace.containsKey(MAIN_SELLER_NAME_LOWER)) {
      price = marketplace.get(MAIN_SELLER_NAME_LOWER);
    }

    return price;
  }

  private String crawlPrimaryImage(JSONObject json) {
    String primaryImage = null;

    if (json.has("Images")) {
      JSONArray jsonArrayImages = json.getJSONArray("Images");

      for (int i = 0; i < jsonArrayImages.length(); i++) {
        JSONArray arrayImage = jsonArrayImages.getJSONArray(i);
        JSONObject jsonImage = arrayImage.getJSONObject(0);

        if (jsonImage.has("IsMain") && jsonImage.getBoolean("IsMain") && jsonImage.has("Path")) {
          primaryImage = changeImageSizeOnURL(jsonImage.getString("Path"));
          break;
        }
      }
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(JSONObject apiInfo) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    if (apiInfo.has("Images")) {
      JSONArray jsonArrayImages = apiInfo.getJSONArray("Images");

      for (int i = 0; i < jsonArrayImages.length(); i++) {
        JSONArray arrayImage = jsonArrayImages.getJSONArray(i);
        JSONObject jsonImage = arrayImage.getJSONObject(0);

        // jump primary image
        if (jsonImage.has("IsMain") && jsonImage.getBoolean("IsMain")) {
          continue;
        }

        if (jsonImage.has("Path")) {
          String urlImage = changeImageSizeOnURL(jsonImage.getString("Path"));
          secondaryImagesArray.put(urlImage);
        }

      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  /**
   * Get the image url and change it size
   * 
   * @param url
   * @return
   */
  private String changeImageSizeOnURL(String url) {
    String[] tokens = url.trim().split("/");
    String dimensionImage = tokens[tokens.length - 2]; // to get dimension image and the image id

    String[] tokens2 = dimensionImage.split("-"); // to get the image-id
    String dimensionImageFinal = tokens2[0] + "-1000-1000";

    return url.replace(dimensionImage, dimensionImageFinal); // The image size is changed
  }

  private Map<String, Float> crawlMarketplace(JSONObject json) {
    Map<String, Float> marketplace = new HashMap<>();

    if (json.has("seller")) {
      String nameSeller = json.getString("seller").toLowerCase().trim();

      if (json.has("bestPriceFormated") && json.has("available") && json.getBoolean("available")) {
        Float price = MathUtils.parseFloatWithComma(json.getString("bestPriceFormated"));
        marketplace.put(nameSeller, price);
      }
    }

    return marketplace;
  }

  private Marketplace assembleMarketplaceFromMap(Map<String, Float> marketplaceMap, String internalId, JSONObject jsonSku, Document doc) {
    Marketplace marketplace = new Marketplace();

    for (String seller : marketplaceMap.keySet()) {
      if (!seller.equalsIgnoreCase(MAIN_SELLER_NAME_LOWER)) {
        Float price = marketplaceMap.get(seller);

        JSONObject sellerJSON = new JSONObject();
        sellerJSON.put("name", seller);
        sellerJSON.put("price", price);
        sellerJSON.put("prices", crawlPrices(internalId, price, jsonSku, doc).toJSON());

        try {
          Seller s = new Seller(sellerJSON);
          marketplace.add(s);
        } catch (Exception e) {
          Logging.printLogError(logger, session, Util.getStackTraceString(e));
        }
      }
    }

    return marketplace;
  }

  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select(".bread-crumb > ul li a");

    for (int i = 1; i < elementCategories.size(); i++) { // first item is the home page
      categories.add(elementCategories.get(i).text().trim());
    }

    return categories;
  }

  private String crawlDescription(Document doc, String internalPid) {
    StringBuilder description = new StringBuilder();

    Element descElement = doc.select("#product-description").first();

    if (descElement != null) {
      description.append(descElement.html());
    }

    Element specElement = doc.select("#caracteristicas").first();

    if (specElement != null) {
      description.append(specElement.html());
    }

    String url = session.getOriginalURL();

    // To capture the installation recommendations, I analyzed a script on this link:
    // https://www.frigelar.com.br/files/inst.js
    // that does exactly what the code below does
    if (!url.contains("janela") && !url.contains("cortina") && !url.contains("portatil") && !url.contains("suporte")
        && !url.contains("kit-instalacao") && !url.contains("kit-wi-fi") && !url.contains("umidificador") && !url.contains("bebedouro")
        && !url.contains("purificador") && !url.contains("refrigerador") && !url.contains("cervejeira") && !url.contains("conservador")
        && !url.contains("freezer") && !url.contains("Ventilador") && !url.contains("ventilador") && !url.contains("adega")
        && !url.contains("frigobar")) {
      description.append(
          "<h4 class=\"sub-titulo\">Recomendações de Instalação</h4><b>Escolha do equipamento:</b><br><br>Você precisa considerar a quantidade de pessoas que vivem no local,"
              + " as dimensões do cômodo onde você vai realizar a instalação do ar condicionado, a incidência de luz solar no "
              + "local além da presença ou não de equipamentos eletrônicos no ambiente. Em caso de dúvidas, utilize nossa "
              + "calculadora ou entre em contato com um de nossos vendedores.<br><br><b>Instalação:</b><br><br> É de extrema "
              + "importância que o produto seja instalado por técnicos credenciados ou autorizados com o fabricante, assegurando "
              + "desta forma os prazos de garantia do produto e as corretas práticas de instalação estabelecidas pelo fabricante "
              + "para um perfeito funcionamento do aparelho.<br><br> <b>Como é a instalação do split?</b><br><br> Lembrando que uma "
              + "unidade do aparelho fica dentro da casa e outra fora, considere que o aparelho é ligado por tubulações frigorígenas "
              + "e fios elétricos conectando as duas unidades.<br><br><b>Para a infraestrutura que liga as duas partes do aparelho são "
              + "usados:</b><br>-Tubos em cobre ou alumínio (também chamados de tubulação frigorígena), com bitolas e espessuras adequadas "
              + "conforme especificação do fabricante.<br>-Fiação elétrica, com cabos dimensionados conforme especificação do fabricante e "
              + "um disjuntor exclusivo para cada aparelho de ar condicionado;<br>-Tubos de PVC para drenagem da água na unidade interna.");
    } else if (!url.contains("janela")) {
      description.append(
          "<h4 class=\"sub-titulo\">Recomendações de Instalação</h4><b>Escolha do equipamento:</b><br><br>Você precisa considerar a quantidade de pessoas que vivem no local, "
              + "as dimensões do cômodo onde você vai realizar a instalação do ar-condicionado, a incidência de luz solar no local além "
              + "da presença ou não de equipamentos eletrônicos no ambiente. Em caso de dúvidas, utilize nossa calculadora ou entre em "
              + "contato com um de nossos vendedores.<br><br><b>Instalação:</b><br>É de extrema importância que o produto seja instalado "
              + "por técnicos credenciados ou autorizados com o fabricante, assegurando desta forma os prazos de garantia do produto e as "
              + "corretas práticas de instalação estabelecidas pelo fabricante para um perfeito funcionamento do aparelho.<br><br><b>Como é "
              + "a instalação do Ar-condicionado de Janela?</b><br><br>Primeiramente, você precisa escolher um local apropriado para a instalação "
              + "do seu Ar-Condicionado de Janela. Uma tomada de voltagem compatível com o aparelho e um disjuntor exclusivo, devem ficar próximos"
              + " ao local de instalação. Tenha cuidado para que a parede não possua fios e/ou canos por perto.<br><br>O aparelho deverá ficar "
              + "a uma altura mínima entre 1,50m e 1,80m do chão e a uma distância mínima de 50cm da parede lateral. Para garantir máxima "
              + "eficiência do aparelho, instale preferencialmente na parte central do ambiente, se for possível.<br><br>1 - Verifique as "
              + "medidas do aparelho e o seu local de instalação.<br>2 - Faça o caixilho (suporte de madeira para o ar-condicionado), caso "
              + "não tenha a estrutura pronta.<br>3 - Corte o revestimento da parede criando a abertura para o seu aparelho.<br>4 - Instale "
              + "o caixilho de madeira dando acabamento para a abertura na parede.<br>5 - Encaixe o aparelho na abertura certificando-se de "
              + "que o mesmo está devidamente preso.<br>6 - Realize o acabamento da instalação sem deixar frisos para escapamento do ar."
              + "<br>Tenha cuidado para que as aberturas laterais e superiores do lado externo do aparelho de ar-condicionado estejam sem obstruções.");
    } else if (!url.contains("cortina")) {
      description.append(
          "<h4 class=\"sub-titulo\">Recomendações de Instalação</h4><br>É de extrema importância que o produto seja instalado por técnicos credenciados ou autorizados com o fabricante, "
              + "assegurando desta forma os prazos de garantia do produto e as corretas práticas de instalação estabelecidas pelo fabricante para um "
              + "perfeito funcionamento do aparelho.<br><br><b>Como é a instalação de uma Cortina de Ar?</b><br><br>Estes aparelhos se adaptam a qualquer "
              + "tamanho de vão, com alcance de 1,30m e 1,75m de largura por até 3m de altura.<br><br>1 - Para a instalação da cortina de ar, escolha um local "
              + "que ofereça uma fixação segura e com tomada de voltagem compatível.<br>2 - Posicione a cortina de ar entre 1,50 e 3,0m de altura, sempre na horizontal "
              + "e com a saída de ar para baixo.<br>3 - Em casos que a largura do vão for maior que o tamanho máximo de módulo disponível, sugerimos a instalação "
              + "de dois ou mais módulos até o fechamento completo do vão, observando a distância de mínima de 20mm entre as cortinas de ar.<br>4"
              + " - Remova o suporte da cortina de ar soltando os parafusos de fixação localizados na parte traseira.<br>5 - Com os suportes removidos "
              + "da cortina de ar, fixe-os firmemente, observando a distância entre os mesmos para posterior encaixe do equipamento.<br>6 - "
              + "Encaixe a cortina de ar na parte superior do suporte e recoloque os parafusos de fixação.");
    } else if (!url.contains("portatil")) {
      description.append(
          "<h4 class=\"sub-titulo\">Recomendações de Instalação</h4><br>A grande vantagem do Ar Condicionado Portátil em relação aos outros modelos, é a sua mobilidade, podendo climatizar "
              + "diferentes ambientes em um único aparelho, pois não requer instalação, sendo necessário apenas ser posicionado próximo a uma "
              + "janela para eliminar o ar quente do ambiente.<br><br>Uma ótima opção para locais onde não seja possível realizar a instalação de um aparelho split.");
    }

    return description.toString();
  }

  /**
   * To crawl this prices is accessed a api Is removed all accents for crawl price 1x like this: Visa
   * à vista R$ 1.790,00
   * 
   * @param internalId
   * @param price
   * @return
   */
  private Prices crawlPrices(String internalId, Float price, JSONObject jsonSku, Document docHome) {
    Prices prices = new Prices();

    if (price != null) {
      String url = "https://www.frigelar.com.br/productotherpaymentsystems/" + internalId;
      Document doc = DataFetcherNO.fetchDocument(DataFetcherNO.GET_REQUEST, session, url, null, cookies);

      prices.setPriceFrom(crawlPriceFrom(jsonSku));

      Integer discountBoleto = 0;
      Integer cardDiscount = 0;

      Element flagBoleto = docHome.select("#desconto-boleto").first();
      Element flagCard = docHome.select("#desconto-avista").first();

      if (flagBoleto != null) {
        String text = flagBoleto.text().replaceAll("[^0-9]", "").trim();

        if (!text.isEmpty()) {
          discountBoleto = Integer.parseInt(text);
        }
      }

      if (flagCard != null) {
        String text = flagCard.text().replaceAll("[^0-9]", "").trim();

        if (!text.isEmpty()) {
          cardDiscount = Integer.parseInt(text);
        }
      }

      if (discountBoleto > 0) {
        prices.setBankTicketPrice(MathUtils.normalizeTwoDecimalPlaces(price - (price * (discountBoleto / 100.0))));
      } else {
        prices.setBankTicketPrice(price);
      }

      Elements cardsElements = doc.select("#ddlCartao option");

      if (!cardsElements.isEmpty()) {
        for (Element e : cardsElements) {
          String text = e.text().toLowerCase();

          if (text.contains("visa")) {
            Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), cardDiscount);
            prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);

          } else if (text.contains("mastercard")) {
            Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), cardDiscount);
            prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);

          } else if (text.contains("diners")) {
            Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), cardDiscount);
            prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);

          } else if (text.contains("american") || text.contains("amex")) {
            Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), cardDiscount);
            prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);

          } else if (text.contains("hipercard") || text.contains("amex")) {
            Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), cardDiscount);
            prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);

          } else if (text.contains("credicard")) {
            Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), cardDiscount);
            prices.insertCardInstallment(Card.CREDICARD.toString(), installmentPriceMap);

          } else if (text.contains("elo")) {
            Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), cardDiscount);
            prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);

          }
        }
      } else {
        Map<Integer, Float> installmentPriceMap = new HashMap<>();

        if (cardDiscount > 0) {
          installmentPriceMap.put(1, MathUtils.normalizeTwoDecimalPlaces(price - (price * (cardDiscount / 100f))));
        } else {
          installmentPriceMap.put(1, price);
        }

        prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      }
    }

    return prices;
  }

  private Map<Integer, Float> getInstallmentsForCard(Document doc, String idCard, Integer discount) {
    Map<Integer, Float> mapInstallments = new HashMap<>();

    Elements installmentsCard = doc.select(".tbl-payment-system#tbl" + idCard + " tr");
    for (Element i : installmentsCard) {
      Element installmentElement = i.select("td.parcelas").first();

      if (installmentElement != null) {
        String textInstallment = installmentElement.text().toLowerCase();
        Integer installment;

        if (textInstallment.contains("vista")) {
          installment = 1;
        } else {
          installment = Integer.parseInt(textInstallment.replaceAll("[^0-9]", "").trim());
        }

        Element valueElement = i.select("td:not(.parcelas)").first();

        if (valueElement != null) {
          Float value = Float.parseFloat(valueElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());

          if (discount != null && installment == 1) {
            value = value - (value * (discount / 100f));
          }
          mapInstallments.put(installment, value);
        }
      }
    }

    return mapInstallments;
  }

  private JSONObject crawlApi(String internalId) {
    String url = "https://www.frigelar.com.br/produto/sku/" + internalId;

    JSONArray jsonArray = DataFetcherNO.fetchJSONArray(DataFetcherNO.GET_REQUEST, session, url, null, cookies);

    if (jsonArray.length() > 0) {
      return jsonArray.getJSONObject(0);
    }

    return new JSONObject();
  }


  private Integer crawlStock(JSONObject jsonProduct) {
    Integer stock = null;

    if (jsonProduct.has("SkuSellersInformation")) {
      JSONObject sku = jsonProduct.getJSONArray("SkuSellersInformation").getJSONObject(0);

      if (sku.has("AvailableQuantity")) {
        stock = sku.getInt("AvailableQuantity");
      }
    }

    return stock;
  }
}
