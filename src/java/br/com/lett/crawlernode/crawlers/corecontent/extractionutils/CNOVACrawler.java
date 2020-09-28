package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.FetchUtilities;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JavanetDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.crawler.EqiCrawlerSession;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

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

      if (session instanceof EqiCrawlerSession) {
         super.config.setFetcher(FetchMode.JAVANET);
      } else {
         super.config.setFetcher(FetchMode.FETCHER);
      }

      super.config.setMustSendRatingToKinesis(true);
   }

   protected String mainSellerNameLower;
   protected String mainSellerNameLower2;
   protected String marketHost;
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());
   protected static final String PROTOCOL = "https";

   private static final String USER_AGENT = FetchUtilities.randUserAgent();

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && href.startsWith(PROTOCOL + "://" + marketHost + "/");
   }

   @Override
   protected Object fetch() {
      Response response = fetchPage(encodeUrlPath(session.getOriginalURL()), PROTOCOL + "://" + this.marketHost + "/", this.dataFetcher);
      Document doc = Jsoup.parse(response.getBody());

      List<Cookie> cookiesResponse = response.getCookies();
      for (Cookie cookieResponse : cookiesResponse) {
         cookies.add(CrawlerUtils.setCookie(cookieResponse.getName(), cookieResponse.getValue(), this.marketHost, "/"));
      }

      return doc;
   }

   protected Response fetchPage(String url, String referer, DataFetcher df) {
      Map<String, String> headers = new HashMap<>();
      headers.put("referer", referer);
      headers.put("authority", this.marketHost);
      headers.put("cache-control", "max-age=0");
      headers.put("upgrade-insecure-requests", "1");
      headers.put("user-agent", USER_AGENT);
      headers.put("accept", "text/html");
      headers.put("sec-fetch-site", "cross-site");
      headers.put("sec-fetch-mode", "navigate");
      headers.put("sec-fetch-user", "?1");
      headers.put("sec-fetch-dest", "document");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");

      Request request = RequestBuilder.create()
            .setUrl(url)
            .setCookies(cookies)
            .setHeaders(headers)
            .setFetcheroptions(FetcherOptionsBuilder.create()
                  .mustUseMovingAverage(false)
                  .mustRetrieveStatistics(true)
                  .build())
            .mustSendContentEncoding(false)
            .setProxyservice(
                  Arrays.asList(
                        ProxyCollection.INFATICA_RESIDENTIAL_BR,
                        ProxyCollection.STORM_RESIDENTIAL_US,
                        ProxyCollection.BUY
                  )
            ).build();

      Response response = df.get(session, request);
      this.cookies.addAll(response.getCookies());

      int statusCode = response.getLastStatusCode();

      if (response.getBody().isEmpty() || (Integer.toString(statusCode).charAt(0) != '2' &&
            Integer.toString(statusCode).charAt(0) != '3'
            && statusCode != 404)) {

         if (df instanceof FetcherDataFetcher) {
            response = new JavanetDataFetcher().get(session, request);
         } else {
            response = new FetcherDataFetcher().get(session, request);
         }

      }

      return response;
   }

   protected String fetchPageHtml(String url, String referer) {
      return fetchPage(url, referer, this.dataFetcher).getBody();
   }

   protected String encodeUrlPath(String url) {
      StringBuilder sb = new StringBuilder();

      try {
         URL u = new URL(url);
         String path = u.getPath();

         sb.append(u.getProtocol() + "://" + u.getHost());
         for (String subPath : path.split("/")) {
            if (subPath.isEmpty()) continue;

            sb.append("/" + URLEncoder.encode(subPath, StandardCharsets.UTF_8.toString()));
         }
      } catch (Exception e) {
         Logging.printLogWarn(logger, CommonMethods.getStackTrace(e));
      }

      return sb.toString();
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

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
         String secondaryImages = !unnavailableForAll ? scrapSecondaryImages(doc, primaryImage) : null;

         String description = crawlDescription(doc);

         if (hasVariations) {
            Elements productVariationElements = doc.select(".produtoSku option[value]:not([value=\"\"])");

            for (Element sku : productVariationElements) {
               String variationInternalID = internalPid + "-" + sku.val();
               boolean unnavailable = sku.text().contains("Esgotado");
               String variationName = assembleVariationName(name, sku);
               RatingsReviews ratingReviews = crawRating(doc, variationInternalID);

               Document variationDocument = sku.hasAttr("selected") ? doc
                     : Jsoup.parse(fetchPageHtml(CrawlerUtils.sanitizeUrl(sku, "data-url", PROTOCOL, this.marketHost), session.getOriginalURL()));

               Offers offers = !unnavailable ? scrapOffers(variationDocument) : new Offers();

               List<String> eans = new ArrayList<>();
               String ean = scrapEan(variationDocument);
               if (ean != null) {
                  eans.add(ean);
               }

               // Creating the product
               Product product = ProductBuilder.create()
                     .setUrl(session.getOriginalURL())
                     .setInternalId(variationInternalID)
                     .setInternalPid(internalPid)
                     .setName(variationName)
                     .setCategory1(categories.getCategory(0))
                     .setCategory2(categories.getCategory(1))
                     .setCategory3(categories.getCategory(2))
                     .setPrimaryImage(primaryImage)
                     .setSecondaryImages(secondaryImages)
                     .setDescription(description)
                     .setEans(eans)
                     .setOffers(offers)
                     .setRatingReviews(ratingReviews)
                     .build();

               products.add(product);
            }
         }
         /*
          * crawling data of only one product in page
          */
         else {
            JSONObject metadataJSON = CrawlerUtils.selectJsonFromHtml(doc, "script", "varsiteMetadata=", ";", true, true);

            String idSKU = scrapSkuIdForSingleProductPage(metadataJSON);
            String internalId = internalPid + (idSKU != null ? "-" + idSKU : "");
            boolean unnavailable = checkUnnavaiabilityForAll(doc);
            Offers offers = !unnavailable ? scrapOffers(doc) : new Offers();
            RatingsReviews ratingReviews = crawRating(doc, internalId);

            List<String> eans = new ArrayList<>();
            String ean = scrapEan(doc);
            if (ean != null) {
               eans.add(ean);
            }

            // Creating the product
            Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setCategory1(categories.getCategory(0))
                  .setCategory2(categories.getCategory(1))
                  .setCategory3(categories.getCategory(2))
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages)
                  .setDescription(description)
                  .setEans(eans)
                  .setOffers(offers)
                  .setRatingReviews(ratingReviews)
                  .build();

            products.add(product);
         }


      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private String scrapSkuIdForSingleProductPage(JSONObject metadata) {
      String skuId = null;

      JSONObject page = metadata.optJSONObject("page");
      if (page != null) {
         JSONObject product = page.optJSONObject("product");
         if (product != null) {
            skuId = product.optString("idSku", null);
         }
      }

      return skuId;
   }

   private String scrapPrimaryImage(Document doc) {
      String primaryImage = null;

      List<String> selectors = Arrays.asList(".carouselBox .thumbsImg li a", ".carouselBox .thumbsImg li a img", "#divFullImage a",
            "#divFullImage a img");

      for (String selector : selectors) {
         Element imageSelector = doc.selectFirst(selector);
         if (imageSelector != null) {

            // Encoding url path of captured url to ensure that java.net can execute the request
            List<String> attrs = Arrays.asList("rev", "href", "src");
            for (String attr : attrs) {
               String image = CrawlerUtils.scrapStringSimpleInfoByAttribute(imageSelector, null, attr);

               if (image != null && !image.isEmpty()) {
                  image = encodeUrlPath(image);

                  image = CrawlerUtils.completeUrl(image, PROTOCOL, this.marketHost);
                  if (image != null && !image.isEmpty()) {
                     primaryImage = image;
                     break;
                  }
               }
            }

            if (primaryImage != null && !primaryImage.isEmpty()) {
               break;
            }
         }
      }

      return primaryImage;
   }

   private String scrapSecondaryImages(Document doc, String primaryImage) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      Elements elements = doc.select(".carouselBox .thumbsImg li:not(:first-child) a");

      for (Element imageElement : elements) {

         List<String> attrs = Arrays.asList("rev", "href", "src");
         for (String attr : attrs) {
            String image = CrawlerUtils.scrapStringSimpleInfoByAttribute(imageElement, null, attr);
            image = encodeUrlPath(image);
            image = CrawlerUtils.completeUrl(image, PROTOCOL, this.marketHost);

            if ((primaryImage == null || !primaryImage.equals(image)) && image != null && !image.isEmpty()) {
               secondaryImagesArray.put(image);
               break;
            }
         }
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();

      Offer principalOffer = null;
      Map<String, Integer> mainSellers = new HashMap<>();
      if (doc.selectFirst(".descricaoAnuncio .productDetails") != null && doc.selectFirst(".buying > a") != null) {
         principalOffer = scrapPrincipalOffer(doc);
         mainSellers.put(principalOffer.getInternalSellerId(), 1);
      }

      Elements sellers = doc.select(".listaLojistas .buying");
      boolean isBuyBoxPage = doc.selectFirst(".sellerList") != null;

      if (isBuyBoxPage) {
         int mainPagePosition = 2; // because first position is the principal seller
         for (Element element : sellers) {
            Element sellerFullNameElement = element.selectFirst(".seller");

            if (sellerFullNameElement != null) {
               mainSellers.put(sellerFullNameElement.attr("data-tooltiplojista-id"), mainPagePosition);
               mainPagePosition++;
            }
         }
      }

      JSONObject sitemetadata = CrawlerUtils.selectJsonFromHtml(doc, "script", "var siteMetadata = ", ";", false, true);
      JSONObject page = sitemetadata.optJSONObject("page");

      if (page != null) {
         JSONObject product = page.optJSONObject("product");

         if (product != null) {
            JSONObject sellersJson = product.optJSONObject("sellers");

            if (sellers != null) {
               JSONObject id = sellersJson.optJSONObject("id");
               Set<String> sellersIds = id != null ? id.keySet() : new HashSet<>();
               int position = 1;

               for (String sellerId : sellersIds) {
                  JSONObject sellerObject = id.optJSONObject(sellerId);

                  String internalSellerId = sellerObject.optString("id");

                  if (principalOffer != null && principalOffer.getInternalSellerId().equalsIgnoreCase(internalSellerId)) {
                     principalOffer.setSellersPagePosition(position);
                  } else {
                     Integer mainPagePosition = mainSellers.containsKey(internalSellerId) ? mainSellers.get(internalSellerId) : null;
                     String sellerFullName = sellerObject.optString("name");
                     boolean isMainRetailer = sellerFullName.equalsIgnoreCase(mainSellerNameLower) || sellerFullName.equalsIgnoreCase(mainSellerNameLower2);
                     Pricing pricing = scrapSellersPricing(sellerObject);

                     offers.add(OfferBuilder.create()
                           .setInternalSellerId(internalSellerId)
                           .setSellerFullName(sellerFullName)
                           .setMainPagePosition(mainPagePosition)
                           .setSellersPagePosition(position)
                           .setIsBuybox(isBuyBoxPage)
                           .setIsMainRetailer(isMainRetailer)
                           .setPricing(pricing)
                           .build());
                  }
                  position++;
               }

            }
         }
      }

      if (principalOffer != null) {
         offers.add(principalOffer);
      }

      return offers;
   }

   private Pricing scrapSellersPricing(JSONObject sellerObj) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(sellerObj, "price", true);
      BankSlip bankSlip = BankSlipBuilder.create()
            .setFinalPrice(spotlightPrice)
            .build();
      CreditCards creditCards = scrapSellersCreditCards(spotlightPrice);

      return PricingBuilder.create()
            .setSpotlightPrice(spotlightPrice)
            .setCreditCards(creditCards)
            .setBankSlip(bankSlip)
            .build();
   }

   private CreditCards scrapSellersCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      installments.add(InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());

      Set<String> allCards = new HashSet<>(this.cards);
      allCards.add(Card.SHOP_CARD.toString());

      for (String brand : allCards) {
         creditCards.add(CreditCardBuilder.create()
               .setBrand(brand)
               .setIsShopCard(brand.equalsIgnoreCase(Card.SHOP_CARD.toString()))
               .setInstallments(installments)
               .build());
      }

      return creditCards;
   }

   private Offer scrapPrincipalOffer(Document doc) throws OfferException, MalformedPricingException {
      boolean isBuyBoxPage = doc.selectFirst(".sellerList") != null;
      String sellerFullName = doc.selectFirst(".buying > a").text().trim();
      String internalSellerId = scrapSellerIdFromButton(doc);
      boolean isMainRetailer = sellerFullName.equalsIgnoreCase(mainSellerNameLower) || sellerFullName.equalsIgnoreCase(mainSellerNameLower2);
      Integer sellersPagePosition = null;
      Pricing pricing = scrapPricingForProductPage(doc);
      String sale = CrawlerUtils.scrapStringSimpleInfo(doc, ".percentual", false);
      List<String> sales = sale != null ? Arrays.asList(sale) : new ArrayList<>();

      return OfferBuilder.create()
            .setInternalSellerId(internalSellerId)
            .setSellerFullName(sellerFullName)
            .setMainPagePosition(1)
            .setIsBuybox(isBuyBoxPage)
            .setIsMainRetailer(isMainRetailer)
            .setSellersPagePosition(sellersPagePosition)
            .setPricing(pricing)
            .setSales(sales)
            .build();
   }

   private Pricing scrapPricingForProductPage(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".productDetails .from strong", null, false, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".productDetails .for strong", null, false, ',', session);

      Double percentage = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".PaymentDiscount .DiscountCondition", null, true, ',', session);
      Double discount = percentage != null ? percentage / 100d : 0d;

      CreditCards creditCards = scrapCreditCardsFromProductPage(doc, discount, spotlightPrice);
      BankSlip bankSlip = scrapBankslip(doc, spotlightPrice, discount);

      return PricingBuilder.create()
            .setPriceFrom(priceFrom)
            .setSpotlightPrice(spotlightPrice)
            .setCreditCards(creditCards)
            .setBankSlip(bankSlip)
            .build();
   }

   private BankSlip scrapBankslip(Document doc, Double spotlightPrice, Double discount) throws MalformedPricingException {
      Double bkPrice = spotlightPrice;
      Double bkDiscount = 0d;

      Element installmentDiscount = doc.selectFirst(".PaymentDiscount .DiscountText");
      if (installmentDiscount != null) {
         String text = installmentDiscount.ownText().toLowerCase();

         if (text.contains("boleto")) {
            bkDiscount = discount;
            bkPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".PaymentDiscount .price.discount", null, true, ',', session);
         }
      }

      return BankSlipBuilder.create()
            .setFinalPrice(bkPrice)
            .setOnPageDiscount(bkDiscount)
            .build();
   }

   private CreditCards scrapCreditCardsFromProductPage(Document doc, Double discount, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments regularCard = scrapInstallments(doc, ".tabsCont #tab01 tr:not(.first)", discount);
      if (regularCard.getInstallments().isEmpty()) {
         regularCard.add(InstallmentBuilder.create()
               .setInstallmentNumber(1)
               .setInstallmentPrice(spotlightPrice)
               .build());
      }

      for (String brand : cards) {
         creditCards.add(CreditCardBuilder.create()
               .setBrand(brand)
               .setIsShopCard(false)
               .setInstallments(regularCard)
               .build());
      }

      Installments shopCard = scrapInstallments(doc, ".tabsCont #tab02 tr:not(.first)", discount);

      if (shopCard.getInstallments().isEmpty()) {
         shopCard = regularCard;
      }

      creditCards.add(CreditCardBuilder.create()
            .setBrand(Card.SHOP_CARD.toString())
            .setIsShopCard(true)
            .setInstallments(shopCard)
            .build());

      return creditCards;
   }


   private Installments scrapInstallments(Document doc, String selector, Double discount) throws MalformedPricingException {
      Installments installments = new Installments();
      Integer maxInstallmentsWithDiscount = scrapInstallmenMaxDiscount(doc);

      Elements installmentsElements = doc.select(selector);
      for (Element e : installmentsElements) {
         Element installmentElement = e.selectFirst("> th");
         if (installmentElement != null) {
            installments.add(scrapInstallment(e, maxInstallmentsWithDiscount, discount));
         }
      }

      return installments;
   }

   private Integer scrapInstallmenMaxDiscount(Document doc) {
      Integer maxInstallmentsWithDiscount = null;
      Element installmentDiscount = doc.selectFirst(".PaymentDiscount .DiscountText");
      if (installmentDiscount != null) {
         String text = installmentDiscount.ownText().toLowerCase();

         if (text.contains("cartão") && text.contains("x")) {
            int firstIndex = text.indexOf('x');
            String installmentText = text.substring(0, firstIndex).replaceAll("[^0-9]", "");
            if (!installmentText.isEmpty()) {
               maxInstallmentsWithDiscount = Integer.parseInt(installmentText);
            }
         }
      }

      return maxInstallmentsWithDiscount;
   }

   private Installment scrapInstallment(Element e, Integer maxInstallmentsWithDiscount, Double discount) throws MalformedPricingException {
      String parcelaText = e.text().toLowerCase();
      Integer installment = null;
      Double interests = 0d;
      Double installmentPrice = CrawlerUtils.scrapDoublePriceFromHtml(e, "> td", null, true, ',', session);

      if (parcelaText.contains("x")) {
         int x = parcelaText.indexOf('x');
         installment = Integer.parseInt(parcelaText.substring(0, x).replaceAll("[^0-9]", "").trim());
      }

      if (parcelaText.contains("juros") && parcelaText.contains("%")) {
         int firstIndex = parcelaText.indexOf("juros");
         int lastIndex = parcelaText.indexOf("%");

         Double i = MathUtils.parseDoubleWithComma(parcelaText.substring(firstIndex, lastIndex));
         interests = i != null ? i : 0d;
      }

      Double installmentDiscount = maxInstallmentsWithDiscount != null && installment <= maxInstallmentsWithDiscount ? discount : 0d;

      return InstallmentBuilder.create()
            .setInstallmentNumber(installment)
            .setInstallmentPrice(installmentPrice)
            .setAmOnPageInterests(interests)
            .setOnPageDiscount(installmentDiscount)
            .build();

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

            return !prodOne.equals(prodTwo);
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
         description.append(CrawlerUtils.crawlDescriptionFromFlixMedia("5779", ean.ownText().replaceAll("[^0-9]", "").trim(), new FetcherDataFetcher(),
               session));
      }

      return description.toString();
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

   private RatingsReviews crawRating(Document doc, String internalId) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = getTotalRating(doc);
      Double avgRating = getTotalAvgRating(doc);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);

      return ratingReviews;
   }

   private Integer getTotalRating(Document doc) {
      Integer total = 0;

      Element finalElement = null;
      Element rating = doc.select(".pr-snapshot-average-based-on-text .count").first();
      Element ratingOneEvaluation = doc.select(".pr-snapshot-average-based-on-text").first();
      Element specialEvaluation = doc.select(".rating-count[itemprop=\"reviewCount\"]").first();

      if (rating != null) {
         finalElement = rating;
      } else if (ratingOneEvaluation != null) {
         finalElement = ratingOneEvaluation;
      } else if (specialEvaluation != null) {
         finalElement = specialEvaluation;
      }

      if (finalElement != null) {
         total = Integer.parseInt(finalElement.ownText().replaceAll("[^0-9]", ""));
      }

      return total;
   }

   /**
    * @param Double
    * @return
    */
   private Double getTotalAvgRating(Document doc) {
      Double avgRating = 0d;

      Element avg = doc.select(".pr-snapshot-rating.rating .pr-rounded.average").first();

      if (avg == null) {
         avg = doc.select(".rating .rating-value").first();
      }

      if (avg != null) {
         avgRating = Double.parseDouble(avg.ownText().replace(",", "."));
      }

      return avgRating;
   }


}
