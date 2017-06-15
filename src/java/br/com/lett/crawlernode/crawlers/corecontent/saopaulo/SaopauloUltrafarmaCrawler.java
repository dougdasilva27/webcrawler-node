package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import models.Marketplace;
import models.prices.Prices;

public class SaopauloUltrafarmaCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.ultrafarma.com.br/";

	public SaopauloUltrafarmaCrawler(Session session) {
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

		if ( isProductPage(this.session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// ID interno
			String id = this.session.getOriginalURL().split("/")[4];
			String internalID = id.replaceAll("[^0-9,]+", "").replaceAll("\\.", "").trim();

			// Pid
			String internalPid = internalID;

			// Disponibilidade
			Element elementBuyButton = doc.select(".div_btn_comprar").first();
			Element elementBuyButtonNew = doc.select(".overlay_button_add_cesta_detalhe").first();
			boolean available = false;
			
			if(elementBuyButton != null) {
				if(elementBuyButton.text().trim().toLowerCase().contains("produto indisponível")){				
					available = false;
				} else {
					available = true;
				}
			} else if(elementBuyButtonNew != null) {
				available = true;
			}

			// Nome
			String name = null;
			Element elementName = doc.select("h1.div_nome_prod").first();
			if(elementName != null) {
				name = elementName.text().trim();
			}

			// Preço
			Float price = null;
			Element elementPrice = doc.select(".txt_preco_por").first();
			if(elementPrice != null) {
				String priceText = elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".");
				
				if(!priceText.isEmpty()) {
					price = Float.parseFloat(priceText);
				}
			}

			// Categorias
			CategoryCollection categories = crawlCategories(doc);

			// Imagem primária
			Elements elementPrimaryImage = doc.select("#imagem-grande");
			String primaryImage = elementPrimaryImage.attr("src");

			// Imagens secundárias
			String secondaryImages = null;

			JSONArray secondaryImagesArray = new JSONArray();
			Elements element_fotosecundaria = doc.select(".cont_chama_produtos div img");
			if (element_fotosecundaria.size()>1) {
				for(int i=1; i<element_fotosecundaria.size();i++){
					Element e = element_fotosecundaria.get(i);
					secondaryImagesArray.put(e.attr("src"));
				}
			}

			if(secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descrição
			String description = "";

			try {

				Element[] sections = new Element[] {
						doc.select(".div_informacoes_prod").first(),
						doc.select(".div_anvisa").first(),
				};
				for(Element e: sections) {
					if(e != null) {
						description = description + e.html(); 
					}
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}

			// Estoque
			Integer stock = null;

			// Marketplace
			Marketplace marketplace = new Marketplace();

			//Prices
			Prices prices = crawlPrices(doc, price);
			
			// Creating the product
			Product product = ProductBuilder.create()
					.setUrl(session.getOriginalURL())
					.setInternalId(internalID)
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
			Logging.printLogDebug(logger, "Not a product page.");
		}
		
		return products;
	}
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return url.startsWith("http://www.ultrafarma.com.br/produto/detalhes");
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
	private Prices crawlPrices(Document document, Float price) {
		Prices prices = new Prices();

		if(price != null) {
			Map<Integer,Float> installmentPriceMap = new TreeMap<Integer, Float>();
	
			installmentPriceMap.put(1, price);
	
			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.HIPER.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
		}

		return prices;
	}
	
	private CategoryCollection crawlCategories(Document doc) {
		CategoryCollection categories = new CategoryCollection();
		
		Elements cats = doc.select(".breadCrumbs li a");
		int i = 0;
		
		for(Element e : cats) {
			if(i != 0) {
				String text = e.ownText().trim();
				categories.add(text);
			}
			i++;
		}
		
		return categories;
	}
}
