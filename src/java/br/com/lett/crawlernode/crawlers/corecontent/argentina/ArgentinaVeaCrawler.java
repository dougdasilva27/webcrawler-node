package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import com.mongodb.util.JSON;

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;

/**
 * Date: 06/12/2016
 * 
 * 1) Only one sku per page.
 * 
 * Price crawling notes:
 * 	1 - This crawler has no product pages
 *	2 - To capture the information is accessed a search API that is expected to return only one product
 *	3 - The pages added are search pages where there is only one product, with its specific keyword
 *	4 - This keyword is used to access the api
 *	5 - No payment information
 *	6 - There is no bank slip
 * 
 * @author Gabriel Dornelas
 *
 */
public class ArgentinaVeaCrawler extends Crawler {

	private final String HOME_PAGE = "https://www.veadigital.com.ar/";

	public ArgentinaVeaCrawler(Session session) {
		super(session);
	}

	@Override
	public void handleCookiesBeforeFetch() {
		Logging.printLogDebug(logger, session, "Adding cookie...");

		Map<String,String> cookiesMap = DataFetcher.fetchCookies(session, "https://www.veadigital.com.ar/Login/PreHome.aspx", cookies, 1);

		for (String cookieName : cookiesMap.keySet()) {
			if ("ASP.NET_SessionId".equals(cookieName)) {
				BasicClientCookie cookie = new BasicClientCookie(cookieName, cookiesMap.get(cookieName));
				cookie.setDomain("www.veadigital.com.ar");
				cookie.setPath("/");
				this.cookies.add(cookie);
			}
		}
	}

	@Override
	public boolean shouldVisit() {
		String href = session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<>();

		if ( isProductPage(session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());


			JSONObject searchJson = crawlProductApi(session.getOriginalURL());
			JSONObject productJson = crawlImportantInformations(searchJson);

			String internalId = crawlInternalId(productJson);
			String internalPid = crawlInternalPid();
			String name = crawlName(productJson);
			Float price = crawlPrice(productJson);
			Integer stock = crawlStock(productJson);
			Prices prices = crawlPrices(price);
			boolean available = crawlAvailability(stock);
			CategoryCollection categories = crawlCategories(productJson);
			String primaryImage = crawlPrimaryImage(productJson);
			String secondaryImages = crawlSecondaryImages();
			String description = crawlDescription(internalId);
			JSONArray marketplace = crawlMarketplace();

			// Creating the product
			Product product = ProductBuilder.create()
					.setUrl(session.getOriginalURL())
					.setInternalId(internalId)
					.setInternalPid(internalPid)
					.setName(name)
					.setPrice(price)
					.setPrices(prices)
					.setAvailable(available)
					.setCategory1(categories.getCategory(0))
					.setCategory2(categories.getCategory(1))
					.setCategory3(categories.getCategory(2))
					.setPrimaryImage(primaryImage)
					.setSecondaryImages(secondaryImages)
					.setDescription(description)
					.setStock(stock)
					.setMarketplace(marketplace)
					.build();

			products.add(product);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return products;

	}

	private boolean isProductPage(String url) {
		if (url.contains("_query")) {
			return true;
		}
		return false;
	}

	private String crawlInternalId(JSONObject json) {
		String internalId = null;

		if(json.has("IdArticulo")){
			internalId = json.getString("IdArticulo");
		}

		return internalId;
	}

	/**
	 * There is no internalPid.
	 * 
	 * @param document
	 * @return
	 */
	private String crawlInternalPid() {
		return null;
	}

	private String crawlName(JSONObject json) {
		String name = null;

		if (json.has("DescripcionArticulo")) {
			name = json.getString("DescripcionArticulo");
		}

		return name;
	}

	private Float crawlPrice(JSONObject json) {
		Float price = null;

		if(json.has("Precio")){
			String priceText = json.getString("Precio").replaceAll(",", "").trim();

			if(!priceText.isEmpty()){
				price = Float.parseFloat(priceText);
			}
		}

		return price;
	}

	private Integer crawlStock(JSONObject json){
		Integer stock = null;

		if (json.has("Stock")) {
			stock = Integer.parseInt(json.getString("Stock"));
		}

		return stock;
	}

	private boolean crawlAvailability(Integer stock) {
		boolean available = false;

		if(stock != null && stock > 0){
			available = true;
		}

		return available;
	}

	private JSONArray crawlMarketplace() {
		return new JSONArray();
	}


	private String crawlPrimaryImage(JSONObject json) {
		String primaryImage = null;


		if(json.has("IdArchivoBig")){
			String image = json.getString("IdArchivoBig").trim();

			if(!image.isEmpty()){
				primaryImage = "https://www.veadigital.com.ar/VeaComprasArchivos/Archivos/" + image;
			} else if(json.has("IdArchivoSmall")){
				image = json.getString("IdArchivoSmall").trim();

				if(!image.isEmpty()){
					primaryImage = "https://www.veadigital.com.ar/VeaComprasArchivos/Archivos/" + image;
				}
			}

		} else if(json.has("IdArchivoSmall")){
			String image = json.getString("IdArchivoSmall").trim();

			if(!image.isEmpty()){
				primaryImage = "https://www.veadigital.com.ar/VeaComprasArchivos/Archivos/" + image;
			}
		}

		if(primaryImage != null && primaryImage.isEmpty()){
			primaryImage = null;
		}

		return primaryImage;
	}

	/**
	 * Has no secondary Images in this market
	 * @param document
	 * @return
	 */
	private String crawlSecondaryImages() {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private CategoryCollection crawlCategories(JSONObject json) {
		CategoryCollection categories = new CategoryCollection();

		if(json.has("Grupo_Tipo")){
			String category = json.getString("Grupo_Tipo");

			if(!category.isEmpty()){
				categories.add(category);
			}
		}

		return categories;
	}

	private String crawlDescription(String internalId) {
		StringBuilder description = new StringBuilder();
		String url = "https://www.veadigital.com.ar/Comprar/HomeService.aspx/ObtenerDetalleDelArticuloLevex";
		String payload = "{code:'"+ internalId +"'}";

		Map<String,String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");

		String response = DataFetcher.fetchPagePOSTWithHeaders(url, session, payload, cookies, 1, headers);

		if(response != null && response.contains("descr")){
			JSONObject jsonD = parseJsonLevex(new JSONObject(response));
			if (jsonD.has("descr")) {
				description.append(jsonD.getString("descr"));
			}
		}

		return description.toString();
	}

	/**
	 * There is no bankSlip price.
	 * 
	 * There is no card payment options, other than cash price.
	 * So for installments, we will have only one installment for each
	 * card brand, and it will be equals to the price crawled on the sku
	 * main page.
	 * 
	 * @param doc
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(Float price) {
		Prices prices = new Prices();

		if(price != null){
			Map<Integer,Float> installmentPriceMap = new TreeMap<>();
			installmentPriceMap.put(1, price);

			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.CABAL.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);
		}

		return prices;
	}

	/**
	 * Crawl api of search when probably has only one product
	 * @param url
	 * @return
	 */
	private JSONObject crawlProductApi(String url) {
		JSONObject json = new JSONObject();

		Map<String,String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");

		// Nome do produto na busca
		String[] tokens = url.split("=");

		String urlSearch = "https://www.veadigital.com.ar/Comprar/HomeService.aspx/ObtenerArticulosPorDescripcionMarcaFamiliaLevex";
		String urlParameters = "{IdMenu:\"\",textoBusqueda:\""+ tokens[tokens.length-1] +"\","
				+ " producto:\"\", marca:\"\", pager:\"\", ordenamiento:0, precioDesde:\"\", precioHasta:\"\"}";

		String jsonString = DataFetcher.fetchPagePOSTWithHeaders(urlSearch, session, urlParameters, cookies, 1, headers);

		if (jsonString != null && jsonString.startsWith("{")) {
			json = new JSONObject(jsonString);
		}

		return json;
	}

	private JSONObject crawlImportantInformations(JSONObject json){
		JSONObject jsonProduct = new JSONObject();

		if (json != null) {
			JSONObject jsonD = parseJsonLevex(json);

			if (jsonD.has("ResultadosBusquedaLevex")) {
				JSONArray products = jsonD.getJSONArray("ResultadosBusquedaLevex");

				if (products.length() > 0) {
					jsonProduct = products.getJSONObject(0);
				}
			}

		}

		return jsonProduct;
	}

	private JSONObject parseJsonLevex(JSONObject json){
		JSONObject jsonD = new JSONObject();

		if (json.has("d")) {
			String dParser = JSON.parse(json.getString("d")).toString();
			jsonD = new JSONObject(dParser);
		}

		return jsonD;
	}

}
