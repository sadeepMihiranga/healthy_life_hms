package lk.healthylife.hms.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name="T_MS_FUNCTION")
public class TMsFunction {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "FUNC_ID")
    private String funcId;

    @Column(name = "FUNC_STATUS")
    private Short funcStatus;

    @Column(name = "FUNC_DESCRIPTION")
    private String dunsDescription;
}
