package br.com.lett.crawlernode.crawlers.corecontent.brasil;

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
import br.com.lett.crawlernode.util.MathCommonsMethods;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 04/09/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilLojasredeCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.lojasrede.com.br/";

	public BrasilLojasredeCrawler(Session session) {
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

		if (isProductPage(this.session.getOriginalURL())) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			String internalId = crawlInternalId(doc);
			String internalPid = crawlInternalPid(doc);
			String name = crawlName(doc);
			Float price = crawlMainPagePrice(doc);
			Prices prices = crawlPrices(doc, price);
			boolean available = crawlAvailability(doc);
			CategoryCollection categories = crawlCategories(doc);
			String primaryImage = crawlPrimaryImage(doc);
			String secondaryImages = crawlSecondaryImages(doc);
			String description = crawlDescription(doc);
			Integer stock = null;
			Marketplace marketplace = new Marketplace();

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
			Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
		}

		return products;
	}

	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		if (url.startsWith(HOME_PAGE + "produto/")) {
			return true;
		}
		return false;
	}

	/*******************
	 * General methods *
	 *******************/

	private String crawlInternalId(Document document) {
		String internalId = null;
		Element internalIdElement = document.select("#hdnProdutoVarianteId").first();

		if (internalIdElement != null) {
			internalId = internalIdElement.val();
		}

		return internalId;
	}

	private String crawlInternalPid(Document document) {
		String internalPid = null;

		Element internalPidElement = document.select("#hdnProdutoId").first();

		if (internalPidElement != null) {
			internalPid = internalPidElement.val();
		}
		
		return internalPid;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select(".fbits-produto-nome.prodTitle.title").first();

		if (nameElement != null) {
			name = nameElement.text().toString().trim();
		}

		return name;
	}

	private Float crawlMainPagePrice(Document document) {
		Float price = null;
		Element mainPagePriceElement = document.select("#fbits-forma-pagamento .precoPor").first();

		if (mainPagePriceElement != null) {
			price = MathCommonsMethods.parseFloat(mainPagePriceElement.text());
		}

		return price;
	}

	private Prices crawlPrices(Document doc, Float price) {
		Prices prices = new Prices();

		if(price != null) {
			Map<Integer, Float> installments = new TreeMap<>();
			
			installments.put(1, price);
			prices.setBankTicketPrice(price);
			
			Element bankTicketPriceElement = doc.select(".fbits-boleto-preco").first();
			if (bankTicketPriceElement != null) {
				prices.setBankTicketPrice(MathCommonsMethods.parseFloat(bankTicketPriceElement.text()));
			} 
				
			Element numParcelas = doc.select(".precoParcela .numeroparcelas").first();
			Element parcelaValor = doc.select(".precoParcela .parcelavalor").first();
			
			if(numParcelas != null && parcelaValor != null) {
				String nText = numParcelas.ownText().replaceAll("[^0-9]", "");
				Float vFloat = MathCommonsMethods.parseFloat(parcelaValor.ownText());
				
				if(!nText.isEmpty() && vFloat != null) {
					installments.put(Integer.parseInt(nText), vFloat);
				}
			}
	
			prices.insertCardInstallment(Card.VISA.toString(), installments);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installments);
			prices.insertCardInstallment(Card.ELO.toString(), installments);
			prices.insertCardInstallment(Card.DINERS.toString(), installments);
			prices.insertCardInstallment(Card.AMEX.toString(), installments);
		}
		return prices;
	}

	private boolean crawlAvailability(Document document) {
		Element notifyMeElement = document.select(".avisoIndisponivel").first();

		if (notifyMeElement != null) {
			if (notifyMeElement.attr("style").equals("display:none;"))
				return true;
		}

		return false;
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select(".fbits-componente-imagem img").first();

		if (primaryImageElement != null) {
			primaryImage = primaryImageElement.attr("data-zoom-image").trim();
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select(
				".fbits-componente-imagemvariantethumb.orientacao-vertical.fbits-produto-imagens .jcarousel ul li a");

		for (int i = 1; i < imagesElement.size(); i++) { 
			secondaryImagesArray.put(imagesElement.get(i).attr("data-zoom-image").trim());
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
		Elements elementCategories = document.select("#fbits-breadcrumb ol li a span");

		for (int i = 1; i < elementCategories.size(); i++) { // first page is home
			String cat = elementCategories.get(i).ownText().trim();

			if(!cat.isEmpty()) {
				categories.add( cat );
			}
		}

		return categories;
	}

	private String crawlDescription(Document document) {
		String description = "";
		Element descriptionElement = document.select(".informacao-abas #conteudo-0").first();
		Element specElement = document.select(".informacao-abas #conteudo-1").first();

		if (descriptionElement != null)
			description = description + descriptionElement.html();
		if (specElement != null)
			description = description + specElement.html();

		return description;
	}

}
