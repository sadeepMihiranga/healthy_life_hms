package lk.healthylife.hms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
@AllArgsConstructor
public class UpdatePasswordDTO {

	@NotEmpty(message = "Username/Code is Required")
	private String username;
	@NotEmpty(message = "Password is Required")
	private String newPassword;
	@NotEmpty(message = "Token is Required")
	private String token;
	@NotEmpty(message = "Token Type is Required")
	private String tokenType;
	
	public UpdatePasswordDTO() {
		super();
	}
}
