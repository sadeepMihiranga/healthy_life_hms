package lk.healthylife.hms.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuccessResponse {

	private Object data;
	private String message;
	private Boolean success;
	private int code = ResponseCode.SUCCESS;

    public SuccessResponse(Object data) {
        this.data = data;
        this.message = "Operation completed successfully";
        this.success = true;
    }

    public SuccessResponse(Object data, String message) {
        this.data = data;
        this.message = message;
        this.success = true;
    }

    @Override
    public String toString() {
        return "{" +
                "data=" + data +
                ", message='" + message + '\'' +
                ", success=" + success +
                ", code=" + code +
                '}';
    }
}
