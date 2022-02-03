package lk.healthylife.hms.util.constant.status;

public enum ReservationStatus {

    CANCELLED((short) 0),
    CONFIRMED((short) 1),
    RELEASED((short) 2);

    private short shortValue;

    ReservationStatus(short shortValue) {
        this.shortValue = shortValue;
    }

    public short getShortValue() {
        return shortValue;
    }

    public static ReservationStatus getNameByCode(short status) {
        for(ReservationStatus reservationStatus : values()) {
            if(status == reservationStatus.getShortValue())
                return reservationStatus;
        }
        return null;
    }
}
