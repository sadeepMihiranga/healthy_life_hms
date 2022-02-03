package lk.healthylife.hms.util.constant.status;


public enum InquiryStatus {

    CREATED((short) 1),
    RESERVED((short) 2),
    TRANSFERRED_TO_ANOTHER((short) 3),
    CANCELED((short) 4);

    private short shortValue;

    InquiryStatus(short shortValue) {
        this.shortValue = shortValue;
    }

    public short getShortValue() {
        return shortValue;
    }

    public static InquiryStatus getNameByCode(short status) {

        for(InquiryStatus inquiryStatus : values()) {
            if(status == inquiryStatus.getShortValue())
                return inquiryStatus;
        }

        return null;
    }
}
