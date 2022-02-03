package lk.healthylife.hms.exception;

import lk.healthylife.hms.response.ResponseCode;
import org.springframework.http.HttpStatus;

public class DataIntegrityViolationException extends BaseException {

    private static String message = "Violates not-null constraint - Some data are required";
    private static HttpStatus status = HttpStatus.BAD_REQUEST;
    private static Integer code = ResponseCode.MISSING_DATA;

    public DataIntegrityViolationException(String message, String description) {
        super(message, code, status, description);
    }

    public DataIntegrityViolationException(String description) {
        super(message, code, status, description);
    }

    public DataIntegrityViolationException() {
        super(message, code, status, "Violates not-null constraint - Some data are required");
    }
}
