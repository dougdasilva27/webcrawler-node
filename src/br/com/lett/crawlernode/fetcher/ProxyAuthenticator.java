package br.com.lett.crawlernode.fetcher;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 * Processor proxy authenticator
 * 
 * @author samirleao
 *
 */

public class ProxyAuthenticator extends Authenticator {

	private String userName; 
	private String password;

	protected PasswordAuthentication getPasswordAuthentication() {
		return new PasswordAuthentication(userName, password.toCharArray());
	}

	public ProxyAuthenticator(String userName, String password) {
		this.userName = userName;
		this.password = password;
	}
	
}
