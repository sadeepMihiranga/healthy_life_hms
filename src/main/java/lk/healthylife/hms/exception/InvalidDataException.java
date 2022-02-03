package lk.healthylife.hms.exception;

import lk.healthylife.hms.response.ResponseCode;
import org.springframework.http.HttpStatus;

/**
 * Exception class for invalid user inputs
 */
public class InvalidDataException extends BaseException {

    private static String message = "Invalid data found in the request.";
    private static HttpStatus status = HttpStatus.BAD_REQUEST;
    private static Integer code = ResponseCode.INVALID_INPUT;

    public InvalidDataException(String description) {
        super(message, code, status, description);
    }

    public InvalidDataException(String message,String description) {
        super(message, code, status, description);
    }

    public InvalidDataException() {

    }
}
