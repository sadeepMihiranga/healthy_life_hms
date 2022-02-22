package lk.healthylife.hms.util.constant;

public enum CommonReferenceTypeCodes {

    PARTY_TYPES("PTYPE"),
    PARTY_CONTACT_TYPES("CONTC"),
    GENDER_TYPES("GENDR"),
    ROOM_TYPES("ROMTP"),
    FACILITY_TYPES("FCLTP"),
    MEASUREMENT_TYPES("UOFMS"),
    PAYMENT_TYPES("PAYTP"),
    BLOOD_GROUP_TYPES("BLDGP"),
    DOC_SPECIALIZATION_TYPES("DCSPC"),
    MEDICINE_BRANDS("MDBRD"),
    MEDICINE_TYPES("MEDTP");

    private String value;
    private short shortValue;
    private int intValue;

    CommonReferenceTypeCodes(String value) {
        this.value = value;
    }

    CommonReferenceTypeCodes(short shortValue) {
        this.shortValue = shortValue;
    }

    CommonReferenceTypeCodes(int intValue) {
        this.intValue = intValue;
    }

    public String getValue() {
        return value;
    }

    public short getShortValue() {
        return shortValue;
    }

    public int getIntValue() {
        return intValue;
    }
}
