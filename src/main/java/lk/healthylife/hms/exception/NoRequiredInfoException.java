package lk.healthylife.hms.exception;

import lk.healthylife.hms.response.ResponseCode;
import org.springframework.http.HttpStatus;

public class NoRequiredInfoException extends BaseException {

	private static final long serialVersionUID = 1L;
	   
	private static String message = "Required Details not found";
	private static HttpStatus status = HttpStatus.BAD_REQUEST;
	private static Integer code = ResponseCode.MISSING_DATA;

	public NoRequiredInfoException(String description) {
		super(message, code, status, description);
	}

	public NoRequiredInfoException(String message, String description) {
		super(message, code, status, description);
	}

	public NoRequiredInfoException() {}
}