package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 09/08/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilVitaesaudeCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.vitaesaude.com.br/";

	public BrasilVitaesaudeCrawler(Session session) {
		super(session);
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

		if ( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
			
			String internalPid = crawlInternalPid(doc);
			String name = crawlName(doc);
			CategoryCollection categories = crawlCategories(doc);
			String primaryImage = crawlPrimaryImage(doc);
			String secondaryImages = crawlSecondaryImages(doc);
			String description = crawlDescription(doc);
			Integer stock = null;
			Marketplace marketplace = crawlMarketplace();

			Elements variationsRadio = doc.select(".ProductOptionList li label");
			Elements variationsBox = doc.select(".ProductOptionList option");
			
			boolean isRadio = variationsRadio.size() > 0;
			Elements variations = isRadio ? variationsRadio : variationsBox;
			
			if(variationsRadio.size() > 0 || variationsBox.size() > 0) {
				for(Element e : variations) {
					//Id variation
					String variationId = isRadio ? e.select("input").val() : e.val().trim();
					
					if(!variationId.isEmpty()) {
						// Variation info
						JSONObject variationInfo = crawlVariationsInfo(internalPid, variationId);
						
						String internalId = internalPid + "-" + variationId;
						String variationName = name + " " + e.ownText().trim();
						Float price = crawlVariationPrice(variationInfo);
						Prices prices = crawlPrices(price, variationInfo);
						boolean available = crawlAvailability(variationInfo);
						
						// Creating the product
						Product product = ProductBuilder.create()
								.setUrl(session.getOriginalURL())
								.setInternalId(internalId)
								.setInternalPid(internalPid)
								.setName(variationName)
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
				} 
			} else {
				/**
				 * Por padrão estou colocando o id do produto como internalId pra prod sem variacao
				 * pois um possivel id do sku, as vezes nao aparece e as vezes vem um nome,
				 * o unico id confiavel e esse id do produto, que é o mesmo para as variações, 
				 * contudo as mesmas possuem um segundo id que e um id da seleção, que com a combinação
				 * com o id do produto deixa aquele produto único.
				 */
				String internalId = internalPid + "-" + internalPid;
				Float price = crawlPrice(doc);
				Prices prices = crawlPrices(price, doc);
				boolean available = crawlAvailability(doc);
				
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
			}
			
		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return products;

	}

	private boolean isProductPage(Document doc) {
		if (doc.select("input[name=product_id]").first() != null) {
			return true;
		}
		return false;
	}

	private String crawlInternalPid(Document doc) {
		String internalPid = null;
		Element pdi = doc.select("input[name=product_id]").first();
		
		if(pdi != null) {
			internalPid = pdi.val();
		}
		
		return internalPid;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("h1.ProdName").first();

		if (nameElement != null) {
			name = nameElement.ownText().trim();
		}

		return name;
	}

	private Float crawlPrice(Document document) {
		Float price = null;

		String priceText = null;
		Element salePriceElement = document.select(".VariationProductDiscount").first();		

		if (salePriceElement != null) {
			priceText = salePriceElement.text();
			price = MathCommonsMethods.parseFloat(priceText);
		}
		
		// Casos que não tem preço a vista em destaque
		if(price == null) {
			Element priceOriginal = document.select("meta[itemprop=price]").first();
			
			if(priceOriginal != null) {
				price = Float.parseFloat(priceOriginal.attr("content"));
			}
		}

		return price;
	}
	
	private Float crawlVariationPrice(JSONObject variationInfo) {
		Float price = null;
		
		if(variationInfo.has("desconto")) {
			String desconto = variationInfo.get("desconto").toString().replaceAll("[^0-9,]", "").replace(",", ".").trim();
			
			if(!desconto.isEmpty()) {
				price = Float.parseFloat(desconto);
			}
		}
		
		if(price == null && variationInfo.has("unformattedPrice")) {
			Double pDouble = variationInfo.getDouble("unformattedPrice");
			price = MathCommonsMethods.normalizeTwoDecimalPlaces(pDouble.floatValue());
		}
		
		return price;
	}

	private Marketplace crawlMarketplace() {
		return new Marketplace();
	}


	private String crawlPrimaryImage(Document doc) {
		String primaryImage = null;
		Element elementPrimaryImage = doc.select(".ProductThumbImage > a").first();
		
		if(elementPrimaryImage != null ) {
			primaryImage = elementPrimaryImage.attr("href");
		} 
		
		return primaryImage;
	}

	/**
	 * @param doc
	 * @return
	 */
	private String crawlSecondaryImages(Document doc) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements images = doc.select(".ProductTinyImageList li");
		
		if(images.size() > 1) {
			Elements scripts = doc.select("script[type=text/javascript]");
			
			for(Element e : scripts) {
				String script = e.outerHtml();
				
				if(script.contains("ThumbURLs")) {
					String[] tokens = script.split(";");
					
					for(String token : tokens) {
						if(token.trim().contains("ThumbURLs[") && !token.contains("ThumbURLs[0]")) {
							secondaryImagesArray.put(token.split("=")[1].trim().replace("//", "/").replace("\"", ""));
						}
					}
						
					break;
				}
			}
		}
		
		for(Element e : images) {
			secondaryImagesArray.put(e.attr("href"));
		}
		
		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	/**
	 * @param document
	 * @return
	 */
	private CategoryCollection crawlCategories(Document document) {
		CategoryCollection categories = new CategoryCollection();
		Elements elementCategories = document.select(".Breadcrumb ul li:not([class=itemCurrent]) a");
		
		for (int i = 1; i < elementCategories.size(); i++) { // primeiro item é a home
			String cat = elementCategories.get(i).ownText().trim();
			
			if(!cat.isEmpty()) {
				categories.add( cat );
			}
		}

		return categories;
	}

	private String crawlDescription(Document doc) {
		StringBuilder description = new StringBuilder();
		
		Element elementDescription = doc.select("#ProductDescription").first();
		
		if (elementDescription != null) {
			description.append(elementDescription.html());		
		}
		
		return description.toString();
	}
	
	private boolean crawlAvailability(Document doc) {
		return doc.select(".avisemeContent[style=\"display:none;\"]").first() != null;		
	}
	
	private boolean crawlAvailability(JSONObject variationInfo) {
		return variationInfo.has("instock") && variationInfo.getBoolean("instock");		
	}

	/**
	 * 
	 * @param doc
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(Float price, Document doc) {
		Prices prices = new Prices();
		
		if (price != null) {
			Map<Integer,Float> installmentPriceMap = new TreeMap<>();
			installmentPriceMap.put(1, price);
			prices.setBankTicketPrice(price);

			Element installmentsElement = doc.select(".ProdParcelas strong").first();
			
			if(installmentsElement != null) {
				String installmentText = installmentsElement.ownText().replaceAll("[^0-9]", "").trim();
				
				if(!installmentText.isEmpty()) {
					Integer installment = Integer.parseInt(installmentText);
					Element valueElement = installmentsElement.select(".ValorProduto").first();
					
					if(valueElement != null) {
						Float value = MathCommonsMethods.parseFloat(valueElement.ownText());
						
						if(value != null) {
							installmentPriceMap.put(installment, value);
						}
					}
				}
			}
			
			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
		}
		
		return prices;
	}
	
	/**
	 * 
	 * @param doc
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(Float price, JSONObject variationInfo) {
		Prices prices = new Prices();
		
		if (price != null) {
			Map<Integer,Float> installmentPriceMap = new TreeMap<>();
			installmentPriceMap.put(1, price);
			prices.setBankTicketPrice(price);

			if(variationInfo.has("parcelas")) {
				Document doc = Jsoup.parse(variationInfo.getString("parcelas"));
				
				Element installmentsElement = doc.select(".ProdParcelas strong").first();
				
				if(installmentsElement != null) {
					String installmentText = installmentsElement.ownText().replaceAll("[^0-9]", "").trim();
					
					if(!installmentText.isEmpty()) {
						Integer installment = Integer.parseInt(installmentText);
						Element valueElement = installmentsElement.select(".ValorProduto").first();
						
						if(valueElement != null) {
							Float value = MathCommonsMethods.parseFloat(valueElement.ownText());
							
							if(value != null) {
								installmentPriceMap.put(installment, value);
							}
						}
					}
				}
				
				prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
				prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
				prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
				prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
			}
		}
			
		
		return prices;
	}

	private JSONObject crawlVariationsInfo(String pid, String variationId) {
		String url = "http://www.vitaesaude.com.br/remote.php?w=GetVariationOptions&productId=" + pid + "&options=" + variationId;
		return DataFetcher.fetchJSONObject(DataFetcher.GET_REQUEST, session, url, null, cookies);
	}
}
