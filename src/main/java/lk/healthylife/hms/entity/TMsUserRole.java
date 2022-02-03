package lk.healthylife.hms.entity;

import lombok.Data;

import javax.persistence.*;


@Data
@Entity
@Table(name="T_MS_USER_ROLE")
public class TMsUserRole {

    @Id
    @GeneratedValue(generator = "UserRoleSequence")
    @SequenceGenerator(name = "UserRoleSequence", schema = "LAKDERANA_BASE", sequenceName = "\"T_RF_USER_ROLES_ID_seq\"", allocationSize = 1)
    @Column(name="USRL_ID")
    private Long usrlId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USRL_USER_ID", referencedColumnName = "USER_ID", nullable = false)
    private TMsUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USRL_ROLE_ID", referencedColumnName = "ROLE_ID", nullable = false)
    private TMsRole role;

    @Column(name="USRL_STATUS")
    private Short usrlStatus;
}
