package lk.healthylife.hms.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.Calendar;
import java.util.Date;

/**
 * The persistent class for the "T_CM_MS_PARTY_TOKEN" database table.
 * 
 */
@Entity
@Table(name="T_MS_PARTY_TOKEN")
@Data
@NoArgsConstructor
public class TMsPartyToken {
	private static final long serialVersionUID = 1L;
	private static final int EXPIRATION = 60;

	@Id
	@Column(name="TOKN_SEQ_NO")
	private String toknSeqNo;

	@Column(name="CREATED_DATE")
	@Temporal(TemporalType.TIMESTAMP)
	private Date createdDate;

	@Column(name="CREATED_USER_CODE")
	private String createdUserCode;

	@Column(name="LAST_MOD_DATE")
	@Temporal(TemporalType.TIMESTAMP)
	private Date lastModDate;

	@Column(name="LAST_MOD_USER_CODE")
	private String lastModUserCode;

	@Column(name="TOKN_EXPIRY_TIME")
	@Temporal(TemporalType.TIMESTAMP)
	private Date toknExpiryTime;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "TOKN_PARTY_CODE", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private TMsParty party;

	@Column(name="TOKN_PIN_NO")
	private String toknPinNo;

	@Column(name="TOKN_REQUEST_TYPE")
	private String toknRequestType;

	@Column(name="TOKN_TOKEN")
	private String toknToken;

	@Column(name="TOKN_STATUS")
	private String toknStatus;

	public void setToknExpiryTime() {
		this.toknExpiryTime = calculateExpiryDate(EXPIRATION);
	}

	private Date calculateExpiryDate(final int expiryTimeInMinutes) {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(new Date().getTime());
        cal.add(Calendar.MINUTE, expiryTimeInMinutes);
        return new Date(cal.getTime().getTime());
    }
}