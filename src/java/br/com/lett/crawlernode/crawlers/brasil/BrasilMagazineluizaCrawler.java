package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.kernel.task.Crawler;
import br.com.lett.crawlernode.kernel.task.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;


/**
 * eg:
 * 
 * SKU with marketplace: http://www.magazineluiza.com.br/whisky-vintage-eau-de-toilette-evaflor-100ml-perfume-masculino/p/9843013/pf/pfpm/
 * 
 * obs: we couldn't find any URL example with more than one different seller on marketplace.
 * 
 * @author Samir Leao
 *
 */
public class BrasilMagazineluizaCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.magazineluiza.com.br/";

	public BrasilMagazineluizaCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getUrl()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			JSONObject skuJsonInfo = crawlFullSKUInfo(doc);

			/*
			 * Id interno -- obtido a partir do id do sku apendado com o full id. O full id será
			 * apendado no início do tratamento de cada caso de produto (produto com variação 
			 * e sem variação) 
			 */
			Element elementInternalId = doc.select("small[itemprop=productID]").first();
			int begin = elementInternalId.text().indexOf(".com") + 4;
			String internalId = elementInternalId.text().substring(begin).replace(")", "").trim();

			// Pid
			String internalPid = internalId;

			// Nome
			String name = null;
			Element elementName = doc.select("h1[itemprop=name]").first();
			if (elementName != null) {
				name = elementName.text();
			}

			// Categorias
			String category1 = "";
			String category2 = "";
			String category3 = "";
			Elements elementsCategories = doc.select(".container-bread-crumb-detail.bread-none-line ul li[typeof=v:Breadcrumb] a");
			ArrayList<String> categories = new ArrayList<String>();
			for(Element categorie : elementsCategories) {
				String cat = categorie.text();
				if (!cat.equals("magazineluiza.com")) {
					categories.add(cat);
				}
			}
			for (String category : categories) {
				if (category1.isEmpty()) {
					category1 = category;
				} else if (category2.isEmpty()) {
					category2 = category;
				} else if (category3.isEmpty()) {
					category3 = category;
				}
			}

			// Imagens
			String primaryImage = null;
			String secondaryImages = null;
			Elements elementsImages = doc.select(".container-little-picture ul li a");
			JSONArray secondaryImagesArray = new JSONArray();

			for(Element e : elementsImages) {
				if( !e.attr("rel").isEmpty() ) {
					String image = parseImage(e.attr("rel"));
					if (primaryImage == null) {
						primaryImage = image;
					} else {
						secondaryImagesArray.put(image);
					}
				}
			}
			if (secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descrição
			String description = "";
			Element elementDescription = doc.select(".factsheet-main-container").first();
			if (elementDescription != null) {
				description = description + elementDescription.html();
			}

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = new JSONArray();

			JSONArray skus = skuJsonInfo.getJSONArray("details");

			/* ******************
			 * Only one product *
			 * ******************/

			if (skus.length() == 1) {

				// availability
				boolean available = true;
				Element elementAvailable = doc.select(".container-btn-buy").first();
				if(elementAvailable == null) {
					available = false;
				} else {
					Elements elementsMarketPlace = doc.select(".market-place-delivery .market-place-delivery__seller--big");
					boolean magazineIsSelling = false;
					for(Element e : elementsMarketPlace) {
						if(e.text().equals("Magazine Luiza")) {
							magazineIsSelling = true;
							break;
						}
					}
					if(magazineIsSelling) available = true;
					else available = false;
				}

				// price
				Float price = null;
				if (available) {
					price = crawlPriceNoVariations(doc);
				}

				// marketplace
				Element marketplaceName = doc.select(".market-place-delivery .market-place-delivery__seller--big").first();
				if (marketplaceName != null) {
					String sellerName = marketplaceName.text().toLowerCase().trim();
					if (!sellerName.equals("magazine luiza")) {
						Float sellerPrice = crawlPriceNoVariations(doc);
						
						JSONObject seller = new JSONObject();
						seller.put("name", sellerName);
						seller.put("price", sellerPrice);

						marketplace.put(seller);
					}
				}				

				Product product = new Product();
				product.setSeedId(this.session.getSeedId());
				product.setUrl(this.session.getUrl());
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
			}

			/* ***********************
			 * More than one product *
			 *************************/
			else {

				for(int i = 0; i < skus.length(); i++) {

					JSONObject sku = skus.getJSONObject(i);

					// internalId
					String internalIdsecondPart = sku.getString("sku");
					String variationInternalId = internalId + "-" + internalIdsecondPart;

					// internalPid
					String variationInternalPid = internalId;

					// name
					String variationName = name;
					if (sku.has("voltage")) {
						variationName = variationName + " - " + sku.getString("voltage");
					}

					// availability
					boolean available = false;
					if (hasOptionSelector(internalIdsecondPart, doc)) {
						available = true;
					}

					// price
					Float price = null;
					if (available) {
						price = crawlPriceVariation(doc);
					}

					// marketplace
					Element marketplaceName = doc.select(".market-place-delivery .market-place-delivery__seller--big").first();
					if (marketplaceName != null) {
						String sellerName = marketplaceName.text().toLowerCase().trim();
						if (!sellerName.equals("magazine luiza")) {
							Float sellerPrice = crawlPriceVariation(doc);
							
							JSONObject seller = new JSONObject();
							seller.put("name", sellerName);
							seller.put("price", sellerPrice);

							marketplace.put(seller);
						}
					}

					Product product = new Product();
					product.setSeedId(this.session.getSeedId());
					product.setUrl(this.session.getUrl());
					product.setInternalId(variationInternalId);
					product.setInternalPid(variationInternalPid);
					product.setName(variationName);
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

				}
			}
		} else {

			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}

		return products;
	}

	private String parseImage(String text) {
		int begin = text.indexOf("largeimage:") + 11;
		String img = text.substring(begin);
		img = img.replace("\'", " ").replace('}', ' ').trim();

		return img;
	}

	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return (url.contains("/p/"));
	}

	/**
	 * Analyze if the current internalId is displayed as an option on the product main page
	 * in a selection box. If it isn't, it means that this products is not being displayed because
	 * it's variations is unavailable.
	 * 
	 * @return boolean true if exists an option if this internalId or false otherwise.
	 */
	private boolean hasOptionSelector(String internalId, Document document) {
		Element skuUl = document.select(".js-buy-option-box.container-basic-information .js-buy-option-list").first();
		if (skuUl != null) {
			Elements skuOptions = skuUl.select("li");
			for (Element option : skuOptions) {
				Element input = option.select("input").first();
				if (input != null) {
					String value = input.attr("value").trim();
					if (value.equals(internalId)) return true;
				}
			}
		}
		return false;
	}

	private Float crawlPriceNoVariations(Document document) {
		Float price = null;
		Element elementPrice = document.select(".content-buy-product meta[itemprop=price]").first();
		if(elementPrice != null) {
			price = Float.parseFloat(elementPrice.attr("content").replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
		}

		return price;
	}

	private Float crawlPriceVariation(Document document) {
		Float price = null;
		Element elementPrice = document.select("#productDiscountPrice").first();
		if(elementPrice != null) {
			price = Float.parseFloat(elementPrice.attr("value"));
		}

		return price;
	}


	/**
	 * Get the script having a json with the sku information.
	 * In the example below the sku has 2 variations. Even if one of them
	 * is'n show on the main page of the sku, this json contains it's information.
	 * 
	 * It was observed that when the variation is unavailable, the ecommerce website
	 * doesn't display the variation as an option to be selected by the user. When the
	 * two variations are unavailable, the website doesn't display none of them. Instead,
	 * it creates a new base product that is displayed as unavailable for the user.
	 * 
	 * For instance, if we have Ar Condicionado Midea 110 Vols and Ar Condicionado Midea 220 Volts
	 * when the two are unavailable, the website only display the product Ar Condicionado Midea.
	 * If one of them is available, only the other is shown on a box selector for the user.
	 * 
	 * The crawler must use the JSON retrieved in this method, so it won't create the new
	 * "false" sku "Ar Condicionado Midea" on database. This way it continues to crawl the two variations
	 * and correctly crawl the availability as false.
	 * 
	 * eg:
	 * 
	 * "reference":"com Função Limpa Fácil",
	 *	"extendedWarranty":true,
	 *	"idSku":"0113562",
	 *	"idSkuFull":"011356201",
	 *	"salePrice":429,
	 *	"imageUrl":"http://i.mlcdn.com.br//micro-ondas-midea-liva-mtas4-30l-com-funcao-limpa-facil/v/210x210/011356201.jpg",
	 *	"fullName":"micro%20ondas%20midea%20liva%20mtas4%2030l%20-%20com%20funcao%20limpa%20facil",
	 *	"details":[
	 *		{
	 *			"color":"Branco",
	 *			"sku":"011356201",
	 *			"voltage":"110 Volts"
	 *		},
	 *		{
	 *			"color":"Branco",
	 *			"sku":"011356301",
	 *			"voltage":"220 Volts"
	 *		}
	 *	],
	 *	"title":"Micro-ondas Midea Liva MTAS4 30L",
	 *	"cashPrice":407.55,
	 *	"brand":"midea",
	 *	"stockAvailability":true
	 * 
	 * @return a json object containing all sku informations in this page.
	 */
	private JSONObject crawlFullSKUInfo(Document document) {
		Elements scriptTags = document.getElementsByTag("script");
		JSONObject skuJson = null;

		for (Element tag : scriptTags){                
			for (DataNode node : tag.dataNodes()) {
				if(tag.html().trim().startsWith("var digitalData = ")) {
					skuJson = new JSONObject
							(
									node.getWholeData().split(Pattern.quote("var digitalData = "))[1] +
									node.getWholeData().split(Pattern.quote("var digitalData = "))[1].split(Pattern.quote("}]};"))[0]
									);

				}
			}        
		}

		return skuJson.getJSONObject("page").getJSONObject("product");
	}

}