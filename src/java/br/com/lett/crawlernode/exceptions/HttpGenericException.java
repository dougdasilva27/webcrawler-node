package br.com.lett.crawlernode.exceptions;

public class HttpGenericException extends RuntimeException {

    public HttpGenericException(String message) {
        super("HTTP error: " + message);
    }
}
