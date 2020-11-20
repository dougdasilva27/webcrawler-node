package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXCrawlersUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

public class BrasilLactaCrawler extends Crawler {

   public BrasilLactaCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = "https://www.lacta.hipervenda.com.br/";
   private static final String SELLER_FULL_NAME = "HiperVenda";
   private static final String SELLER_FULL_NAME_2 = "Hiper Venda";
   private static final String SELLER_FULL_NAME_3 = "Lacta";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, SELLER_FULL_NAME.toLowerCase(), HOME_PAGE, cookies, dataFetcher);
      vtexUtil.setHasBankTicket(false);
      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

      if (skuJson.length() > 1) {
         String internalPid = vtexUtil.crawlInternalPid(skuJson);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb li > a", true);
         String description = scrapDescription(internalPid, vtexUtil);
         JSONArray eanArray = CrawlerUtils.scrapEanFromVTEX(doc);

         // sku data in json
         JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

         for (int i = 0; i < arraySkus.length(); i++) {
            JSONObject jsonSku = arraySkus.getJSONObject(i);

            String internalId = vtexUtil.crawlInternalId(jsonSku);
            JSONObject apiJSON = vtexUtil.crawlApi(internalId);
            String name = vtexUtil.crawlName(jsonSku, skuJson, " ");
            String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
            String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
            String ean = i < eanArray.length() ? eanArray.getString(i) : null;
            List<String> eans = new ArrayList<>();
            eans.add(ean);

            boolean availableToBuy = jsonSku.optBoolean("available", false);
            Offers offers = availableToBuy ? scrapOffer(apiJSON, internalId) : new Offers();

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
                  .build();


            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private Offers scrapOffer(JSONObject apiJSON, String internalId) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      JSONArray offersArray = apiJSON.optJSONArray("SkuSellersInformation");
      if (offersArray != null) {
         int position = 1;
         for (Object o : offersArray) {
            JSONObject offerJson = o instanceof JSONObject ? (JSONObject) o : new JSONObject();
            String sellerFullName = offerJson.optString("Name", null);
            String sellerId = offerJson.optString("SellerId", null);
            boolean isBuyBox = offersArray.length() > 1;
            boolean isMainRetailer = SELLER_FULL_NAME.equalsIgnoreCase(sellerFullName) || SELLER_FULL_NAME_2.equalsIgnoreCase(sellerFullName)
                  || SELLER_FULL_NAME_3.equalsIgnoreCase(sellerFullName);

            Pricing pricing = scrapPricing(internalId, apiJSON, offerJson);

            offers.add(OfferBuilder.create()
                  .setInternalSellerId(sellerId)
                  .setSellerFullName(SELLER_FULL_NAME)
                  .setMainPagePosition(position)
                  .setIsBuybox(isBuyBox)
                  .setIsMainRetailer(isMainRetailer)
                  .setPricing(pricing)
                  .build());
            position++;
         }
      }

      return offers;
   }

   private Pricing scrapPricing(String internalId, JSONObject apiJson, JSONObject sellerJson) throws MalformedPricingException {
      boolean isDefaultSeller = sellerJson.optBoolean("IsDefaultSeller", true);

      JSONObject pricesJson = isDefaultSeller ? apiJson : sellerJson;
      Double spotlightPrice = pricesJson.optDouble("Price", 0d);
      Double priceFrom = pricesJson.optDouble("Price", 0d);

      if (priceFrom <= spotlightPrice) {
         priceFrom = null;
      }

      CreditCards creditCards = scrapCreditCards(internalId, spotlightPrice, isDefaultSeller);

      return PricingBuilder.create()
            .setSpotlightPrice(spotlightPrice)
            .setPriceFrom(priceFrom)
            .setCreditCards(creditCards)
            .build();
   }


   private CreditCards scrapCreditCards(String internalId, Double spotlightPrice, boolean isDefaultSeller) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      if (isDefaultSeller) {
         String pricesApi = "https://www.lacta.hipervenda.com.br/productotherpaymentsystems/" + internalId;
         Request request = RequestBuilder.create().setUrl(pricesApi).setCookies(cookies).build();
         Document doc = Jsoup.parse(this.dataFetcher.get(session, request).getBody());

         Elements cardsElements = doc.select("#ddlCartao option");

         if (!cardsElements.isEmpty()) {
            for (Element e : cardsElements) {
               String text = e.text().toLowerCase();
               String idCard = e.val();
               String card = null;
               Installments installments = scrapInstallments(doc, idCard, spotlightPrice);

               if (text.contains("visa")) {
                  card = Card.VISA.toString();
               } else if (text.contains("mastercard")) {
                  card = Card.MASTERCARD.toString();
               } else if (text.contains("cabal")) {
                  card = Card.CABAL.toString();
               } else if (text.contains("nativa")) {
                  card = Card.NATIVA.toString();
               } else if (text.contains("naranja")) {
                  card = Card.NARANJA.toString();
               } else if (text.contains("american express")) {
                  card = Card.AMEX.toString();
               }

               if (card != null) {
                  creditCards.add(CreditCardBuilder.create()
                        .setBrand(card)
                        .setInstallments(installments)
                        .setIsShopCard(false)
                        .build());
               }
            }
         }
      }

      if (creditCards.getCreditCards().isEmpty()) {
         Installments installments = new Installments();
         installments.add(InstallmentBuilder.create()
               .setInstallmentNumber(1)
               .setInstallmentPrice(spotlightPrice)
               .build());

         for (String card : cards) {
            creditCards.add(CreditCardBuilder.create()
                  .setBrand(card)
                  .setInstallments(installments)
                  .setIsShopCard(false)
                  .build());
         }
      }


      return creditCards;
   }

   public Installments scrapInstallments(Document doc, String idCard, Double spotlightPrice) throws MalformedPricingException {
      Installments installments = new Installments();

      Elements installmentsCard = doc.select(".tbl-payment-system#tbl" + idCard + " tr");
      for (Element i : installmentsCard) {
         Element installmentElement = i.select("td.parcelas").first();

         if (installmentElement != null) {
            String textInstallment = installmentElement.text().toLowerCase();
            Integer installment = null;

            if (textInstallment.contains("vista")) {
               installment = 1;
            } else {
               String text = textInstallment.replaceAll("[^0-9]", "").trim();

               if (!text.isEmpty()) {
                  installment = Integer.parseInt(text);
               }
            }

            Element valueElement = i.select("td:not(.parcelas)").first();

            if (valueElement != null && installment != null) {
               Double value = MathUtils.parseDoubleWithComma(valueElement.text());

               installments.add(InstallmentBuilder.create()
                     .setInstallmentNumber(installment)
                     .setInstallmentPrice(value)
                     .build());
            }
         }
      }

      if (installments.getInstallment(1) == null) {
         installments.add(InstallmentBuilder.create()
               .setInstallmentNumber(1)
               .setInstallmentPrice(spotlightPrice)
               .build());
      }

      return installments;
   }

   private String scrapDescription(String internalPid, VTEXCrawlersUtils vtexUtils) {
      StringBuilder description = new StringBuilder();

      JSONObject descApi = vtexUtils.crawlDescriptionAPI(internalPid, "productId");
      description.append(descApi.optString("description"));

      JSONArray tabela = descApi.optJSONArray("Tabela Nutricional");
      if (tabela != null) {
         for (int i = 0; i < tabela.length(); i++) {
            description.append(" " + tabela.optString(i));
         }
      }

      return description.toString().trim();
   }
}
