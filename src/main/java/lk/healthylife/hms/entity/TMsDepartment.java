package lk.healthylife.hms.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name="T_MS_DEPARTMENT")
public class TMsDepartment extends AuditModel {

    @Id
    @Column(name = "DPMT_CODE")
    private String dpmtCode;

    @Column(name = "DPMT_NAME")
    private String dpmtName;

    @Column(name = "DPMT_DESCRIPTION")
    private String dpmtDescription;

    @Column(name = "DPMT_STATUS")
    private Short dpmtStatus;

    @JoinColumn(name = "DPMT_HEAD")
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    private TMsParty departmentHead;
}
