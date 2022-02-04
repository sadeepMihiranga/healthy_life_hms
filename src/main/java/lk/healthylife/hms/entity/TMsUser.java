package lk.healthylife.hms.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name="T_MS_USER")
public class TMsUser {

    @Id
    @GeneratedValue(generator = "UserSequence")
    @SequenceGenerator(name = "UserSequence", schema = "HEALTHYLIFE_BASE", sequenceName = "\"T_MS_USER_USER_ID_seq\"", allocationSize = 1)
    @Column(name = "USER_ID")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USER_PARTY_CODE", nullable = false)
    private TMsParty party;

    @Column(name = "USER_PASSWORD")
    private String userPassword;

    @Column(name = "USER_USERNAME")
    private String userUsername;

    @Column(name = "USER_STATUS")
    private Short userStatus;
}
