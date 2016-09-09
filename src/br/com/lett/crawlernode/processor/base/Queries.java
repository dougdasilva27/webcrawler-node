package br.com.lett.crawlernode.processor.base;

/**
 * Classe contém atributos com consultas e apontamentos para arquivos importantes
 * @author doug
 *
 */
public class Queries {
	
	public static final String queryForProcessedProducts = "SELECT * FROM processed WHERE lmt > ";
	public static final String queryForLettClassProducts = "SELECT denomination, mistake, extra FROM lett_class LEFT JOIN lett_class_mistake ON (lett_class.id = id_lett_class);";
	public static final String queryForLettBrandProducts = "SELECT denomination, supplier as lett_supplier, mistake, ignored FROM lett_brand LEFT JOIN lett_brand_mistake ON (lett_brand.id = id_brand);";
	public static final String queryMarkets = "SELECT * FROM market";
	public static final String queryForSelectProcessedProduct_part1 = "SELECT * FROM processed WHERE internal_id LIKE '";
	public static final String queryForSelectProcessedProduct_part2 = "' AND market =";
	
}
