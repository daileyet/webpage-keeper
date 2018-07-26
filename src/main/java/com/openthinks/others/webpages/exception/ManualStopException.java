/**
 * 
 */
package com.openthinks.others.webpages.exception;

/**
 * @author dailey.yet@outlook.com
 *
 */
public class ManualStopException extends RuntimeException {

	private static final long serialVersionUID = 5417664054262488718L;

	public ManualStopException() {
		super();
	}

	public ManualStopException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ManualStopException(String message, Throwable cause) {
		super(message, cause);
	}

	public ManualStopException(String message) {
		super(message);
	}

	public ManualStopException(Throwable cause) {
		super(cause);
	}

}
