package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 08/08/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilDrogariaminasbrasilCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.drogariaminasbrasil.com.br";

	public BrasilDrogariaminasbrasilCrawler(Session session) {
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

			String internalId = crawlInternalId(doc);
			String internalPid = crawlInternalPid(doc);
			String name = crawlName(doc);
			Float price = crawlPrice(doc, internalId);
			Prices prices = crawlPrices(price, doc);
			boolean available = crawlAvailability(doc);
			CategoryCollection categories = crawlCategories(doc);
			String primaryImage = crawlPrimaryImage(doc);
			String secondaryImages = crawlSecondaryImages(doc);
			String description = crawlDescription(doc);
			Integer stock = null;
			Marketplace marketplace = crawlMarketplace();

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

	private boolean isProductPage(Document doc) {
		if (doc.select(".PaginaProduto").first() != null) {
			return true;
		}
		return false;
	}

	private String crawlInternalId(Document doc) {
		String internalId = null;

		Element internalIdElement = doc.select("input[name=product]").first();
		if (internalIdElement != null) {
			internalId = internalIdElement.val();
		}

		return internalId;
	}
	
	private String crawlInternalPid(Document doc) {
		String internalPid = null;
		Element pdi = doc.select(".Codigo span[itemprop=productid]").first();
		
		if(pdi != null) {
			internalPid = pdi.ownText().trim();
		}
		
		return internalPid;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select(".NomeProduto h1").first();

		if (nameElement != null) {
			name = nameElement.ownText().trim();
		}

		return name;
	}

	private Float crawlPrice(Document document, String internalId) {
		Float price = null;

		String priceText = null;
		Element salePriceElement = document.select("#product-price-" + internalId).first();		

		if (salePriceElement != null) {
			priceText = salePriceElement.text();
			price = MathUtils.parseFloat(priceText);
		}

		return price;
	}

	private Marketplace crawlMarketplace() {
		return new Marketplace();
	}


	private String crawlPrimaryImage(Document doc) {
		String primaryImage = null;
		Element elementPrimaryImage = doc.select(".Fotos .Principal > a").first();
		
		if(elementPrimaryImage != null ) {
			primaryImage = elementPrimaryImage.attr("href");
		} 
		
		return primaryImage;
	}

	/**
	 * In the time when this crawler was made, this market hasn't secondary Images
	 * @param doc
	 * @return
	 */
	private String crawlSecondaryImages(Document doc) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements images = doc.select(".more-views-list > li:not([class=active]) a");
		
		for(Element e : images) {
			secondaryImagesArray.put(e.attr("href"));
		}
		
		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	/**
	 * No momento que o crawler foi feito não foi achado
	 * produtos com categorias
	 * @param document
	 * @return
	 */
	private CategoryCollection crawlCategories(Document document) {
		CategoryCollection categories = new CategoryCollection();
		Elements elementCategories = document.select(".Navegacao li[itemprop] > a span");
		
		for (int i = 0; i < elementCategories.size(); i++) { 
			String cat = elementCategories.get(i).ownText().trim();
			
			if(!cat.isEmpty()) {
				categories.add( cat );
			}
		}

		return categories;
	}

	private String crawlDescription(Document doc) {
		StringBuilder description = new StringBuilder();
		
		Element elementDescription = doc.select(".BreveDescricao").first();
		
		if (elementDescription != null) {
			description.append(elementDescription.html());		
		}
		
		Element elementInfo = doc.select("#Informacoes").first();
		
		if (elementInfo != null) {
			description.append(elementInfo.html());		
		}
		
		Element elementCaract = doc.select("#Caracteristicas").first();
		
		if (elementCaract != null) {
			description.append(elementCaract.html());		
		}
		
		Element elementAditional = doc.select("#InformacoesAdicionais").first();
		
		if (elementAditional != null) {
			description.append(elementAditional.html());		
		}
		
		return description.toString();
	}
	
	private boolean crawlAvailability(Document doc) {
		return doc.select(".Botao.Comprar").first() != null;		
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
			prices.setBankTicketPrice(price);

			Elements cards = doc.select(".CartoesParcelamento .CartaoParcelamento");
			
			if(cards.size() > 0) {
				for(Element card : cards) {
					Map<Integer,Float> installmentPriceMap = new HashMap<>();
					Elements table = card.select("table tr");
					
					for(Element e : table) {
						Elements colunas = e.select("td");
						
						if(colunas.size() > 1) {
							setInstallments(colunas, 0, 1, installmentPriceMap);
							
							if(colunas.size() > 3) {
								setInstallments(colunas, 2, 3, installmentPriceMap);
							}
						}
					}
					
					String cardOficialName = crawlCardName(card);
					
					prices.insertCardInstallment(cardOficialName, installmentPriceMap);
				}
			} else {
				Map<Integer,Float> installmentPriceMap = new HashMap<>();
				installmentPriceMap.put(1, price);
				
				prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
				prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
				prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
				prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
				prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			}
		}
		
		return prices;
	}
	
	private String crawlCardName (Element card) {
		String cardOficialName = null;
		Element name = card.select("> span").first();
		
		if(name != null) {
			String cardName = name.ownText().toLowerCase().trim();
			
			switch(cardName) {
				case "visa":
					cardOficialName = Card.VISA.toString();
					break;
			
				case "mastercard":
					cardOficialName = Card.MASTERCARD.toString();
					break;
				
				case "american express":
					cardOficialName = Card.AMEX.toString();
					break;
					
				case "diners":
					cardOficialName = Card.DINERS.toString();
					break;
				
				case "elo":
					cardOficialName = Card.ELO.toString();
					break;
				
				default: break;
			}
		}
		
		
		return cardOficialName;
	}
	
	private void setInstallments(Elements colunas, int indexInstallment, int indexValue, Map<Integer,Float> installmentPriceMap) {
		String installmentText = colunas.get(indexInstallment).text().replaceAll("[^0-9]", "").trim();
		Integer installment = installmentText.isEmpty() ? null : Integer.parseInt(installmentText);
		
		String valueText = colunas.get(indexValue).text().replaceAll("[^0-9,]", "").replace(",", ".").trim();
		Float value = valueText.isEmpty() ? null : Float.parseFloat(valueText);
		
		if(installment != null && value != null) {
			installmentPriceMap.put(installment, value);
		}
	}

}
