package lk.healthylife.hms.util.constant;

public enum ReportCodes {

    INQUIRY_DETAILED("RTINQDT");

    private String value;
    ReportCodes(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
