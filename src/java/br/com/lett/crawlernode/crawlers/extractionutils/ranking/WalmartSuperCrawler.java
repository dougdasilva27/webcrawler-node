package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import com.google.common.net.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WalmartSuperCrawler extends CrawlerRankingKeywords {

   public WalmartSuperCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.JSOUP;
   }

   protected String HOME_PAGE = "https://super.walmart.com.mx";
   public static final List<String> PROXIES = Arrays.asList(
      ProxyCollection.NETNUT_RESIDENTIAL_MX,
      ProxyCollection.NETNUT_RESIDENTIAL_ANY_HAPROXY,
      ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
      ProxyCollection.SMART_PROXY_MX_HAPROXY
   );

   private Map<String, String> getHeaders() {
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
   protected void processBeforeFetch() {
      String payload = "{\n" +
         "    \"query\": \"mutation setPickup( $input:SetFulfillmentPickupInput! $includePartialFulfillmentSwitching:Boolean! = false $enableAEBadge:Boolean! = false $enableBadges:Boolean! = false $includeQueueing:Boolean! = false $includeExpressSla:Boolean! = false $enableACCScheduling:Boolean! = false $enableWeeklyReservationCartBookslot:Boolean! = false $enableWalmartPlusFreeDiscountedExpress:Boolean! = false $enableCartBookslotShortcut:Boolean! = false $enableFutureInventoryCartBookslot:Boolean! = false $enableWplusCashback:Boolean! = false $enableExpressReservationEndTime:Boolean! = false $enableBenefitSavings:Boolean! = false $enableCartLevelMSI:Boolean! = false ){fulfillmentMutations{setPickup(input:$input){...CartFragment}}}fragment CartFragment on Cart{id checkoutable installmentDetails @include(if:$enableCartLevelMSI){bankInstallmentOptions{bank installments payments{displayValue value}minAmount{displayValue value}currencyUnit}}customer{id isGuest}cartGiftingDetails{isGiftOrder hasGiftEligibleItem isAddOnServiceAdjustmentNeeded isWalmartProtectionPlanPresent isAppleCarePresent}addressMode lineItems{id quantity quantityString quantityLabel orderedSalesUnit hasShippingRestriction isPreOrder isFutureInventoryItem @include(if:$enableFutureInventoryCartBookslot) isGiftEligible isSubstitutionSelected displayAddOnServices createdDateTime discounts{key displayValue displayLabel value terms subType}isWplusEarlyAccess isEventActive eventType selectedAddOnServices{offerId quantity groupType isGiftEligible error{code upstreamErrorCode errorMsg}}bundleComponents{offerId quantity product{name usItemId imageInfo{thumbnailUrl}}}registryId fulfillmentPreference selectedVariants{name value}priceInfo{priceDisplayCodes{showItemPrice priceDisplayCondition finalCostByWeight}itemPrice{...lineItemPriceInfoFragment}wasPrice{...lineItemPriceInfoFragment}unitPrice{...lineItemPriceInfoFragment}linePrice{...lineItemPriceInfoFragment}savedPrice{...lineItemPriceInfoFragment}tierPrice priceChange{priceChangeIndicator oldItemPrice{...lineItemPriceInfoFragment}priceDifference{...lineItemPriceInfoFragment}}}promotions{name promoId awards{match metadata{minQty maxOffer amount}}}product{id name usItemId isSubstitutionEligible personalizable sponsoredProduct{spQs clickBeacon spTags}sellerDisplayName fulfillmentBadge variants{availabilityStatus}seller{name sellerId}imageInfo{thumbnailUrl}addOnServices{serviceType serviceTitle serviceSubTitle groups{groupType groupTitle assetUrl shortDescription unavailabilityReason services{displayName selectedDisplayName offerId usItemId currentPrice{priceString price}serviceMetaData giftEligible}}}itemType offerId sellerId sellerName hasSellerBadge orderLimit orderMinLimit weightUnit weightIncrement salesUnit salesUnitType sellerType isAlcohol fulfillmentType fulfillmentSpeed fulfillmentTitle classType rhPath availabilityStatus brand category{categoryPath}departmentName configuration snapEligible preOrder{isPreOrder}badges @include(if:$enableBadges){...BadgesFragment}shopSimilar}registryInfo{registryId registryType}personalizedItemDetails{personalizedConfigID personalizedConfigAttributes{name value}}wirelessPlan{planId mobileNumber postPaidPlan{...postpaidPlanDetailsFragment}}fulfillmentSourcingDetails{currentSelection requestedSelection fulfillmentBadge}availableQty expiresAt @include(if:$includeQueueing) showExpirationTimer @include(if:$includeQueueing) isFutureInventoryItem @include(if:$enableFutureInventoryCartBookslot) futureInventoryAvailability @include(if:$enableFutureInventoryCartBookslot){pickup{itemAvailableFrom}delivery{itemAvailableFrom}}isPetRxItem}priceChange{totalItemCount priceDropItemDetails{itemCount itemsIds}priceUpItemDetails{itemCount itemsIds}}fulfillment{isExplicitIntent intent assortmentIntent deliveryStore{isExpressDeliveryOnly storeFeeTier storeId}pickupStore{storeId addressLineOne city stateCode countryCode postalCode storeFeeTier}accessPoint{...accessPointCartFragment}reservation{...reservationFragment}storeId displayStoreSnackBarMessage homepageBookslotDetails{title subTitle expiryText expiryTime slotExpiryText bannerType @include(if:$enableWeeklyReservationCartBookslot) fulfillmentOption @include(if:$enableWeeklyReservationCartBookslot) weeklyReservationFulfillmentDetails @include(if:$enableWeeklyReservationCartBookslot){addressId accessPointId postalCode storeId}}deliveryAddress{addressLineOne addressLineTwo city state postalCode firstName lastName id phone}fulfillmentItemGroups{...on FCGroup{__typename groupId defaultMode collapsedItemIds startDate endDate checkoutable checkoutableErrors{code shouldDisableCheckout itemIds upstreamErrors{offerId upstreamErrorCode}}priceDetails{subTotal{...priceTotalFields}}fulfillmentSwitchInfo{fulfillmentType benefit{type price itemCount date isWalmartPlusProgram}partialItemIds @include(if:$includePartialFulfillmentSwitching)}shippingOptions{__typename itemIds availableShippingOptions{__typename id shippingMethod deliveryDate price{__typename displayValue value}label{prefix suffix}isSelected isDefault slaTier}}hasMadeShippingChanges slaGroups{__typename label deliveryDate sellerGroups{__typename id name isProSeller type catalogSellerId shipOptionGroup{__typename deliveryPrice{__typename displayValue value}itemIds shipMethod}}warningLabel}hasFulfillmentCharges}...on SCGroup{__typename groupId defaultMode availableSlots @include(if:$enableCartBookslotShortcut){...SlotsFragment}slotIntent @include(if:$enableCartBookslotShortcut) hasInHomeSlot @include(if:$enableCartBookslotShortcut) collapsedItemIds checkoutable checkoutableErrors{code shouldDisableCheckout itemIds upstreamErrors{offerId upstreamErrorCode}}priceDetails{subTotal{...priceTotalFields}}fulfillmentSwitchInfo{fulfillmentType benefit{type price itemCount date isWalmartPlusProgram}partialItemIds @include(if:$includePartialFulfillmentSwitching)}itemGroups{__typename label itemIds accessPoint{...accessPointCartFragment}}accessPoint{...accessPointCartFragment}reservation{...reservationFragment}noReservationSubTitle @include(if:$enableWalmartPlusFreeDiscountedExpress) hasFulfillmentCharges}...on DigitalDeliveryGroup{__typename groupId defaultMode collapsedItemIds checkoutable checkoutableErrors{code shouldDisableCheckout itemIds upstreamErrors{offerId upstreamErrorCode}}priceDetails{subTotal{...priceTotalFields}}itemGroups{__typename label itemIds accessPoint{...accessPointCartFragment}}}...on Unscheduled{__typename groupId defaultMode collapsedItemIds checkoutable checkoutableErrors{code shouldDisableCheckout itemIds upstreamErrors{offerId upstreamErrorCode}}priceDetails{subTotal{...priceTotalFields}}itemGroups{__typename label itemIds accessPoint{...accessPointCartFragment}}accessPoint{...accessPointCartFragment}reservation{...reservationFragment}fulfillmentSwitchInfo{fulfillmentType benefit{type price itemCount date isWalmartPlusProgram}partialItemIds @include(if:$includePartialFulfillmentSwitching)}isSpecialEvent @include(if:$enableAEBadge) hasFulfillmentCharges}...on AutoCareCenter{__typename groupId defaultMode collapsedItemIds startDate endDate accBasketType checkoutable checkoutableErrors{code shouldDisableCheckout itemIds upstreamErrors{offerId upstreamErrorCode}}priceDetails{subTotal{...priceTotalFields}}itemGroups{__typename label itemIds accessPoint{...accessPointCartFragment}}accFulfillmentGroups @include(if:$enableACCScheduling){collapsedItemIds itemGroupType reservation{...reservationFragment}suggestedSlotAvailability{...suggestedSlotAvailabilityFragment}itemGroups{__typename label itemIds}}accessPoint{...accessPointCartFragment}reservation{...reservationFragment}fulfillmentSwitchInfo{fulfillmentType benefit{type price itemCount date isWalmartPlusProgram}partialItemIds @include(if:$includePartialFulfillmentSwitching)}hasFulfillmentCharges}...on MPGroup{__typename groupId sellerId sellerName defaultMode collapsedItemIds checkoutable checkoutableErrors{code shouldDisableCheckout itemIds upstreamErrors{offerId upstreamErrorCode}}priceDetails{subTotal{...priceTotalFields}}itemGroups{__typename label itemIds accessPoint{...accessPointCartFragment}}accessPoint{...accessPointCartFragment}reservation{...reservationFragment}fulfillmentSwitchInfo{fulfillmentType benefit{type price itemCount date isWalmartPlusProgram}partialItemIds @include(if:$includePartialFulfillmentSwitching)}hasFulfillmentCharges}}suggestedSlotAvailability{...suggestedSlotAvailabilityFragment}}priceDetails{subTotal{...priceTotalFields}fees{...priceTotalFields}taxTotal{...priceTotalFields}grandTotal{...priceTotalFields}belowMinimumFee{...priceTotalFields}savedPriceSubTotal{...priceTotalFields}originalSubTotal{...priceTotalFields}minimumThreshold{value displayValue}ebtSnapMaxEligible{displayValue value}balanceToMinimumThreshold{value displayValue}totalItemQuantity rewards{totalCashBack{displayValue value}strikeOut{displayValue value}displayMsg subType promotions @include(if:$enableWplusCashback){type subtype cashback{displayValue value}strikeOut{displayValue value}description cashBackData{type value}}}discounts{...PriceDetailRowFields}}affirm{isMixedPromotionCart message{description termsUrl imageUrl monthlyPayment termLength isZeroAPR}nonAffirmGroup{...nonAffirmGroupFields}affirmGroups{...on AffirmItemGroup{__typename message{description termsUrl imageUrl monthlyPayment termLength isZeroAPR}flags{type displayLabel}name label itemCount itemIds defaultMode}}}toastWarning{displayText warningValue}checkoutableErrors{code shouldDisableCheckout itemIds upstreamErrors{offerId upstreamErrorCode}}checkoutableWarnings{code itemIds}operationalErrors{offerId itemId requestedQuantity adjustedQuantity code upstreamErrorCode}cartCustomerContext{...cartCustomerContextFragment}}fragment postpaidPlanDetailsFragment on PostPaidPlan{espOrderSummaryId espOrderId espOrderLineId warpOrderId warpSessionId isPostpaidExpired devicePayment{...postpaidPlanPriceFragment}devicePlan{price{...postpaidPlanPriceFragment}frequency duration annualPercentageRate}deviceDataPlan{...deviceDataPlanFragment}}fragment deviceDataPlanFragment on DeviceDataPlan{carrierName planType expiryTime activationFee{...postpaidPlanPriceFragment}planDetails{price{...postpaidPlanPriceFragment}frequency name}agreements{...agreementFragment}}fragment postpaidPlanPriceFragment on PriceDetailRow{key label displayValue value strikeOutDisplayValue strikeOutValue info{title message}}fragment agreementFragment on CarrierAgreement{name type format value docTitle label}fragment priceTotalFields on PriceDetailRow{label displayValue value key strikeOutDisplayValue strikeOutValue program @include(if:$enableWalmartPlusFreeDiscountedExpress)}fragment lineItemPriceInfoFragment on Price{displayValue value}fragment accessPointCartFragment on AccessPoint{id assortmentStoreId name nodeAccessType accessType fulfillmentType fulfillmentOption marketType displayName timeZone bagFeeValue isActive address{addressLineOne addressLineTwo city postalCode state phone}}fragment suggestedSlotAvailabilityFragment on SuggestedSlotAvailability{isPickupAvailable isDeliveryAvailable nextPickupSlot{startTime endTime slaInMins}nextDeliverySlot{startTime endTime slaInMins}nextUnscheduledPickupSlot{startTime endTime slaInMins}nextSlot{__typename...on RegularSlot{fulfillmentOption fulfillmentType startTime}...on DynamicExpressSlot{fulfillmentOption fulfillmentType startTime slaInMins sla{value displayValue}}...on UnscheduledSlot{fulfillmentOption fulfillmentType startTime unscheduledHoldInDays}...on InHomeSlot{fulfillmentOption fulfillmentType startTime}}}fragment reservationFragment on Reservation{expiryTime isUnscheduled isWeeklyReservation @include(if:$enableWeeklyReservationCartBookslot) expired showSlotExpiredError reservedSlot{__typename...on RegularSlot{id price{total{value displayValue}expressFee{value displayValue}baseFee{value displayValue}memberBaseFee{value displayValue}totalSavings{displayValue}baseExpressFee @include(if:$enableWalmartPlusFreeDiscountedExpress){displayValue}}nodeAccessType accessPointId fulfillmentOption startTime fulfillmentType slotMetadata endTime available supportedTimeZone isAlcoholRestricted isPopular storeFeeTier}...on DynamicExpressSlot{id price{total{value displayValue}expressFee{value displayValue}baseFee{value displayValue}memberBaseFee{value displayValue}totalSavings{displayValue}baseExpressFee @include(if:$enableWalmartPlusFreeDiscountedExpress){displayValue}}accessPointId fulfillmentOption startTime endTime @include(if:$enableExpressReservationEndTime) fulfillmentType slotMetadata available sla @include(if:$includeExpressSla){value displayValue}slaInMins maxItemAllowed supportedTimeZone isAlcoholRestricted storeFeeTier}...on UnscheduledSlot{price{total{value displayValue}expressFee{value displayValue}baseFee{value displayValue}memberBaseFee{value displayValue}totalSavings{displayValue}baseExpressFee @include(if:$enableWalmartPlusFreeDiscountedExpress){displayValue}}accessPointId fulfillmentOption startTime fulfillmentType slotMetadata unscheduledHoldInDays supportedTimeZone}...on InHomeSlot{id price{total{value displayValue}expressFee{value displayValue}baseFee{value displayValue}memberBaseFee{value displayValue}totalSavings{displayValue}baseExpressFee @include(if:$enableWalmartPlusFreeDiscountedExpress){displayValue}}accessPointId fulfillmentOption startTime fulfillmentType slotMetadata endTime available supportedTimeZone isAlcoholRestricted}}}fragment nonAffirmGroupFields on NonAffirmGroup{label itemCount itemIds collapsedItemIds}fragment cartCustomerContextFragment on CartCustomerContext{isMembershipOptedIn isEligibleForFreeTrial membershipData{isActiveMember isPaidMember}paymentData{hasCreditCard hasCapOne hasDSCard hasEBT isCapOneLinked showCapOneBanner wplusNoBenefitBanner hasBenefitMembership @include(if:$enableBenefitSavings)}}fragment BadgesFragment on UnifiedBadge{flags{__typename...on BaseBadge{id text key type query}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought criteria{name value}}}labels{__typename...on BaseBadge{id text key}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought}}tags{__typename...on BaseBadge{id text key}...on PreviouslyPurchasedBadge{id text key}}}fragment SlotsFragment on AvailableSlot{...on RegularSlot{__typename accessPointId nodeAccessType cartFulfillmentOption price{baseFee{displayValue}originaltotal{value displayValue}total{value displayValue}optedInTotal{displayValue}memberBaseFee{displayValue}memberExpressDiscount{value displayValue}}slotMetadata startTime supportedTimeZone id allowedAmendTime available endTime isAlcoholRestricted isVulnerable slotExpiryTime slotIndicator isPrimary slotsIds isPopular isWeeklyReserved}...on InHomeSlot{__typename accessPointId nodeAccessType cartFulfillmentOption price{baseFee{displayValue}originaltotal{value displayValue}total{value displayValue}optedInTotal{displayValue}memberBaseFee{displayValue}memberExpressDiscount{value displayValue}}slotMetadata startTime supportedTimeZone id allowedAmendTime available endTime isAlcoholRestricted isVulnerable slotExpiryTime slotIndicator isPrimary}...on DynamicExpressSlot{__typename accessPointId nodeAccessType cartFulfillmentOption price{baseFee{displayValue}originaltotal{value displayValue}total{value displayValue}optedInTotal{displayValue}memberBaseFee{displayValue}memberExpressDiscount{value displayValue}}slotMetadata startTime supportedTimeZone id allowedAmendTime available endTime isAlcoholRestricted isVulnerable slotExpiryTime slotIndicator isPrimary isSelectable maxItemAllowed sla{value displayValue}}}fragment PriceDetailRowFields on PriceDetailRow{__typename key label displayValue}\",\n" +
         "    \"variables\": {\n" +
         "        \"input\": {\n" +
         "            \"enableLiquorBox\": false,\n" +
         "            \"accessPointId\":\"" + session.getOptions().optString("accessPointId") + "\",\n" +
         "            \"cartId\":\"" + session.getOptions().optString("cartId") + "\",\n" +
         "            \"postalCode\":\"" + session.getOptions().optString("postalCode") + "\",\n" +
         "            \"storeId\":" + Integer.parseInt(session.getOptions().optString("store_id")) + ",\n" +
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

   private JSONObject getJsonFromHtml(Document doc) {
      String script = CrawlerUtils.scrapScriptFromHtml(doc, "#__NEXT_DATA__");
      JSONArray scriptArray = JSONUtils.stringToJsonArray(script);

      if (scriptArray == null || scriptArray.isEmpty()) {
         return new JSONObject();
      }

      Object json = scriptArray.get(0);
      JSONObject jsonObject = (JSONObject) json;
      return JSONUtils.getValueRecursive(jsonObject, "props.pageProps.initialData.searchResult.itemStacks.0", JSONObject.class, new JSONObject());
   }

   @Override
   protected Document fetchDocument(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
      headers.put(HttpHeaders.ORIGIN, HOME_PAGE);
      headers.put(HttpHeaders.REFERER, HOME_PAGE);
      headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
      headers.put("authority", "super.walmart.com.mx");
      headers.put("accept-language", "en-US,en;q=0.9,pt;q=0.8,pt-PT;q=0.7");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setProxyservice(PROXIES)
         .setCookies(this.cookies)
         .setHeaders(getHeaders())
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher()), session, "get");
      return Jsoup.parse(response.getBody());
   }

   @Override
   public void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 40;
      this.log("Página " + this.currentPage);
      String url = "https://super.walmart.com.mx/search?q=" + this.keywordEncoded + "&page=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      JSONObject search = getJsonFromHtml(this.currentDoc);
      if (this.totalProducts == 0) {
         this.totalProducts = search.optInt("count", 0);
      }

      JSONArray products = search.optJSONArray("items");

      if (products != null && !products.isEmpty()) {
         for (Object o : products) {
            if (o instanceof JSONObject) {
               JSONObject product = (JSONObject) o;
               String productUrl = getOriginalUrl(product);
               String internalId = product.optString("usItemId");
               String name = product.optString("name");
               String imageUrl = product.optString("image");
               boolean isSponsored = product.optJSONObject("sponsoredProduct") != null;
               boolean isAvailable = product.optString("availabilityStatusDisplayValue", "").equals("In stock");
               Integer price = isAvailable ? product.optInt("price", 0) * 100 : null;

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setName(name)
                  .setPriceInCents(price)
                  .setAvailability(isAvailable)
                  .setImageUrl(imageUrl)
                  .setIsSponsored(isSponsored)
                  .build();

               saveDataProduct(productRanking);
            }

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }

      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private String getOriginalUrl(JSONObject productJson) {
      String suffixUrl = productJson.optString("canonicalUrl");
      if (suffixUrl != null && !suffixUrl.isEmpty()) {
         return "https://super.walmart.com.mx" + suffixUrl;
      }
      return null;
   }
}
