package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;

public class SaopauloExtraCrawler extends Crawler {

	public SaopauloExtraCrawler(Session session) {
		super(session);
	}

	@Override
	public void handleCookiesBeforeFetch() {

		// Criando cookie da loja 21 = São Paulo capital
		BasicClientCookie cookie = new BasicClientCookie("ep.selected_store", "21");
		cookie.setDomain(".deliveryextra.com.br");
		cookie.setPath("/");
		cookie.setExpiryDate(new Date(System.currentTimeMillis() + 604800000L + 604800000L));
		this.cookies.add(cookie);

		// Criando cookie simulando um usuário logado
		BasicClientCookie cookie2 = new BasicClientCookie("ep.customer_logged", "-2143598207");
		cookie2.setDomain(".deliveryextra.com.br");
		cookie2.setPath("/");
		cookie2.setExpiryDate(new Date(System.currentTimeMillis() + 604800000L + 604800000L));
		this.cookies.add(cookie2);
	}


	@Override
	public boolean shouldVisit() {
		String href = session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && href.startsWith("http://www.deliveryextra.com.br/");
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<>();

		if(session.getOriginalURL().contains("/produto/")) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			JSONObject chaordicSKUJson = crawlChaordicSKUJson(doc);

			// internal id
			String internalId = crawlInternalId(doc);

			// internal pid
			String internalPid = internalId;

			// name
			String name = crawlName(doc);

			// price
			Float price = crawlPrice(doc);

			// availability
			boolean available = crawlAvailability(chaordicSKUJson);

			// categories
			ArrayList<String> categories = crawlCategories(doc);
			String category1 = getCategory(categories, 0);
			String category2 = getCategory(categories, 1);
			String category3 = getCategory(categories, 2);

			// primary image
			String primaryImage = crawlPrimaryImage(doc);

			// secondary images
			String secondaryImages = crawlSecondaryImages(doc);			

			// description
			String description = "";   
			Element element_moreinfo = doc.select("#nutritionalChart .product-info .product-info__text").first();
			if(element_moreinfo != null) {
				description = description + element_moreinfo.html();
			}

			// stock
			Integer stock = null;

			// marketplace
			JSONArray marketplace = null;

			// Prices 
			Prices prices = crawlPrices(doc, price);

			Product product = new Product();

			product.setUrl(session.getOriginalURL());
			product.setInternalId(internalId);
			product.setInternalPid(internalPid);
			product.setName(name);
			product.setPrice(price);
			product.setPrices(prices);
			product.setCategory1(category1);
			product.setCategory2(category2);
			product.setCategory3(category3);
			product.setPrimaryImage(primaryImage);
			product.setSecondaryImages(secondaryImages);
			product.setDescription(description);
			product.setStock(stock);
			product.setMarketplace(marketplace);
			product.setAvailable(available);

			products.add(product);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return products;
	}

	private String crawlInternalId(Document document) {
		return Integer.toString(Integer.parseInt(session.getOriginalURL().split("/")[4]));
	}

	private String crawlName(Document document) {
		String name = null;
		Element elementName = document.select(".product-header h1.product-header__heading").first();
		if (elementName != null) {
			name = elementName.text().replace("'", "").trim();
		}
		return name;
	}

	private Float crawlPrice(Document document) {
		Float price = null;

		// treating two cases: preco de / preco por
		Element elementPrice = document.select("#productForm .product-control__price.product-control__container.price_per .value.inline--middle").first();
		if (elementPrice == null) {
			elementPrice = document.select("#productForm .product-control__price.product-control__container .value.inline--middle").first();
			if (elementPrice != null) {
				price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}
		} else {
			price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
		}
		return price;
	}

	private boolean crawlAvailability(JSONObject chaordicSKUJson) {
		boolean available = false;
		if (chaordicSKUJson.has("status")) {
			if (chaordicSKUJson.getString("status").equals("available")) {
				available = true;
			}
		}
		return available;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();

		Elements elementsCategories = document.select("#breadCrumbArea .breadcrumbs.group ul li a");

		for (int i = 1; i < elementsCategories.size(); i++) {
			categories.add(elementsCategories.get(i).text().trim());
		}

		return categories;
	}

	private String getCategory(ArrayList<String> categories, int n) {
		if (n < categories.size()) {
			return categories.get(n);
		}

		return "";
	}

	private String crawlPrimaryImage(Document doc) {
		String primaryImage = null;
		Element primaryImageElement = doc.select("#product-image a").first();
		
		if (primaryImageElement != null) {
			String href = primaryImageElement.attr("href");
			
			if (!href.isEmpty()) {
				if (!href.startsWith("http://www.deliveryextra.com.br")) {
					primaryImage = "http://www.deliveryextra.com.br" + href;
				} else {
					primaryImage = href;
				}
			}
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;

		JSONArray secondaryImagesArray = new JSONArray();

		Elements secondaryImagesElements = document.select("#product-image__gallery a");

		for (int i = 1; i < secondaryImagesElements.size(); i++) { //starts with index 1 because de primary image is the first image
			Element e = secondaryImagesElements.get(i);

			if (e != null) {
				String dataZoomAttr = e.attr("data-zoom");

				if (!dataZoomAttr.isEmpty()) {
					if (!dataZoomAttr.startsWith("http://www.deliveryextra.com.br")) {
						dataZoomAttr = "http://www.deliveryextra.com.br" + dataZoomAttr;
					}
					secondaryImagesArray.put(dataZoomAttr);
				}
			}
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}	

	/**
	 * In this market, installments not appear in product page
	 * 
	 * @param doc
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(Document doc, Float price){
		Prices prices = new Prices();

		if(price != null){
			Map<Integer,Float> installmentPriceMap = new HashMap<>();

			installmentPriceMap.put(1, price);
			prices.insertBankTicket(price);

			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
		}

		return prices;
	}

	/**
	 * e.g:
	 * 
	 * "product":{
	 *		"images":{
	 *			"1200x1200":"/img/uploads/1/607/507607.jpg",
	 *			"80x80":"/img/uploads/1/607/507607x80x80.jpg",
	 *			"200x200":"/img/uploads/1/607/507607x200x200.jpg"
	 *		},
	 * 		"skus":[
	 *			{
	 *			"sku":"1051366",
	 *			"status":"available"
	 *			}
	 *		],
	 *		"price":8.85,
	 *		"old_price":8.85,
	 *		"name":"Sabonete Líquido Antibacteriano PROTEX Omega 3 250ml",
	 *		"description":"Sabonete Liquido protex omega 3 250ML Características e Benefícios: Protex OMEGA 3 combina a proteção antibacteriana prolongada de Protex com o OMEGA 3. Sua formula ajuda a manter a pele saudável, protegida e com sensação hidratante. Elimina 99,9% das bactérias. Ingredientes Ativos: Triclocarban. Instruções de Uso: Molhar o corpo ou a região desejada, ensaboar normalmente e enxaguar.",
	 *		"id":"329106",
	 *		"categories":[
	 *			{
	 *			"name":"Higiene e cuidados diários",
	 *			"id":"127"
	 *			},
	 *			{
	 *			"name":"Sabonetes",
	 *			"id":"2258",
	 *			"parents":[
	 *				"127"
	 *			]
	 *			}
	 *		],
	 *		"url":"deliveryextra.com.br/produto/329106/sabonete-liquido-antibacteriano-protex-omega-3-250ml",
	 *		"status":"available"
	 *	}
	 * 
	 * @param document
	 * @return
	 */
	private JSONObject crawlChaordicSKUJson(Document document) {
		Elements scriptTags = document.getElementsByTag("script");
		JSONObject skuJson = null;

		for (Element tag : scriptTags){                
			for (DataNode node : tag.dataNodes()) {
				if(tag.html().trim().startsWith("window.chaordic_meta")) {
					skuJson = new JSONObject
							(node.getWholeData().split(Pattern.quote("window.chaordic_meta= "))[1] +
									node.getWholeData().split(Pattern.quote("window.chaordic_meta= "))[1].split(Pattern.quote("}]}"))[0]
									);

				}
			}        
		}

		if (skuJson.has("product")) {
			return skuJson.getJSONObject("product");
		}

		return null;
	}

}