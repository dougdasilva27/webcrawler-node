package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

public class BrasilRicardoeletroCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.ricardoeletro.com.br/";
  private static final String SELLER_NAME = "ricardo eletro";

  public BrasilRicardoeletroCrawler(Session session) {
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

      // ID interno
      String internalId = null;
      Element elementInternalID = doc.select("#ProdutoDetalhesCodigoProduto").first();
      if (elementInternalID != null) {
        internalId = elementInternalID.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").trim();
      }

      // Pid
      String internalPid = crawlInternalPid(doc);

      // Nome
      String name = null;
      Element elementName = doc.select("#ProdutoDetalhesNomeProduto h1").first();
      if (elementName != null) {
        name = elementName.ownText().replace("'", "").replace("’", "").trim();

        Element nameVariation = doc.select(".selectAtributo option[selected]").first();

        if (nameVariation != null) {
          String textName = nameVariation.text();

          if (textName.contains("|")) {
            name = name + " " + textName.split("\\|")[0].trim();;
          } else {
            name = name + " " + textName.trim();
          }
        }
      }

      // Marketplace
      Map<String, Prices> marketplaceMap = crawlMarketplaces(internalPid, doc);
      Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, Arrays.asList(SELLER_NAME), session);

      // Prices
      Prices prices = crawlPricesPrinciaplSeller(marketplaceMap);

      // Disponibilidade
      boolean available = marketplaceMap.containsKey(SELLER_NAME);

      // Preço
      Float price = null;
      Element elementPrice = doc.select("#ProdutoDetalhesPrecoComprarAgoraPrecoDePreco").first();
      if (elementPrice != null && available) {
        price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
      }

      // Categorias
      String category1 = "";
      String category2 = "";
      String category3 = "";
      Elements elementCategories = doc.select("#Breadcrumbs .breadcrumbs-itens").select("a");

      for (Element e : elementCategories) {
        if (category1.isEmpty()) {
          category1 = e.text();
        } else if (category2.isEmpty()) {
          category2 = e.text();
        } else if (category3.isEmpty()) {
          category3 = e.text();
        }
      }

      // Imagens
      Elements elementPrimaryImages = doc.select("#ProdutoDetalhesFotosFotosPequenas").select("a.zoom-gallery img");
      String primaryImage = null;
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      for (Element e : elementPrimaryImages) {

        // Tirando o 87x87 para pegar imagem original
        if (primaryImage == null) {
          primaryImage = e.attr("src").replace("/87x87", "");
        } else {
          secondaryImagesArray.put(e.attr("src").replace("/87x87", ""));
        }

      }

      if (secondaryImagesArray.length() > 0) {
        secondaryImages = secondaryImagesArray.toString();
      }

      // Descrição
      String description = "";
      Element elementDescription = doc.select("#aba-descricao").first();
      Element elementDescription2 = doc.select("#aba-caracteristicas").first();

      if (elementDescription != null) {
        description += elementDescription.html().replace("’", "").trim();
      }

      if (elementDescription2 != null) {
        description += elementDescription2.html().replace("’", "").trim();
      }

      // Estoque
      Integer stock = crawlStock(doc);

      Product product = new Product();

      product.setUrl(this.session.getOriginalURL());
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

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }

  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(Document doc) {
    return !doc.select("#ProdutoDetalhesCodigoProduto").isEmpty();
  }

  private Document crawlDocMarketplaces(String internalPid) {
    Document docMarketplace = new Document("");

    if (internalPid != null) {
      String url = "https://www.ricardoeletro.com.br/Produto/VejaMaisParceiros/1/" + internalPid;

      docMarketplace = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, cookies);
    }

    return docMarketplace;
  }

  private String crawlInternalPid(Document doc) {
    String internalPid = null;
    Element pid = doc.select("input[idproduto]").first();

    if (pid != null) {
      internalPid = pid.attr("idproduto");
    }

    return internalPid;
  }

  private String crawlPrincipalSeller(Document doc) {
    String principalSeller = null;

    Element seller = doc.select(".info-parceiro strong").first();

    if (seller != null) {
      principalSeller = seller.ownText().trim().toLowerCase();
    }

    return principalSeller;
  }


  private Map<String, Prices> crawlMarketplaces(String internalPid, Document doc) {

    Document docMarketplaceInfo = crawlDocMarketplaces(internalPid);
    String principalSeller = crawlPrincipalSeller(doc);

    Map<String, Prices> marketplace = new HashMap<>();

    Elements lines = docMarketplaceInfo.select(".modal-linha-parceiro");

    for (Element linePartner : lines) {
      Element priceElement = linePartner.select(".valor-unitario").first();
      Element nameElement = linePartner.select(".nome-loja > a").first();

      if (priceElement != null && nameElement != null) {
        String partnerName = nameElement.text().trim().toLowerCase();
        Float partnerPrice = MathUtils.parseFloatWithComma(priceElement.ownText());

        if (!partnerName.isEmpty() && partnerPrice != null) {
          marketplace.put(partnerName, crawlPrices(doc, partnerPrice, partnerName.equals(principalSeller), internalPid));
        }
      }
    }

    if (marketplace.isEmpty()) {
      Element priceElement = doc.select("#ProdutoDetalhesPrecoComprarAgoraPrecoDePreco").first();

      if (priceElement != null) {
        Float price = MathUtils.parseFloatWithComma(priceElement.ownText());

        if (price != null) {
          marketplace.put(principalSeller, crawlPrices(doc, price, true, internalPid));
        }
      }
    }

    return marketplace;
  }

  private Integer crawlStock(Document doc) {
    Integer stock = null;
    Elements scripts = doc.select("script");
    JSONObject jsonDataLayer = new JSONObject();

    for (Element e : scripts) {
      String dataLayer = e.outerHtml().trim();

      if (dataLayer.contains("var dataLayer = [")) {
        int x = dataLayer.indexOf("= [") + 3;
        int y = dataLayer.indexOf("];", x);

        try {
          jsonDataLayer = new JSONObject(dataLayer.substring(x, y));
        } catch (JSONException jsonException) {
          Logging.printLogWarn(logger, session, "Data layer is not a Json");
        }
      }
    }

    if (jsonDataLayer.has("productID")) {
      String productId = jsonDataLayer.getString("productID");

      if (jsonDataLayer.has("productSKUList")) {
        JSONArray skus = jsonDataLayer.getJSONArray("productSKUList");

        for (int i = 0; i < skus.length(); i++) {
          JSONObject sku = skus.getJSONObject(i);

          if (sku.has("id")) {
            String id = sku.getString("id").trim();

            if (id.equals(productId)) {
              if (sku.has("stock")) {
                stock = sku.getInt("stock");
              }
              break;
            }
          }
        }
      }
    }

    return stock;
  }

  private Prices crawlPrices(Document doc, Float price, boolean principalSeller, String internalPid) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentsPriceMap = new HashMap<>();
      installmentsPriceMap.put(1, price);
      prices.setBankTicketPrice(price);

      if (principalSeller) {
        String url = "https://www.ricardoeletro.com.br/Pagamento/ExibeFormasPagamento/" + internalPid;
        Document docPrices = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, cookies);

        Elements installmentsElements = docPrices.select(".lista-parcelas tr");

        if (!installmentsElements.isEmpty()) {
          for (Element e : installmentsElements) {
            Elements values = e.select("td");

            if (values.size() > 2) {
              String installment = values.get(0).ownText().replaceAll("[^0-9]", "").trim();
              Float value = MathUtils.parseFloatWithComma(values.get(2).ownText());

              if (!installment.isEmpty() && value != null) {
                installmentsPriceMap.put(Integer.parseInt(installment), value);
              }
            }
          }
        } else {
          Element parcel1 = doc.select(".produto-detalhes-preco-parcelado-parcelas").first();
          setParcels(parcel1, installmentsPriceMap);

          Elements parcels = doc.select(".produto-detalhes-preco-parcelado");
          if (parcels.size() > 1) {
            setParcels(parcels.get(1), installmentsPriceMap);
          }
        }
      }

      prices.setBankTicketPrice(installmentsPriceMap.get(1));
      prices.insertCardInstallment(Card.VISA.toString(), installmentsPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentsPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentsPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentsPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentsPriceMap);
    }

    return prices;
  }

  private Prices crawlPricesPrinciaplSeller(Map<String, Prices> marketplaceMap) {
    if (marketplaceMap.containsKey(SELLER_NAME)) {
      return marketplaceMap.get(SELLER_NAME);
    }

    return new Prices();
  }

  private void setParcels(Element e, Map<Integer, Float> installmentsPriceMap) {
    if (e != null) {
      String parcela = e.text().toLowerCase();

      int x = parcela.indexOf("x");
      int y = parcela.indexOf("r$");

      Integer installment = Integer.parseInt(parcela.substring(0, x).replaceAll("[^0-9]", "").trim());
      Float priceInstallment = Float.parseFloat(parcela.substring(y).replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());

      installmentsPriceMap.put(installment, priceInstallment);
    }
  }
}
