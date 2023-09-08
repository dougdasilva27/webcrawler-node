
package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchUtilities;
import br.com.lett.crawlernode.core.fetcher.methods.HttpClientFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.*;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.apache.http.HttpHeaders;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.bson.internal.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Date: 08/10/2018
 *
 * @author Gabriel Dornelas
 */
public class MercadolivreCrawler extends Crawler {


   private String getCep() {
      return session.getOptions().optString("cp");
   }

   private String getDomainCookie() {
      return session.getOptions().optString("domain");
   }

   private String homePage;
   private String mainSellerNameLower;
   protected boolean allow3PSellers = isAllow3PSellers();
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString());
   private List<String> sellerVariations;
   protected boolean acceptCatalog = isAcceptCatalog();

   private boolean isAcceptCatalog() {
      return session.getOptions().optBoolean("accept_catalog", true);
   }


   protected MercadolivreCrawler(Session session) {
      super(session);
   }

   public boolean isAllow3PSellers() {
      return session.getOptions().optBoolean("allow_3p", false);
   }

   public void setHomePage(String homePage) {
      this.homePage = homePage;
   }

   public void setMainSellerNameLower(String mainSellerNameLower) {
      this.mainSellerNameLower = mainSellerNameLower;
   }

   public void setSellerVariations(List<String> sellerVariations) {
      this.sellerVariations = sellerVariations;
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(homePage));
   }

   @Override
   protected Response fetchResponse() {
      Document doc;
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.USER_AGENT, FetchUtilities.randUserAgent());
      Response response = null;

      if (acceptCatalog || isOwnProduct()) {

         if (getCep() != null && !getCep().isEmpty()) {
            BasicClientCookie cookie = new BasicClientCookie("cp", getCep());
            cookie.setDomain(getDomainCookie());
            cookie.setPath("/");
            this.cookies.add(cookie);
         }
         boolean success;
         int tries = 1;

         do {
            Request request = RequestBuilder.create()
               .setUrl(session.getOriginalURL())
               .setCookies(cookies)
               .setSendUserAgent(true)
               .setHeaders(headers)
               .build();

            response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new JsoupDataFetcher(), new HttpClientFetcher()), session);

            doc = Jsoup.parse(response.getBody());
            String description = CrawlerUtils.scrapStringSimpleInfo(doc, ".ui-pdp-description__content", true);
            success = description != null && !description.isEmpty();
            if (success) {
               Logging.printLogInfo(logger, session, "HTML has description!");
            } else {
               Logging.printLogError(logger, session, "HTML not have description. Attempt: " + tries);
            }

         } while (!success && tries++ <= 4);

      }

      return response;
   }

   private boolean isOwnProduct() {
      if (session.getOriginalURL().startsWith("https://produto.mercadolivre.com") || session.getOriginalURL().startsWith("https://articulo.mercadolibre.com")) {
         Logging.printLogDebug(logger, session, "Is a own product " + this.session.getOriginalURL());
         return true;
      } else {
         Logging.printLogDebug(logger, session, "Is not a own product " + this.session.getOriginalURL());
         return false;
      }
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=itemId], #productInfo input[name=\"item_id\"]", "value");
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "a.breadcrumb:not(.shortened)");

         Map<String, Document> variations = getVariationsHtmls(doc);
         // This condition happens because meli has a bug, has two ways to select size on the page below
         // https://produto.mercadolivre.com.br/MLB-1394918791-fralda-infantil-pampers-premium-care-_JM
         if (variations.size() == 1 && doc.select(".variation-list li:not(.variations-selected) a.ui-list__item-option").size() > 1) {
            JSONObject productJson = CrawlerUtils.selectJsonFromHtml(doc, "script", "new meli.Variations(", ");", false, false);
            JSONArray skus = productJson.optJSONArray("model") != null ? productJson.optJSONArray("model") : new JSONArray();

            for (Object sku : skus) {
               JSONObject skuJson = (JSONObject) sku;

               String sellerFullName = scrapSellerFullName(doc);
               boolean isMainSeller = mainSellerNameLower.equalsIgnoreCase(sellerFullName);

               if (isMainSeller) {
                  String variationId = skuJson.optString("id");
                  String internalId = internalPid + "-" + variationId;

                  String name = crawlName(doc, skuJson);
                  String nameUrl = extractNameUrl(session.getOriginalURL());
                  List<String> images = scrapSpecialImages(skuJson, nameUrl);
                  String primaryImage = !images.isEmpty() ? images.get(0).replace(".webp", ".jpg") : null;
                  String secondaryImages = scrapSecondaryImages(images);
                  String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".vip-section-specs", ".section-specs", ".item-description"));

                  RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
                  ratingReviewsCollection.addRatingReviews(crawlRating(doc, internalPid, internalId));
                  RatingsReviews ratingReviews = ratingReviewsCollection.getRatingReviews(internalId);
                  boolean availableToBuy = skuJson.optInt("available_quantity", 0) > 0;
                  Offers offers = availableToBuy ? scrapOffers(doc) : new Offers();

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
                     .setRatingReviews(ratingReviews)
                     .setOffers(offers)
                     .build();

                  products.add(product);
               }
            }
         } else {
            for (Entry<String, Document> entry : variations.entrySet()) {
               Document docVariation = entry.getValue();

               String sellerFullName = scrapSellerFullName(docVariation);
               boolean isMainSeller = mainSellerNameLower.equalsIgnoreCase(sellerFullName);

               if (isMainSeller) {
                  String variationId = CrawlerUtils.scrapStringSimpleInfoByAttribute(docVariation, "input[name=variation]", "value");
                  String internalId = variationId == null || variations.size() < 2 ? internalPid : internalPid + "-" + variationId;

                  String name = crawlName(docVariation);
                  String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(docVariation, "figure.gallery-image-container a", Arrays.asList("href"), "https:",
                     "http2.mlstatic.com");
                  String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(docVariation, "figure.gallery-image-container a", Arrays.asList("href"),
                     "https:", "http2.mlstatic.com", primaryImage);
                  String description =
                     CrawlerUtils.scrapSimpleDescription(docVariation, Arrays.asList(".vip-section-specs", ".section-specs", ".item-description"));

                  RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
                  ratingReviewsCollection.addRatingReviews(crawlRating(docVariation, internalPid, internalId));
                  RatingsReviews ratingReviews = ratingReviewsCollection.getRatingReviews(internalId);
                  boolean availableToBuy = !docVariation.select(".item-actions [value=\"Comprar agora\"]").isEmpty()
                     || !docVariation.select(".item-actions [value=\"Comprar ahora\"]").isEmpty()
                     || !docVariation.select(".item-actions [value~=Comprar]").isEmpty()
                     || !docVariation.select(".ui-pdp-actions__container .andes-button__content").isEmpty();
                  Offers offers = availableToBuy ? scrapOffers(docVariation) : new Offers();

                  // Creating the product
                  Product product = ProductBuilder.create()
                     .setUrl(entry.getKey())
                     .setInternalId(internalId)
                     .setInternalPid(internalPid)
                     .setName(name)
                     .setCategory1(categories.getCategory(0))
                     .setCategory2(categories.getCategory(1))
                     .setCategory3(categories.getCategory(2))
                     .setPrimaryImage(primaryImage != null ? primaryImage.replace(".webp", ".jpg") : null)
                     .setSecondaryImages(secondaryImages != null ? secondaryImages.replace(".webp", ".jpg") : null)
                     .setDescription(description)
                     .setRatingReviews(ratingReviews)
                     .setOffers(offers)
                     .build();

                  products.add(product);
               }

            }
         }
      } else if (isProductPageNewSite(doc)) {
         MercadolivreNewCrawler meli = new MercadolivreNewCrawler(session, dataFetcher, mainSellerNameLower, allow3PSellers, logger, sellerVariations);
         Product product = meli.extractInformation(doc, null);
         Elements squareVariation = doc.select(".ui-pdp-variations .ui-pdp-variations__picker a");
         Elements dropdownVariation = doc.select(".andes-dropdown__popover ul li");

        /*
        There are two ways to capture variations
https://articulo.mercadolibre.cl/MLC-599227007-1-acondicionador-herbal-essences-coleccion-completa-_JM?attributes=RXNlbmNpYQ%3D%3D%3AQWxvZSAmIE1hbmdv&quantity=1
https://articulo.mercadolibre.cl/MLC-599229057-pack-6-shampoo-herbal-essences-coleccion-completa-_JM?searchVariation=80283561021#searchVariation=80283561021&position=3&search_layout=grid&type=item&tracking_id=e803c413-b044-485f-9df8-0e3f148927ce
         */
         if (product != null) {
            if (squareVariation.isEmpty() && dropdownVariation.isEmpty()) {
               products.add(product);

            } else {

               String urlToCaptureVariations = getDomain();

               if (!squareVariation.isEmpty()) {

                  squareVariation.parallelStream()
                     .map(element -> {
                        Request request = RequestBuilder.create()
                           .setUrl(urlToCaptureVariations + element.attr("href"))
                           .setCookies(cookies)
                           .build();

                        if (urlToCaptureVariations != null && urlToCaptureVariations.contains("br")) {
                           String body = dataFetcher.get(session, request).getBody();
                           Document currentDoc = Jsoup.parse(body);
                           if (currentDoc.selectFirst(".ui-empty-state.not-found-page") != null) {
                              request = RequestBuilder.create()
                                 .setUrl("https://produto.mercadolivre.com.br" + element.attr("href"))
                                 .setCookies(cookies)
                                 .build();
                           } else {
                              return new Pair<>(body, element.attr("title"));
                           }
                        }
                        return new Pair<>(dataFetcher.get(session, request).getBody(), element.attr("title"));
                     })
                     .forEach(responsePair -> {
                        try {
                           Product p = meli.extractInformation(Jsoup.parse(responsePair.getFirst()), product.getRatingReviews());

                           if (p != null) {
                              products.add(p);
                           }
                        } catch (OfferException | MalformedPricingException | MalformedProductException e) {
                           throw new IllegalStateException(e);
                        }
                     });

               } else if (!dropdownVariation.isEmpty()) {

                  String titleProduct = CrawlerUtils.scrapStringSimpleInfo(doc, ".ui-pdp-variations__picker p", true);

                  if (titleProduct != null) {
                     dropdownVariation.parallelStream()
                        .map(element -> {
                           Request request = null;
                           try {
                              request = RequestBuilder.create()
                                 .setUrl(session.getOriginalURL().split("=")[0] + "=" + getParamsUrlVariations(element, titleProduct))
                                 .setCookies(cookies)
                                 .build();
                           } catch (UnsupportedEncodingException e) {
                              e.printStackTrace();
                           }
                           return new Pair<>(dataFetcher.get(session, request).getBody(), element.attr("title"));
                        })
                        .forEach(responsePair -> {
                           try {
                              Product p = meli.extractInformation(Jsoup.parse(responsePair.getFirst()), product.getRatingReviews());

                              if (p != null) {
                                 products.add(p);
                              }
                           } catch (OfferException | MalformedPricingException | MalformedProductException e) {
                              throw new IllegalStateException(e);
                           }
                        });
                  }
               }
            }
         }
      }

      return products;
   }

   private boolean isProductPageNewSite(Document doc) {
      return !doc.select("h1.ui-pdp-title").isEmpty();
   }

   private String getParamsUrlVariations(Element element, String tittleProduct) throws UnsupportedEncodingException {
      String nameProduct = CrawlerUtils.scrapStringSimpleInfo(element, ".ui-pdp-dropdown-selector__item--label", true);
      String encodedParams = null;
      if (nameProduct != null) {
         encodedParams = URLEncoder.encode(Base64.encode(tittleProduct.replace(":", "").getBytes(StandardCharsets.UTF_8)) + ":" + Base64.encode(nameProduct.getBytes(StandardCharsets.UTF_8)), "UTF-8");
      }

      return encodedParams;
   }

   private String getDomain() {

      String slug = null;
      Pattern pattern = Pattern.compile("https:\\/\\/(.*?)\\/");
      Matcher matcher = pattern.matcher(session.getOriginalURL());
      if (matcher.find()) {
         slug = matcher.group(1);
      }
      if (slug != null) {
         slug = "https://" + slug;
      }
      return slug;
   }

   private boolean isProductPage(Document doc) {
      return !doc.select(".vip-nav-bounds .layout-main").isEmpty();
   }

   private String extractNameUrl(String url) {
      return url.split("-_")[0].replaceAll("(?i)https:\\/\\/produto\\.mercadolivre\\.com\\.br\\/MLB[-][0-9]+[-]\\s?", "");
   }

   private String scrapSecondaryImages(List<String> images) {
      String secondaryImages = null;
      JSONArray imagesArray = new JSONArray();

      if (!images.isEmpty()) {
         images.remove(0);

         for (String image : images) {
            imagesArray.put(image.replace(".webp", ".jpg"));
         }
      }

      if (imagesArray.length() > 0) {
         secondaryImages = imagesArray.toString();
      }

      return secondaryImages;
   }

   private List<String> scrapSpecialImages(JSONObject skuJson, String nameUrl) {
      List<String> imagesList = new ArrayList<>();

      JSONArray images = skuJson.optJSONArray("picture_ids");
      if (images != null) {
         for (Object o : images) {
            if (o != null) {
               imagesList.add("https://http2.mlstatic.com/" + nameUrl + "-D_NQ_NP_" + o + "-F.jpg");
            }
         }
      }

      return imagesList;
   }

   private Map<String, Document> getVariationsHtmls(Document doc) {
      Map<String, Document> variations = new HashMap<>();

      String originalUrl = session.getOriginalURL();
      variations.putAll(getSizeVariationsHmtls(doc, originalUrl));

      Elements colors = doc.select(".variation-list--full li:not(.variations-selected)");
      for (Element e : colors) {
         String dataValue = e.attr("data-value");
         String url =
            originalUrl + (originalUrl.contains("?") ? "&" : "?") + "attribute=COLOR_SECONDARY_COLOR%7C" + dataValue + "&quantity=1&noIndex=true";
         Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
         Document docColor = Jsoup.parse(this.dataFetcher.get(session, request).getBody());
         variations.putAll(getSizeVariationsHmtls(docColor, url));
      }

      return variations;
   }

   private Map<String, Document> getSizeVariationsHmtls(Document doc, String urlColor) {
      Map<String, Document> variations = new HashMap<>();
      variations.put(urlColor, doc);

      Elements sizes = doc.select(".variation-list li:not(.variations-selected) a.ui-list__item-option");
      for (Element e : sizes) {
         String url = e.attr("href");
         Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
         Document docSize = Jsoup.parse(this.dataFetcher.get(session, request).getBody());

         String variationId = CrawlerUtils.scrapStringSimpleInfoByAttribute(docSize, "input[name=variation]", "value");
         if (sizes.size() > 1 && (variationId == null || variationId.trim().isEmpty())) {
            continue;
         }

         String redirectUrl = session.getRedirectedToURL(url);
         variations.put(redirectUrl != null ? redirectUrl : url, docSize);
      }

      return variations;
   }

   private static String crawlName(Document doc) {
      StringBuilder name = new StringBuilder();
      name.append(CrawlerUtils.scrapStringSimpleInfo(doc, "h1.item-title__primary", true));

      Element sizeElement = doc.selectFirst(".variation-list li.variations-selected");
      if (sizeElement != null) {
         name.append(" ").append(sizeElement.attr("data-title"));
      }

      Element colorElement = doc.selectFirst(".variation-list--full li.variations-selected");
      if (colorElement != null) {
         name.append(" ").append(colorElement.attr("data-title"));
      }

      return name.toString();
   }

   private static String crawlName(Document doc, JSONObject skuJson) {
      StringBuilder name = new StringBuilder();
      name.append(CrawlerUtils.scrapStringSimpleInfo(doc, "h1.item-title__primary", true));

      JSONArray attributes = skuJson.optJSONArray("attribute_combinations");
      if (attributes != null) {
         for (Object att : attributes) {
            JSONObject attribute = (JSONObject) att;

            String variationName = attribute.optString("value_name");
            if (variationName != null && !variationName.isEmpty()) {
               name.append(" " + variationName);
            }
         }
      }

      return name.toString();
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      String sellerFullName = scrapSellerFullName(doc);

      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(doc);


      offers.add(OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(sellerFullName)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(mainSellerNameLower.equalsIgnoreCase(sellerFullName))
         .setPricing(pricing)
         .setSales(sales)
         .build());

      scrapSellersPage(offers, doc);

      return offers;
   }

   private void scrapSellersPage(Offers offers, Document doc) throws OfferException, MalformedPricingException {
      String sellersPageUrl = CrawlerUtils.scrapUrl(doc, ".ui-pdp-other-sellers__link", "href", "https", "www.mercadolivre.com.br");
      if (sellersPageUrl != null) {
         String nextUrl = sellersPageUrl;

         int sellersPagePosition = 1;
         boolean mainOfferFound = false;

         do {
            Request request = RequestBuilder.create()
               .setUrl(nextUrl)
               .setCookies(cookies)
               .build();

            Document sellersHtml = Jsoup.parse(this.dataFetcher.get(session, request).getBody());
            nextUrl = CrawlerUtils.scrapUrl(sellersHtml, ".andes-pagination__button--next:not(.andes-pagination__button--disabled) a", "href", "https", "www.mercadolivre.com.br");

            Elements offersElements = sellersHtml.select("form.ui-pdp-buybox");
            if (!offersElements.isEmpty()) {
               for (Element e : offersElements) {
                  String sellerName = CrawlerUtils.scrapStringSimpleInfo(e, ".ui-pdp-action-modal__link", true);
                  if (sellerName != null && !mainOfferFound && offers.containsSeller(sellerName)) {
                     Offer offerMainPage = offers.getSellerByName(sellerName);
                     offerMainPage.setSellersPagePosition(sellersPagePosition);
                     offerMainPage.setIsBuybox(true);

                     mainOfferFound = true;
                  } else {
                     Pricing pricing = scrapPricingSellersPage(doc);
                     List<String> sales = scrapSales(doc);

                     offers.add(OfferBuilder.create()
                        .setUseSlugNameAsInternalSellerId(true)
                        .setSellerFullName(sellerName)
                        .setSellersPagePosition(sellersPagePosition)
                        .setIsBuybox(true)
                        .setIsMainRetailer(mainSellerNameLower.equalsIgnoreCase(sellerName))
                        .setPricing(pricing)
                        .setSales(sales)
                        .build());
                  }

                  sellersPagePosition++;
               }
            } else {
               break;
            }

         } while (nextUrl != null);
      }
   }

   private String scrapSellerFullName(Document doc) {
      String sellerName = null;
      Element sellerNameElement = doc.selectFirst(".official-store-info .title");

      if (sellerNameElement != null) {
         sellerName = sellerNameElement.ownText().toLowerCase().trim();
      } else {
         sellerNameElement = doc.selectFirst(".new-reputation > a");

         if (sellerNameElement != null) {
            sellerName = CommonMethods.getLast(sellerNameElement.attr("href").split("/"));
            try {
               sellerName = URLDecoder.decode(sellerName, "UTF-8").toLowerCase();
            } catch (UnsupportedEncodingException e) {
               Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
            }
         }
      }
      return sellerName;
   }

   private List<String> scrapSales(Element doc) {
      List<String> sales = new ArrayList<>();

      Element salesOneElement = doc.selectFirst(".price-tag.discount-arrow p, .ui-pdp-price__second-line__label");
      String firstSales = salesOneElement != null ? salesOneElement.text() : null;

      if (firstSales != null && !firstSales.isEmpty()) {
         sales.add(firstSales);
      }

      return sales;
   }

   private Pricing scrapPricingSellersPage(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-tag__disabled .price-tag-fraction", null, false, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".ui-pdp-price__second-line .price-tag-fraction", null, false, ',', session);
      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice, true);
      BankSlip bankTicket = BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bankTicket)
         .build();
   }


   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-tag.price-tag__del del .price-tag-symbol", "content", false, '.', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".item-price span.price-tag:not(.price-tag__del) .price-tag-symbol", "content", false, '.', session);
      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice, false);
      BankSlip bankTicket = BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bankTicket)
         .build();
   }


   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice, boolean sellersPage) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = sellersPage ? scrapInstallmentsSellersPage(doc) : scrapInstallments(doc);
      if (installments.getInstallments().isEmpty()) {
         installments.add(InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   public Installments scrapInstallments(Document doc) throws MalformedPricingException {
      Installments installments = new Installments();

      Element installmentsElement = doc.selectFirst(".payment-installments .highlight-info span");
      String installmentString = installmentsElement != null ? installmentsElement.text().replaceAll("[^0-9]", "").trim() : null;
      int installment = installmentString != null && !installmentString.isEmpty() ? Integer.parseInt(installmentString) : null;

      String priceSelector = ".payment-installments .ch-price";
      Element valueElement = doc.selectFirst(priceSelector);
      String priceStr = null;

      if (valueElement != null) {
         priceStr = valueElement.ownText() + ",";
         Element elementCents = doc.selectFirst(priceSelector + " sup");

         if (elementCents != null) {
            priceStr += elementCents.text();
         } else {
            priceStr += "00";
         }
      }

      String valueString = priceStr != null ? priceStr.replaceAll("[^0-9]", "").trim() : null;
      Double value = valueString != null ? MathUtils.parseDoubleWithComma(valueString) : null;


      installments.add(InstallmentBuilder.create()
         .setInstallmentNumber(installment)
         .setInstallmentPrice(value)
         .build());

      return installments;
   }

   public Installments scrapInstallmentsSellersPage(Element doc) throws MalformedPricingException {
      Installments installments = new Installments();

      Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(".ui-pdp-payment--md .ui-pdp-media__title", doc, false);
      if (!pair.isAnyValueNull()) {
         installments.add(InstallmentBuilder.create()
            .setInstallmentNumber(pair.getFirst())
            .setInstallmentPrice(MathUtils.normalizeTwoDecimalPlaces(((Float) pair.getSecond()).doubleValue()))
            .build());
      }

      return installments;
   }

   private RatingsReviews crawlRating(Document doc, String internalPid, String internalId) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = getTotalNumOfRatings(doc);
      Double avgRating = getTotalAvgRating(doc);
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(doc, internalPid, internalId);

      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   private Double getTotalAvgRating(Document doc) {
      Double avgRating = 0d;

      Element avg = doc.selectFirst(".review-summary-average");
      if (avg != null) {
         String text = avg.ownText().replaceAll("[^0-9.]", "");

         if (!text.isEmpty()) {
            avgRating = Double.parseDouble(text);
         }
      }

      return avgRating;
   }

   /**
    * Number of ratings appear in html
    *
    * @param docRating
    * @return
    */
   private Integer getTotalNumOfRatings(Document docRating) {
      Integer totalRating = 0;
      Element totalRatingElement = docRating.selectFirst(".core-review .average-legend");

      if (totalRatingElement != null) {
         String text = totalRatingElement.text().replaceAll("[^0-9]", "").trim();

         if (!text.isEmpty()) {
            totalRating = Integer.parseInt(text);
         }
      }

      return totalRating;
   }


   private Document acessHtmlWithAdvanedRating(String internalPid, String internalId) {

      Document docRating = new Document("");

      StringBuilder url = new StringBuilder();
      url.append("https://produto.mercadolivre.com.br/noindex/catalog/reviews/")
         .append(internalPid)
         .append("?noIndex=true")
         .append("&itemId=" + internalPid)
         .append("&contextual=true")
         .append("&access=view_all")
         .append("&quantity=1")
         .append("&variation=" + internalId.replace(internalPid + "-", ""));

      Map<String, String> headers = new HashMap<>();
      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.87 Safari/537.36");
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");

      Request request = RequestBuilder.create().setUrl(url.toString()).setCookies(cookies).build();
      String response = dataFetcher.get(session, request).getBody().trim();

      docRating = Jsoup.parse(response);

      return docRating;

   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc, String internalPid, String internalId) {
      Document docRating;

      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      docRating = acessHtmlWithAdvanedRating(internalPid, internalId);

      Elements reviews = docRating.select(".reviews-rating .review-rating-row.is-rated");

      for (Element review : reviews) {

         Element elementStarNumber = review.selectFirst(".review-rating-label");

         if (elementStarNumber != null) {


            String stringStarNumber = elementStarNumber.text().replaceAll("[^0-9]", "").trim();
            Integer numberOfStars = !stringStarNumber.isEmpty() ? Integer.parseInt(stringStarNumber) : 0;

            Element elementVoteNumber = review.selectFirst(".review-rating-total");

            if (elementVoteNumber != null) {

               String vN = elementVoteNumber.text().replaceAll("[^0-9]", "").trim();
               Integer numberOfVotes = !vN.isEmpty() ? Integer.parseInt(vN) : 0;

               switch (numberOfStars) {
                  case 5:
                     star5 = numberOfVotes;
                     break;
                  case 4:
                     star4 = numberOfVotes;
                     break;
                  case 3:
                     star3 = numberOfVotes;
                     break;
                  case 2:
                     star2 = numberOfVotes;
                     break;
                  case 1:
                     star1 = numberOfVotes;
                     break;
                  default:
                     break;
               }
            }
         }
      }

      return new AdvancedRatingReview.Builder()
         .totalStar1(star1)
         .totalStar2(star2)
         .totalStar3(star3)
         .totalStar4(star4)
         .totalStar5(star5)
         .build();
   }

}
