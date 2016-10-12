package br.com.lett.crawlernode.exceptions;

/**
 * When a proxy service is not found.
 * 
 * @author Samir Leao
 *
 */
public class ProxyServiceNotFoundException extends Exception {
	
	private static final long serialVersionUID = 1L;

	public ProxyServiceNotFoundException() {
		super("Proxy service not found.");
	}
	
	public ProxyServiceNotFoundException(String proxyService) {
		super("Proxy service not found. [" + proxyService + "]");
	}

}
