package lk.healthylife.hms.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name="T_MS_PARTY_CONTACT")
public class TMsPartyContact {

    @Id
    @GeneratedValue(generator = "PartyContactSequence")
    @SequenceGenerator(name = "PartyContactSequence", schema = "LAKDERANA_BASE", sequenceName = "\"T_MS_PARTY_CONTACT_PTCN_ID_seq\"", allocationSize = 1)
    @Column(name = "PTCN_ID")
    private Long ptcnId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="PTCN_PRTY_CODE", nullable = false)
    private TMsParty party;

    @Column(name = "PTCN_CONTACT_TYPE")
    private String ptcnContactType;

    @Column(name = "PTCN_CONTACT_NUMBER")
    private String ptcnContactNumber;

    @Column(name = "PTCN_STATUS")
    private Short ptcnStatus;
}
