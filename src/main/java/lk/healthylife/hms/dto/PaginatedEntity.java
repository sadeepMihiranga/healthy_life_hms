package lk.healthylife.hms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedEntity {

    private List<? extends Paginated> entities;
    private Integer totalNoOfPages;
    private Long totalNoOfRecords;
}
