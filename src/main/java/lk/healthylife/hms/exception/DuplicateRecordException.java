package lk.healthylife.hms.exception;

import lk.healthylife.hms.response.ResponseCode;
import org.springframework.http.HttpStatus;

/**
 * Exception class for Duplicate record errors
 */
public class DuplicateRecordException extends BaseException{
    private static String message = "Duplicate Record";
    private static HttpStatus status = HttpStatus.CONFLICT;
    private static Integer code = ResponseCode.DUPLICATE_DATA;

    public DuplicateRecordException(String description) {
        super(message, code, status, description);
    }

    public DuplicateRecordException(String message, String description) {
        super(message, code, status, description);
    }
}
