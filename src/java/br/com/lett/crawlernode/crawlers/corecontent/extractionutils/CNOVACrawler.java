package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.FetchUtilities;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import exceptions.OfferException;
import models.Marketplace;
import models.Offer;
import models.Offer.OfferBuilder;
import models.Offers;
import models.prices.Prices;

/************************************************************************************************************************************************************************************
 * Crawling notes (18/08/2016):
 * 
 * 1) For this crawler, we have one url for mutiples skus.
 * 
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is marketplace information in this ecommerce.
 * 
 * 4) To get marketplaces we use the url "url + id +/lista-de-lojistas.html"
 * 
 * 5) The sku page identification is done simply looking the URL format or simply looking the html
 * element.
 * 
 * 6) Even if a product is unavailable, its price is not displayed, then price is null.
 * 
 * 7) There is internalPid for skus in this ecommerce. The internalPid is a number that is the same
 * for all the variations of a given sku.
 * 
 * 8) The first image in secondary images is the primary image.
 * 
 * 9) When the sku has variations, the variation name it is added to the name found in the main
 * page.
 * 
 * 10) When the market crawled not appear on the page of the partners, the sku is unavailable.
 * 
 * 11) Em alguns casos de produtos com variações, o internalId não é o mesmo número que devemos usar
 * para montar a url da lista de lojistas. Nesses casos, nós devemos procurar por um outro id na
 * página principal.
 * 
 * 12) Em casos de produtos com variação de voltagem, os ids que aparecem no seletor de internalIDs
 * não são os mesmos para acessar a página de marketplace. Em alguns casos esses ids de página de
 * marketplace aparacem na url e no id do produto (Cod item ID). Nesses casos eu pego esses dois ids
 * e acesso a página de marketplace de cada um, e nessa página pego o nome do produto e o document
 * dela e coloco em um mapa. Quando entro na variação eu pego esse mapa e proucuro o document com o
 * nome do produto.
 * 
 * 13)Quando os ids da url e o (Cod Item ID) são iguais, pego os mesmos marketplaces para as
 * variações, vale lembrar que esse market a página de marketplace é a mesma para as variações.
 * 
 * 14)Quando as variações não são de voltagem, os ids para entrar na página de marketplace aparaecem
 * no seletor de internalID das variações, logo entro diretamente nas páginas de marketplaces de
 * cada um normalmente.
 * 
 * 15)Para produtos sem variações, apenas troco o final da url de “.html” para
 * “/lista-de-lojistas.html”.
 * 
 * Examples: ex1 (available):
 * http://www.extra.com.br/Eletronicos/Televisores/SmartTV/Smart-TV-LED-39-HD-Philco-PH39U21DSGW-com-Conversor-Digital-MidiaCast-PVR-Wi-Fi-Entradas-HDMI-e-Endrada-USB-7323247.html
 * ex2 (unavailable):
 * http://www.extra.com.br/Eletroportateis/Cafeteiras/CafeteirasEletricas/Cafeteira-Eletrica-Philco-PH16-Vermelho-Aco-Escovado-4451511.html
 * ex3 (only_marketplace):
 * http://www.extra.com.br/CamaMesaBanho/ToalhaAvulsa/Banho/Toalha-de-Banho-Desiree-Pinta-e-Borda---Santista-4811794.html
 * ex4 (Product with marketplace special):
 * http://www.extra.com.br/Eletrodomesticos/FornodeMicroondas/Microondas-30-Litros-Midea-Liva-Grill---MTAG42-7923503.html
 *
 * Optimizations notes: No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public abstract class CNOVACrawler extends Crawler {

  public CNOVACrawler(Session session) {
    super(session);
    super.config.setFetcher(FetchMode.FETCHER);
  }

  protected String mainSellerNameLower;
  protected String mainSellerNameLower2;
  protected String marketHost;
  protected static final String PROTOCOL = "https";

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && href.startsWith(PROTOCOL + "://" + marketHost + "/");
  }

  @Override
  protected Object fetch() {
    Document doc = new Document("");
    String page = fetchPage(session.getOriginalURL());

    if (page != null) {
      doc = Jsoup.parse(page);
    }

    return doc;
  }

  protected String fetchPage(String url) {
    Map<String, String> headers = new HashMap<>();
    headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
    headers.put("Accept-Enconding", "");
    headers.put("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
    headers.put("Cache-Control", "no-cache");
    headers.put("Connection", "keep-alive");
    headers.put("Host", this.marketHost);
    headers.put("Referer", PROTOCOL + "://" + this.marketHost + "/");
    headers.put("Upgrade-Insecure-Requests", "1");
    headers.put("User-Agent", FetchUtilities.randUserAgent());

    Request request = RequestBuilder.create()
        .setUrl(url)
        .setCookies(cookies)
        .setHeaders(headers)
        .setProxyservice(
            Arrays.asList(
                ProxyCollection.INFATICA_RESIDENTIAL_BR,
                ProxyCollection.BUY,
                ProxyCollection.STORM_RESIDENTIAL_US
            )
        ).build();
    return this.dataFetcher.get(session, request).getBody();
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      List<String> sellersNameList = Arrays.asList(mainSellerNameLower, mainSellerNameLower2);

      // Pegando url padrão no doc da página, para lidar com casos onde a url no banco é diferente
      String modifiedURL = getRedirectUrl().split("\\?")[0];

      boolean hasVariations = hasProductVariations(doc);
      if (hasVariations) {
        Logging.printLogDebug(logger, session, "Multiple skus in this page.");
      }

      // true if all the skus on a page are unnavailable
      boolean unnavailableForAll = checkUnnavaiabilityForAll(doc);

      String internalPid = crawlInternalPid(doc);
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".produtoNome h1 b", true);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb span:not(:first-child) a");
      String primaryImage = scrapPrimaryImage(doc);
      String secondaryImages = !unnavailableForAll ? CrawlerUtils.scrapSimpleSecondaryImages(doc, ".carouselBox .thumbsImg li a",
          Arrays.asList("rev", "href", "src"), PROTOCOL, marketHost, primaryImage) : null;

      String description = crawlDescription(doc);

      if (hasVariations) {
        Elements productVariationElements = doc.select(".produtoSku option[value]:not([value=\"\"])");

        List<String> idsForUrlMarketPlace = scrapSellersIDs(modifiedURL, doc, productVariationElements, unnavailableForAll);
        // Pegando os documents das páginas de marketPlace para produtos especiais
        Map<String, Document> documentsMarketPlaces = fetchSellersHtmlForSpecialProducts(idsForUrlMarketPlace, modifiedURL);

        for (Element sku : productVariationElements) {
          String variationInternalID = internalPid + "-" + sku.val();
          boolean unnavailable = sku.text().contains("Esgotado");
          String variationName = assembleVariationName(name, sku);

          Document variationDocument = sku.hasAttr("selected") ? doc
              : Jsoup.parse(fetchPage(CrawlerUtils.sanitizeUrl(sku, "data-url", PROTOCOL, this.marketHost)));

          Map<String, Prices> marketplaceMap = new HashMap<>();
          Offers offers = new Offers();

          if (!unnavailable) {
            Document docMarketplace = getDocumentMarketpalceForSku(documentsMarketPlaces, variationName, sku, modifiedURL);
            offers = scrapBuyBox(variationDocument, docMarketplace);
            marketplaceMap = crawlMarketplaces(docMarketplace, doc);
          }
          Marketplace marketplace = unnavailable ? new Marketplace()
              : CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, sellersNameList, Card.VISA, session);
          boolean available = !unnavailable && CrawlerUtils.getAvailabilityFromMarketplaceMap(marketplaceMap, sellersNameList);
          Prices prices = CrawlerUtils.getPrices(marketplaceMap, sellersNameList);
          Float price = CrawlerUtils.extractPriceFromPrices(prices, Card.VISA);

          List<String> eans = new ArrayList<>();
          String ean = scrapEan(variationDocument);
          if (ean != null) {
            eans.add(ean);
          }

          // Creating the product
          Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(variationInternalID).setInternalPid(internalPid)
              .setName(variationName).setPrice(price)
              .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1)).setCategory3(
                  categories.getCategory(2))
              .setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description).setMarketplace(marketplace).setEans(eans)
              .setOffers(offers).build();

          products.add(product);
        }
      }
      /*
       * crawling data of only one product in page
       */
      else {
        String internalIdSecondPart = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#ctl00_Conteudo_hdnIdSkuSelecionado", "value");
        String internalId = internalPid + "-" + internalIdSecondPart;
        Document docMarketplace = fetchDocumentMarketPlace(internalIdSecondPart, modifiedURL);
        Map<String, Prices> marketplaceMap = crawlMarketplaces(docMarketplace, doc);
        Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, sellersNameList, Card.VISA, session);
        boolean available = !unnavailableForAll && CrawlerUtils.getAvailabilityFromMarketplaceMap(marketplaceMap, sellersNameList);
        Prices prices = CrawlerUtils.getPrices(marketplaceMap, sellersNameList);
        Float price = CrawlerUtils.extractPriceFromPrices(prices, Card.VISA);
        Offers offers = scrapBuyBox(doc, docMarketplace);
        List<String> eans = new ArrayList<>();
        String ean = scrapEan(doc);
        if (ean != null) {
          eans.add(ean);
        }

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(price).setPrices(prices)
            .setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1)).setCategory3(categories
                .getCategory(2)).setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages).setDescription(description).setMarketplace(marketplace).setEans(eans).setOffers(offers).build();

        products.add(product);
      }


    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private String scrapPrimaryImage(Document doc) {
    String primaryImage = null;

    List<String> selectors = Arrays.asList(".carouselBox .thumbsImg li a", ".carouselBox .thumbsImg li a img", "#divFullImage a",
        "#divFullImage a img");

    for (String selector : selectors) {
      Element imageSelector = doc.selectFirst(selector);
      if (imageSelector != null) {
        String image = CrawlerUtils.sanitizeUrl(imageSelector, Arrays.asList("rev", "href", "src"), "https", this.marketHost);

        if (image != null && !image.isEmpty()) {
          primaryImage = image;
          break;
        }
      }
    }

    return primaryImage;
  }

  private Offers scrapBuyBox(Document doc, Document docMarketplace) {
    Offers offers = new Offers();
    try {

      if (doc.selectFirst(".descricaoAnuncio .productDetails") != null) {
        Offer offer = scrapPrincipalOffer(doc);

        if (offer != null) {
          offers.add(offer);
        }
      }

      Elements sellers = doc.select(".listaLojistas .buying");
      boolean isBuyBoxPage = doc.selectFirst(".sellerList") != null;

      if (isBuyBoxPage) {
        int mainPagePosition = 2; // because first position is the principal seller
        for (Element element : sellers) {
          Element sellerFullNameElement = element.selectFirst(".seller");
          Element mainPriceElement = element.selectFirst(".sale");

          if (sellerFullNameElement != null && mainPriceElement != null) {
            String sellerFullName = sellerFullNameElement.text();
            String slugSellerName = CrawlerUtils.toSlug(sellerFullName);
            String internalSellerId = sellerFullNameElement.attr("data-tooltiplojista-id");
            Double mainPrice = MathUtils.parseDoubleWithComma(mainPriceElement.text());

            Offer offer = new OfferBuilder().setSellerFullName(sellerFullName).setSlugSellerName(slugSellerName).setInternalSellerId(internalSellerId)
                .setMainPagePosition(mainPagePosition)
                .setIsBuybox(isBuyBoxPage).setMainPrice(mainPrice).build();

            offers.add(offer);

            mainPagePosition++;
          }
        }
      }

      Elements sellersElements = docMarketplace.select("#sellerList tr[data-id-lojista]");
      int position = 1;
      for (Element e : sellersElements) {
        String internalSellerId = e.attr("data-id-lojista");

        if (offers.contains(internalSellerId)) {
          Offer offer = offers.get(internalSellerId);
          offer.setSellersPagePosition(position);
        } else {
          Element sellerFullNameElement = e.selectFirst(".lojista > a[title]");
          Element mainPriceElement = e.selectFirst(".valor");

          if (sellerFullNameElement != null && mainPriceElement != null) {
            String sellerFullName = sellerFullNameElement.attr("title");
            String slugSellerName = CrawlerUtils.toSlug(sellerFullName);
            Double mainPrice = MathUtils.parseDoubleWithComma(mainPriceElement.text());
            Integer mainPagePosition = offers.isEmpty() ? 1 : null;

            Offer offer = new OfferBuilder().setSellerFullName(sellerFullName).setSlugSellerName(slugSellerName).setInternalSellerId(internalSellerId)
                .setSellersPagePosition(position)
                .setMainPagePosition(mainPagePosition).setIsBuybox(isBuyBoxPage).setMainPrice(mainPrice).build();

            offers.add(offer);
          }
        }
        position++;
      }


    } catch (OfferException e) {
      Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
    }

    return offers;
  }

  private Offer scrapPrincipalOffer(Document doc) throws OfferException {
    Offer offer = null;

    String sellerFullName = null;
    String slugSellerName = null;
    String internalSellerId = scrapSellerIdFromButton(doc);
    Double mainPrice = null;
    boolean isBuyBoxPage = doc.selectFirst(".sellerList") != null;
    Element elementMainSeller = doc.selectFirst(".buying > a");
    Element elementPrice = doc.selectFirst(".productDetails .sale.price");

    if (elementMainSeller != null) {
      sellerFullName = elementMainSeller.text().trim();
      slugSellerName = CrawlerUtils.toSlug(sellerFullName);
    }

    if (elementPrice != null) {
      mainPrice = MathUtils.parseDoubleWithComma(elementPrice.text());
    }

    if (sellerFullName != null && !sellerFullName.isEmpty()) {
      offer = new OfferBuilder().setSellerFullName(sellerFullName).setSlugSellerName(slugSellerName).setInternalSellerId(internalSellerId)
          .setMainPrice(mainPrice).setIsBuybox(isBuyBoxPage)
          .setMainPagePosition(1).build();
    }

    return offer;
  }

  private String scrapSellerIdFromButton(Document doc) {
    String internalSellerId = null;

    Element button = doc.selectFirst("#btnAdicionarCarrinho, .retirar-eleito > a");
    if (button != null) {
      String href = button.attr("href");

      if (href.toLowerCase().contains("idlojista")) {
        String[] params = button.attr("href").split("&");

        for (String param : params) {
          if (param.toLowerCase().startsWith("idlojista")) {
            internalSellerId = CommonMethods.getLast(param.split("="));
            break;
          }
        }
      } else {
        Element buying = doc.selectFirst(".buying a");
        if (buying != null) {
          internalSellerId = CommonMethods.getLast(buying.attr("href").split("Lojista/")).split("/")[0].trim();
        }
      }
    }

    return internalSellerId;
  }

  private boolean isProductPage(Document doc) {
    return !doc.select(".produtoNome").isEmpty();
  }

  private boolean hasProductVariations(Document document) {
    Elements skuChooser = document.select(".produtoSku option[value]:not([value=\"\"])");

    if (skuChooser.size() > 1) {
      if (skuChooser.size() == 2) {
        String prodOne = skuChooser.get(0).text();
        if (prodOne.contains("|")) {
          prodOne = prodOne.split("\\|")[0].trim();
        }

        String prodTwo = skuChooser.get(1).text();
        if (prodTwo.contains("|")) {
          prodTwo = prodTwo.split("\\|")[0].trim();
        }

        if (prodOne.equals(prodTwo)) {
          return false;
        }
      }
      return true;
    }

    return false;
  }

  private String crawlInternalPid(Document document) {
    String internalPid = null;
    Elements elementInternalId = document.select("script[type=text/javascript]");

    String idenfyId = "idProduct";

    for (Element e : elementInternalId) {
      String script = e.outerHtml();

      if (script.contains(idenfyId)) {
        script = script.replaceAll("\"", "");

        int x = script.indexOf(idenfyId);
        int y = script.indexOf(',', x + idenfyId.length());

        internalPid = script.substring(x + idenfyId.length(), y).replaceAll("[^0-9]", "").trim();
      }
    }


    return internalPid;
  }

  private String getRedirectUrl() {
    String urlRedirect = null;

    if (session.getRedirectedToURL(session.getOriginalURL()) != null) {
      urlRedirect = session.getRedirectedToURL(session.getOriginalURL());
    } else {
      urlRedirect = session.getOriginalURL();
    }


    return urlRedirect;
  }

  private Map<String, Document> fetchSellersHtmlForSpecialProducts(List<String> idsForUrlMarketPlace, String url) {
    Map<String, Document> documentsMarketPlaces = new HashMap<>();

    if (!idsForUrlMarketPlace.isEmpty()) {
      Document docMarketPlaceProdOne = this.fetchDocumentMarketPlace(idsForUrlMarketPlace.get(0), url);
      String[] namev = docMarketPlaceProdOne.select("#ctl00_Conteudo_lnkProdutoDescricao").text().split("-");
      documentsMarketPlaces.put(namev[namev.length - 1].trim().toLowerCase(), docMarketPlaceProdOne);

      if (idsForUrlMarketPlace.size() == 2) {
        Document docMarketPlaceProdTwo = this.fetchDocumentMarketPlace(idsForUrlMarketPlace.get(1), url);
        String[] namev2 = docMarketPlaceProdTwo.select("#ctl00_Conteudo_lnkProdutoDescricao").text().split("-");
        documentsMarketPlaces.put(namev2[namev2.length - 1].trim(), docMarketPlaceProdTwo);
      }
    }

    return documentsMarketPlaces;
  }

  private List<String> scrapSellersIDs(String url, Document doc, Elements skuOptions, boolean unnavailableForAll) {
    List<String> ids = new ArrayList<>();
    if (!unnavailableForAll) {

      String firstIdMainPage = CommonMethods.getLast(url.split("-")).replaceAll("[^0-9]", "").trim();
      String secondIdMainPage = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#ctl00_Conteudo_hdnIdSkuSelecionado", "value");
      ids.add(firstIdMainPage);

      // se os ids forem iguais, não há necessidade de enviar os 2
      if (!firstIdMainPage.equals(secondIdMainPage)) {
        ids.add(secondIdMainPage);
      }

      // Ids variations
      boolean correctId = false;
      for (Element e : skuOptions) {
        String id = e.attr("value").trim();

        if (id.equals(firstIdMainPage) || id.equals(secondIdMainPage)) {
          correctId = true;
          break;
        }
      }

      if (correctId) {
        ids.clear();
      }

    }

    return ids;
  }

  private Document fetchDocumentMarketPlace(String id, String url) {
    String urlMarketPlace;

    if (id != null) {
      urlMarketPlace = url.replace(CommonMethods.getLast(url.split("-")), id + "/lista-de-lojistas.html");
    } else {
      urlMarketPlace = url.replace(".html", "/lista-de-lojistas.html");
    }

    return Jsoup.parse(fetchPage(urlMarketPlace));
  }

  private Map<String, Prices> crawlMarketplaces(Document docMarketplaceInfo, Document doc) {
    Map<String, Prices> marketplace = new HashMap<>();

    Float principalSellerPrice = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".sale.price", null, true, ',', session);

    Element principalSeller = docMarketplaceInfo.selectFirst("table#sellerList tbody tr:first-child");
    if (principalSeller != null) {
      String partnerName = CrawlerUtils.scrapStringSimpleInfo(principalSeller, "a.seller", false);
      Float partnerPrice = CrawlerUtils.scrapFloatPriceFromHtml(principalSeller, ".valor", null, false, ',', session);

      if (partnerName != null && partnerPrice != null) {
        partnerName = partnerName.trim().toLowerCase();

        if (partnerPrice.equals(principalSellerPrice)) {
          marketplace.put(partnerName, crawlPrices(doc, partnerPrice));
        } else {
          Prices prices = new Prices();
          prices.setBankTicketPrice(partnerPrice);

          Map<Integer, Float> installmentPriceMap = new HashMap<>();
          installmentPriceMap.put(1, partnerPrice);

          Elements installments = principalSeller.select(".valorTotal span strong");

          if (installments.size() > 1) {
            Integer installment = Integer.parseInt(installments.get(0).text().trim());
            Float value = MathUtils.parseFloatWithComma(installments.get(1).text());

            if (value != null && value > 0) {
              installmentPriceMap.put(installment, value);
            }
          }

          prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);

          Element comprar = principalSeller.selectFirst(".adicionarCarrinho > a.bt-comprar-disabled");

          if (comprar == null || (comprar != null && principalSeller.selectFirst(".retirar a.bt-retirar") != null)) {
            marketplace.put(partnerName, prices);
          }
        }
      }

      Elements lines = docMarketplaceInfo.select("table#sellerList tbody tr:not(:first-child)");
      for (Element linePartner : lines) {
        String partnerName2 = CrawlerUtils.scrapStringSimpleInfo(linePartner, "a.seller", false);
        Float partnerPrice2 = CrawlerUtils.scrapFloatPriceFromHtml(linePartner, ".valor", null, false, ',', session);

        if (partnerName2 != null && partnerPrice2 != null) {
          partnerName2 = partnerName2.trim().toLowerCase();

          Prices prices = new Prices();
          prices.setBankTicketPrice(partnerPrice2);

          Map<Integer, Float> installmentPriceMap = new HashMap<>();
          installmentPriceMap.put(1, partnerPrice2);

          Elements installments = linePartner.select(".valorTotal span strong");

          if (installments.size() > 1) {
            Integer installment = Integer.parseInt(installments.get(0).text().trim());
            Float value = MathUtils.parseFloatWithComma(installments.get(1).text());

            if (value != null && value > 0) {
              installmentPriceMap.put(installment, value);
            }
          }

          prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);

          Element comprar = linePartner.select(".adicionarCarrinho > a.bt-comprar-disabled").first();

          if (comprar == null || (comprar != null && linePartner.select(".retirar a.bt-retirar") != null)) {
            marketplace.put(partnerName2, prices);
          }
        }
      }
    } else {
      String frontPageSeller = CrawlerUtils.scrapStringSimpleInfo(doc, ".buying > a", false);

      if (frontPageSeller != null) {
        marketplace.put(frontPageSeller.toLowerCase(), crawlPrices(doc, principalSellerPrice));
      }
    }

    return marketplace;
  }

  private String assembleVariationName(String name, Element sku) {
    String nameV = name;

    if (sku != null) {
      String[] tokens = sku.text().split("\\|");
      String variation = tokens[0].trim();

      if (!variation.isEmpty()) {
        nameV += (" - " + variation).trim();
      }
    }
    return nameV;
  }

  /*******************
   * General methods *
   *******************/

  private boolean checkUnnavaiabilityForAll(Document doc) {
    return doc.select(".alertaIndisponivel").first() != null;
  }

  private String crawlDescription(Document document) {
    StringBuilder description = new StringBuilder();
    Element elementProductDetails = document.select("#detalhes").first();
    if (elementProductDetails != null) {
      description.append(elementProductDetails.html());
    }

    Element ean = document.select(".productEan").first();
    if (ean != null) {
      description.append(CrawlerUtils.crawlDescriptionFromFlixMedia("5779", ean.ownText().replaceAll("[^0-9]", "").trim(), this.dataFetcher,
          session));
    }

    return description.toString();
  }

  private Document getDocumentMarketpalceForSku(Map<String, Document> documentsMarketPlaces, String name, Element sku, String url) {
    Document docMarketplaceInfo = new Document(url);

    if (documentsMarketPlaces.size() > 0) {

      if (documentsMarketPlaces.size() == 1) {
        docMarketplaceInfo = documentsMarketPlaces.entrySet().iterator().next().getValue();
      } else if (name != null) {
        String[] tokens = name.split("-");
        String nameV = tokens[tokens.length - 1].trim().toLowerCase();

        if (documentsMarketPlaces.containsKey(nameV)) {
          docMarketplaceInfo = documentsMarketPlaces.get(nameV);
        }
      }

    } else {
      docMarketplaceInfo = fetchDocumentMarketPlace(sku.attr("value"), url);
    }

    return docMarketplaceInfo;
  }

  private Prices crawlPrices(Document doc, Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();
      Map<Integer, Float> installmentPriceShopMap = new HashMap<>();
      prices.setBankTicketPrice(price);
      installmentPriceMap.put(1, price);
      installmentPriceShopMap.put(1, price);

      Float discountPrice = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".price.discount", null, false, ',', session);
      if (discountPrice != null) {
        prices.setBankTicketPrice(discountPrice);
        installmentPriceMap.put(1, discountPrice);
        installmentPriceShopMap.put(1, discountPrice);
      }

      Elements installments = doc.select(".tabsCont #tab01 tr");
      for (Element e : installments) {
        String id = e.id();

        if (!id.contains("CartaoFlex")) {
          setInstallment(e, installmentPriceMap);
        }
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);

      Elements installmentsShopCard = doc.select(".tabsCont #tab02 tr");
      for (Element e : installmentsShopCard) {
        setInstallment(e, installmentPriceShopMap);
      }

      prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceShopMap);
    }

    return prices;

  }

  private void setInstallment(Element e, Map<Integer, Float> installmentPriceMap) {
    Element parcela = e.selectFirst("> th");
    if (parcela != null) {
      String parcelaText = parcela.text().toLowerCase();
      Integer installment = null;

      if (parcelaText.contains("x")) {
        int x = parcelaText.indexOf('x');
        installment = Integer.parseInt(parcelaText.substring(0, x).replaceAll("[^0-9]", "").trim());
      }

      Float value = CrawlerUtils.scrapFloatPriceFromHtml(e, "> td", null, true, ',', session);
      if (value != null && installment != null && !installmentPriceMap.containsKey(installment)) {
        installmentPriceMap.put(installment, value);
      }
    }
  }

  private String scrapEan(Document doc) {
    String ean = null;

    Element totalElement = doc.selectFirst(".productCodSku .productEan");
    if (totalElement != null) {
      String text = totalElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        ean = text;
      }
    }

    return ean;
  }
}
