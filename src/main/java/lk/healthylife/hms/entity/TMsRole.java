package lk.healthylife.hms.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name="T_MS_ROLE")
public class TMsRole {

    @Id
    @GeneratedValue(generator = "RoleSequence")
    @SequenceGenerator(name = "RoleSequence", schema = "LAKDERANA_BASE", sequenceName = "\"T_MS_ROLE_ID_seq\"", allocationSize = 1)
    @Column(name = "ROLE_ID")
    private Long roleId;

    @Column(name = "ROLE_NAME")
    private String roleName;

    @Column(name = "ROLE_DESCRIPTION")
    private String roleDescription;

    @Column(name = "ROLE_STATUS")
    private Short roleStatus;

    @OneToMany(mappedBy = "role")
    private List<TMsUserRole> userRoles;
}
