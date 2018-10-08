package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

public class RiodejaneiroDrogariavenancioCrawler extends Crawler {

  public RiodejaneiroDrogariavenancioCrawler(Session session) {
    super(session);
  }


  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && href.startsWith("http://www.drogariavenancio.com.br/");
  }


  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (session.getOriginalURL().contains("/produto/")) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      Element variationElement = doc.select(".variacao").first();

      // Pre id interno
      String preInternalId = null;
      Element elementPreInternalId = doc.select("#miolo .produtoPrincipal .info .codigo").first();
      if (elementPreInternalId != null) {
        preInternalId = elementPreInternalId.text().split(":")[1].trim();
      }

      // Pid
      String internalPid = null;
      Element elementInternalPid = doc.select("input[name=IdProduto]").first();
      if (elementInternalPid != null) {
        internalPid = elementInternalPid.attr("value").trim();
      }

      // Pre nome
      String preName = null;
      Element elementName = doc.select("#NomeProduto").first();
      if (elementName != null) {
        preName = elementName.text();
      }

      // Preço
      Float price = null;
      Element elementPrice = doc.select(".valores .preco .precoPor span").first();
      if (elementPrice != null) {
        price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
      }

      // Categorias
      String category1 = "";
      String category2 = "";
      String category3 = "";
      ArrayList<String> categories = new ArrayList<>();

      Elements categ = doc.select("#miolo .breadCrumbs a");

      if (categ.size() > 1)
        categories.add(categ.get(1).text());
      Elements subCategoriesElements = doc.select("#miolo .breadCrumbs h3 p");
      for (Element e : subCategoriesElements) {
        categories.add(e.text());
      }
      for (String category : categories) {
        if (category1.isEmpty()) {
          category1 = category;
        } else if (category2.isEmpty()) {
          category2 = category;
        } else if (category3.isEmpty()) {
          category3 = category;
        }
      }

      // Descricao
      String description = "";
      Element elementDescriptionTab1 = doc.select("#miolo .abas div .aba1").first();
      Element elementDescriptionTab2 = doc.select("#miolo .abas div .aba2").first();
      if (elementDescriptionTab1 != null)
        description = description + elementDescriptionTab1.text();
      if (elementDescriptionTab2 != null)
        description = description + elementDescriptionTab2.text();

      // Marketplace
      Marketplace marketplace = new Marketplace();

      // Separando dois caso
      // Caso 1 - produto não possui variação
      // Caso 2 - produto possui múltiplas variações

      if (variationElement == null || (variationElement != null && variationElement.select(".optionsVariacao li").size() == 0)) { // Caso 1

        // Id interno
        Element internalIdElement = doc.select("input[name=IdProduto]").first();
        String internalId = null;
        if (internalIdElement != null) {
          internalId = preInternalId + "-" + internalIdElement.attr("value");
        }

        // Nome
        String name = preName;

        // Disponibilidade
        boolean available = true;
        Element elementButtonSoldOut = doc.select(".esgotado-detalhes[style=display:block;]").first();
        if (elementButtonSoldOut != null) {
          available = false;
        }

        // Imagem primária
        String primaryImage = null;
        Element elementPrimaryImage = doc.select(".produtoPrincipal .imagem .holder .cloud-zoom .foto").first();
        if (elementPrimaryImage != null) {
          String src = elementPrimaryImage.attr("src");
          if (!src.contains("imagemindisponivel")) {
            primaryImage = src;
          }
        }

        // Imagens secundárias
        String secondaryImages = null;
        JSONArray secondaryImagesArray = new JSONArray();
        Elements elementsSecondaryImage = doc.select(".produtoPrincipal .imagem .multifotos ul li a .foto");

        if (elementsSecondaryImage.size() > 0) {
          for (Element secondaryImage : elementsSecondaryImage) {
            String image = secondaryImage.attr("src").replaceAll("/produto/", "/Produto/");
            if (!image.equals(primaryImage) && !image.contains("imagemindisponivel")) {
              secondaryImagesArray.put(image);
            }
          }
        }
        if (secondaryImagesArray.length() > 0) {
          secondaryImages = secondaryImagesArray.toString();
        }

        // Estoque
        Integer stock = null;

        // Prices
        Prices prices = crawlPrices(doc, price);

        Product product = new Product();

        product.setUrl(session.getOriginalURL());
        product.setInternalId(internalId);
        product.setInternalPid(internalPid);
        product.setName(name);
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
        product.setAvailable(available);

        products.add(product);
      }

      else { // Caso 2

        Elements elementsVariations = doc.select(".optionsVariacao li");
        for (Element elementVariation : elementsVariations) {

          // Id interno
          // O id interno é montado usando o código do produto appendado do id interno da variação
          String internalId = null;
          String posInternalId = null;
          posInternalId = elementVariation.attr("data-value").split("#")[3];
          internalId = preInternalId + "-" + posInternalId;

          // Nome
          String name = null;
          Element elementPosName = elementVariation.select("[title]").first();
          if (elementPosName != null) {
            name = preName + " - " + elementPosName.attr("title");
          }

          // Estoque
          Integer stock = null;
          String scriptName = elementVariation.attr("onclick");
          stock = Integer.valueOf(scriptName.split("'")[5]);

          // Preço
          String plus = scriptName.split("'")[7].replace(',', '.');
          Float priceVariation = normalizeTwoDecimalPlaces(price + Float.valueOf(plus));

          // prices
          Prices prices = crawlPricesVariation(doc, priceVariation);

          // Disponibilidade
          boolean available = true;
          if (stock == 0) {
            available = false;
          }

          // Imagens

          // Requisição POST para conseguir dados da imagem
          String requestURL = "https://www.drogariavenancio.com.br/ajax/gradesku_imagem_ajax.asp";
          String requestParameters = assembleUrlParameters(session.getOriginalURL().split("/")[4], posInternalId);

          String response = DataFetcher.fetchString(DataFetcher.POST_REQUEST, session, requestURL, requestParameters, null);

          String imageId = parseImageId(response);
          Element elementPrimaryImage = doc.select(".produtoPrincipal .imagem .holder .cloud-zoom .foto").first();
          String primaryImage = null;
          if (elementPrimaryImage != null && imageId != null) {
            primaryImage = "http://static-webv8.jet.com.br/drogariavenancio/Produto/" + imageId + ".jpg";
          }

          String secondaryImages = null;
          JSONArray secondaryImagesArray = new JSONArray();
          Elements elementsSecondaryImage = doc.select(".produtoPrincipal .imagem .multifotos ul li a .foto");

          if (elementsSecondaryImage.size() > 1) {
            for (Element secondaryImage : elementsSecondaryImage) {
              String tmp = secondaryImage.attr("src").replaceAll("/produto/", "/Produto/");
              if (!tmp.equals(primaryImage) && !tmp.contains("imagemindisponivel")) {
                secondaryImagesArray.put(secondaryImage.attr("src"));
              }
            }
          }
          if (secondaryImagesArray.length() > 0) {
            secondaryImages = secondaryImagesArray.toString();
          }

          Product product = new Product();

          product.setUrl(session.getOriginalURL());
          product.setInternalId(internalId);
          product.setInternalPid(internalPid);
          product.setName(name);
          product.setPrice(priceVariation);
          product.setPrices(prices);
          product.setCategory1(category1);
          product.setCategory2(category2);
          product.setCategory3(category3);
          product.setPrimaryImage(primaryImage);
          product.setSecondaryImages(secondaryImages);
          product.setDescription(description);
          product.setStock(stock);
          product.setMarketplace(marketplace);
          product.setAvailable(available);

          products.add(product);
        }

      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }

  private String assembleUrlParameters(String idProduto, String variacaoCombinacao) {
    String urlParameters = "IdProduto=" + idProduto + "&VariacaoCombinacao=" + variacaoCombinacao + "&paginaOrigem=Grade" + "&pIdEmpresa=" + 40
        + "&pNomePasta=drogariavenancio" + "&pCaminhoDesignLoja=http://static-webv8.jet.com.br/drogariavenancio/";

    return urlParameters;

  }

  private String parseImageId(String response) {
    String tmp = response.split("\\.")[0];
    if (tmp != null) {
      return tmp.substring(tmp.indexOf('=') + 1);
    }
    return null;
  }

  /**
   * Round and normalize Double to have only two decimal places eg: 23.45123 --> 23.45
   * 
   * @param number
   * @return A rounded Double with only two decimal places
   */
  public static Float normalizeTwoDecimalPlaces(Float number) {
    BigDecimal big = new BigDecimal(number);
    String rounded = big.setScale(2, BigDecimal.ROUND_HALF_EVEN).toString();

    return Float.parseFloat(rounded);
  }

  /**
   * In cases with variations, to crawl installment is required a default installment in html So is
   * calculated installment price.
   * 
   * @param doc
   * @param price
   * @param internalPid
   * @return
   */
  private Prices crawlPricesVariation(Document doc, Float price) {
    Prices prices = new Prices();

    if (prices != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();

      installmentPriceMap.put(1, price);
      prices.setBankTicketPrice(price);

      Element installments = doc.select(".parcelamento b").first();

      if (installments != null) {
        Integer installment = Integer.parseInt(installments.ownText().replaceAll("[^0-9]", ""));

        Float value = MathUtils.normalizeTwoDecimalPlaces(price / installment);
        installmentPriceMap.put(installment, value);
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
    }

    return prices;
  }

  /**
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Document doc, Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();

      Element priceFrom = doc.select(".precoDe span[id]").first();
      if (priceFrom != null) {
        prices.setPriceFrom(MathUtils.parseDoubleWithComma(priceFrom.text()));
      }

      installmentPriceMap.put(1, price);
      prices.setBankTicketPrice(price);

      Element installments = doc.select(".parcelamento").first();

      if (installments != null) {
        Element installmentElement = installments.select("b").first();
        if (installmentElement != null) {
          String text = installmentElement.ownText().replaceAll("[^0-9]", "").trim();
          if (!text.isEmpty()) {
            Integer installment = Integer.parseInt(text);

            Element valueElement = installments.select("span").first();

            if (valueElement != null) {
              Float value = MathUtils.parseFloatWithComma(valueElement.text());

              installmentPriceMap.put(installment, value);
            }
          }
        }

      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
    }

    return prices;
  }
}
