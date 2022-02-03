package lk.healthylife.hms.exception;

import lk.healthylife.hms.response.ResponseCode;
import org.springframework.http.HttpStatus;

/**
 * Exception class for unavailable data
 */
public class DataNotFoundException extends BaseException {

    private static String message = "Some required data not found in the request";
    private static HttpStatus status = HttpStatus.BAD_REQUEST;
    private static Integer code = ResponseCode.MISSING_DATA;

    public DataNotFoundException(String message, String description) {
        super(message, code, status, description);
    }

    public DataNotFoundException(String description) {
        super(message, code, status, description);
    }
}
