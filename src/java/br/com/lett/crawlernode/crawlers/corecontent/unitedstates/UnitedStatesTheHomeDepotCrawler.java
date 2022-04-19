package br.com.lett.crawlernode.crawlers.corecontent.unitedstates;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebElement;

import java.util.*;

public class UnitedStatesTheHomeDepotCrawler extends Crawler {
   private static final List<String> cards = Arrays.asList(Card.MASTERCARD.toString());
   private static final String SELLER_NAME = "The Home Depot";
   private String CURRENT_URL = null;
   private Integer VARIATION = 1;
   private static final String HOME_PAGE = "https://www.homedepot.com/";
   private Document currentDoc;

   public UnitedStatesTheHomeDepotCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
      super.config.setParser(Parser.HTML);
   }

   @Override
   public void handleCookiesBeforeFetch() {
      String localizer = session.getOptions().optString("localizer");

      Cookie cookie = new Cookie.Builder("THD_LOCALIZER", localizer)
         .domain(".homedepot.com")
         .path("/")
         .isHttpOnly(false)
         .isSecure(false)
         .build();
      this.cookiesWD.add(cookie);
   }

   @Override
   protected Response fetchResponse() {
      String url = "https://www.homedepot.com/federation-gateway/graphql?opname=productClientOnlyProduct";
      String productId = getProductIdFromUrl();

      Map<String, String> headers = new HashMap<>();
      headers.put("cookie", CommonMethods.cookiesToString(this.cookies));
      headers.put("authority", "www.homedepot.com");
      headers.put("referer", session.getOriginalURL());
      headers.put("origin", HOME_PAGE);
      headers.put("accept", "*/*");
      headers.put("content-type", "application/json");
      headers.put("x-debug", "false");
      headers.put("x-experience-name", "general-merchandise");
      headers.put("x-hd-dc", "origin");

      /*
       * TODO:
       *  - Receber o storeId pelas options
       *  - Receber o zipCode pelas options
       *  - Talvez seja possível modificar o payload para receber somente as informações necessárias
       * **/
      String payload = "{\"operationName\":\"productClientOnlyProduct\",\"variables\":{\"skipSpecificationGroup\":false,\"skipSubscribeAndSave\":false,\"skipKPF\":false,\"itemId\":\"" + productId + "\",\"storeId\":\"721\",\"zipCode\":\"37013\"},\"query\":\"query productClientOnlyProduct($storeId: String, $zipCode: String, $itemId: String!, $dataSource: String, $loyaltyMembershipInput: LoyaltyMembershipInput, $skipSpecificationGroup: Boolean = false, $skipSubscribeAndSave: Boolean = false, $skipKPF: Boolean = false) {\\n  product(itemId: $itemId, dataSource: $dataSource, loyaltyMembershipInput: $loyaltyMembershipInput) {\\n    fulfillment(storeId: $storeId, zipCode: $zipCode) {\\n      backordered\\n      fulfillmentOptions {\\n        type\\n        services {\\n          type\\n          locations {\\n            isAnchor\\n            inventory {\\n              isLimitedQuantity\\n              isOutOfStock\\n              isInStock\\n              quantity\\n              isUnavailable\\n              maxAllowedBopisQty\\n              minAllowedBopisQty\\n              __typename\\n            }\\n            type\\n            storeName\\n            locationId\\n            curbsidePickupFlag\\n            isBuyInStoreCheckNearBy\\n            distance\\n            state\\n            storePhone\\n            __typename\\n          }\\n          deliveryTimeline\\n          deliveryDates {\\n            startDate\\n            endDate\\n            __typename\\n          }\\n          deliveryCharge\\n          dynamicEta {\\n            hours\\n            minutes\\n            __typename\\n          }\\n          hasFreeShipping\\n          freeDeliveryThreshold\\n          totalCharge\\n          __typename\\n        }\\n        fulfillable\\n        __typename\\n      }\\n      anchorStoreStatus\\n      anchorStoreStatusType\\n      backorderedShipDate\\n      bossExcludedShipStates\\n      sthExcludedShipState\\n      bossExcludedShipState\\n      excludedShipStates\\n      seasonStatusEligible\\n      onlineStoreStatus\\n      onlineStoreStatusType\\n      inStoreAssemblyEligible\\n      __typename\\n    }\\n    info {\\n      dotComColorEligible\\n      hidePrice\\n      ecoRebate\\n      quantityLimit\\n      sskMin\\n      sskMax\\n      unitOfMeasureCoverage\\n      wasMaxPriceRange\\n      wasMinPriceRange\\n      fiscalYear\\n      productDepartment\\n      classNumber\\n      forProfessionalUseOnly\\n      globalCustomConfigurator {\\n        customButtonText\\n        customDescription\\n        customExperience\\n        customExperienceUrl\\n        customTitle\\n        __typename\\n      }\\n      paintBrand\\n      movingCalculatorEligible\\n      label\\n      prop65Warning\\n      returnable\\n      recommendationFlags {\\n        visualNavigation\\n        reqItems\\n        batItems\\n        packages\\n        __typename\\n      }\\n      replacementOMSID\\n      hasSubscription\\n      minimumOrderQuantity\\n      projectCalculatorEligible\\n      subClassNumber\\n      calculatorType\\n      isLiveGoodsProduct\\n      protectionPlanSku\\n      hasServiceAddOns\\n      consultationType\\n      __typename\\n    }\\n    itemId\\n    dataSources\\n    identifiers {\\n      canonicalUrl\\n      brandName\\n      itemId\\n      modelNumber\\n      productLabel\\n      storeSkuNumber\\n      upcGtin13\\n      specialOrderSku\\n      toolRentalSkuNumber\\n      rentalCategory\\n      rentalSubCategory\\n      upc\\n      productType\\n      isSuperSku\\n      parentId\\n      roomVOEnabled\\n      sampleId\\n      __typename\\n    }\\n    availabilityType {\\n      discontinued\\n      status\\n      type\\n      buyable\\n      __typename\\n    }\\n    details {\\n      description\\n      collection {\\n        url\\n        collectionId\\n        __typename\\n      }\\n      highlights\\n      descriptiveAttributes {\\n        name\\n        value\\n        bulleted\\n        sequence\\n        __typename\\n      }\\n      infoAndGuides {\\n        name\\n        url\\n        __typename\\n      }\\n      installation {\\n        leadGenUrl\\n        __typename\\n      }\\n      __typename\\n    }\\n    media {\\n      images {\\n        url\\n        type\\n        subType\\n        sizes\\n        __typename\\n      }\\n      video {\\n        shortDescription\\n        thumbnail\\n        url\\n        videoStill\\n        link {\\n          text\\n          url\\n          __typename\\n        }\\n        title\\n        type\\n        videoId\\n        longDescription\\n        __typename\\n      }\\n      threeSixty {\\n        id\\n        url\\n        __typename\\n      }\\n      augmentedRealityLink {\\n        usdz\\n        image\\n        __typename\\n      }\\n      richContent {\\n        content\\n        displayMode\\n        richContentSource\\n        __typename\\n      }\\n      __typename\\n    }\\n    pricing(storeId: $storeId) {\\n      promotion {\\n        dates {\\n          end\\n          start\\n          __typename\\n        }\\n        type\\n        description {\\n          shortDesc\\n          longDesc\\n          __typename\\n        }\\n        dollarOff\\n        percentageOff\\n        savingsCenter\\n        savingsCenterPromos\\n        specialBuySavings\\n        specialBuyDollarOff\\n        specialBuyPercentageOff\\n        experienceTag\\n        subExperienceTag\\n        anchorItemList\\n        itemList\\n        reward {\\n          tiers {\\n            minPurchaseAmount\\n            minPurchaseQuantity\\n            rewardPercent\\n            rewardAmountPerOrder\\n            rewardAmountPerItem\\n            rewardFixedPrice\\n            __typename\\n          }\\n          __typename\\n        }\\n        nvalues\\n        brandRefinementId\\n        __typename\\n      }\\n      value\\n      alternatePriceDisplay\\n      alternate {\\n        bulk {\\n          pricePerUnit\\n          thresholdQuantity\\n          value\\n          __typename\\n        }\\n        unit {\\n          caseUnitOfMeasure\\n          unitsOriginalPrice\\n          unitsPerCase\\n          value\\n          __typename\\n        }\\n        __typename\\n      }\\n      original\\n      mapAboveOriginalPrice\\n      message\\n      preferredPriceFlag\\n      specialBuy\\n      unitOfMeasure\\n      __typename\\n    }\\n    reviews {\\n      ratingsReviews {\\n        averageRating\\n        totalReviews\\n        __typename\\n      }\\n      __typename\\n    }\\n    seo {\\n      seoKeywords\\n      seoDescription\\n      __typename\\n    }\\n    specificationGroup @skip(if: $skipSpecificationGroup) {\\n      specifications {\\n        specName\\n        specValue\\n        __typename\\n      }\\n      specTitle\\n      __typename\\n    }\\n    taxonomy {\\n      breadCrumbs {\\n        label\\n        url\\n        browseUrl\\n        creativeIconUrl\\n        deselectUrl\\n        dimensionName\\n        refinementKey\\n        __typename\\n      }\\n      brandLinkUrl\\n      __typename\\n    }\\n    favoriteDetail {\\n      count\\n      __typename\\n    }\\n    sizeAndFitDetail {\\n      attributeGroups {\\n        attributes {\\n          attributeName\\n          dimensions\\n          __typename\\n        }\\n        dimensionLabel\\n        productType\\n        __typename\\n      }\\n      __typename\\n    }\\n    subscription @skip(if: $skipSubscribeAndSave) {\\n      defaultfrequency\\n      discountPercentage\\n      subscriptionEnabled\\n      __typename\\n    }\\n    badges(storeId: $storeId) {\\n      label\\n      color\\n      creativeImageUrl\\n      endDate\\n      message\\n      name\\n      timerDuration\\n      timer {\\n        timeBombThreshold\\n        daysLeftThreshold\\n        dateDisplayThreshold\\n        message\\n        __typename\\n      }\\n      __typename\\n    }\\n    keyProductFeatures @skip(if: $skipKPF) {\\n      keyProductFeaturesItems {\\n        features {\\n          name\\n          refinementId\\n          refinementUrl\\n          value\\n          __typename\\n        }\\n        __typename\\n      }\\n      __typename\\n    }\\n    seoDescription\\n    installServices {\\n      scheduleAMeasure\\n      __typename\\n    }\\n    dataSource\\n    __typename\\n  }\\n}\\n\"}";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setCookies(cookies)
         .setPayload(payload)
         .setProxyservice(List.of(ProxyCollection.NETNUT_RESIDENTIAL_US_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY))
         .build();

      Response response = this.dataFetcher.post(session, request);

      if (!response.isSuccess()) {
         response = new JsoupDataFetcher().get(session, request);

         if (!response.isSuccess()) {
            int tries = 0;
            while (!response.isSuccess() && tries < 3) {
               tries++;
               if (tries % 2 == 0) {
                  response = new JsoupDataFetcher().post(session, request);
               } else {
                  response = this.dataFetcher.post(session, request);
               }
            }
         }
      }

         return response;
   }

   private String getProductIdFromUrl() {
      String productId = CommonMethods.getLast(session.getOriginalURL().split("/"));
      if (productId.contains("?")) {
         productId = productId.split("\\?")[0];
      }
         return productId;
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (doc != null) {
         Elements variations = scrapVariation(doc);

         for (int i = 0; i < variations.size(); i++) {
            this.currentDoc = fetchVariation(doc);
            JSONObject productJson = scrapJsonFromHtml(this.currentDoc);

            if (productJson != null) {
               Logging.printLogDebug(logger, session, "Product page identified: " + this.CURRENT_URL);

               String internalId = productJson.optString("productID");
               String name = productJson.optString("name");
               String description = productJson.optString("description");
               JSONArray images = productJson.optJSONArray("image");
               String primaryImage = scrapFirstImage(images);
               List<String> secondaryImages = scrapSecondaryImages(images);
               CategoryCollection categories = scrapCategories(this.currentDoc);
               boolean available = scrapAvailability(this.currentDoc);
               RatingsReviews ratingsReviews = scrapRatingReviews(productJson);

               Offers offers = available ? scrapOffers(productJson) : new Offers();

               Product product = ProductBuilder.create()
                  .setUrl(this.CURRENT_URL)
                  .setInternalId(internalId)
                  .setInternalPid(internalId)
                  .setName(name)
                  .setOffers(offers)
                  .setCategories(categories)
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages)
                  .setDescription(description)
                  .setRatingReviews(ratingsReviews)
                  .build();

               products.add(product);
            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page:   " + this.session.getOriginalURL());
      }

      return products;
   }

   private String scrapFirstImage(JSONArray imagesArray) {
      if (imagesArray != null && imagesArray.length() > 1) {
         Object image = imagesArray.get(0);
         return image.toString();
      }
      return null;
   }

   private List<String> scrapSecondaryImages(JSONArray imagesArray) {
      List<String> secondaryImages = new ArrayList<>();

      if (imagesArray != null && imagesArray.length() > 2) {
         for (int i = 1; i < imagesArray.length(); i++) {
            secondaryImages.add(imagesArray.get(i).toString());
         }
      }

      return secondaryImages;
   }

   private Boolean scrapAvailability(Document doc) {
      Element availability = doc.selectFirst(".buybox__actions");

      if (availability != null) {
         return true;
      }

      return false;
   }

   private Offers scrapOffers(JSONObject productJson) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(productJson);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(new Offer.OfferBuilder()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(this.SELLER_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject productJson) throws MalformedPricingException {
      Integer priceInt = JSONUtils.getValueRecursive(productJson, "offers.price", Integer.class);
      Double spotlightPrice = null;
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(this.currentDoc, ".price-detailed__was-price span.u__strike span", null, false, '.', session);

      if (priceInt != null) {
         spotlightPrice = Double.valueOf(priceInt);
      } else {
         spotlightPrice = JSONUtils.getValueRecursive(productJson, "offers.price", Double.class);
      }

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();
   }


   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      Integer installmentNumber = 1;
      Double finalPrice = spotlightPrice;
      Double installmentPrice = spotlightPrice;

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(installmentNumber)
         .setInstallmentPrice(installmentPrice)
         .setFinalPrice(finalPrice)
         .build());

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private Document fetchVariation(Document doc) {
      if (this.VARIATION != 1) {
         doc = null;
      }
      this.VARIATION++;
      return doc;
   }

   private Elements scrapVariation(Document doc) {
      Element variationsDiv = doc.selectFirst(".super-sku__inline-attribute-wrapper--tile");
      Elements variations = new Elements();

      if (variationsDiv != null) {
         variations = variationsDiv.select("div.super-sku__inline-tile--space button");
      } else {
         variations = doc.select(".super-sku__inline-attribute .super-sku__inline-attribute-wrapper button");
      }

      if (variations.size() == 0) {
         variations = doc.select(".product-details__title");
      }

      return variations;
   }

   JSONObject scrapJsonFromHtml(Document doc) {
      JSONObject productJson = CrawlerUtils.selectJsonFromHtml(doc, "script[data-th=\"server\"]", null, null, false, false);

      if (productJson == null) {
         return null;
      }

      if (productJson.isEmpty()) {
         productJson = CrawlerUtils.selectJsonFromHtml(doc, "script[data-th=\"client\"]", null, null, false, false);
      }

      return productJson;
   }

   private RatingsReviews scrapRatingReviews(JSONObject json) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      JSONArray reviews = json.optJSONArray("review");

      Integer totalNumOfEvaluations = reviews.length();
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(reviews);
      Double avgRating = CrawlerUtils.extractRatingAverageFromAdvancedRatingReview(advancedRatingReview);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(JSONArray reviews) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;


      if (reviews != null) {

         for (Object rating : reviews) {
            Integer star = JSONUtils.getValueRecursive(rating, "reviewRating.ratingValue", Integer.class, 0);

            switch (star) {
               case 1:
                  star1++;
                  break;
               case 2:
                  star2++;
                  break;
               case 3:
                  star3++;
                  break;
               case 4:
                  star4++;
                  break;
               case 5:
                  star5++;
                  break;
               default:
                  break;
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

   protected CategoryCollection scrapCategories(Document doc) {
      CategoryCollection categories = new CategoryCollection();
      Elements breadcrumbs = doc.select("[name=\"breadcrumbs\"] a");

      for (Element category : breadcrumbs) {
         String categoryName = CrawlerUtils.scrapStringSimpleInfo(category, "a", false);
         if (categoryName != null) categories.add(categoryName);
      }

      return categories;
   }
}
