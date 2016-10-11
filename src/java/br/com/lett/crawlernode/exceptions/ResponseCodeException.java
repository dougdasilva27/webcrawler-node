package br.com.lett.crawlernode.exceptions;

/**
 * Checked exception used when request return
 * a status code indicating a bloquing or a server error.
 * 
 * @author Samir Leao
 *
 */
public class ResponseCodeException extends Exception {
	
	private static final long serialVersionUID = 1L;

	public ResponseCodeException(int code) {
		super("Error status code. [" + code + "]");
	}

}
