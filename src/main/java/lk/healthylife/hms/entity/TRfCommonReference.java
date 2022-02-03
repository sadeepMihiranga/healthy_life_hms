package lk.healthylife.hms.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name="T_RF_COMMON_REFERENCE")
public class TRfCommonReference {

    @Id
    @Column(name = "CMRF_CODE")
    private String cmrfCode;

    @JoinColumn(name = "CMRF_CMRT_CODE")
    @ManyToOne(cascade = CascadeType.ALL)
    private TRfCommonReferenceType referenceType;

    @Column(name = "CMRF_DESCRIPTION")
    private String cmrfDescription;

    @Column(name = "CMRF_STRING_VALUE")
    private String cmrfStringValue;

    @Column(name = "CMRF_NUMBER_VALUES")
    private String cmrfNumberValue;

    @Column(name = "CMRF_STATUS")
    private Short cmrfStatus;
}
