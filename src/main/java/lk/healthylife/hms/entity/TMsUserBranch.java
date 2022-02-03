package lk.healthylife.hms.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name="T_MS_USER_BRANCH")
public class TMsUserBranch {

    @Id
    @GeneratedValue(generator = "UserBranchSequence")
    @SequenceGenerator(name = "UserBranchSequence", schema = "LAKDERANA_BASE", sequenceName = "\"T_RF_BRANCH_BRNH_ID_seq\"", allocationSize = 1)
    @Column(name = "USBR_ID")
    private Long usbrId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USBR_USER_ID", referencedColumnName = "USER_ID", nullable = false)
    private TMsUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USBR_BRANCH_ID", referencedColumnName = "BRNH_ID", nullable = false)
    private TRfBranch branch;

    @Column(name = "USBR_STATUS")
    private Short usbrStatus;
}
