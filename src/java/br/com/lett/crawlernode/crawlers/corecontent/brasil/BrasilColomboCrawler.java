package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

public class BrasilColomboCrawler extends Crawler {

  public BrasilColomboCrawler(Session session) {
    super(session);
  }


  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && href.startsWith("https://www.colombo.com.br");
  }


  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    Element productElement = doc.select(".detalhe-produto").first();

    if (session.getOriginalURL().contains("www.colombo.com.br/produto/") && !session.getOriginalURL().contains("?") && (productElement != null)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      Elements selections = doc.select(".dados-itens-table.dados-itens-detalhe tr[data-item]");

      // ID interno
      String internalId = null;
      Element elementInternalID = doc.select("input[type=radio][checked]").first();
      if (elementInternalID != null) {
        internalId = elementInternalID.attr("value").trim();
      } else {
        elementInternalID = doc.select("#itemAviso").first();
        if (elementInternalID != null) {
          internalId = elementInternalID.attr("value").trim();
        }

      }

      // Pid
      String internalPid = null;
      Element elementInternalPid = doc.select(".codigo-produto").first();
      if (elementInternalPid != null) {
        internalPid = elementInternalPid.attr("content").trim();
        if (internalPid.isEmpty()) {
          internalPid = elementInternalPid.text().replaceAll("[^0-9]", "").trim();
        }
      }

      // Nome
      String name = null;
      Element elementName = doc.select("h1.nome-produto").first();
      if (elementName != null) {
        name = elementName.ownText().trim();
      }

      // Preço
      Float price = null;
      Element elementPrice = doc.select(".dados-condicao--texto b span").first();
      if (elementPrice != null) {
        String priceText = elementPrice.text().trim();
        if (!priceText.isEmpty()) {
          price = Float.parseFloat(elementPrice.text().trim().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
        }
      }

      // Prices
      Prices prices = crawlPrices(doc, price);

      // Categoria
      String category1 = "";
      String category2 = "";
      String category3 = "";
      Elements elementCategories = doc.select(".breadcrumb a");
      for (int i = 1; i < elementCategories.size(); i++) {
        if (category1.isEmpty()) {
          category1 = elementCategories.get(i).text().trim();
        } else if (category2.isEmpty()) {
          category2 = elementCategories.get(i).text().trim();
        } else if (category3.isEmpty()) {
          category3 = elementCategories.get(i).text().trim();
        }
      }

      // Descrição
      String description = "";
      Element elementDescription = doc.select("#produto-descricao").first();
      Element elementSpecs = doc.select("#produto-caracteristicas").first();
      if (elementDescription != null) {
        description = description + elementDescription.html().trim();
      }
      if (elementSpecs != null) {
        description = description + elementSpecs.html();
      }

      Element elementPrimaryImage = doc.select("li.js_slide picture img[data-slide-position=0]").first();
      String primaryImage = null;
      if (elementPrimaryImage != null) {
        primaryImage = sanitizeImageURL(elementPrimaryImage.attr("src").trim().replace("400x400", "800x800"));
      } else {
        elementPrimaryImage = doc.select("li.js_slide picture img[data-slide-position=1]").first();

        if (elementPrimaryImage != null) {
          primaryImage = sanitizeImageURL(elementPrimaryImage.attr("src").trim().replace("400x400", "800x800"));
        }
      }

      // Imagens -- Caso principal
      Elements elementSecondaryImages = doc.select("li.js_slide picture img");
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      for (Element el : elementSecondaryImages) {
        String image = sanitizeImageURL(el.attr("src").trim().replace("400x400", "800x800"));
        if (image != null && !image.equals(primaryImage)) { // imagem primária
          secondaryImagesArray.put(image);
        }
      }

      if (secondaryImagesArray.length() > 0) {
        secondaryImages = secondaryImagesArray.toString();
      }

      // Estoque
      Integer stock = null;

      // Marketplace
      Marketplace marketplace = new Marketplace();

      if (selections.size() <= 1) { // sem variações

        // Disponibilidade
        boolean available = true;
        Element elementUnavailable = doc.select("#dados-produto-indisponivel.avisoIndisponivel:not(.hide)").first();
        if (elementUnavailable != null) {
          available = false;
        }
        if (available == false) {
          price = null;
          prices = new Prices();
        }


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

      else { // múltiplas variações

        Elements variations = doc.select(".dados-itens-table.dados-itens-detalhe tr[data-item]");

        for (Element e : variations) {

          // ID interno
          String variationInternalId = null;
          Element variationElementInternalID = e.select("input").first();
          if (variationElementInternalID != null) {
            variationInternalId = e.attr("data-item").trim();
          }

          // Nome
          String variationName = null;
          Element variationElementName = e.select(".dados-itens-table-caracteristicas").first();
          if (variationElementName != null) {
            variationName = name + " " + variationElementName.textNodes().get(0).toString().trim();
          }

          // Available
          boolean variationAvailable = true;
          Element variationElementAvailable = e.select(".dados-itens-table-estoque").first();
          if (variationElementAvailable != null) {
            String tmp = variationElementAvailable.text().trim();
            if (tmp.contains("Esgotado")) {
              variationAvailable = false;
            }
          }


          Product product = new Product();
          product.setUrl(session.getOriginalURL());
          product.setInternalId(variationInternalId);
          product.setInternalPid(internalPid);
          product.setName(variationName);
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
          product.setAvailable(variationAvailable);

          products.add(product);

        }

      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }

  private String sanitizeImageURL(String imageURL) {
    String sanitizedURL = imageURL;

    if (imageURL != null) {
      if (imageURL.contains("?")) { // removendo parâmetros da url da imagem, senão não passa no crawler de imagens
        int index = imageURL.indexOf('?');
        sanitizedURL = imageURL.substring(0, index);
      }
    }

    try {
      URI rawUri = new URI(sanitizedURL);
      if (rawUri.getScheme() == null) {
        return new URIBuilder().setScheme("https").setHost(rawUri.getHost()).setPath(rawUri.getPath()).build().toString();
      }

      return rawUri.toString();

    } catch (URISyntaxException uriSyntaxException) {
      Logging.printLogDebug(logger, session, "Not a valid image URL " + CommonMethods.getStackTrace(uriSyntaxException));
    }

    return sanitizedURL;
  }

  private Prices crawlPrices(Document doc, Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();

      installmentPriceMap.put(1, price);

      Element bankPrice = doc.select(".dados-preco-valor").first();

      if (bankPrice != null) {
        Float bankTicketPrice = Float.parseFloat(bankPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());
        prices.setBankTicketPrice(bankTicketPrice);
      }

      Element installments = doc.select(".parcelas-produto-table").first();

      if (installments != null) {
        Elements parcelas = installments.select("tr");

        for (Element e : parcelas) {
          Element index = e.select(".parcelas-produto-table-index").first();

          if (index != null) {
            Integer installment = Integer.parseInt(index.text().replaceAll("[^0-9]", "").trim());

            Element valor = e.select(".parcelas-produto-table-valor").first();

            if (valor != null) {
              Float value = Float.parseFloat(valor.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());

              installmentPriceMap.put(installment, value);
            }
          }
        }

        prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.HIPER.toString(), installmentPriceMap);
      }
    }

    return prices;
  }
}
