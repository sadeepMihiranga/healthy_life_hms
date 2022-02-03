package lk.healthylife.hms.exception;

import lk.healthylife.hms.response.ResponseCode;
import org.springframework.http.HttpStatus;

/**
 * Exception class for internal server errors
 */
public class OperationException extends BaseException {

    private static String message = "Error Processing the Operation";
    private static HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
    private static Integer code = ResponseCode.ERROR_OPERATION;

    public OperationException(String description) {
        super(message, code, status, description);
    }

    public OperationException(String message,String description) {
        super(message, code, status, description);
    }
}
