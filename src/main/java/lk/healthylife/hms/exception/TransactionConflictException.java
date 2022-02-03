package lk.healthylife.hms.exception;

import lk.healthylife.hms.response.ResponseCode;
import org.springframework.http.HttpStatus;

public class TransactionConflictException extends BaseException {

    private static String message = "Conflict";
    private static HttpStatus status = HttpStatus.CONFLICT;
    private static Integer code = ResponseCode.CONFLICT_DATA;

    public TransactionConflictException(String description) {
        super(message, code, status, description);
    }

    public TransactionConflictException(String message, String description) {
        super(message, code, status, description);
    }
}
