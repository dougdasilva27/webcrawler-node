package br.com.lett.crawlernode.crawlers.saopaulo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.core.task.Crawler;
import br.com.lett.crawlernode.util.Logging;

public class SaopauloExtraCrawler extends Crawler {

	public SaopauloExtraCrawler(CrawlerSession session) {
		super(session);
	}
	
//	@Override
//	public void handleCookiesBeforeFetch() {
//	    
//		// Criando cookie da loja 21 = São Paulo capital
//	    BasicClientCookie cookie = new BasicClientCookie("ep.selected_store", "21");
//	    cookie.setDomain(".deliveryextra.com.br");
//	    cookie.setPath("/");
//	    cookie.setExpiryDate(new Date(System.currentTimeMillis() + 604800000L + 604800000L));
//	    this.cookies.add(cookie);
//	    
//		// Criando cookie simulando um usuário logado
//	    BasicClientCookie cookie2 = new BasicClientCookie("ep.customer_logged", "-2143598207");
//	    cookie2.setDomain(".deliveryextra.com.br");
//	    cookie2.setPath("/");
//	    cookie2.setExpiryDate(new Date(System.currentTimeMillis() + 604800000L + 604800000L));
//	    this.cookies.add(cookie2);
//	}


	@Override
	public boolean shouldVisit() {
		String href = session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && href.startsWith("http://www.deliveryextra.com.br/");
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

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
			String primaryImage = crawlPrimaryImage(chaordicSKUJson);

			// secondary images
			String secondaryImages = null;			

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

			Product product = new Product();
			
			product.setUrl(session.getOriginalURL());
			product.setInternalId(internalId);
			product.setInternalPid(internalPid);
			product.setName(name);
			product.setPrice(price);
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

	private String crawlPrimaryImage(JSONObject chaordicSKUJson) {
		String primaryImage = null;
		if (chaordicSKUJson.has("images")) {
			JSONObject images = chaordicSKUJson.getJSONObject("images");
			if (images.has("1200x1200")) {
				primaryImage = images.getString("1200x1200").trim();
			} 
			else if (images.has("200x200")) {
				primaryImage = images.getString("200x200").trim();
			}
			else if (images.has("80x80")) {
				primaryImage = images.getString("80x80").trim();
			}
		}
		
		if (primaryImage != null) primaryImage = "http://www.deliveryextra.com.br" + primaryImage;
		
		return primaryImage;
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