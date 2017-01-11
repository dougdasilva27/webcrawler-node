package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;

public class SaopauloSondaCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.sondadelivery.com.br/";

	public SaopauloSondaCrawler(Session session) {
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
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// ID interno
			String internalID = Integer.toString(Integer.parseInt(this.session.getOriginalURL().split("/")[this.session.getOriginalURL().split("/").length-1]));

			// Nome
			String name = crawlName(doc);

			// Preço
			Float price = crawlPrice(doc);

			// Disponibilidade
			boolean available = crawlAvailability(doc);

			// Categorias
			Elements elementCategory1 = doc.select(".HeaderDetalhesdoProduto h2");
			String category1 = elementCategory1.text().trim();
			Elements elementCategory2 = doc.select(".HeaderDetalhesdoProduto .spnSubTitulo");
			String category2 = elementCategory2.text().trim();
			String category3 = "";

			// Imagem primária
			String primaryImage = null;
			Element elementPrimaryImage = doc.select(".box-Imagem img").first();
			if (elementPrimaryImage != null) {
				primaryImage = elementPrimaryImage.attr("src");
			}
			if(primaryImage != null && primaryImage.contains("nome_da_imagem_do_sem_foto.gif")) {
				primaryImage = ""; //TODO: Verificar o nome da foto genérica
			}
			String secondaryImages = null;

			// Descrição
			String description = "";
			Elements elementDescription = doc.select("article.bx-detalhe-descricao");
			for(int i = 0; i < elementDescription.size(); i++) {
				String information = elementDescription.get(i).select("h2").text();
				if(information.equals("Ingredientes")) {
					description = description + elementDescription.get(i).select("p").html();
				}
				else if(information.equals("Tabela Nutricional")) {
					description = description + elementDescription.get(i).select("table tbody").html();
				}
			}

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			// Prices
			Prices prices = crawlPrices(doc, price);
			
			Product product = new Product();
			
			product.setUrl(this.session.getOriginalURL());
			product.setInternalId(internalID);
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

	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return url.contains("/delivery.aspx/produto/");
	}

	private String crawlName(Document doc) {
		String name = null;
		Element elementName = doc.select(".box-DetalhesProdMed h3").first();
		if (elementName != null) {
			name = elementName.text().replace("'", "").trim();
		}

		return name;
	}

	private boolean crawlAvailability(Document doc) {
		Element notifymeButton = doc.select(".btn.btnaviso").first();
		return (notifymeButton == null);
	}

	private Float crawlPrice(Document doc) {
		Float price = null;
		Element elementPrice = doc.select(".box-DetalhesProdMed h2").first();
		if (elementPrice != null) {
			price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
		}

		return price;
	}
	
	/**
	 * In this market, installments not appear in product page
	 * Has two prices in product Page, normal cards and Shopcard
	 * Ex: 
	 * 
	 * R$ 9,99
	 * No Cartão Sonda: R$ 9,49
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
			prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
			
			// shop card
			Element shopPriceElement = doc.select(".noBorder span[style]").first();
			
			if(shopPriceElement != null){
				Float priceShop = MathCommonsMethods.parseFloat(shopPriceElement.text());
				
				Map<Integer,Float> installmentPriceMapShop = new HashMap<>();
				installmentPriceMapShop.put(1, priceShop);
				
				prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMapShop);
			} else {
				prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);
			}
		}

		return prices;
	}
}