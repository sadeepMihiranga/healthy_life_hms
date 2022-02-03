package lk.healthylife.hms.exception;

import lk.healthylife.hms.response.ResponseCode;
import org.springframework.http.HttpStatus;

public class ConstraintException extends BaseException {

    private static String message = "Constraints Violation";
    private static HttpStatus status = HttpStatus.BAD_REQUEST;
    private static Integer code = ResponseCode.INVALID_INPUT;

    public ConstraintException(String description) {
        super(message, code, status, description);
    }

    public ConstraintException(String message,String description) {
        super(message, code, status, description);
    }
}
