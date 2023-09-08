package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MexicoWalmartCrawler extends CrawlerRankingKeywords {

   public MexicoWalmartCrawler(Session session) {
      super(session);
   }

   @Override
   protected void processBeforeFetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/117.0");
      headers.put("Referer", "https://www.walmart.com.mx/search?q=" + this.keywordEncoded + "&page=" + this.currentPage);
      headers.put("Origin", "https://www.walmart.com.mx");

      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.walmart.com.mx/")
         .setHeaders(headers)
         .setProxyservice(List.of(
            ProxyCollection.BUY_HAPROXY
         ))
         .build();

      this.cookies = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher()), session, "get").getCookies();
   }

   @Override
   public void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 48;

      this.log("Página " + this.currentPage);
      String url = "https://www.walmart.com.mx/orchestra/graphql/search?query=" + this.keywordEncoded + "&page=" + this.currentPage + "&prg=desktop&sort=best_match&ps=44&limit=40&additionalQueryParams.isMoreOptionsTileEnabled=true&searchArgs.query=" + this.keywordEncoded + "&searchArgs.prg=desktop&fitmentFieldParams=true_false_false&enableFashionTopNav=false&enableRelatedSearches=false&enablePortableFacets=true&enableFacetCount=true&fetchMarquee=true&fetchSkyline=true&fetchGallery=false&fetchSbaTop=true&fetchDac=false&tenant=MX_EA_GLASS&enableFlattenedFitment=false&enableMultiSave=false";

      this.log("Link onde são feitos os crawlers: " + url);

      JSONObject search = fetchJSONApi(url);

      JSONArray results = JSONUtils.getValueRecursive(search, "itemStacks.0.itemsV2", ".", JSONArray.class, new JSONArray());
      if (results != null && !results.isEmpty()) {

         for (Object o : results) {
            if (o instanceof JSONObject) {
               JSONObject product = (JSONObject) o;
               if (this.totalProducts == 0) {
                  setTotalProducts(search);
               }
               String productUrl = CrawlerUtils.completeUrl(product.optString("canonicalUrl"), "https", "www.walmart.com.mx");
               String internalId = product.optString("usItemId");
               String name = product.optString("name");
               String imageUrl = CrawlerUtils.completeUrl(JSONUtils.getValueRecursive(product, "imageInfo.thumbnailUrl", String.class), "https", "i5.walmartimages.com.mx");
               Integer price = scrapPrice(product);
               boolean isAvailable = price != null;

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setName(name)
                  .setPriceInCents(price)
                  .setAvailability(isAvailable)
                  .setImageUrl(imageUrl)
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

   protected void setTotalProducts(JSONObject search) {
      this.totalProducts = search.optInt("aggregatedCount", 0);
      this.log("Total da busca: " + this.totalProducts);
   }

   private Integer scrapPrice(JSONObject product) {
      JSONObject currentPriceJson = JSONUtils.getValueRecursive(product, "priceInfo.currentPrice", JSONObject.class, new JSONObject());
      Integer price = CommonMethods.doublePriceToIntegerPrice(JSONUtils.getDoubleValueFromJSON(currentPriceJson, "price", false), null);

      return price != null ? price * 100 : null;
   }

   private JSONObject fetchJSONApi(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/117.0");
      headers.put("Accept", "application/json");
      headers.put("Referer", "https://www.walmart.com.mx/search?q=" + this.keywordEncoded + "&page=" + this.currentPage);
      headers.put("Content-Type", "application/json");
      headers.put("x-o-segment", "oaoh");
      headers.put("WM_MP", "true");
      headers.put("x-o-ccm", "server");
      headers.put("x-o-gql-query", "query Search");
      headers.put("X-APOLLO-OPERATION-NAME", "Search");
      headers.put("WM_CONSUMER.BANNER", "GM");
      headers.put("x-latency-trace", "1");
      headers.put("x-enable-server-timing", "1");
      headers.put("WM_PAGE_URL", "https://www.walmart.com.mx/search?q=" + this.keywordEncoded + "&page=" + this.currentPage);
      headers.put("x-o-platform", "rweb");
      headers.put("x-o-bu", "WALMART-MX");
      headers.put("x-o-mart", "B2C");
      headers.put("x-o-vertical", "EA");
      headers.put("Origin", "https://www.walmart.com.mx");

      String payload = "{\"query\":\"query Search( $query:String $limit:Int $page:Int $prg:Prg! $facet:String $sort:Sort = best_match $catId:String $max_price:String $min_price:String $spelling:Boolean = true $affinityOverride:AffinityOverride $storeSlotBooked:String $ps:Int $ptss:String $recall_set:String $fitmentFieldParams:JSON ={}$fitmentSearchParams:JSON ={}$fetchMarquee:Boolean! $trsp:String $fetchSkyline:Boolean! $fetchGallery:Boolean! $fetchSbaTop:Boolean! $fetchDac:Boolean! $additionalQueryParams:JSON ={}$searchArgs:SearchArgumentsForCLS $enablePortableFacets:Boolean = false $enableFashionTopNav:Boolean = false $intentSource:IntentSource $tenant:String! $enableFacetCount:Boolean = true $pageType:String! = \\\"SearchPage\\\" $enableFlattenedFitment:Boolean = false $enableRelatedSearches:Boolean = false $enableMultiSave:Boolean = false ){search( query:$query limit:$limit page:$page prg:$prg facet:$facet sort:$sort cat_id:$catId max_price:$max_price min_price:$min_price spelling:$spelling affinityOverride:$affinityOverride storeSlotBooked:$storeSlotBooked ps:$ps ptss:$ptss recall_set:$recall_set trsp:$trsp intentSource:$intentSource additionalQueryParams:$additionalQueryParams pageType:$pageType ){query searchResult{...SearchResultFragment}}contentLayout( channel:\\\"WWW\\\" pageType:$pageType tenant:$tenant version:\\\"v1\\\" searchArgs:$searchArgs ){modules( p13n:{page:$page userReqInfo:{refererContext:{query:$query}}}){...ModuleFragment configs{__typename...on EnricherModuleConfigsV1{zoneV1}...SearchNonItemFragment...on TempoWM_GLASSWWWSponsoredProductCarouselConfigs{_rawConfigs}...on TempoWM_GLASSWWWEmailSignUpWidgetConfigs{_rawConfigs}...on _TempoWM_GLASSWWWSearchSortFilterModuleConfigs{facetsV1 @skip(if:$enablePortableFacets){...FacetFragment}topNavFacets @include(if:$enablePortableFacets){...FacetFragment}allSortAndFilterFacets @include(if:$enablePortableFacets){...FacetFragment}}...on _TempoWM_GLASSWWWSearchGuidedNavModuleConfigs{guidedNavigation{...GuidedNavFragment}}...on TempoWM_GLASSWWWBrowsingHistoryConfigs{...BrowsingHistoryFragment}...on TempoWM_GLASSWWWSearchACCStoreSelectionConfigs{ctaText userInfoMessage headingDetails{heading headingWhenFulfillmentIsSelectedAsPickup}}...on TempoWM_GLASSWWWPillsModuleConfigs{moduleSource pillsV2{...PillsModuleFragment}}...TileTakeOverProductFragment...on TempoWM_GLASSWWWSearchFitmentModuleConfigs{fitments( fitmentSearchParams:$fitmentSearchParams fitmentFieldParams:$fitmentFieldParams ){...FitmentFragment sisFitmentResponse{...SearchResultFragment}}}...on TempoWM_GLASSWWWStoreSelectionHeaderConfigs{fulfillmentMethodLabel storeDislayName}...on TempoWM_GLASSWWWBenefitProgramBannerPlaceholderConfigs{_rawConfigs}...FashionTopNavFragment @include(if:$enableFashionTopNav)...BrandAmplifierAdConfigs @include(if:$fetchSbaTop)...BannerModuleFragment...SearchNonProductBannerFragment...SearchSubscriptionBannerFragment...MarqueeDisplayAdConfigsFragment @include(if:$fetchMarquee)...SkylineDisplayAdConfigsFragment @include(if:$fetchSkyline)...GalleryDisplayAdConfigsFragment @include(if:$fetchGallery)...DynamicAdContainerConfigsFragment @include(if:$fetchDac)...HorizontalChipModuleConfigsFragment...SkinnyBannerFragment...SponsoredVideoAdFragment}}...LayoutFragment pageMetadata{location{pickupStore deliveryStore intent postalCode stateOrProvinceCode city storeId accessPointId accessType spokeNodeId}pageContext}}}fragment SearchResultFragment on SearchInterface{title aggregatedCount...BreadCrumbFragment...DebugFragment...ItemStacksFragment...SearchNonProductFragment...PageMetaDataFragment...PaginationFragment...SpellingFragment...SpanishTranslationFragment...RequestContextFragment...ErrorResponse...RelatedSearch @include(if:$enableRelatedSearches) modules{facetsV1 @skip(if:$enablePortableFacets){...FacetFragment}topNavFacets @include(if:$enablePortableFacets){...FacetFragment}allSortAndFilterFacets @include(if:$enablePortableFacets){...FacetFragment}guidedNavigation{...GuidedNavFragment}guidedNavigationV2{...PillsModuleFragment}pills{...PillsModuleFragment}spellCheck{title subTitle urlLinkText url}}pac{relevantPT{productType score}showPAC reasonCode}}fragment ModuleFragment on TempoModule{__typename type name version moduleId schedule{priority}matchedTrigger{zone}}fragment LayoutFragment on ContentLayout{layouts{id layout}}fragment BreadCrumbFragment on SearchInterface{breadCrumb{id name url cat_level}}fragment DebugFragment on SearchInterface{debug{sisUrl adsUrl presoDebugInformation{explainerToolsResponse}}}fragment ItemStacksFragment on SearchInterface{itemStacks{displayMessage meta{adsBeacon{adUuid moduleInfo max_ads}spBeaconInfo{adUuid moduleInfo pageViewUUID placement max}query isPartialResult stackId stackType stackName title subTitle titleKey queryUsedForSearchResults layoutEnum totalItemCount totalItemCountDisplay viewAllParams{query cat_id sort facet affinityOverride recall_set min_price max_price}borderColor iconUrl}itemsV2{...ItemFragment...InGridMarqueeAdFragment...InGridAdFragment...TileTakeOverTileFragment}}}fragment ItemFragment on Product{__typename buyBoxSuppression similarItems id usItemId fitmentLabel name checkStoreAvailabilityATC seeShippingEligibility brand type shortDescription weightIncrement topResult imageInfo{...ProductImageInfoFragment}aspectInfo{name header id snippet}canonicalUrl externalInfo{url}itemType category{path{name url}}badges{flags{__typename...on BaseBadge{key bundleId @include(if:$enableMultiSave) text type id styleId}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought}}tags{__typename...on BaseBadge{key text type}}groups{__typename name members{...on BadgeGroupMember{__typename id key memberType rank slaText styleId text type templates{regular faster unavailable}}...on CompositeGroupMember{__typename join memberType styleId suffix members{__typename id key memberType rank slaText styleId text type}}}}}classType averageRating numberOfReviews esrb mediaRating salesUnitType sellerId sellerName hasSellerBadge isEarlyAccessItem earlyAccessEvent annualEvent annualEventV2 availabilityStatusV2{display value}groupMetaData{groupType groupSubType numberOfComponents groupComponents{quantity offerId componentType productDisplayName}}productLocation{displayValue aisle{zone aisle}}fulfillmentSpeed offerId preOrder{...PreorderFragment}pac{showPAC reasonCode fulfillmentPacModule}fulfillmentSummary{storeId deliveryDate}priceInfo{...ProductPriceInfoFragment}variantCriteria{...VariantCriteriaFragment}snapEligible fulfillmentTitle fulfillmentType brand manufacturerName showAtc sponsoredProduct{spQs clickBeacon spTags viewBeacon}showOptions showBuyNow quickShop quickShopCTALabel rewards{eligible state minQuantity rewardAmt promotionId selectionToken rewardMultiplierStr cbOffer term expiry description}promoDiscount{discount discountEligible discountEligibleVariantPresent promotionId promoOffer state}arExperiences{isARHome isZeekit isAROptical}eventAttributes{...ProductEventAttributesFragment}subscription{subscriptionEligible}hasCarePlans petRx{eligible singleDispense}vision{ageGroup visionCenterApproved}showExploreOtherConditionsCTA isPreowned pglsCondition newConditionProductId}fragment ProductImageInfoFragment on ProductImageInfo{id name thumbnailUrl size}fragment ProductPriceInfoFragment on ProductPriceInfo{priceRange{minPrice maxPrice priceString}currentPrice{...ProductPriceFragment priceDisplay}comparisonPrice{...ProductPriceFragment}wasPrice{...ProductPriceFragment}unitPrice{...ProductPriceFragment}listPrice{...ProductPriceFragment}savingsAmount{...ProductSavingsFragment}shipPrice{...ProductPriceFragment}subscriptionPrice{priceString subscriptionString}priceDisplayCodes{priceDisplayCondition finalCostByWeight submapType}wPlusEarlyAccessPrice{memberPrice{...ProductPriceFragment}savings{...ProductSavingsFragment}eventStartTime eventStartTimeDisplay}subscriptionDualPrice subscriptionPercentage}fragment PreorderFragment on PreOrder{isPreOrder preOrderMessage preOrderStreetDateMessage streetDate streetDateDisplayable streetDateType}fragment ProductPriceFragment on ProductPrice{price priceString variantPriceString priceType currencyUnit priceDisplay}fragment ProductSavingsFragment on ProductSavings{amount percent priceString}fragment ProductEventAttributesFragment on EventAttributes{priceFlip specialBuy}fragment VariantCriteriaFragment on VariantCriterion{name type id displayName isVariantTypeSwatch variantList{id images name rank swatchImageUrl availabilityStatus products selectedProduct{canonicalUrl usItemId}}}fragment InGridMarqueeAdFragment on MarqueePlaceholder{__typename type moduleLocation lazy}fragment InGridAdFragment on AdPlaceholder{__typename type moduleLocation lazy adUuid hasVideo moduleInfo}fragment TileTakeOverTileFragment on TileTakeOverProductPlaceholder{__typename type tileTakeOverTile{span title subtitle image{src alt assetId assetName}logoImage{src alt}backgroundColor titleTextColor subtitleTextColor tileCta{ctaLink{clickThrough{value}linkText title}ctaType ctaTextColor}adsEnabled adCardLocation enableLazyLoad}}fragment SearchNonProductFragment on SearchInterface{nonProduct{title image imageAlt displayName description url urlAlt}}fragment PageMetaDataFragment on SearchInterface{pageMetadata{storeSelectionHeader{fulfillmentMethodLabel storeDislayName}title canonical description location{addressId}subscriptionEligible}}fragment PaginationFragment on SearchInterface{paginationV2{maxPage pageProperties}}fragment SpanishTranslationFragment on SearchInterface{translation{metadata{originalQuery translatedQuery isTranslated translationOfferType moduleSource}translationModule{title urlLinkText originalQueryUrl}}}fragment SpellingFragment on SearchInterface{spelling{correctedTerm}}fragment RequestContextFragment on SearchInterface{requestContext{vertical hasGicIntent isFitmentFilterQueryApplied searchMatchType categories{id name}}}fragment ErrorResponse on SearchInterface{errorResponse{correlationId source errorCodes errors{errorType statusCode statusMsg source}}}fragment GuidedNavFragment on GuidedNavigationSearchInterface{title url}fragment PillsModuleFragment on PillsSearchInterface{title url image:imageV1{src alt assetId assetName}}fragment BrowsingHistoryFragment on TempoWM_GLASSWWWBrowsingHistoryConfigs{title products{id usItemId imageInfo{thumbnailUrl}classType canonicalUrl name}}fragment BannerViewConfigFragment on BannerViewConfigCLS{title image imageAlt displayName description url urlAlt appStoreLink appStoreLinkAlt playStoreLink playStoreLinkAlt}fragment BannerModuleFragment on TempoWM_GLASSWWWSearchBannerConfigs{moduleType viewConfig{...BannerViewConfigFragment}}fragment SearchNonProductBannerFragment on TempoWM_GLASSWWWSearchNonProductBannerConfigs{nps{query normalisedQuery nonProduct{title image imageAlt displayName description url urlAlt}}viewConfig{...BannerViewConfigFragment}}fragment SearchSubscriptionBannerFragment on TempoWM_GLASSWWWSearchSubscriptionBannerConfigs{viewConfig{...BannerViewConfigFragment}}fragment FacetFragment on Facet{title name expandOnLoad type layout min max selectedMin selectedMax unboundedMax stepSize isSelected values{id title name expandOnLoad description type itemCount @include(if:$enableFacetCount) isSelected baseSeoURL values{id title name expandOnLoad description type itemCount @include(if:$enableFacetCount) isSelected baseSeoURL values{id title name expandOnLoad description type itemCount @include(if:$enableFacetCount) isSelected baseSeoURL values{id title name expandOnLoad description type itemCount @include(if:$enableFacetCount) isSelected baseSeoURL values{id title name expandOnLoad description type itemCount @include(if:$enableFacetCount) isSelected baseSeoURL values{id title name expandOnLoad description type itemCount @include(if:$enableFacetCount) isSelected baseSeoURL values{id title name expandOnLoad description type itemCount @include(if:$enableFacetCount) isSelected baseSeoURL values{id title name expandOnLoad description type itemCount @include(if:$enableFacetCount) isSelected baseSeoURL}}}}}}}}}fragment FitmentFragment on Fitments{partTypeIDs isNarrowSearch fitmentOptionalFields{...FitmentFieldFragment}result{status formId position quantityTitle extendedAttributes{...FitmentFieldFragment}labels{...LabelFragment}resultSubTitle notes suggestions{...FitmentSuggestionFragment}}redirectUrl{title clickThrough{value}}labels{...LabelFragment}savedVehicle{vehicleType{...VehicleFieldFragment}vehicleYear{...VehicleFieldFragment}vehicleMake{...VehicleFieldFragment}vehicleModel{...VehicleFieldFragment}additionalAttributes{...VehicleFieldFragment}}fitmentFields{...VehicleFieldFragment}fitmentForms{id fields{...FitmentFieldFragment}title labels{...LabelFragment}garage{vehicles{...AutoVehicle}}}}fragment LabelFragment on FitmentLabels{ctas{...FitmentLabelEntityFragment}messages{...FitmentLabelEntityFragment}links{...FitmentLabelEntityFragment}images{...FitmentLabelEntityFragment}}fragment FitmentLabelEntityFragment on FitmentLabelEntity{id label labelV1 @include(if:$enableFlattenedFitment)}fragment VehicleFieldFragment on FitmentVehicleField{id label value}fragment FitmentFieldFragment on FitmentField{id displayName value extended data{value label}dependsOn isRequired errorMessage}fragment FitmentSuggestionFragment on FitmentSuggestion{id position loadIndex speedRating searchQueryParam labels{...LabelFragment}cat_id fitmentSuggestionParams{id value}optionalSuggestionParams{id data{label value}}}fragment MarqueeDisplayAdConfigsFragment on TempoWM_GLASSWWWMarqueeDisplayAdConfigs{_rawConfigs ad{...DisplayAdFragment}}fragment DisplayAdFragment on Ad{...AdFragment adContent{type data{__typename...AdDataDisplayAdFragment}}}fragment AdFragment on Ad{status moduleType platform pageId pageType storeId stateCode zipCode pageContext moduleConfigs adsContext adRequestComposite}fragment AdDataDisplayAdFragment on AdData{...on DisplayAd{json status}}fragment GalleryDisplayAdConfigsFragment on TempoWM_GLASSWWWGalleryDisplayAdConfigs{_rawConfigs}fragment SkylineDisplayAdConfigsFragment on TempoWM_GLASSWWWSkylineDisplayAdConfigs{_rawConfigs ad{...SkylineDisplayAdFragment}}fragment SkylineDisplayAdFragment on Ad{...SkylineAdFragment adContent{type data{__typename...SkylineAdDataDisplayAdFragment}}}fragment SkylineAdFragment on Ad{status moduleType platform pageId pageType storeId stateCode zipCode pageContext moduleConfigs adsContext adRequestComposite}fragment SkylineAdDataDisplayAdFragment on AdData{...on DisplayAd{json status}}fragment DynamicAdContainerConfigsFragment on TempoWM_GLASSWWWDynamicAdContainerConfigs{_rawConfigs adModules{moduleType moduleLocation priority adServers{adServer}}zoneLocation lazy}fragment BrandAmplifierAdConfigs on TempoWM_GLASSWWWBrandAmplifierAdConfigs{_rawConfigs moduleLocation ad{...SponsoredBrandsAdFragment}}fragment SponsoredBrandsAdFragment on Ad{...AdFragment adContent{type data{__typename...AdDataSponsoredBrandsFragment}}}fragment AdDataSponsoredBrandsFragment on AdData{...on SponsoredBrands{adUuid adExpInfo moduleInfo brands{logo{featuredHeadline featuredImage featuredImageName featuredUrl logoClickTrackUrl}products{...ProductFragment}}}}fragment ProductFragment on Product{usItemId offerId badges{flags{__typename...on BaseBadge{id text key query type}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought criteria{name value}}}labels{__typename...on BaseBadge{id text key}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought}}tags{__typename...on BaseBadge{id text key}}groups{__typename name members{...on BadgeGroupMember{__typename id key memberType rank slaText styleId text type}...on CompositeGroupMember{__typename join memberType styleId suffix members{__typename id key memberType rank slaText styleId text type}}}}}priceInfo{priceDisplayCodes{rollback reducedPrice eligibleForAssociateDiscount clearance strikethrough submapType priceDisplayCondition unitOfMeasure pricePerUnitUom}currentPrice{price priceString priceDisplay}wasPrice{price priceString}priceRange{minPrice maxPrice priceString}unitPrice{price priceString}savingsAmount{priceString}comparisonPrice{priceString}subscriptionPrice{priceString subscriptionString price minPrice maxPrice intervalFrequency duration percentageRate durationUOM interestUOM}}snapEligible showOptions sponsoredProduct{spQs clickBeacon spTags}canonicalUrl numberOfReviews averageRating availabilityStatus imageInfo{thumbnailUrl allImages{id url}}name fulfillmentBadge classType type showAtc p13nData{predictedQuantity flags{PREVIOUSLY_PURCHASED{text}CUSTOMERS_PICK{text}}labels{PREVIOUSLY_PURCHASED{text}CUSTOMERS_PICK{text}}}brand}fragment SearchNonItemFragment on TempoWM_GLASSWWWSearchNonItemConfigs{title subTitle urlLinkText url}fragment HorizontalChipModuleConfigsFragment on TempoWM_GLASSWWWHorizontalChipModuleConfigs{chipModuleSource:moduleSource chipModule{title url{linkText title clickThrough{type value}}}chipModuleWithImages{title url{linkText title clickThrough{type value}}image{assetId assetName alt clickThrough{type value}height src title width}}}fragment SkinnyBannerFragment on TempoWM_GLASSWWWSkinnyBannerConfigs{campaignsV1{bannerType desktopBannerHeight bannerImage{src title alt assetId assetName}mobileBannerHeight mobileImage{src title alt assetId assetName}clickThroughUrl{clickThrough{value}}backgroundColor heading{title fontColor}subHeading{title fontColor}bannerCta{ctaLink{linkText clickThrough{value}}textColor ctaType}}}fragment TileTakeOverProductFragment on TempoWM_GLASSWWWTileTakeOverProductConfigs{dwebSlots mwebSlots overrideDefaultTiles TileTakeOverProductDetailsV1{pageNumber span dwebPosition mwebPosition title subtitle image{src alt assetId assetName}logoImage{src alt}backgroundColor titleTextColor subtitleTextColor tileCta{ctaLink{clickThrough{value}linkText title uid}ctaType ctaTextColor}adsEnabled adCardLocation enableLazyLoad}}fragment FashionTopNavFragment on TempoWM_GLASSWWWCategoryTopNavConfigs{navHeaders{header{linkText clickThrough{value}}headerImageGroup{headerImage{alt src assetId assetName}imgTitle imgSubText imgLink{linkText title clickThrough{value}}}categoryGroup{category{linkText clickThrough{value}}startNewColumn subCategoryGroup{subCategory{linkText clickThrough{value}}isBold openInNewTab}}}}fragment RelatedSearch on SearchInterface{relatedSearch{title url imageUrl}}fragment AutoVehicle on AutoVehicle{cid color default documentType fitment{baseBodyType baseVehicleId engineOptions{id isSelected label}smartSubModel tireSizeOptions{diameter isCustom isSelected loadIndex positions ratio speedRating tirePressureFront tirePressureRear tireSize width}trim}isDually licensePlate licensePlateState make model source sourceType subModel{subModelId subModelName}subModelOptions{subModelId subModelName}vehicleId vehicleType vin year}fragment SponsoredVideoAdFragment on TempoWM_GLASSWWWSponsoredVideoAdConfigs{__typename sponsoredVideoAd{ad{adContent{data{...on SponsoredVideos{adUuid hasVideo moduleInfo}}}}}}\",\"variables\":{\"id\":\"\",\"dealsId\":\"\",\"query\":\"" + this.keywordEncoded + "\",\"page\":" + this.currentPage + ",\"prg\":\"desktop\",\"catId\":\"\",\"facet\":\"\",\"sort\":\"best_match\",\"rawFacet\":\"\",\"seoPath\":\"\",\"ps\":44,\"limit\":40,\"ptss\":\"\",\"trsp\":\"\",\"beShelfId\":\"\",\"recall_set\":\"\",\"module_search\":\"\",\"min_price\":\"\",\"max_price\":\"\",\"storeSlotBooked\":\"\",\"additionalQueryParams\":{\"hidden_facet\":null,\"translation\":null,\"isMoreOptionsTileEnabled\":true},\"searchArgs\":{\"query\":\"" + this.keywordEncoded + "\",\"cat_id\":\"\",\"prg\":\"desktop\",\"facet\":\"\"},\"fitmentFieldParams\":{\"powerSportEnabled\":true,\"dynamicFitmentEnabled\":false,\"extendedAttributesEnabled\":false},\"fitmentSearchParams\":{\"id\":\"\",\"dealsId\":\"\",\"query\":\"" + this.keywordEncoded + "\",\"page\":" + this.currentPage + ",\"prg\":\"desktop\",\"catId\":\"\",\"facet\":\"\",\"sort\":\"best_match\",\"rawFacet\":\"\",\"seoPath\":\"\",\"ps\":40,\"limit\":40,\"ptss\":\"\",\"trsp\":\"\",\"beShelfId\":\"\",\"recall_set\":\"\",\"module_search\":\"\",\"min_price\":\"\",\"max_price\":\"\",\"storeSlotBooked\":\"\",\"additionalQueryParams\":{\"hidden_facet\":null,\"translation\":null,\"isMoreOptionsTileEnabled\":true},\"searchArgs\":{\"query\":\"" + this.keywordEncoded + "\",\"cat_id\":\"\",\"prg\":\"desktop\",\"facet\":\"\"},\"cat_id\":\"\",\"_be_shelf_id\":\"\"},\"enableFashionTopNav\":false,\"enableRelatedSearches\":false,\"enablePortableFacets\":true,\"enableFacetCount\":true,\"fetchMarquee\":true,\"fetchSkyline\":true,\"fetchGallery\":false,\"fetchSbaTop\":true,\"fetchDac\":false,\"tenant\":\"MX_EA_GLASS\",\"enableFlattenedFitment\":false,\"enableMultiSave\":false,\"pageType\":\"SearchPage\"}}";

      Request request = RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .setCookies(this.cookies)
         .setProxyservice(List.of(
            ProxyCollection.BUY_HAPROXY
         ))
         .build();

      Response response = new JsoupDataFetcher().post(session, request);

      JSONObject json = CrawlerUtils.stringToJson(response.getBody());

      return JSONUtils.getValueRecursive(json, "data.search.searchResult", JSONObject.class);

   }
}
