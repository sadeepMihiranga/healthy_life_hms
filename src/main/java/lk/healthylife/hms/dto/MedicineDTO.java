package lk.healthylife.hms.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicineDTO implements Paginated {

    private Long medicineId;
    @NotBlank(message = "Medicine Name is mandatory")
    private String name;
    @NotBlank(message = "Brand Name is mandatory")
    private String brand;
    private String brandName;
    @NotBlank(message = "Type Name is mandatory")
    private String type;
    private String typeName;
    private String description;
    private BigDecimal cost;
    @NotNull(message = "Dose is mandatory")
    private BigDecimal dose;
    @NotBlank(message = "Dose UOM is mandatory")
    private String doseUom;
    private String doseUomName;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate manufactureDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expireDate;
    private Integer availableQty;
    private Short status;
    private Long branchId;
    private String branchName;
    private LocalDateTime createdDate;
    private String createdUserCode;
    private LocalDateTime lastUpdatedDate;
    private String lastUpdatedUserCode;
}
