package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import com.google.common.net.HttpHeaders;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.CreditCards;
import models.pricing.Pricing;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.*;

public class WalmartSuperCrawler extends Crawler {
   public WalmartSuperCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.JSOUP);
      super.config.setParser(Parser.HTML);
   }

   private Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString());
   private static final String HOME_PAGE = "https://super.walmart.com.mx";
   protected String SELLER_FULL_NAME = "Walmart Super Mexico";
   String store_id = session.getOptions().optString("store_id");

   public static final List<String> PROXIES = Arrays.asList(
      ProxyCollection.NETNUT_RESIDENTIAL_MX,
      ProxyCollection.NETNUT_RESIDENTIAL_ANY_HAPROXY,
      ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
      ProxyCollection.SMART_PROXY_MX_HAPROXY
   );

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   private static Map<String, String> getHeaders() {
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
      headers.put(HttpHeaders.ORIGIN, HOME_PAGE);
      headers.put(HttpHeaders.REFERER, HOME_PAGE);
      headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
      headers.put("authority", "super.walmart.com.mx");
      headers.put("accept-language", "en-US,en;q=0.9,pt;q=0.8,pt-PT;q=0.7");
      return headers;
   }

   @Override
   public void handleCookiesBeforeFetch() {
      String payload = "{\n" +
         "    \"query\": \"mutation setPickup( $input:SetFulfillmentPickupInput! $includePartialFulfillmentSwitching:Boolean! = false $enableAEBadge:Boolean! = false $enableBadges:Boolean! = false $includeQueueing:Boolean! = false $includeExpressSla:Boolean! = false $enableACCScheduling:Boolean! = false $enableWeeklyReservationCartBookslot:Boolean! = false $enableWalmartPlusFreeDiscountedExpress:Boolean! = false $enableCartBookslotShortcut:Boolean! = false $enableFutureInventoryCartBookslot:Boolean! = false $enableWplusCashback:Boolean! = false $enableExpressReservationEndTime:Boolean! = false $enableBenefitSavings:Boolean! = false $enableCartLevelMSI:Boolean! = false ){fulfillmentMutations{setPickup(input:$input){...CartFragment}}}fragment CartFragment on Cart{id checkoutable installmentDetails @include(if:$enableCartLevelMSI){bankInstallmentOptions{bank installments payments{displayValue value}minAmount{displayValue value}currencyUnit}}customer{id isGuest}cartGiftingDetails{isGiftOrder hasGiftEligibleItem isAddOnServiceAdjustmentNeeded isWalmartProtectionPlanPresent isAppleCarePresent}addressMode lineItems{id quantity quantityString quantityLabel orderedSalesUnit hasShippingRestriction isPreOrder isFutureInventoryItem @include(if:$enableFutureInventoryCartBookslot) isGiftEligible isSubstitutionSelected displayAddOnServices createdDateTime discounts{key displayValue displayLabel value terms subType}isWplusEarlyAccess isEventActive eventType selectedAddOnServices{offerId quantity groupType isGiftEligible error{code upstreamErrorCode errorMsg}}bundleComponents{offerId quantity product{name usItemId imageInfo{thumbnailUrl}}}registryId fulfillmentPreference selectedVariants{name value}priceInfo{priceDisplayCodes{showItemPrice priceDisplayCondition finalCostByWeight}itemPrice{...lineItemPriceInfoFragment}wasPrice{...lineItemPriceInfoFragment}unitPrice{...lineItemPriceInfoFragment}linePrice{...lineItemPriceInfoFragment}savedPrice{...lineItemPriceInfoFragment}tierPrice priceChange{priceChangeIndicator oldItemPrice{...lineItemPriceInfoFragment}priceDifference{...lineItemPriceInfoFragment}}}promotions{name promoId awards{match metadata{minQty maxOffer amount}}}product{id name usItemId isSubstitutionEligible personalizable sponsoredProduct{spQs clickBeacon spTags}sellerDisplayName fulfillmentBadge variants{availabilityStatus}seller{name sellerId}imageInfo{thumbnailUrl}addOnServices{serviceType serviceTitle serviceSubTitle groups{groupType groupTitle assetUrl shortDescription unavailabilityReason services{displayName selectedDisplayName offerId usItemId currentPrice{priceString price}serviceMetaData giftEligible}}}itemType offerId sellerId sellerName hasSellerBadge orderLimit orderMinLimit weightUnit weightIncrement salesUnit salesUnitType sellerType isAlcohol fulfillmentType fulfillmentSpeed fulfillmentTitle classType rhPath availabilityStatus brand category{categoryPath}departmentName configuration snapEligible preOrder{isPreOrder}badges @include(if:$enableBadges){...BadgesFragment}shopSimilar}registryInfo{registryId registryType}personalizedItemDetails{personalizedConfigID personalizedConfigAttributes{name value}}wirelessPlan{planId mobileNumber postPaidPlan{...postpaidPlanDetailsFragment}}fulfillmentSourcingDetails{currentSelection requestedSelection fulfillmentBadge}availableQty expiresAt @include(if:$includeQueueing) showExpirationTimer @include(if:$includeQueueing) isFutureInventoryItem @include(if:$enableFutureInventoryCartBookslot) futureInventoryAvailability @include(if:$enableFutureInventoryCartBookslot){pickup{itemAvailableFrom}delivery{itemAvailableFrom}}isPetRxItem}priceChange{totalItemCount priceDropItemDetails{itemCount itemsIds}priceUpItemDetails{itemCount itemsIds}}fulfillment{isExplicitIntent intent assortmentIntent deliveryStore{isExpressDeliveryOnly storeFeeTier storeId}pickupStore{storeId addressLineOne city stateCode countryCode postalCode storeFeeTier}accessPoint{...accessPointCartFragment}reservation{...reservationFragment}storeId displayStoreSnackBarMessage homepageBookslotDetails{title subTitle expiryText expiryTime slotExpiryText bannerType @include(if:$enableWeeklyReservationCartBookslot) fulfillmentOption @include(if:$enableWeeklyReservationCartBookslot) weeklyReservationFulfillmentDetails @include(if:$enableWeeklyReservationCartBookslot){addressId accessPointId postalCode storeId}}deliveryAddress{addressLineOne addressLineTwo city state postalCode firstName lastName id phone}fulfillmentItemGroups{...on FCGroup{__typename groupId defaultMode collapsedItemIds startDate endDate checkoutable checkoutableErrors{code shouldDisableCheckout itemIds upstreamErrors{offerId upstreamErrorCode}}priceDetails{subTotal{...priceTotalFields}}fulfillmentSwitchInfo{fulfillmentType benefit{type price itemCount date isWalmartPlusProgram}partialItemIds @include(if:$includePartialFulfillmentSwitching)}shippingOptions{__typename itemIds availableShippingOptions{__typename id shippingMethod deliveryDate price{__typename displayValue value}label{prefix suffix}isSelected isDefault slaTier}}hasMadeShippingChanges slaGroups{__typename label deliveryDate sellerGroups{__typename id name isProSeller type catalogSellerId shipOptionGroup{__typename deliveryPrice{__typename displayValue value}itemIds shipMethod}}warningLabel}hasFulfillmentCharges}...on SCGroup{__typename groupId defaultMode availableSlots @include(if:$enableCartBookslotShortcut){...SlotsFragment}slotIntent @include(if:$enableCartBookslotShortcut) hasInHomeSlot @include(if:$enableCartBookslotShortcut) collapsedItemIds checkoutable checkoutableErrors{code shouldDisableCheckout itemIds upstreamErrors{offerId upstreamErrorCode}}priceDetails{subTotal{...priceTotalFields}}fulfillmentSwitchInfo{fulfillmentType benefit{type price itemCount date isWalmartPlusProgram}partialItemIds @include(if:$includePartialFulfillmentSwitching)}itemGroups{__typename label itemIds accessPoint{...accessPointCartFragment}}accessPoint{...accessPointCartFragment}reservation{...reservationFragment}noReservationSubTitle @include(if:$enableWalmartPlusFreeDiscountedExpress) hasFulfillmentCharges}...on DigitalDeliveryGroup{__typename groupId defaultMode collapsedItemIds checkoutable checkoutableErrors{code shouldDisableCheckout itemIds upstreamErrors{offerId upstreamErrorCode}}priceDetails{subTotal{...priceTotalFields}}itemGroups{__typename label itemIds accessPoint{...accessPointCartFragment}}}...on Unscheduled{__typename groupId defaultMode collapsedItemIds checkoutable checkoutableErrors{code shouldDisableCheckout itemIds upstreamErrors{offerId upstreamErrorCode}}priceDetails{subTotal{...priceTotalFields}}itemGroups{__typename label itemIds accessPoint{...accessPointCartFragment}}accessPoint{...accessPointCartFragment}reservation{...reservationFragment}fulfillmentSwitchInfo{fulfillmentType benefit{type price itemCount date isWalmartPlusProgram}partialItemIds @include(if:$includePartialFulfillmentSwitching)}isSpecialEvent @include(if:$enableAEBadge) hasFulfillmentCharges}...on AutoCareCenter{__typename groupId defaultMode collapsedItemIds startDate endDate accBasketType checkoutable checkoutableErrors{code shouldDisableCheckout itemIds upstreamErrors{offerId upstreamErrorCode}}priceDetails{subTotal{...priceTotalFields}}itemGroups{__typename label itemIds accessPoint{...accessPointCartFragment}}accFulfillmentGroups @include(if:$enableACCScheduling){collapsedItemIds itemGroupType reservation{...reservationFragment}suggestedSlotAvailability{...suggestedSlotAvailabilityFragment}itemGroups{__typename label itemIds}}accessPoint{...accessPointCartFragment}reservation{...reservationFragment}fulfillmentSwitchInfo{fulfillmentType benefit{type price itemCount date isWalmartPlusProgram}partialItemIds @include(if:$includePartialFulfillmentSwitching)}hasFulfillmentCharges}...on MPGroup{__typename groupId sellerId sellerName defaultMode collapsedItemIds checkoutable checkoutableErrors{code shouldDisableCheckout itemIds upstreamErrors{offerId upstreamErrorCode}}priceDetails{subTotal{...priceTotalFields}}itemGroups{__typename label itemIds accessPoint{...accessPointCartFragment}}accessPoint{...accessPointCartFragment}reservation{...reservationFragment}fulfillmentSwitchInfo{fulfillmentType benefit{type price itemCount date isWalmartPlusProgram}partialItemIds @include(if:$includePartialFulfillmentSwitching)}hasFulfillmentCharges}}suggestedSlotAvailability{...suggestedSlotAvailabilityFragment}}priceDetails{subTotal{...priceTotalFields}fees{...priceTotalFields}taxTotal{...priceTotalFields}grandTotal{...priceTotalFields}belowMinimumFee{...priceTotalFields}savedPriceSubTotal{...priceTotalFields}originalSubTotal{...priceTotalFields}minimumThreshold{value displayValue}ebtSnapMaxEligible{displayValue value}balanceToMinimumThreshold{value displayValue}totalItemQuantity rewards{totalCashBack{displayValue value}strikeOut{displayValue value}displayMsg subType promotions @include(if:$enableWplusCashback){type subtype cashback{displayValue value}strikeOut{displayValue value}description cashBackData{type value}}}discounts{...PriceDetailRowFields}}affirm{isMixedPromotionCart message{description termsUrl imageUrl monthlyPayment termLength isZeroAPR}nonAffirmGroup{...nonAffirmGroupFields}affirmGroups{...on AffirmItemGroup{__typename message{description termsUrl imageUrl monthlyPayment termLength isZeroAPR}flags{type displayLabel}name label itemCount itemIds defaultMode}}}toastWarning{displayText warningValue}checkoutableErrors{code shouldDisableCheckout itemIds upstreamErrors{offerId upstreamErrorCode}}checkoutableWarnings{code itemIds}operationalErrors{offerId itemId requestedQuantity adjustedQuantity code upstreamErrorCode}cartCustomerContext{...cartCustomerContextFragment}}fragment postpaidPlanDetailsFragment on PostPaidPlan{espOrderSummaryId espOrderId espOrderLineId warpOrderId warpSessionId isPostpaidExpired devicePayment{...postpaidPlanPriceFragment}devicePlan{price{...postpaidPlanPriceFragment}frequency duration annualPercentageRate}deviceDataPlan{...deviceDataPlanFragment}}fragment deviceDataPlanFragment on DeviceDataPlan{carrierName planType expiryTime activationFee{...postpaidPlanPriceFragment}planDetails{price{...postpaidPlanPriceFragment}frequency name}agreements{...agreementFragment}}fragment postpaidPlanPriceFragment on PriceDetailRow{key label displayValue value strikeOutDisplayValue strikeOutValue info{title message}}fragment agreementFragment on CarrierAgreement{name type format value docTitle label}fragment priceTotalFields on PriceDetailRow{label displayValue value key strikeOutDisplayValue strikeOutValue program @include(if:$enableWalmartPlusFreeDiscountedExpress)}fragment lineItemPriceInfoFragment on Price{displayValue value}fragment accessPointCartFragment on AccessPoint{id assortmentStoreId name nodeAccessType accessType fulfillmentType fulfillmentOption marketType displayName timeZone bagFeeValue isActive address{addressLineOne addressLineTwo city postalCode state phone}}fragment suggestedSlotAvailabilityFragment on SuggestedSlotAvailability{isPickupAvailable isDeliveryAvailable nextPickupSlot{startTime endTime slaInMins}nextDeliverySlot{startTime endTime slaInMins}nextUnscheduledPickupSlot{startTime endTime slaInMins}nextSlot{__typename...on RegularSlot{fulfillmentOption fulfillmentType startTime}...on DynamicExpressSlot{fulfillmentOption fulfillmentType startTime slaInMins sla{value displayValue}}...on UnscheduledSlot{fulfillmentOption fulfillmentType startTime unscheduledHoldInDays}...on InHomeSlot{fulfillmentOption fulfillmentType startTime}}}fragment reservationFragment on Reservation{expiryTime isUnscheduled isWeeklyReservation @include(if:$enableWeeklyReservationCartBookslot) expired showSlotExpiredError reservedSlot{__typename...on RegularSlot{id price{total{value displayValue}expressFee{value displayValue}baseFee{value displayValue}memberBaseFee{value displayValue}totalSavings{displayValue}baseExpressFee @include(if:$enableWalmartPlusFreeDiscountedExpress){displayValue}}nodeAccessType accessPointId fulfillmentOption startTime fulfillmentType slotMetadata endTime available supportedTimeZone isAlcoholRestricted isPopular storeFeeTier}...on DynamicExpressSlot{id price{total{value displayValue}expressFee{value displayValue}baseFee{value displayValue}memberBaseFee{value displayValue}totalSavings{displayValue}baseExpressFee @include(if:$enableWalmartPlusFreeDiscountedExpress){displayValue}}accessPointId fulfillmentOption startTime endTime @include(if:$enableExpressReservationEndTime) fulfillmentType slotMetadata available sla @include(if:$includeExpressSla){value displayValue}slaInMins maxItemAllowed supportedTimeZone isAlcoholRestricted storeFeeTier}...on UnscheduledSlot{price{total{value displayValue}expressFee{value displayValue}baseFee{value displayValue}memberBaseFee{value displayValue}totalSavings{displayValue}baseExpressFee @include(if:$enableWalmartPlusFreeDiscountedExpress){displayValue}}accessPointId fulfillmentOption startTime fulfillmentType slotMetadata unscheduledHoldInDays supportedTimeZone}...on InHomeSlot{id price{total{value displayValue}expressFee{value displayValue}baseFee{value displayValue}memberBaseFee{value displayValue}totalSavings{displayValue}baseExpressFee @include(if:$enableWalmartPlusFreeDiscountedExpress){displayValue}}accessPointId fulfillmentOption startTime fulfillmentType slotMetadata endTime available supportedTimeZone isAlcoholRestricted}}}fragment nonAffirmGroupFields on NonAffirmGroup{label itemCount itemIds collapsedItemIds}fragment cartCustomerContextFragment on CartCustomerContext{isMembershipOptedIn isEligibleForFreeTrial membershipData{isActiveMember isPaidMember}paymentData{hasCreditCard hasCapOne hasDSCard hasEBT isCapOneLinked showCapOneBanner wplusNoBenefitBanner hasBenefitMembership @include(if:$enableBenefitSavings)}}fragment BadgesFragment on UnifiedBadge{flags{__typename...on BaseBadge{id text key type query}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought criteria{name value}}}labels{__typename...on BaseBadge{id text key}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought}}tags{__typename...on BaseBadge{id text key}...on PreviouslyPurchasedBadge{id text key}}}fragment SlotsFragment on AvailableSlot{...on RegularSlot{__typename accessPointId nodeAccessType cartFulfillmentOption price{baseFee{displayValue}originaltotal{value displayValue}total{value displayValue}optedInTotal{displayValue}memberBaseFee{displayValue}memberExpressDiscount{value displayValue}}slotMetadata startTime supportedTimeZone id allowedAmendTime available endTime isAlcoholRestricted isVulnerable slotExpiryTime slotIndicator isPrimary slotsIds isPopular isWeeklyReserved}...on InHomeSlot{__typename accessPointId nodeAccessType cartFulfillmentOption price{baseFee{displayValue}originaltotal{value displayValue}total{value displayValue}optedInTotal{displayValue}memberBaseFee{displayValue}memberExpressDiscount{value displayValue}}slotMetadata startTime supportedTimeZone id allowedAmendTime available endTime isAlcoholRestricted isVulnerable slotExpiryTime slotIndicator isPrimary}...on DynamicExpressSlot{__typename accessPointId nodeAccessType cartFulfillmentOption price{baseFee{displayValue}originaltotal{value displayValue}total{value displayValue}optedInTotal{displayValue}memberBaseFee{displayValue}memberExpressDiscount{value displayValue}}slotMetadata startTime supportedTimeZone id allowedAmendTime available endTime isAlcoholRestricted isVulnerable slotExpiryTime slotIndicator isPrimary isSelectable maxItemAllowed sla{value displayValue}}}fragment PriceDetailRowFields on PriceDetailRow{__typename key label displayValue}\",\n" +
         "    \"variables\": {\n" +
         "        \"input\": {\n" +
         "            \"enableLiquorBox\": false,\n" +
         "            \"accessPointId\":\"" + session.getOptions().optString("accessPointId") + "\",\n" +
         "            \"cartId\":\"" + session.getOptions().optString("cartId") + "\",\n" +
         "            \"postalCode\":\"" + session.getOptions().optString("postalCode") + "\",\n" +
         "            \"storeId\":" + Integer.parseInt(store_id) + ",\n" +
         "            \"marketType\": \"XP\"\n" +
         "        },\n" +
         "        \"includePartialFulfillmentSwitching\": false,\n" +
         "        \"enableAEBadge\": true,\n" +
         "        \"enableBadges\": true,\n" +
         "        \"includeExpressSla\": true,\n" +
         "        \"includeQueueing\": true,\n" +
         "        \"enableWeeklyReservationCartBookslot\": true,\n" +
         "        \"enableACCScheduling\": true,\n" +
         "        \"enableWalmartPlusFreeDiscountedExpress\": true,\n" +
         "        \"enableCartBookslotShortcut\": false,\n" +
         "        \"enableFutureInventoryCartBookslot\": false,\n" +
         "        \"enableWplusCashback\": false,\n" +
         "        \"enableBenefitSavings\": false,\n" +
         "        \"enableCartLevelMSI\": true\n" +
         "    }\n" +
         "}";

      Request request = Request.RequestBuilder.create()
         .setUrl(HOME_PAGE + "/orchestra/graphql")
         .setPayload(payload)
         .setHeaders(getHeaders())
         .setProxyservice(PROXIES)
         .build();
      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher()), session, "post");

      for (Cookie cookie : response.getCookies()) {
         BasicClientCookie basicClientCookie = new BasicClientCookie(cookie.getName(), cookie.getValue());
         basicClientCookie.setDomain(".walmart.com.mx");
         this.cookies.add(basicClientCookie);
      }
   }

   @Override
   protected Response fetchResponse() {
      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setHeaders(getHeaders())
         .setCookies(this.cookies)
         .setProxyservice(PROXIES)
         .build();

      return CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher()), session, "get");
   }

   JSONObject getJsonFromHtml(Document doc) {
      String script = CrawlerUtils.scrapScriptFromHtml(doc, "#__NEXT_DATA__");
      JSONArray scriptArray = JSONUtils.stringToJsonArray(script);

      if (scriptArray == null || scriptArray.isEmpty()) {
         return new JSONObject();
      }

      Object json = scriptArray.get(0);
      JSONObject jsonObject = (JSONObject) json;
      return JSONUtils.getValueRecursive(jsonObject, "props.pageProps.initialData.data", JSONObject.class, new JSONObject());
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();
      JSONObject json = getJsonFromHtml(doc);
      JSONObject productJson = json.optJSONObject("product");

      if (!json.isEmpty() && productJson.has("usItemId")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String internalId = productJson.optString("usItemId");
         String name = productJson.optString("name");
         String primaryImage = JSONUtils.getValueRecursive(productJson, "imageInfo.thumbnailUrl", String.class, null);
         List<String> secondaryImages = crawlSecondaryImages(productJson, primaryImage);
         List<String> categories = crawlCategories(productJson);
         String description = crawlDescription(json);
         List<String> eans = new ArrayList<>();
         eans.add(internalId);
         boolean available = crawlAvailability(productJson);
         Offers offers = available ? crawlOffers(productJson) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setEans(eans)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private Offers crawlOffers(JSONObject json) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(json);
      List<String> sales = new ArrayList<>();

      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setSales(sales)
         .setPricing(pricing)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject json) throws MalformedPricingException {
      JSONObject spotlightPriceObject = JSONUtils.getValueRecursive(json, "priceInfo.currentPrice", JSONObject.class, new JSONObject());
      JSONObject priceFromObject = JSONUtils.getValueRecursive(json, "priceInfo.wasPrice", JSONObject.class, new JSONObject());
      Double spotlightPrice = null;
      Double priceFrom = null;

      if (!spotlightPriceObject.isEmpty()) {
         int priceInt = spotlightPriceObject.optInt("price", 0);
         spotlightPrice = priceInt != 0 ? priceInt * 1.0 : null;
      }
      if (!priceFromObject.isEmpty()) {
         int priceInt = priceFromObject.optInt("price", 0);
         priceFrom = priceInt != 0 ? priceInt * 1.0 : null;
      }

      CreditCards creditCards = CrawlerUtils.scrapCreditCards(spotlightPrice, cards);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();
   }


   private boolean crawlAvailability(JSONObject apiJson) {
      return apiJson.optString("availabilityStatus").equals("IN_STOCK");
   }

   private List<String> crawlSecondaryImages(JSONObject json, String primaryImage) {
      List<String> secondaryImages = new ArrayList<>();
      JSONArray images = JSONUtils.getValueRecursive(json, "imageInfo.allImages", JSONArray.class, new JSONArray());

      for (Object objImage : images) {
         JSONObject imageObject = (JSONObject) objImage;
         String image = imageObject.optString("url", "");
         if (!image.isEmpty()) {
            secondaryImages.add(image);
         }
      }

      if (primaryImage != null) {
         secondaryImages.remove(primaryImage);
      }

      return secondaryImages;
   }

   private CategoryCollection crawlCategories(JSONObject json) {
      CategoryCollection categories = new CategoryCollection();
      JSONArray pathsCategories = JSONUtils.getValueRecursive(json, "category.path", JSONArray.class, new JSONArray());

      for (Object objCategory : pathsCategories) {
         JSONObject categoryObject = (JSONObject) objCategory;
         String category = categoryObject.optString("name", "");
         if (!category.isEmpty()) {
            categories.add(category);
         }
      }

      return categories;
   }

   private String crawlDescription(JSONObject json) {
      StringBuilder description = new StringBuilder();
      String longDescription = JSONUtils.getValueRecursive(json, "idml.longDescription", String.class, "");
      String shortDescription = JSONUtils.getValueRecursive(json, "idml.shortDescription", String.class, "");

      if (longDescription != null && !longDescription.isEmpty()) {
         description.append(longDescription);
      }
      if (shortDescription != null && !shortDescription.isEmpty()) {
         description.append(longDescription);
      }

      return description.toString();
   }
}
