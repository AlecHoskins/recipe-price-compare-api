package com.alechoskins.recipepricecompare.scraper;


import com.alechoskins.recipepricecompare.model.WmProduct;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;


import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class WmpScraper {
    private static final int OZ = 0;
    private static final int LB = 1;
    private static final int NO_UNIT_PROVIDED = 2;

    ObjectMapper objectMapper = new ObjectMapper();

    public WmpScraper() throws IOException {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }


    // *************************************** METHODS ************************************************

    //CREATES LIST FROM JSON
    public List<WmProduct> getProductList(String userSearchTerm) {

        List <WmProduct> wmProductLi = new ArrayList<>();

        String fullJson = getFullJson(userSearchTerm);
        if(fullJson.isBlank()){
            return wmProductLi;
        }

        //wmProductArr exists because JSONArray couldn't map straight to string...
        //...and looping through JSONArray messes up format
        JSONArray jsonItemArr = JsonPath.read(fullJson,"$..itemsV2[*]");
        WmProduct[] wmProductArr = objectMapper.convertValue(jsonItemArr,WmProduct[].class);

        for (WmProduct wmProduct : wmProductArr) {

            //set searchTerm
            if (wmProduct.getName().toLowerCase().contains(userSearchTerm.toLowerCase())) {
                wmProduct.setUserSearchTerm(userSearchTerm);

                //set price
                if(wmProduct.getPriceInfo() != null){
                    if(wmProduct.getPriceInfo().getCurrentPrice() != null){
                        if(wmProduct.getPriceInfo().getCurrentPrice().getPrice() != null){
                            wmProduct.setPrice(wmProduct.getPriceInfo().getCurrentPrice().getPrice());
                        }
                    }
                }

                //determine unit of measurement
                int unitOfMeasurement = 0;
                if(wmProduct.getPriceInfo() != null){
                    if(wmProduct.getPriceInfo().getUnitPrice() != null){
                        if(wmProduct.getPriceInfo().getUnitPrice().getPriceString() != null){
                            unitOfMeasurement = checkUnitOfMeasurement(wmProduct.getPriceInfo().getUnitPrice());

                            BigDecimal centsPerOz = wmProduct.getPriceInfo().getUnitPrice().getPrice();
                            if(unitOfMeasurement == OZ) {
                                wmProduct.setCentsPerOz(centsPerOz.multiply(BigDecimal.valueOf(100)));
                            }
                            else if  (unitOfMeasurement == LB) {
                                wmProduct.setCentsPerOz(
                                        //divide by 16, round 2 decimal places
                                        (centsPerOz.multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(16))).setScale(2, RoundingMode.CEILING)
                                );
                            }
                        }
                    }
                }

                //setWeight
                if (wmProduct.getPrice() != null && wmProduct.getCentsPerOz() != null){
                    BigDecimal price = wmProduct.getPrice();
                    BigDecimal centsPerOz = wmProduct.getCentsPerOz();
                    //set oz or lbcd
                    //weight = total / pricePerOz
                    wmProduct.setWeight(String.valueOf(
                            (price.multiply(BigDecimal.valueOf(100)).divide(centsPerOz,RoundingMode.CEILING)))
                            +" Oz");
                }

                //set thumbnailUrl
                if(wmProduct.getImageInfo()!=null){
                    wmProduct.setThumbnailUrl(wmProduct.getImageInfo().getThumbnailUrl());
                }

                wmProductLi.add(wmProduct);
            }
        }
        return wmProductLi;
    }

    private String getFullJson(String userSearchTerm){
        //gets response with searchterm, returns json
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("application/json");

        String bodyContent = "{\"query\":\"query Search( $query:String $page:Int $prg:Prg! $facet:String $sort:Sort = best_match $catId:String $max_price:String $min_price:String $spelling:Boolean = true $affinityOverride:AffinityOverride $storeSlotBooked:String $ps:Int $ptss:String $recall_set:String $fitmentFieldParams:JSON ={}$fitmentSearchParams:JSON ={}$fetchMarquee:Boolean! $trsp:String $fetchSkyline:Boolean! $fetchSbaTop:Boolean! $additionalQueryParams:JSON ={}$searchArgs:SearchArgumentsForCLS $enablePortableFacets:Boolean = false $intentSource:IntentSource $tenant:String! ){search( query:$query page:$page prg:$prg facet:$facet sort:$sort cat_id:$catId max_price:$max_price min_price:$min_price spelling:$spelling affinityOverride:$affinityOverride storeSlotBooked:$storeSlotBooked ps:$ps ptss:$ptss recall_set:$recall_set trsp:$trsp intentSource:$intentSource additionalQueryParams:$additionalQueryParams ){query searchResult{...SearchResultFragment}}contentLayout( channel:\\\"WWW\\\" pageType:\\\"SearchPage\\\" tenant:$tenant searchArgs:$searchArgs ){modules{...ModuleFragment configs{...SearchNonItemFragment __typename...on TempoWM_GLASSWWWSponsoredProductCarouselConfigs{_rawConfigs}...on _TempoWM_GLASSWWWSearchSortFilterModuleConfigs{facetsV1 @skip(if:$enablePortableFacets){...FacetFragment}topNavFacets @include(if:$enablePortableFacets){...FacetFragment}allSortAndFilterFacets @include(if:$enablePortableFacets){...FacetFragment}}...on _TempoWM_GLASSWWWSearchGuidedNavModuleConfigs{guidedNavigation{...GuidedNavFragment}}...on TempoWM_GLASSWWWPillsModuleConfigs{moduleSource pillsV2{...PillsModuleFragment}}...TileTakeOverProductFragment...on TempoWM_GLASSWWWSearchFitmentModuleConfigs{fitments( fitmentSearchParams:$fitmentSearchParams fitmentFieldParams:$fitmentFieldParams ){...FitmentFragment sisFitmentResponse{...SearchResultFragment}}}...on TempoWM_GLASSWWWStoreSelectionHeaderConfigs{fulfillmentMethodLabel storeDislayName}...BrandAmplifierAdConfigs @include(if:$fetchSbaTop)...BannerModuleFragment...MarqueeDisplayAdConfigsFragment @include(if:$fetchMarquee)...SkylineDisplayAdConfigsFragment @include(if:$fetchSkyline)...HorizontalChipModuleConfigsFragment}}...LayoutFragment pageMetadata{location{pickupStore deliveryStore intent postalCode stateOrProvinceCode city storeId accessPointId accessType}pageContext}}}fragment SearchResultFragment on SearchInterface{title aggregatedCount...BreadCrumbFragment...DebugFragment...ItemStacksFragment...PageMetaDataFragment...PaginationFragment...SpellingFragment...SpanishTranslationFragment...RequestContextFragment...ErrorResponse modules{facetsV1 @skip(if:$enablePortableFacets){...FacetFragment}topNavFacets @include(if:$enablePortableFacets){...FacetFragment}allSortAndFilterFacets @include(if:$enablePortableFacets){...FacetFragment}guidedNavigation{...GuidedNavFragment}guidedNavigationV2{...PillsModuleFragment}pills{...PillsModuleFragment}spellCheck{title subTitle urlLinkText url}}}fragment ModuleFragment on TempoModule{name version type moduleId schedule{priority}matchedTrigger{zone}}fragment LayoutFragment on ContentLayout{layouts{id layout}}fragment BreadCrumbFragment on SearchInterface{breadCrumb{id name url}}fragment DebugFragment on SearchInterface{debug{sisUrl adsUrl}}fragment ItemStacksFragment on SearchInterface{itemStacks{displayMessage meta{adsBeacon{adUuid moduleInfo max_ads}query stackId stackType title layoutEnum totalItemCount totalItemCountDisplay viewAllParams{query cat_id sort facet affinityOverride recall_set min_price max_price}}itemsV2{...ItemFragment...InGridMarqueeAdFragment...TileTakeOverTileFragment}}}fragment ItemFragment on Product{__typename id usItemId fitmentLabel name checkStoreAvailabilityATC seeShippingEligibility brand type shortDescription imageInfo{...ProductImageInfoFragment}canonicalUrl externalInfo{url}itemType category{path{name url}}badges{flags{...on BaseBadge{key text type id}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought}}tags{...on BaseBadge{key text type}}}classType averageRating numberOfReviews esrb mediaRating salesUnitType sellerId sellerName hasSellerBadge availabilityStatusV2{display value}groupMetaData{groupType groupSubType numberOfComponents groupComponents{quantity offerId componentType productDisplayName}}productLocation{displayValue aisle{zone aisle}}fulfillmentSpeed offerId preOrder{...PreorderFragment}priceInfo{...ProductPriceInfoFragment}variantCriteria{...VariantCriteriaFragment}snapEligible fulfillmentBadge fulfillmentTitle fulfillmentType brand manufacturerName showAtc sponsoredProduct{spQs clickBeacon spTags}showOptions showBuyNow rewards{eligible state minQuantity rewardAmt promotionId selectionToken cbOffer term expiry description}}fragment ProductImageInfoFragment on ProductImageInfo{thumbnailUrl}fragment ProductPriceInfoFragment on ProductPriceInfo{priceRange{minPrice maxPrice}currentPrice{...ProductPriceFragment}wasPrice{...ProductPriceFragment}unitPrice{...ProductPriceFragment}listPrice{...ProductPriceFragment}shipPrice{...ProductPriceFragment}subscriptionPrice{priceString subscriptionString}priceDisplayCodes{priceDisplayCondition finalCostByWeight submapType}}fragment PreorderFragment on PreOrder{isPreOrder preOrderMessage preOrderStreetDateMessage}fragment ProductPriceFragment on ProductPrice{price priceString}fragment VariantCriteriaFragment on VariantCriterion{name type id isVariantTypeSwatch variantList{id images name rank swatchImageUrl availabilityStatus products selectedProduct{canonicalUrl usItemId}}}fragment InGridMarqueeAdFragment on MarqueePlaceholder{__typename type moduleLocation lazy}fragment TileTakeOverTileFragment on TileTakeOverProductPlaceholder{__typename type tileTakeOverTile{span title subtitle image{src alt}logoImage{src alt}backgroundColor titleTextColor subtitleTextColor tileCta{ctaLink{clickThrough{value}linkText title}ctaType ctaTextColor}}}fragment PageMetaDataFragment on SearchInterface{pageMetadata{storeSelectionHeader{fulfillmentMethodLabel storeDislayName}title canonical description location{addressId}}}fragment PaginationFragment on SearchInterface{paginationV2{maxPage pageProperties}}fragment SpanishTranslationFragment on SearchInterface{translation{metadata{originalQuery translatedQuery isTranslated translationOfferType moduleSource}translationModule{title urlLinkText originalQueryUrl}}}fragment SpellingFragment on SearchInterface{spelling{correctedTerm}}fragment RequestContextFragment on SearchInterface{requestContext{vertical isFitmentFilterQueryApplied searchMatchType categories{id name}}}fragment ErrorResponse on SearchInterface{errorResponse{correlationId source errorCodes errors{errorType statusCode statusMsg source}}}fragment GuidedNavFragment on GuidedNavigationSearchInterface{title url}fragment PillsModuleFragment on PillsSearchInterface{title url image:imageV1{src alt}}fragment BannerModuleFragment on TempoWM_GLASSWWWSearchBannerConfigs{moduleType viewConfig{title image imageAlt displayName description url urlAlt appStoreLink appStoreLinkAlt playStoreLink playStoreLinkAlt}}fragment FacetFragment on Facet{title name type layout min max selectedMin selectedMax unboundedMax stepSize isSelected values{id name description type itemCount isSelected baseSeoURL}}fragment FitmentFragment on Fitments{partTypeIDs result{status formId position quantityTitle extendedAttributes{...FitmentFieldFragment}labels{...LabelFragment}resultSubTitle notes suggestions{...FitmentSuggestionFragment}}labels{...LabelFragment}savedVehicle{vehicleYear{...VehicleFieldFragment}vehicleMake{...VehicleFieldFragment}vehicleModel{...VehicleFieldFragment}additionalAttributes{...VehicleFieldFragment}}fitmentFields{...VehicleFieldFragment}fitmentForms{id fields{...FitmentFieldFragment}title labels{...LabelFragment}}}fragment LabelFragment on FitmentLabels{ctas{...FitmentLabelEntityFragment}messages{...FitmentLabelEntityFragment}links{...FitmentLabelEntityFragment}images{...FitmentLabelEntityFragment}}fragment FitmentLabelEntityFragment on FitmentLabelEntity{id label}fragment VehicleFieldFragment on FitmentVehicleField{id label value}fragment FitmentFieldFragment on FitmentField{id displayName value extended data{value label}dependsOn}fragment FitmentSuggestionFragment on FitmentSuggestion{id position loadIndex speedRating searchQueryParam labels{...LabelFragment}cat_id fitmentSuggestionParams{id value}}fragment MarqueeDisplayAdConfigsFragment on TempoWM_GLASSWWWMarqueeDisplayAdConfigs{_rawConfigs ad{...DisplayAdFragment}}fragment DisplayAdFragment on Ad{...AdFragment adContent{type data{__typename...AdDataDisplayAdFragment}}}fragment AdFragment on Ad{status moduleType platform pageId pageType storeId stateCode zipCode pageContext moduleConfigs adsContext adRequestComposite}fragment AdDataDisplayAdFragment on AdData{...on DisplayAd{json status}}fragment SkylineDisplayAdConfigsFragment on TempoWM_GLASSWWWSkylineDisplayAdConfigs{_rawConfigs ad{...SkylineDisplayAdFragment}}fragment SkylineDisplayAdFragment on Ad{...SkylineAdFragment adContent{type data{__typename...SkylineAdDataDisplayAdFragment}}}fragment SkylineAdFragment on Ad{status moduleType platform pageId pageType storeId stateCode zipCode pageContext moduleConfigs adsContext adRequestComposite}fragment SkylineAdDataDisplayAdFragment on AdData{...on DisplayAd{json status}}fragment BrandAmplifierAdConfigs on TempoWM_GLASSWWWBrandAmplifierAdConfigs{_rawConfigs moduleLocation ad{...SponsoredBrandsAdFragment}}fragment SponsoredBrandsAdFragment on Ad{...AdFragment adContent{type data{__typename...AdDataSponsoredBrandsFragment}}}fragment AdDataSponsoredBrandsFragment on AdData{...on SponsoredBrands{adUuid adExpInfo moduleInfo brands{logo{featuredHeadline featuredImage featuredImageName featuredUrl logoClickTrackUrl}products{...ProductFragment}}}}fragment ProductFragment on Product{usItemId offerId badges{flags{__typename...on BaseBadge{id text key query type}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought criteria{name value}}}labels{__typename...on BaseBadge{id text key}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought}}tags{__typename...on BaseBadge{id text key}}}priceInfo{priceDisplayCodes{rollback reducedPrice eligibleForAssociateDiscount clearance strikethrough submapType priceDisplayCondition unitOfMeasure pricePerUnitUom}currentPrice{price priceString}wasPrice{price priceString}priceRange{minPrice maxPrice priceString}unitPrice{price priceString}}showOptions sponsoredProduct{spQs clickBeacon spTags}canonicalUrl numberOfReviews averageRating availabilityStatus imageInfo{thumbnailUrl allImages{id url}}name fulfillmentBadge classType type showAtc p13nData{predictedQuantity flags{PREVIOUSLY_PURCHASED{text}CUSTOMERS_PICK{text}}labels{PREVIOUSLY_PURCHASED{text}CUSTOMERS_PICK{text}}}}fragment SearchNonItemFragment on TempoWM_GLASSWWWSearchNonItemConfigs{title subTitle urlLinkText url}fragment HorizontalChipModuleConfigsFragment on TempoWM_GLASSWWWHorizontalChipModuleConfigs{chipModuleSource:moduleSource chipModule{title url{linkText title clickThrough{type value}}}chipModuleWithImages{title url{linkText title clickThrough{type value}}image{alt clickThrough{type value}height src title width}}}fragment TileTakeOverProductFragment on TempoWM_GLASSWWWTileTakeOverProductConfigs{TileTakeOverProductDetails{span dwebPosition mwebPosition title subtitle image{src alt}logoImage{src alt}backgroundColor titleTextColor subtitleTextColor tileCta{ctaLink{clickThrough{value}linkText title}ctaType ctaTextColor}}}\",\"variables\":" +
                "{\"id\":\"\",\"dealsId\":\"\",\"query\":\"USER_SEARCH_TERM\",\"page\":1,\"prg\":\"desktop\",\"catId\":\"\",\"facet\":\"\",\"sort\":\"best_match\",\"rawFacet\":\"\",\"seoPath\":\"\",\"ps\":40,\"ptss\":\"\",\"trsp\":\"\",\"beShelfId\":\"\",\"recall_set\":\"\",\"module_search\":\"\",\"min_price\":\"\",\"max_price\":\"\",\"storeSlotBooked\":\"\",\"additionalQueryParams\":{\"hidden_facet\":null,\"translation\":null},\"searchArgs\":{\"query\":\"USER_SEARCH_TERM\",\"cat_id\":\"\",\"prg\":\"desktop\",\"facet\":\"\"},\"fitmentFieldParams\":null,\"fitmentSearchParams\":{\"id\":\"\",\"dealsId\":\"\",\"query\":\"USER_SEARCH_TERM\",\"page\":1,\"prg\":\"desktop\",\"catId\":\"\",\"facet\":\"\",\"sort\":\"best_match\",\"rawFacet\":\"\",\"seoPath\":\"\",\"ps\":40,\"ptss\":\"\",\"trsp\":\"\",\"beShelfId\":\"\",\"recall_set\":\"\",\"module_search\":\"\",\"min_price\":\"\",\"max_price\":\"\",\"storeSlotBooked\":\"\",\"additionalQueryParams\":{\"hidden_facet\":null,\"translation\":null},\"searchArgs\":{\"query\":\"USER_SEARCH_TERM\",\"cat_id\":\"\",\"prg\":\"desktop\",\"facet\":\"\"},\"cat_id\":\"\",\"_be_shelf_id\":\"\"},\"enablePortableFacets\":true,\"fetchMarquee\":true,\"fetchSkyline\":true,\"fetchSbaTop\":true,\"tenant\":\"WM_GLASS\"}}";
        bodyContent = bodyContent.replaceAll("USER_SEARCH_TERM", userSearchTerm);
        RequestBody body = RequestBody.create(mediaType,bodyContent);

        Request request = new Request.Builder()
                .url("https://www.walmart.com/orchestra/home/graphql/search?query=lettuce&page=1&prg=desktop&sort=best_match&ps=40&searchArgs.query=lettuce&searchArgs.prg=desktop&fitmentFieldParams=true&enablePortableFacets=true&enableFacetCount=true&fetchMarquee=true&fetchSkyline=true&fetchSbaTop=true&tenant=WM_GLASS")
                .method("POST", body)
                .addHeader("authority", "www.walmart.com")
                .addHeader("accept", "application/json")
                .addHeader("accept-language", "en-US,en;q=0.9")
                .addHeader("content-type", "application/json")
                .addHeader("cookie", "QuantumMetricUserID=af885fbba6fa818b8df5bebb4d06a8c5; wm_ul_plus=INACTIVE|1637163011144; _vc=aek5wgyM1tkeiWBDTPIi%2FkWzaX6IMlaFN4Jn%2BRW7Yac%3D; SPID=27544fee73eb599379510c097639e5a7f453b3d5cb82ae80d5cb696623f55ed603ebf80a93b7a4f9046d22b3742dfd6dwmcxo; CID=2a5b8c0b-49ca-4bdf-b9dd-0580f0454485; hasCID=1; type=REGISTERED; oneapp_customer=true; AID=wmlspartner%253D0%253Areflectorid%253D0000000000000000000000%253Alastupd%253D1652578015895; _m=9; vtc=QkrzKpUC9VOR57nHIpW22I; _pxvid=1ccc0ac4-d3ee-11ec-b1d1-797652794357; userContext=eyJhZGRyZXNzRGF0YSI6eyJoYXNEZWxpdmVyYWJsZUFkZHJlc3MiOnRydWV9LCJoYXNJdGVtU3Vic2NyaXB0aW9uIjpmYWxzZSwiaGFzTWVtYmVyc2hpcEluZm8iOmZhbHNlLCJpc0RlZmF1bHQiOmZhbHNlLCJwYXltZW50RGF0YSI6eyJjYXBpdGFsT25lQmFubmVyU25vb3plVFMiOjAsImhhc0NhcE9uZSI6ZmFsc2UsImhhc0NhcE9uZUxpbmtlZCI6ZmFsc2UsImhhc0NyZWRpdENhcmQiOnRydWUsImhhc0RpcmVjdGVkU3BlbmRDYXJkIjpmYWxzZSwiaGFzRUJUIjp0cnVlLCJoYXNHaWZ0Q2FyZCI6ZmFsc2UsInNob3dDYXBPbmVCYW5uZXIiOnRydWUsIndwbHVzTm9CZW5lZml0QmFubmVyIjp0cnVlfSwicHJvZmlsZURhdGEiOnsiaXNBc3NvY2lhdGUiOmZhbHNlLCJpc1Rlc3RBY2NvdW50IjpmYWxzZSwibWVtYmVyc2hpcE9wdEluIjp7ImlzT3B0ZWRJbiI6ZmFsc2V9fX0%3D; _gcl_au=1.1.1573175268.1652578041; _uetvid=2a5586d0d3ee11ec88f09f495a5ccc83; _ga=GA1.2.1716409259.1652578054; _pxhd=37d32386e76a25678b459756a0e6c93d9a522bf0552513b996a3611c705d562c:1ccc0ac4-d3ee-11ec-b1d1-797652794357; TBV=7; tb_sw_supported=true; bm_mi=E241E71C8C1234857EB4B42F6608FAAB~YAAQ1UJ1aBUgf6qBAQAAm1cuxRDjpu5DOQ8auq5cBwFozUxEy22QHwQsBKFWV+R6caJ8l/avGc29qDHP3oOEJfZCqon/vQJBM/1HkZhsx3dlxCmdAgR02LnNsK+QE2F+2ywqGM7rwTjy/evbI59rA5EqSJd79Oehmw70v2+TpzsU5Ain0s5TEzKYhiFHRUWoaG3l/106b88HmnHHQspJXW2UVH+F+PO7miBjAqcIaxbQswquCRWYrPXH3Af3W+ImGHKXGQX4hAqaJNT7l5C+2rH9C+f7Amp8ngQgzv6LB+YnM35w3UHukHKp1GSzFqW9svL8Jmme~1; pxcts=7671f2b3-faf8-11ec-a786-6f6a784f7541; ak_bmsc=3E59C1C82FA9A36BE7CB3E44A890C68C~000000000000000000000000000000~YAAQ1UJ1aK0gf6qBAQAAwFwuxRAVODiH448oxCntuUYrhWF2c6o0kRxMlUinNLNLLRtcAAis0BPoJSP9/sVaU9pUtvO4rPkDbCTMGl1+4q5J49/mVb2/u9DF8TknoAHhea2NHlz3BhkNSkq3WwhwGZl34D4/WzLI1IlodA42z/2Nbf7EPyxAtapggNhv0kWnnCpTzuasOYjIDj2YkQ4U+1S+GVeMnilCi5kPFcg6D4uXAOedCC622GAFp8xtRc+B7qO1wPmDxrJy166s09TSrdhRcL34blOA33CONsxKhOZpj1xNwnFHsBbIdTpz1UezraxYWL8UM3hMd0D6y9Ar3GjUvNlFYKSMtraeT3bb4BDdiD4A/4dNV4uDiblKsDbpFG7YE3sYJRXpFEoxlWEEKMAiAqY00zQmMo5pzrhsfW5ieno=; locDataV3=eyJpc0RlZmF1bHRlZCI6ZmFsc2UsImlzRXhwbGljaXQiOmZhbHNlLCJpbnRlbnQiOiJQSUNLVVAiLCJwaWNrdXAiOlt7ImJ1SWQiOiIwIiwibm9kZUlkIjoiODgwIiwiZGlzcGxheU5hbWUiOiJJcnZpbmcgU3VwZXJjZW50ZXIiLCJub2RlVHlwZSI6IlNUT1JFIiwiYWRkcmVzcyI6eyJwb3N0YWxDb2RlIjoiNzUwNjIiLCJhZGRyZXNzTGluZTEiOiI0MTAwIFcgQWlycG9ydCBGd3kiLCJjaXR5IjoiSXJ2aW5nIiwic3RhdGUiOiJUWCIsImNvdW50cnkiOiJVUyIsInBvc3RhbENvZGU5IjoiNzUwNjItNTkxMyJ9LCJnZW9Qb2ludCI6eyJsYXRpdHVkZSI6MzIuODM0ODIyLCJsb25naXR1ZGUiOi05Ny4wMDYzMTJ9LCJpc0dsYXNzRW5hYmxlZCI6dHJ1ZSwic2NoZWR1bGVkRW5hYmxlZCI6dHJ1ZSwidW5TY2hlZHVsZWRFbmFibGVkIjp0cnVlLCJodWJOb2RlSWQiOiI4ODAiLCJzdG9yZUhycyI6IjA2OjAwLTIzOjAwIiwic3VwcG9ydGVkQWNjZXNzVHlwZXMiOlsiUElDS1VQX0NVUkJTSURFIiwiUElDS1VQX0lOU1RPUkUiXX1dLCJzaGlwcGluZ0FkZHJlc3MiOnsiaWQiOiJjZDE1ODNhNS1iYTdjLTQ5ZTgtODU5Mi1jNjQwZGZmYWFlNTkiLCJhZGRyZXNzTGluZU9uZSI6IjE4MDMgUm9iaW5zb24gU3QiLCJsYXRpdHVkZSI6MzIuNzk2Mjg5LCJsb25naXR1ZGUiOi05Ni45NTA4MSwicG9zdGFsQ29kZSI6Ijc1MDYwNTkzMyIsImNpdHkiOiJJcnZpbmciLCJzdGF0ZSI6IlRYIiwiY291bnRyeUNvZGUiOiJVU0EiLCJpc0Fwb0ZwbyI6ZmFsc2UsImlzUG9Cb3giOmZhbHNlLCJhZGRyZXNzVHlwZSI6IlJFU0lERU5USUFMIiwibG9jYXRpb25BY2N1cmFjeSI6ImhpZ2giLCJtb2RpZmllZERhdGUiOjE2MDM5MDA4MjkwMjksImdpZnRBZGRyZXNzIjpmYWxzZX0sImFzc29ydG1lbnQiOnsibm9kZUlkIjoiODgwIiwiZGlzcGxheU5hbWUiOiJJcnZpbmcgU3VwZXJjZW50ZXIiLCJhY2Nlc3NQb2ludHMiOm51bGwsInN1cHBvcnRlZEFjY2Vzc1R5cGVzIjpbXSwiaW50ZW50IjoiUElDS1VQIiwic2NoZWR1bGVFbmFibGVkIjpmYWxzZX0sImRlbGl2ZXJ5Ijp7ImJ1SWQiOiIwIiwibm9kZUlkIjoiODgwIiwiZGlzcGxheU5hbWUiOiJJcnZpbmcgU3VwZXJjZW50ZXIiLCJub2RlVHlwZSI6IlNUT1JFIiwiYWRkcmVzcyI6eyJwb3N0YWxDb2RlIjoiNzUwNjIiLCJhZGRyZXNzTGluZTEiOiI0MTAwIFcgQWlycG9ydCBGd3kiLCJjaXR5IjoiSXJ2aW5nIiwic3RhdGUiOiJUWCIsImNvdW50cnkiOiJVUyIsInBvc3RhbENvZGU5IjoiNzUwNjItNTkxMyJ9LCJnZW9Qb2ludCI6eyJsYXRpdHVkZSI6MzIuODM0ODIyLCJsb25naXR1ZGUiOi05Ny4wMDYzMTJ9LCJpc0dsYXNzRW5hYmxlZCI6dHJ1ZSwic2NoZWR1bGVkRW5hYmxlZCI6dHJ1ZSwidW5TY2hlZHVsZWRFbmFibGVkIjp0cnVlLCJhY2Nlc3NQb2ludHMiOlt7ImFjY2Vzc1R5cGUiOiJERUxJVkVSWV9BRERSRVNTIn1dLCJodWJOb2RlSWQiOiI4ODAiLCJpc0V4cHJlc3NEZWxpdmVyeU9ubHkiOmZhbHNlLCJzdXBwb3J0ZWRBY2Nlc3NUeXBlcyI6WyJERUxJVkVSWV9BRERSRVNTIl19LCJpbnN0b3JlIjpmYWxzZSwicmVmcmVzaEF0IjoxNjU2ODkyMTc0MDU4LCJ2YWxpZGF0ZUtleSI6InByb2Q6djI6MmE1YjhjMGItNDljYS00YmRmLWI5ZGQtMDU4MGYwNDU0NDg1In0%3D; assortmentStoreId=880; hasLocData=1; TB_Latency_Tracker_100=1; TB_Navigation_Preload_01=1; TB_SFOU-100=1; bstc=SE-eCMlICiQD9-qvrSLwM0; mobileweb=0; xpa=0t4gT|3Fi1g|3_gkh|3pRU7|4QHB7|4z-gR|5q86Y|8peS7|8zlYn|CN28l|DAwQd|FYe-R|HF4Pf|Hv6FZ|Jzlyf|L0YiL|LTD5Y|LguYm|PNKHT|QJg9U|UBwCK|UP-4s|Umo04|V0SkO|Zwajy|bgfnN|ccDGr|fZ8fK|fhOgT|gKUSD|hPI48|hqy5q|jUi64|kiHpc|lpOQb|nybjr|nzyw-|rdfjX|rxmwe|tKtX9|xidcU|yQ2ZK|zCylr; exp-ck=0t4gT13_gkh28zlYn2DAwQd3FYe-R1HF4Pf1L0YiL1QJg9U1UBwCK3UP-4s1V0SkO1ccDGr1fZ8fK1fhOgT1gKUSD2jUi641lpOQb1nybjr1nzyw-1; TS013ed49a=01538efd7cd198530d2287b9fc9ba151f52a8b0f44487402e054e6751b649b09dc4b4eaf142918268a7a09e36ce53f77fe1207c072; _astc=c1c40572ecec276aedb2c6f83bdd39d4; xpm=1%2B1656870573%2BQkrzKpUC9VOR57nHIpW22I~2a5b8c0b-49ca-4bdf-b9dd-0580f0454485%2B0; QuantumMetricSessionID=4a00ad59e74660694bda714fa5c39815; wmtboid=1656870980-2538443774-8641510720-27004721; dimensionData=746; wmlh=e5c2e32aa61615ac284a9276de14ed3362054381b92089a82376571b8d47debe; auth=MTAyOTYyMDE4THxCdE5YOtCe7FmxOdlhT34BeQbeQ5dHamicFDm5UE6zoUQaVjJ610C%2FNjPX7YpN0nPRxljEX4TgaU4lLbWC%2FDAxll6p58K75cKoQBqK4VPSEKCrJp1xdiMCoIH%2BXg3plrvBbkT8GAVfcnvLIPG4V%2B0GQffyXCzILWCwhT2pCKVbYhr%2FX93kIR5u%2B5zHJCU5hT70vcJxXRTkPu9weKikig88%2FbreJ29hxGEqOcjsqKqJ00UMGQyiYLY97sfSUmPHa5SdH68pzE0%2Bt9S8DUJrtQbMWSJ3u47VqIiCWH%2Fx5ufj0ju2a2ARuLX5AjPYu62w57XqUkHur2tmeIf5Inre2vFiSNPsKDb5A9Kl%2FbsYrD8XHZwr0o%2BDdmET694i7AyzYO8nP9uTIah1EyjmyJznmOpcuYM3JDB83VjsuzF2PrQ%3D; TS01b0be75=01538efd7ce426c5177c3124a66e9fb5b350f3b2ce27f2c8179682fda40c4d3cdc4fa71a225c35f36bf510e7ffed4063b419631a87; com.wm.reflector=\"reflectorid:0000000000000000000000@lastupd:1656873249000@firstcreate:1652578015895\"; adblocked=true; _pxff_cfp=1; _px3=5d5c298d4c383f6ba930da29168b79ed3269a99cda362604092febf97a0b2c0a:3MdTU1FgvKSYHMrw114p0G9Enlh40Dad3i0y03YQj9S2UIo4XJSjzpYbOVl6oFrXeSjCx8sqY2/8KEzUsyMWew==:1000:84QjkTZP65E2kwo1t9b5djHh2x206lSqGFX5SdYjWpN52TaDbkOzGu0D8puBPqffBnxZu2QFrPkRQbVCjq+IR/iYes6s3BnX3iFuvuXHd7dfwQlx8CHTp+2MZQ1ka6hCy7Srbbf1uavdxexSNlreCRove2PKYwZdIxgesG3FODGT8f0W8fdnCLopsLJ23qC8ApTx2SrQgSSPQGhM20m96g==; xptwg=3895814893:24BBDE3F2666DE0:5F38D2B:CA0168AE:6C68D3A1:32CD687E:; akavpau_p2=1656873859~id=1703fa58d7306e6b79252ab8cdd864d9; bm_sv=07AD5213C671C1E8CC0B5CE28648BBC9~YAAQ2kJ1aH8ZcrOBAQAAYK9XxRClrJP9d0FWioKvzW92KYzEMiEuYYcl634wHYGfvIejiAl/4f3VkonkQUHPcBJ3r6shEGyel2FUld+2nHHv2FFuj+4lSCM1Hx05uRxBdTH9IdHdtnLxzhbnp0b8I0CH9q/cFgF/4sdDhb3fKW0FfcqDOf0ppq4dwRRlFVOofATJ8aHpbG++KOC7ReLeHOee6zYJS/NnApP/MceNUmjnYuZygeENVcTSG2x1xjwzQNU=~1; ACID=4de10fc1-b721-47a5-b6e9-be80f9c28870; TS013ed49a=01538efd7cd198530d2287b9fc9ba151f52a8b0f44487402e054e6751b649b09dc4b4eaf142918268a7a09e36ce53f77fe1207c072; _m=9; assortmentStoreId=880; auth=MTAyOTYyMDE4THxCdE5YOtCe7FmxOdlhT34BeQbeQ5dHamicFDm5UE6zoUQaVjJ610C%2FNjPX7YpN0nPRxljEX4TgaU4lLbWC%2FDAxll6p58K75cKoQBqK4VPSEKCrJp1xdiMCoIH%2BXg3plrvBbkT8GAVfcnvLIPG4V%2B0GQffyXCzILWCwhT2pCKVbYhr%2FX93kIR5u%2B5zHJCU5hT70vcJxXRTkPu9weKikig88%2FbreJ29hxGEqOcjsqKqJ00UMGQyiYLY97sfSUmPHa5SdH68pzE0%2Bt9S8DUJrtQbMWSJ3u47VqIiCWH%2Fx5ufj0ju2a2ARuLX5AjPYu62w57XqUkHur2tmeIf5Inre2vFiSNPsKDb5A9Kl%2FbsYrD8XHZwr0o%2BDdmET694i7AyzYO8nP9uTIah1EyjmyJznmOpcuYM3JDB83VjsuzF2PrQ%3D; bm_mi=D2D434B475A2F0E928A793395C41D89F~YAAQ0EJ1aBx2S7CBAQAAJVhIxRBFuQegGOjaEsAYqmVCDQX2InjwjaMlilVH4L6DuwuYyeVI1frkYHT8qaGmLNkhcQA3bRQqai5xUHMYyrSHLGke1lMSZWxtar5cIUwj2OkKMehibBQkcHOyytLg9YyUbjsHMpzANaK7z2TSr4P3FiL6mtKx+nF2+nVtLehin9IVhLMBFHOnO1j5PieAS106Rewyrj5f5NyOdCd5bq9bhhEpSX+ORAMquVrz5Z3YvOTDIMZi6mPCfwhygCEo5z0RIs/rs0MWjQXq9edXcjcj8XIRwxBwg+tB+8CR77fY/nYv1Xiw~1; bm_sv=07AD5213C671C1E8CC0B5CE28648BBC9~YAAQzUJ1aDgecJqBAQAAmxdkxRA4p0kf+YP+1NCb9s1Mvj4hrWhO7BbSApPkY2pvUaCy7I1o9RTIk5jYl3bHPzpaOpoqdPb6IlCFBfz+tSJNNebRylOdlpH8mfM0YrmAoL/6cHnqBjccYVobiK6/NycFVfu2NgU4wIG6XIWw6GCTdO10gKowwoYYczPeCvHG+peUqr37MhjNtl6bmDNX2DcCfOzH/6NH0fofqmNCGHJCZ/rqWCGWNVr/n3iFc9Quexs=~1; bstc=SE-eCMlICiQD9-qvrSLwM0; com.wm.reflector=\"reflectorid:0000000000000000000000@lastupd:1656874080000@firstcreate:1652578015895\"; exp-ck=0t4gT13_gkh28zlYn2DAwQd3FYe-R1HF4Pf1L0YiL1QJg9U1UBwCK3UP-4s1V0SkO1ccDGr1fZ8fK1fhOgT1gKUSD2jUi641lpOQb1nybjr1nzyw-1; hasACID=true; hasLocData=1; locDataV3=eyJpc0RlZmF1bHRlZCI6ZmFsc2UsImlzRXhwbGljaXQiOmZhbHNlLCJpbnRlbnQiOiJQSUNLVVAiLCJwaWNrdXAiOlt7ImJ1SWQiOiIwIiwibm9kZUlkIjoiODgwIiwiZGlzcGxheU5hbWUiOiJJcnZpbmcgU3VwZXJjZW50ZXIiLCJub2RlVHlwZSI6IlNUT1JFIiwiYWRkcmVzcyI6eyJwb3N0YWxDb2RlIjoiNzUwNjIiLCJhZGRyZXNzTGluZTEiOiI0MTAwIFcgQWlycG9ydCBGd3kiLCJjaXR5IjoiSXJ2aW5nIiwic3RhdGUiOiJUWCIsImNvdW50cnkiOiJVUyIsInBvc3RhbENvZGU5IjoiNzUwNjItNTkxMyJ9LCJnZW9Qb2ludCI6eyJsYXRpdHVkZSI6MzIuODM0ODIyLCJsb25naXR1ZGUiOi05Ny4wMDYzMTJ9LCJpc0dsYXNzRW5hYmxlZCI6dHJ1ZSwic2NoZWR1bGVkRW5hYmxlZCI6dHJ1ZSwidW5TY2hlZHVsZWRFbmFibGVkIjp0cnVlLCJodWJOb2RlSWQiOiI4ODAiLCJzdG9yZUhycyI6IjA2OjAwLTIzOjAwIiwic3VwcG9ydGVkQWNjZXNzVHlwZXMiOlsiUElDS1VQX0NVUkJTSURFIiwiUElDS1VQX0lOU1RPUkUiXX1dLCJzaGlwcGluZ0FkZHJlc3MiOnsiaWQiOiJjZDE1ODNhNS1iYTdjLTQ5ZTgtODU5Mi1jNjQwZGZmYWFlNTkiLCJhZGRyZXNzTGluZU9uZSI6IjE4MDMgUm9iaW5zb24gU3QiLCJsYXRpdHVkZSI6MzIuNzk2Mjg5LCJsb25naXR1ZGUiOi05Ni45NTA4MSwicG9zdGFsQ29kZSI6Ijc1MDYwNTkzMyIsImNpdHkiOiJJcnZpbmciLCJzdGF0ZSI6IlRYIiwiY291bnRyeUNvZGUiOiJVU0EiLCJpc0Fwb0ZwbyI6ZmFsc2UsImlzUG9Cb3giOmZhbHNlLCJhZGRyZXNzVHlwZSI6IlJFU0lERU5USUFMIiwibG9jYXRpb25BY2N1cmFjeSI6ImhpZ2giLCJtb2RpZmllZERhdGUiOjE2MDM5MDA4MjkwMjksImdpZnRBZGRyZXNzIjpmYWxzZX0sImFzc29ydG1lbnQiOnsibm9kZUlkIjoiODgwIiwiZGlzcGxheU5hbWUiOiJJcnZpbmcgU3VwZXJjZW50ZXIiLCJhY2Nlc3NQb2ludHMiOm51bGwsInN1cHBvcnRlZEFjY2Vzc1R5cGVzIjpbXSwiaW50ZW50IjoiUElDS1VQIiwic2NoZWR1bGVFbmFibGVkIjpmYWxzZX0sImRlbGl2ZXJ5Ijp7ImJ1SWQiOiIwIiwibm9kZUlkIjoiODgwIiwiZGlzcGxheU5hbWUiOiJJcnZpbmcgU3VwZXJjZW50ZXIiLCJub2RlVHlwZSI6IlNUT1JFIiwiYWRkcmVzcyI6eyJwb3N0YWxDb2RlIjoiNzUwNjIiLCJhZGRyZXNzTGluZTEiOiI0MTAwIFcgQWlycG9ydCBGd3kiLCJjaXR5IjoiSXJ2aW5nIiwic3RhdGUiOiJUWCIsImNvdW50cnkiOiJVUyIsInBvc3RhbENvZGU5IjoiNzUwNjItNTkxMyJ9LCJnZW9Qb2ludCI6eyJsYXRpdHVkZSI6MzIuODM0ODIyLCJsb25naXR1ZGUiOi05Ny4wMDYzMTJ9LCJpc0dsYXNzRW5hYmxlZCI6dHJ1ZSwic2NoZWR1bGVkRW5hYmxlZCI6dHJ1ZSwidW5TY2hlZHVsZWRFbmFibGVkIjp0cnVlLCJhY2Nlc3NQb2ludHMiOlt7ImFjY2Vzc1R5cGUiOiJERUxJVkVSWV9BRERSRVNTIn1dLCJodWJOb2RlSWQiOiI4ODAiLCJpc0V4cHJlc3NEZWxpdmVyeU9ubHkiOmZhbHNlLCJzdXBwb3J0ZWRBY2Nlc3NUeXBlcyI6WyJERUxJVkVSWV9BRERSRVNTIl19LCJpbnN0b3JlIjpmYWxzZSwicmVmcmVzaEF0IjoxNjU2ODk1NTkzODMxLCJ2YWxpZGF0ZUtleSI6InByb2Q6djI6MmE1YjhjMGItNDljYS00YmRmLWI5ZGQtMDU4MGYwNDU0NDg1In0%3D; locGuestData=eyJpbnRlbnQiOiJTSElQUElORyIsImlzRXhwbGljaXQiOmZhbHNlLCJzdG9yZUludGVudCI6IlBJQ0tVUCIsIm1lcmdlRmxhZyI6ZmFsc2UsImlzRGVmYXVsdGVkIjpmYWxzZSwicGlja3VwIjp7Im5vZGVJZCI6IjExNzgiLCJ0aW1lc3RhbXAiOjE2NTQ3MjgyMDc4MzF9LCJwb3N0YWxDb2RlIjp7InRpbWVzdGFtcCI6MTY1NDcyODIwNzgzMSwiYmFzZSI6Ijc2MDIxIn0sInZhbGlkYXRlS2V5IjoicHJvZDp2Mjo0ZGUxMGZjMS1iNzIxLTQ3YTUtYjZlOS1iZTgwZjljMjg4NzAifQ%3D%3D; mobileweb=0; vtc=QkrzKpUC9VOR57nHIpW22I; xpa=0t4gT|3Fi1g|3_gkh|3pRU7|4QHB7|4z-gR|5q86Y|8peS7|8zlYn|CN28l|DAwQd|FYe-R|HF4Pf|Hv6FZ|Jzlyf|L0YiL|LTD5Y|LguYm|PNKHT|QJg9U|UBwCK|UP-4s|Umo04|V0SkO|Zwajy|bgfnN|ccDGr|fZ8fK|fhOgT|gKUSD|hPI48|hqy5q|jUi64|kiHpc|lpOQb|nybjr|nzyw-|rdfjX|rxmwe|tKtX9|xidcU|yQ2ZK|zCylr; xpm=1%2B1656872266%2BQkrzKpUC9VOR57nHIpW22I~2a5b8c0b-49ca-4bdf-b9dd-0580f0454485%2B0; xptwg=1323620301:13DB882089C7AD0:3379965:36AEDBC2:82A8EF4C:19E8DD7D:; TS01b0be75=01538efd7c16d8c9ec64a05be84d4a28057e1594cf258eddd38816a19623d65821ac2eb57ae8f2d471f3273ebe04bf708a018a9c0a; _pxhd=37d32386e76a25678b459756a0e6c93d9a522bf0552513b996a3611c705d562c:1ccc0ac4-d3ee-11ec-b1d1-797652794357; akavpau_p2=1656874680~id=d12aaae3ee02574f881c2c51f72cfda5; wmtboid=1656870513-6150728226-20938649280-65433279")
                .addHeader("device_profile_ref_id", "9uk325BJcmFTWtjzz4jfcLQdIbiTB44ay6w_")
                .addHeader("origin", "https://www.walmart.com")
                .addHeader("referer", "https://www.walmart.com/search?q=beef")
                .addHeader("sec-fetch-dest", "empty")
                .addHeader("sec-fetch-mode", "cors")
                .addHeader("sec-fetch-site", "same-origin")
                .addHeader("traceparent", "8qwm0N8XIJHAeuiHAskJYqf4lHJ-ILw-2wu4")
                .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36")
                .addHeader("wm_mp", "true")
                .addHeader("wm_page_url", "https://www.walmart.com/search?q=beef")
                .addHeader("wm_qos.correlation_id", "8qwm0N8XIJHAeuiHAskJYqf4lHJ-ILw-2wu4")
                .addHeader("x-apollo-operation-name", "Search")
                .addHeader("x-enable-server-timing", "1")
                .addHeader("x-latency-trace", "1")
                .addHeader("x-o-bu", "WALMART-US")
                .addHeader("x-o-ccm", "server")
                .addHeader("x-o-correlation-id", "8qwm0N8XIJHAeuiHAskJYqf4lHJ-ILw-2wu4")
                .addHeader("x-o-gql-query", "query Search")
                .addHeader("x-o-mart", "B2C")
                .addHeader("x-o-platform", "rweb")
                .addHeader("x-o-platform-version", "main-1.4.0-ab7aaa")
                .addHeader("x-o-segment", "oaoh")
                .build();
        try {
            Response response = client.newCall(request).execute();
            System.out.println(response.code());
            assert response.body() != null;
            return response.body().string();
        } catch (IOException e) {
            System.out.println("Bad request to Walmart.com");
            return "";
        }
    }

    private int checkUnitOfMeasurement(WmProduct.PriceInfo.UnitPrice unitPrice){
        if (unitPrice.getPriceString() != null) {
            if (unitPrice.getPriceString().toLowerCase().contains("oz")) {
                return OZ;
            } else if (unitPrice.getPriceString().toLowerCase().contains("lb")) {
                return LB;
            }
        }
        return NO_UNIT_PROVIDED;
    }
}
