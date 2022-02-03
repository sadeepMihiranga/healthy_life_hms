package lk.healthylife.hms.service;

public interface NumberGeneratorService {

    String generateNumber(String refNumType, String increase, String subTypeRef1, String subTypeRef2, String subTypeRef3,
                          String subTypeRef4, String year, String month);
}
