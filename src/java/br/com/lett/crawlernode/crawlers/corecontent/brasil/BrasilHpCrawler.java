package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;

/**
 * Date: 15/12/16
 * 
 * 1 - This market has only one sku per page
 * 2 - Has marketplace information in this one
 * 3 - Has installments informations
 * 
 * @author gabriel
 *
 */
public class BrasilHpCrawler extends Crawler {

	private static final String HOME_PAGE = "http://www.lojahp.com.br/";

	public BrasilHpCrawler(Session session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<>();

		if ( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());


			String internalId = crawlInternalId(doc);
			String internalPid = crawlInternalPid();
			String name = crawlName(doc);
			String primaryImage = crawlPrimaryImage(doc);
			String secondaryImages = crawlSecondaryImages(doc);
			Integer stock = null;
			
			Map<String, Prices> marketplaceMap = crawlMarketplace(doc);
			JSONArray marketplace = assembleMarketplaceFromMap(marketplaceMap, doc);
			boolean available = crawlAvailability(marketplaceMap);
			Float price = crawlPrice(doc, available);
			Prices prices = crawlPrices(doc, price);
			
			String description = crawlDescription(doc);
			CategoryCollection categories = crawlCategories(doc);

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

	private boolean isProductPage(Document document) {
		Element elementProduct = document.select("#ctl00_Conteudo_hdnIdSkuSelecionado").first();
		return elementProduct != null;
	}


	private String crawlInternalId(Document document) {
		String internalId = null;

		Element skuId = document.select("#ctl00_Conteudo_hdnIdSkuSelecionado").first();
		if (skuId != null) {
			internalId = skuId.val();
		}

		return internalId;
	}


	private String crawlInternalPid() {
		return null;
	}

	private String crawlName(Document document) {
		String name = null;

		Element elementName = document.select(".produtoNome h1.name b").first();
		if(elementName != null) {
			name = elementName.ownText().trim();
		}

		return name;
	}
	
	private Map<String,Prices> crawlMarketplace(Document doc) {
		Map<String,Prices> marketplaces = new HashMap<>();
		Element seller = doc.select("a.seller").first();
		
		if(seller != null) {
			Float price = crawlPrice(doc, true);
			String name = seller.text().toLowerCase().trim();
			Prices prices = crawlPrices(doc, price);

			if(!name.isEmpty() && price != null) {
				marketplaces.put(name, prices);
			}
		}
		
		return marketplaces;
	}

	private JSONArray assembleMarketplaceFromMap(Map<String, Prices> marketplaceMap, Document doc) {
		JSONArray marketplace = new JSONArray();

		String hpSellerName = "hp";
		
		for(Entry<String, Prices> sellerName : marketplaceMap.entrySet()) {
			if ( !sellerName.getKey().equals(hpSellerName) ) {
				JSONObject seller = new JSONObject();
				seller.put("name", sellerName.getKey());
				seller.put("price", crawlPrice(doc, true));
				seller.put("prices", sellerName.getValue().toJSON());
				
				marketplace.put(seller);
			}
		}
		
		return marketplace;
	}
	
	private Float crawlPrice(Document document, boolean available) {
		Float price = null;

		if(available) {
			Element elementPrice = document.select("i.sale.price").first();
			if (elementPrice != null) {
				price = MathCommonsMethods.parseFloat(elementPrice.ownText());
			}
		}
		
		return price;
	}
	
	private boolean crawlAvailability(Map<String, Prices> marketplaceMap) {
		boolean available = false;

		if (marketplaceMap.containsKey("hp")){
			available = true;
		}

		return available;
	}


	private Prices crawlPrices(Document doc, Float price) {
		Prices prices = new Prices();

		if(price != null) {
			Element discount = doc.select(".price.discount").first();
			
			if(discount != null) {
				prices.setBankTicketPrice(MathCommonsMethods.parseFloat(discount.text()));
			} else {
				prices.setBankTicketPrice(price);
			}

			Map<Integer, Float> installmentsMap = new HashMap<>();
			
			Elements installmentsElements = doc.select(".parcelamento .tabCont.selected .parcelCartao table tr");
			
			for(Element e : installmentsElements) {
				String text = e.text().toLowerCase().trim();
				
				String[] tokens = text.split(" ");
				String parcel = tokens[0].replaceAll("[^0-9]", "").trim();
				
				if(!parcel.isEmpty()) {
					Integer installment = Integer.parseInt(parcel);
					Float value = MathCommonsMethods.parseFloat(tokens[tokens.length-1]);
					
					installmentsMap.put(installment, value);
				}
			}

			prices.insertCardInstallment(Card.VISA.toString(), installmentsMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentsMap);
			prices.insertCardInstallment(Card.AMEX.toString(), installmentsMap);
			prices.insertCardInstallment(Card.DINERS.toString(), installmentsMap);
			prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentsMap);
			prices.insertCardInstallment(Card.ELO.toString(), installmentsMap);
			prices.insertCardInstallment(Card.MULTIBENEFICIOS.toString(), installmentsMap);
			prices.insertCardInstallment(Card.MULTICASH.toString(), installmentsMap);
			prices.insertCardInstallment(Card.MULTICHEQUE.toString(), installmentsMap);
			prices.insertCardInstallment(Card.MULTIEMPRESARIAL.toString(), installmentsMap);
		}

		return prices;
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;

		Element elementPrimaryImage = document.select("#divFullImage a").first();
		if(elementPrimaryImage != null) {
			String image = elementPrimaryImage.attr("href").trim();
			
			if(image.isEmpty()) {
				Element img = elementPrimaryImage.select("> img").first();
				
				if(img != null) {
					image = img.attr("src");
				}
			} 
			
			primaryImage = image;
			
		}

		return primaryImage;
	}


	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;

		Elements elementImages = document.select(".thumbsImg li a");
		JSONArray secondaryImagesArray = new JSONArray();

		for(int i = 1; i < elementImages.size(); i++) { // skip the first because it's the same as the primary image
			String imageURL = elementImages.get(i).attr("rev").trim();
			
			if(imageURL.isEmpty()){
				imageURL = elementImages.get(i).attr("href").trim();
			}

			secondaryImagesArray.put(imageURL);
		}			
		
		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private CategoryCollection crawlCategories(Document document) {
		CategoryCollection categories = new CategoryCollection();
		Elements elementCategories = document.select(".breadcrumb span a span");

		for (int i = 1; i < elementCategories.size(); i++) { // start with index 1 because the first item is the home page
			categories.add( elementCategories.get(i).text().trim() );
		}

		return categories;
	}

	private String crawlDescription(Document document) {
		StringBuilder description = new StringBuilder();

		Element skuInformation = document.select(".detalhesProduto").first();
		if (skuInformation != null) {
			description.append(skuInformation.html());
		}

		return description.toString();
	}
}