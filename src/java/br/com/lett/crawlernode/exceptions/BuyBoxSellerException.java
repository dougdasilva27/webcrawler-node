package br.com.lett.crawlernode.exceptions;

import br.com.lett.crawlernode.core.models.BuyBoxSeller;

/**
 * Throws this exception when has missing fileds on {@link BuyBoxSeller}
 * 
 * @author Gabriel
 *
 */
public class BuyBoxSellerException extends Exception {

  private static final long serialVersionUID = 1L;

  public BuyBoxSellerException() {
    super("Missing fields.");
  }

  public BuyBoxSellerException(String field) {
    super("Missing field. [" + field + "]");
  }

  public BuyBoxSellerException(String field, Object value) {
    super("Field " + field + " cannot have this value [" + value + "]");
  }
}
