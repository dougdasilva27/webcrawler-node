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

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.POSTFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

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
public class ArgentinaJumboCrawler extends Crawler {

	private final String HOME_PAGE = "https://www.jumbo.com.ar/";

	public ArgentinaJumboCrawler(Session session) {
		super(session);
	}

	@Override
	public void handleCookiesBeforeFetch() {
		Logging.printLogDebug(logger, session, "Adding cookie...");

		Map<String,String> cookiesMap = DataFetcher.fetchCookies(session, "https://www.jumbo.com.ar/Comprar/Home.aspx", cookies, 1);

		for(String cookieName : cookiesMap.keySet()){
			if(cookieName.equals("ASP.NET_SessionId")){
				BasicClientCookie cookie = new BasicClientCookie(cookieName, cookiesMap.get(cookieName));
				cookie.setDomain("www.jumbo.com.ar");
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

			String productUrl = crawlNewUrl();

			JSONObject searchJson = crawlProductApi(productUrl);
			JSONArray productsArray = crawlProducts(searchJson);
			
			for(int i = 0; i < productsArray.length(); i++) {
				JSONObject productJson = productsArray.getJSONObject(i);
	
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
				Marketplace marketplace = crawlMarketplace();
	
				// Creating the product
				Product product = ProductBuilder.create()
						.setUrl(productUrl)
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
			}

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
		String internalPid = null;
		return internalPid;
	}

	private String crawlName(JSONObject json) {
		String name = null;

		if(json.has("DescripcionArticulo")){
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

		if(json.has("Stock")){
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

	private Marketplace crawlMarketplace() {
		return new Marketplace();
	}

	private String crawlPrimaryImage(JSONObject json) {
		String primaryImage = null;


		if(json.has("IdArchivoBig")) {
			String image = json.getString("IdArchivoBig").trim();

			if(!image.isEmpty()){
				primaryImage = "https://www.jumbo.com.ar/JumboComprasArchivos/Archivos/" + image;
			} else if(json.has("IdArchivoSmall")){
				image = json.getString("IdArchivoSmall").trim();

				if(!image.isEmpty()){
					primaryImage = "https://www.jumbo.com.ar/JumboComprasArchivos/Archivos/" + image;
				}
			}

		} else if(json.has("IdArchivoSmall")) {
			String image = json.getString("IdArchivoSmall").trim();

			if(!image.isEmpty()){
				primaryImage = "https://www.jumbo.com.ar/JumboComprasArchivos/Archivos/" + image;
			}
		}

		if(primaryImage != null && primaryImage.isEmpty()) {
			primaryImage = null;
		}

		return primaryImage;
	}

	/**
	 * There is no secondary Images in this market
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

	/**
	 * We don't crawl categories because this market,
	 * only have info of categories in categorie page,
	 * so if we need categories information, we will see
	 * in share of categories.
	 * 
	 * @param json
	 * @return
	 */
	private CategoryCollection crawlCategories(JSONObject json) {
		CategoryCollection categories = new CategoryCollection();

		return categories;
	}

	private String crawlDescription(String internalId) {
		StringBuilder description = new StringBuilder();
		String url = "https://www.jumbo.com.ar/Comprar/HomeService.aspx/ObtenerDetalleDelArticuloLevex";
		String payload = "{code:'"+ internalId +"'}";

		Map<String,String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");

		String response = POSTFetcher.fetchPagePOSTWithHeaders(url, session, payload, cookies, 1, headers);

		if (response != null && response.contains("descr")) {
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
	private JSONObject crawlProductApi(String url){
		JSONObject json = new JSONObject();

		Map<String,String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");

		// Nome do produto na busca
		String[] tokens = url.split("=");

		String urlSearch = "https://www.jumbo.com.ar/Comprar/HomeService.aspx/ObtenerArticulosPorDescripcionMarcaFamiliaLevex";
		String urlParameters = "{IdMenu:\"\",textoBusqueda:\""+ CommonMethods.removeAccents(tokens[tokens.length-1]) +"\","
				+ " producto:\"\", marca:\"\", pager:\"\", ordenamiento:0, precioDesde:\"\", precioHasta:\"\"}";

		String jsonString = POSTFetcher.fetchPagePOSTWithHeaders(urlSearch, session, urlParameters, cookies, 1, headers);

		if (jsonString != null && jsonString.startsWith("{")) {
			json = new JSONObject(jsonString);
		}

		return json;
	}

	/**
	 * Crawl part of json
	 * @param json
	 * @return
	 */
	private JSONArray crawlProducts(JSONObject json){
		if(json != null){
			JSONObject jsonD = parseJsonLevex(json);

			if(jsonD.has("ResultadosBusquedaLevex")){
				JSONArray products = jsonD.getJSONArray("ResultadosBusquedaLevex");

				return products;
			}

		}

		return new JSONArray();
	}

	/**
	 * Json comes like this
	 * { "d" : {\"articulo\" : {\"descripcion\" : \"Agua\", ....
	 * 
	 * so in this function we get all information in object "d"
	 * @param json
	 * @return
	 */
	private JSONObject parseJsonLevex(JSONObject json){
		JSONObject jsonD = new JSONObject();

		if(json.has("d")){
			String dParser = JSON.parse(json.getString("d")).toString();
			jsonD = new JSONObject(dParser);
		}

		return jsonD;
	}

	/**
	 * The product url is a search with the exact name of it, when the product has already been discovered we get its name updated
	 * in a "add to cart" api to update your url, this api requires the internalId of the product
	 * @return
	 */
	private String crawlNewUrl() {
		String url = session.getOriginalURL();
		
		if(session.getInternalId() != null && !session.getInternalId().isEmpty()) {
			String urlFisrtPeace = "https://www.jumbo.com.ar/Comprar/Home.aspx?#_atCategory=false&_atGrilla=true&_query=";
			String nameEncoded = crawlNameFromAPI(session.getInternalId());
			
			if(nameEncoded != null) {
				url = urlFisrtPeace + nameEncoded;
			}
		}
		
		return url;
	}
	
	/**
	 * "add to cart" api
	 * 
	 * {\"Carrito\":{\"Total\":\"0,00\",\"Puntos\":\"0\",\"Mensajes\":\"\",
	 * \"Articulos\":[{\"idArticulo\":\"450570\",\"imagenChica\":\"209001\",\"imagenGrande\":\"209206\",
	 * \"descripcion\":\"Pimienta Negra En Grano X 50Gr-Bsa-Gr.-50\",\"precioDeVenta\":\"59.90\",
	 * \"totalArticulo\":\"59.90\",\"unidadDeMedida\":\"100 Gr\",\"precioUnidadDeMedida\":\"119,80\",\"stockMaximo\":\"43.00\",
	 * \"cantidadPedida\":\"1.00\",\"idMenu\":\"20859\",\"Observaciones\":\"\",\"pesable\":\"False\",\"caracteristicasProducto\":\"\",
	 * \"Grupo_Marca\":\"SIN MARCA\"}]}}
	 * 
	 * @param internalId
	 * @return
	 */
	private String crawlNameFromAPI(String internalId) {
		String name = null;
		
		String url = "https://www.jumbo.com.ar/Comprar/HomeService.aspx/SalvarArticuloEnCarrito";
		String payload = "{\"accion\": \"agregarProducto\", "
				+ "\"articulos\": '[{\"id\":\""+ internalId +"\",\"cant\":\"1\",\"unidad\":\"\",\"remplazo\":\"0\",\"descripcionLarga\":\"\","
						+ "\"precioVenta\":\"\",\"strPesable\":\"\"}]'}";
		
		Map<String,String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		
		String jsonString = POSTFetcher.fetchPagePOSTWithHeaders(url, session, payload, cookies, 1, headers);
		
		if(jsonString != null && jsonString.startsWith("{") && jsonString.endsWith("}")) {
			JSONObject jsonCart = new JSONObject();
			try {
				jsonCart = parseJsonLevex(new JSONObject(jsonString)).getJSONObject("Carrito");
			} catch (Exception e) {
				Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
			}
			
			if(jsonCart.has("Articulos")) {
				JSONObject articulo = jsonCart.getJSONArray("Articulos").getJSONObject(0);
				
				if(articulo.has("descripcion")) {
					name = articulo.getString("descripcion").replaceAll(" ", "%20").replaceAll("´", "%B4");
				}
			}
		}
		
		return name;
	}
}
