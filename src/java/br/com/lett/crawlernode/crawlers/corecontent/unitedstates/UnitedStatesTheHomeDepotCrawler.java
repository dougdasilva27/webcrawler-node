package br.com.lett.crawlernode.crawlers.corecontent.unitedstates;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
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
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.*;

public class UnitedStatesTheHomeDepotCrawler extends Crawler {
   private static final List<String> cards = Arrays.asList(Card.MASTERCARD.toString());
   private static final String SELLER_NAME = "The Home Depot";
   private String INTERNAL_ID;
   private static final String HOME_PAGE = "https://www.homedepot.com/";

   public UnitedStatesTheHomeDepotCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
      super.config.setParser(Parser.JSON);
   }

   @Override
   protected Response fetchResponse() {
      String url = "https://www.homedepot.com/federation-gateway/graphql?opname=productClientOnlyProduct";
      INTERNAL_ID = getProductIdFromUrl();

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
       * **/
      String payload = "{\"operationName\":\"productClientOnlyProduct\",\"variables\":{\"skipSpecificationGroup\":false,\"skipSubscribeAndSave\":false,\"skipKPF\":false,\"itemId\":\"" + INTERNAL_ID + "\",\"storeId\":\"721\",\"zipCode\":\"37013\"},\"query\":\"query productClientOnlyProduct($storeId: String, $zipCode: String, $itemId: String!, $dataSource: String, $loyaltyMembershipInput: LoyaltyMembershipInput, $skipSpecificationGroup: Boolean = false, $skipSubscribeAndSave: Boolean = false, $skipKPF: Boolean = false) {\\n  product(itemId: $itemId, dataSource: $dataSource, loyaltyMembershipInput: $loyaltyMembershipInput) {\\n    fulfillment(storeId: $storeId, zipCode: $zipCode) {\\n      backordered\\n      fulfillmentOptions {\\n        type\\n        services {\\n          type\\n          locations {\\n            isAnchor\\n            inventory {\\n              isLimitedQuantity\\n              isOutOfStock\\n              isInStock\\n              quantity\\n              isUnavailable\\n              maxAllowedBopisQty\\n              minAllowedBopisQty\\n              __typename\\n            }\\n            type\\n            storeName\\n            locationId\\n            curbsidePickupFlag\\n            isBuyInStoreCheckNearBy\\n            distance\\n            state\\n            storePhone\\n            __typename\\n          }\\n          deliveryTimeline\\n          deliveryDates {\\n            startDate\\n            endDate\\n            __typename\\n          }\\n          deliveryCharge\\n          dynamicEta {\\n            hours\\n            minutes\\n            __typename\\n          }\\n          hasFreeShipping\\n          freeDeliveryThreshold\\n          totalCharge\\n          __typename\\n        }\\n        fulfillable\\n        __typename\\n      }\\n      anchorStoreStatus\\n      anchorStoreStatusType\\n      backorderedShipDate\\n      bossExcludedShipStates\\n      sthExcludedShipState\\n      bossExcludedShipState\\n      excludedShipStates\\n      seasonStatusEligible\\n      onlineStoreStatus\\n      onlineStoreStatusType\\n      inStoreAssemblyEligible\\n      __typename\\n    }\\n    info {\\n      dotComColorEligible\\n      hidePrice\\n      ecoRebate\\n      quantityLimit\\n      sskMin\\n      sskMax\\n      unitOfMeasureCoverage\\n      wasMaxPriceRange\\n      wasMinPriceRange\\n      fiscalYear\\n      productDepartment\\n      classNumber\\n      forProfessionalUseOnly\\n      globalCustomConfigurator {\\n        customButtonText\\n        customDescription\\n        customExperience\\n        customExperienceUrl\\n        customTitle\\n        __typename\\n      }\\n      paintBrand\\n      movingCalculatorEligible\\n      label\\n      prop65Warning\\n      returnable\\n      recommendationFlags {\\n        visualNavigation\\n        reqItems\\n        batItems\\n        packages\\n        __typename\\n      }\\n      replacementOMSID\\n      hasSubscription\\n      minimumOrderQuantity\\n      projectCalculatorEligible\\n      subClassNumber\\n      calculatorType\\n      isLiveGoodsProduct\\n      protectionPlanSku\\n      hasServiceAddOns\\n      consultationType\\n      __typename\\n    }\\n    itemId\\n    dataSources\\n    identifiers {\\n      canonicalUrl\\n      brandName\\n      itemId\\n      modelNumber\\n      productLabel\\n      storeSkuNumber\\n      upcGtin13\\n      specialOrderSku\\n      toolRentalSkuNumber\\n      rentalCategory\\n      rentalSubCategory\\n      upc\\n      productType\\n      isSuperSku\\n      parentId\\n      roomVOEnabled\\n      sampleId\\n      __typename\\n    }\\n    availabilityType {\\n      discontinued\\n      status\\n      type\\n      buyable\\n      __typename\\n    }\\n    details {\\n      description\\n      collection {\\n        url\\n        collectionId\\n        __typename\\n      }\\n      highlights\\n      descriptiveAttributes {\\n        name\\n        value\\n        bulleted\\n        sequence\\n        __typename\\n      }\\n      infoAndGuides {\\n        name\\n        url\\n        __typename\\n      }\\n      installation {\\n        leadGenUrl\\n        __typename\\n      }\\n      __typename\\n    }\\n    media {\\n      images {\\n        url\\n        type\\n        subType\\n        sizes\\n        __typename\\n      }\\n      video {\\n        shortDescription\\n        thumbnail\\n        url\\n        videoStill\\n        link {\\n          text\\n          url\\n          __typename\\n        }\\n        title\\n        type\\n        videoId\\n        longDescription\\n        __typename\\n      }\\n      threeSixty {\\n        id\\n        url\\n        __typename\\n      }\\n      augmentedRealityLink {\\n        usdz\\n        image\\n        __typename\\n      }\\n      richContent {\\n        content\\n        displayMode\\n        richContentSource\\n        __typename\\n      }\\n      __typename\\n    }\\n    pricing(storeId: $storeId) {\\n      promotion {\\n        dates {\\n          end\\n          start\\n          __typename\\n        }\\n        type\\n        description {\\n          shortDesc\\n          longDesc\\n          __typename\\n        }\\n        dollarOff\\n        percentageOff\\n        savingsCenter\\n        savingsCenterPromos\\n        specialBuySavings\\n        specialBuyDollarOff\\n        specialBuyPercentageOff\\n        experienceTag\\n        subExperienceTag\\n        anchorItemList\\n        itemList\\n        reward {\\n          tiers {\\n            minPurchaseAmount\\n            minPurchaseQuantity\\n            rewardPercent\\n            rewardAmountPerOrder\\n            rewardAmountPerItem\\n            rewardFixedPrice\\n            __typename\\n          }\\n          __typename\\n        }\\n        nvalues\\n        brandRefinementId\\n        __typename\\n      }\\n      value\\n      alternatePriceDisplay\\n      alternate {\\n        bulk {\\n          pricePerUnit\\n          thresholdQuantity\\n          value\\n          __typename\\n        }\\n        unit {\\n          caseUnitOfMeasure\\n          unitsOriginalPrice\\n          unitsPerCase\\n          value\\n          __typename\\n        }\\n        __typename\\n      }\\n      original\\n      mapAboveOriginalPrice\\n      message\\n      preferredPriceFlag\\n      specialBuy\\n      unitOfMeasure\\n      __typename\\n    }\\n    reviews {\\n      ratingsReviews {\\n        averageRating\\n        totalReviews\\n        __typename\\n      }\\n      __typename\\n    }\\n    seo {\\n      seoKeywords\\n      seoDescription\\n      __typename\\n    }\\n    specificationGroup @skip(if: $skipSpecificationGroup) {\\n      specifications {\\n        specName\\n        specValue\\n        __typename\\n      }\\n      specTitle\\n      __typename\\n    }\\n    taxonomy {\\n      breadCrumbs {\\n        label\\n        url\\n        browseUrl\\n        creativeIconUrl\\n        deselectUrl\\n        dimensionName\\n        refinementKey\\n        __typename\\n      }\\n      brandLinkUrl\\n      __typename\\n    }\\n    favoriteDetail {\\n      count\\n      __typename\\n    }\\n    sizeAndFitDetail {\\n      attributeGroups {\\n        attributes {\\n          attributeName\\n          dimensions\\n          __typename\\n        }\\n        dimensionLabel\\n        productType\\n        __typename\\n      }\\n      __typename\\n    }\\n    subscription @skip(if: $skipSubscribeAndSave) {\\n      defaultfrequency\\n      discountPercentage\\n      subscriptionEnabled\\n      __typename\\n    }\\n    badges(storeId: $storeId) {\\n      label\\n      color\\n      creativeImageUrl\\n      endDate\\n      message\\n      name\\n      timerDuration\\n      timer {\\n        timeBombThreshold\\n        daysLeftThreshold\\n        dateDisplayThreshold\\n        message\\n        __typename\\n      }\\n      __typename\\n    }\\n    keyProductFeatures @skip(if: $skipKPF) {\\n      keyProductFeaturesItems {\\n        features {\\n          name\\n          refinementId\\n          refinementUrl\\n          value\\n          __typename\\n        }\\n        __typename\\n      }\\n      __typename\\n    }\\n    seoDescription\\n    installServices {\\n      scheduleAMeasure\\n      __typename\\n    }\\n    dataSource\\n    __typename\\n  }\\n}\\n\"}";

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
   public List<Product> extractInformation(JSONObject response) throws Exception {
      List<Product> products = new ArrayList<>();

      JSONObject productJson = JSONUtils.getValueRecursive(response, "data.product", JSONObject.class);

      if (productJson != null && !productJson.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = INTERNAL_ID;
         String name = JSONUtils.getValueRecursive(productJson, "identifiers.productLabel", String.class);
         String description = JSONUtils.getValueRecursive(productJson, "details.description", String.class);
         JSONArray images = JSONUtils.getValueRecursive(productJson, "media.images", JSONArray.class);
         String primaryImage = scrapFirstImage(images);
         List<String> secondaryImages = scrapSecondaryImages(images);
         CategoryCollection categories = scrapCategories(productJson);
         boolean available = scrapAvailability(productJson);

         Offers offers = available ? scrapOffers(productJson) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(this.session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setOffers(offers)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page:   " + this.session.getOriginalURL());
      }

      return products;
   }

   private String scrapFirstImage(JSONArray imagesArray) {
      if (imagesArray != null && imagesArray.length() > 1) {
         Object imageOptions = imagesArray.get(0);
         String primaryImageUrl = JSONUtils.getStringValue((JSONObject) imageOptions, "url");

         if (primaryImageUrl != null) {
            primaryImageUrl.replaceAll("<SIZE>", "1000");
            return primaryImageUrl.replaceAll("<SIZE>", "1000");
         }
      }
      return null;
   }

   private List<String> scrapSecondaryImages(JSONArray imagesArray) {
      List<String> secondaryImages = new ArrayList<>();

      if (imagesArray != null && imagesArray.length() > 2) {
         for (int i = 1; i < imagesArray.length(); i++) {
            Object imageOptions = imagesArray.get(i);
            String secondaryImageUrl = JSONUtils.getStringValue((JSONObject) imageOptions, "url");
            if (secondaryImageUrl != null) {
               secondaryImageUrl = secondaryImageUrl.replaceAll("<SIZE>", "1000");
               secondaryImages.add(secondaryImageUrl);
            }
         }
      }

      return secondaryImages;
   }

   private Boolean scrapAvailability(JSONObject productJson) {
      Boolean availability = JSONUtils.getValueRecursive(productJson, "availabilityType.status", Boolean.class);

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
      JSONObject pricing = productJson.optJSONObject("pricing");

      Double priceFrom = pricing.optDouble("original", 0d);
      Double spotlightPrice = pricing.optDouble("value", 0d);

      if (priceFrom == 0d) {
         priceFrom = null;
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

   protected CategoryCollection scrapCategories(JSONObject productJson) {
      CategoryCollection categories = new CategoryCollection();
      JSONArray breadcrumbs = JSONUtils.getValueRecursive(productJson, "taxonomy.breadCrumbs", JSONArray.class);

      for (Object category : breadcrumbs) {
         String categoryName = JSONUtils.getStringValue((JSONObject) category, "label");
         if (categoryName != null) categories.add(categoryName);
      }

      return categories;
   }
}
