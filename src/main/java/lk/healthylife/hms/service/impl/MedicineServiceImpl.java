package lk.healthylife.hms.service.impl;

import lk.healthylife.hms.config.AuditorAwareImpl;
import lk.healthylife.hms.config.EntityValidator;
import lk.healthylife.hms.dto.MedicineDTO;
import lk.healthylife.hms.dto.PaginatedEntity;
import lk.healthylife.hms.exception.InvalidDataException;
import lk.healthylife.hms.exception.NoRequiredInfoException;
import lk.healthylife.hms.exception.OperationException;
import lk.healthylife.hms.service.CommonReferenceService;
import lk.healthylife.hms.service.MedicineService;
import lk.healthylife.hms.util.DateConversion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.sql.DataSource;
import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static lk.healthylife.hms.util.constant.CommonReferenceTypeCodes.*;
import static lk.healthylife.hms.util.constant.Constants.STATUS_ACTIVE;
import static lk.healthylife.hms.util.constant.Constants.STATUS_INACTIVE;

@Slf4j
@Service
public class MedicineServiceImpl extends EntityValidator implements MedicineService {

    private final DataSource dataSource;

    private final CommonReferenceService commonReferenceService;

    private final AuditorAwareImpl auditorAware;

    @PersistenceContext
    private EntityManager entityManager;

    public MedicineServiceImpl(DataSource dataSource,
                               CommonReferenceService commonReferenceService,
                               AuditorAwareImpl auditorAware) {
        this.dataSource = dataSource;
        this.commonReferenceService = commonReferenceService;
        this.auditorAware = auditorAware;
    }

    @Override
    public List<MedicineDTO> getAllMedicinesDropdown() {

        List<MedicineDTO> medicineDTOList = new ArrayList<>();

        final String queryString = "SELECT MEDI_ID, MEDI_NAME FROM T_MS_MEDICINE\n" +
                "WHERE MEDI_STAUS = :status AND MEDI_BRANCH_ID IN (:branchIdList)" +
                "ORDER BY CREATED_DATE";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("branchIdList", captureBranchIds());

        List<Map<String,Object>> result = extractResultSet(query);

        if(result.size() == 0)
            return Collections.emptyList();

        for (Map<String,Object> room : result) {

            MedicineDTO medicineDTO = new MedicineDTO();

            medicineDTO.setMedicineId(extractLongValue(String.valueOf(room.get("MEDI_ID"))));
            medicineDTO.setName(extractValue(String.valueOf(room.get("MEDI_NAME"))));

            medicineDTOList.add(medicineDTO);
        }

        return medicineDTOList;
    }

    @Transactional
    @Override
    public MedicineDTO insertMedicine(MedicineDTO medicineDTO) {

        BigInteger insertedRowId = null;

        validateEntity(medicineDTO);

        if(medicineDTO.getDose().compareTo(BigDecimal.ZERO) <= 0 || medicineDTO.getDose().compareTo(BigDecimal.valueOf(20)) > 0)
            throw new InvalidDataException("Invalid dose of medicine");

        try {
            if (!DateConversion.isDateAfter(DateConversion.convertLocalDateToString(medicineDTO.getManufactureDate()),
                    DateConversion.convertLocalDateToString(medicineDTO.getExpireDate()), DateConversion.STANDARD_DATE_FORMAT))
                throw new InvalidDataException("Expire Date should be higher that Manufacture Date");
        }catch (ParseException e) {
            throw new OperationException("Error while saving Medicine");
        }

        validatePartyReferenceDetailsOnPersist(medicineDTO);

        try (Connection connection = dataSource.getConnection()) {

            CallableStatement statement = connection.prepareCall("{CALL INSERT INTO T_MS_MEDICINE(MEDI_NAME, MEDI_BRAND, MEDI_TYPE, \n" +
                    "MEDI_DESCRIPTION, MEDI_COST, MEDI_DOSE, MEDI_DOSE_UOM, MEDI_MANUFACTURE_DATE, MEDI_EXPIRE_DATE, \n" +
                    "MEDI_AVAILABLE_QUANTITY, MEDI_STAUS, CREATED_DATE, CREATED_USER_CODE, MEDI_BRANCH_ID) \n" +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING MEDI_ID INTO ? }");

            statement.setString(1, medicineDTO.getName());
            statement.setString(2, medicineDTO.getBrand());
            statement.setString(3, medicineDTO.getType());
            statement.setString(4, medicineDTO.getDescription());
            statement.setBigDecimal(5, medicineDTO.getCost());
            statement.setBigDecimal(6, medicineDTO.getDose());
            statement.setString(7, medicineDTO.getDoseUom());
            statement.setTimestamp(8, Timestamp.valueOf(medicineDTO.getManufactureDate().atStartOfDay()));
            statement.setTimestamp(9, Timestamp.valueOf(medicineDTO.getExpireDate().atStartOfDay()));
            statement.setInt(10, medicineDTO.getAvailableQty());
            statement.setShort(11, STATUS_ACTIVE.getShortValue());
            statement.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(13, auditorAware.getCurrentAuditor().get());
            statement.setLong(14, captureBranchIds().get(0));

            statement.registerOutParameter( 15, Types.BIGINT);

            int updateCount = statement.executeUpdate();

            if (updateCount > 0)
                insertedRowId = BigInteger.valueOf(statement.getLong(15));

        } catch (Exception e) {
            log.error("Error while persisting Medicine : " + e.getMessage());
            throw new OperationException("Error while persisting Medicine");
        }

        return getMedicineById(insertedRowId.longValue());
    }

    @Transactional
    @Override
    public MedicineDTO updateMedicine(Long medicineId, MedicineDTO medicineDTO) {

        validateMedicineId(medicineId);

        validatePartyReferenceDetailsOnPersist(medicineDTO);

        final Query query = entityManager
                .createNativeQuery("UPDATE T_MS_MEDICINE SET MEDI_NAME = :name, MEDI_BRAND = :brand, MEDI_TYPE = :type,\n" +
                        "MEDI_DESCRIPTION = :description, MEDI_COST = :cost, MEDI_DOSE = :dose, MEDI_DOSE_UOM = :uom,\n" +
                        "MEDI_MANUFACTURE_DATE = :manufactureDate, MEDI_EXPIRE_DATE = :expireDate, MEDI_AVAILABLE_QUANTITY = :qty,\n" +
                        "LAST_MOD_DATE = :lastModDate, LAST_MOD_USER_CODE = :lastModUser\n" +
                        "WHERE MEDI_ID = :medicineId AND MEDI_STAUS = :status AND MEDI_BRANCH_ID IN (:branchIdList)")
                .setParameter("name", medicineDTO.getName())
                .setParameter("brand", medicineDTO.getBrand())
                .setParameter("type", medicineDTO.getType())
                .setParameter("description", medicineDTO.getDescription())
                .setParameter("cost", medicineDTO.getCost())
                .setParameter("dose", medicineDTO.getDose())
                .setParameter("uom", medicineDTO.getDoseUom())
                .setParameter("manufactureDate", medicineDTO.getManufactureDate())
                .setParameter("expireDate", medicineDTO.getExpireDate())
                .setParameter("qty", medicineDTO.getAvailableQty())
                .setParameter("lastModDate", LocalDateTime.now())
                .setParameter("lastModUser", auditorAware.getCurrentAuditor().get())
                .setParameter("status", STATUS_ACTIVE.getShortValue())
                .setParameter("medicineId", medicineId)
                .setParameter("branchIdList", captureBranchIds());

        query.executeUpdate();

        return getMedicineById(medicineId);
    }

    @Transactional
    @Override
    public Boolean removeMedicine(Long medicineId) {

        validateMedicineId(medicineId);

        final Query query = entityManager.createNativeQuery("UPDATE T_MS_MEDICINE SET MEDI_STAUS = :statusInActive\n" +
                        "WHERE MEDI_ID = :medicineId AND MEDI_STAUS = :statusActive AND MEDI_BRANCH_ID IN (:branchIdList)")
                .setParameter("statusInActive", STATUS_INACTIVE.getShortValue())
                .setParameter("medicineId", medicineId)
                .setParameter("statusActive", STATUS_ACTIVE.getShortValue())
                .setParameter("branchIdList", captureBranchIds());

        return query.executeUpdate() == 0 ? false : true;
    }

    @Override
    public PaginatedEntity medicinePaginatedSearch(String name, String brand, String type, Integer page, Integer size) {

        PaginatedEntity paginatedMedicineList = null;
        List<MedicineDTO> medicineList = null;

        page = validatePaginateIndexes(page, size);

        type = type.isEmpty() ? null : type;
        brand = brand.isEmpty() ? null : brand;

        final String countQueryString = "SELECT COUNT(MEDI_ID) FROM T_MS_MEDICINE \n" +
                "WHERE MEDI_STAUS = :status AND MEDI_BRANCH_ID IN (:branchIdList)\n" +
                "AND (:brand IS NULL OR (:brand IS NOT NULL) AND MEDI_BRAND = :brand) \n" +
                "AND (:type IS NULL OR (:type IS NOT NULL) AND MEDI_TYPE = :type) \n" +
                "AND (upper(MEDI_NAME) LIKE ('%'||upper(:name)||'%'))";

        Query query = entityManager.createNativeQuery(countQueryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("brand", brand);
        query.setParameter("type", type);
        query.setParameter("name", name);
        query.setParameter("branchIdList", captureBranchIds());

        final int selectedRecordCount = ((Number) query.getSingleResult()).intValue();

        query = null;

        final String queryString = "SELECT md.MEDI_ID, md.MEDI_NAME, md.MEDI_BRAND, md.MEDI_TYPE, md.MEDI_DESCRIPTION, \n" +
                "md.MEDI_COST, md.MEDI_DOSE, md.MEDI_DOSE_UOM, md.MEDI_MANUFACTURE_DATE, md.MEDI_EXPIRE_DATE, \n" +
                "md.MEDI_AVAILABLE_QUANTITY, md.MEDI_STAUS, md.CREATED_DATE, md.CREATED_USER_CODE, \n" +
                "md.MEDI_BRANCH_ID, md.LAST_MOD_DATE, md.LAST_MOD_USER_CODE, brand.CMRF_DESCRIPTION AS BRAND_NAME, \n" +
                "type.CMRF_DESCRIPTION AS TYPE_NAME, uom.CMRF_DESCRIPTION AS DOSE_UOM_NAME\n" +
                "FROM T_MS_MEDICINE md \n" +
                "LEFT JOIN T_RF_COMMON_REFERENCE brand ON md.MEDI_BRAND = brand.CMRF_CODE\n" +
                "LEFT JOIN T_RF_COMMON_REFERENCE type ON md.MEDI_TYPE = type.CMRF_CODE\n" +
                "LEFT JOIN T_RF_COMMON_REFERENCE uom ON md.MEDI_DOSE_UOM = uom.CMRF_CODE\n" +
                "WHERE md.MEDI_STAUS = :status AND md.MEDI_BRANCH_ID IN (:branchIdList)\n" +
                "AND (:brand IS NULL OR (:brand IS NOT NULL) AND md.MEDI_BRAND = :brand) \n" +
                "AND (:type IS NULL OR (:type IS NOT NULL) AND md.MEDI_TYPE = :type) \n" +
                "AND (upper(md.MEDI_NAME) LIKE ('%'||upper(:name)||'%')) \n" +
                "ORDER BY md.CREATED_DATE OFFSET :page ROWS FETCH NEXT :size ROWS ONLY";

        query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("brand", brand);
        query.setParameter("type", type);
        query.setParameter("name", name);
        query.setParameter("branchIdList", captureBranchIds());
        query.setParameter("size", size);
        query.setParameter("page", size * page);

        List<Map<String,Object>> result = extractResultSet(query);

        paginatedMedicineList = new PaginatedEntity();
        medicineList = new ArrayList<>();

        for (Map<String,Object> medicine : result) {

            MedicineDTO medicineDTO = new MedicineDTO();

            createDTO(medicineDTO, medicine);

            medicineList.add(medicineDTO);
        }

        paginatedMedicineList
                .setTotalNoOfPages(getTotalNoOfPages(selectedRecordCount, size));
        paginatedMedicineList.setTotalNoOfRecords(Long.valueOf(selectedRecordCount));
        paginatedMedicineList.setEntities(medicineList);

        return paginatedMedicineList;
    }

    @Override
    public MedicineDTO getMedicineById(Long medicineId) {

        MedicineDTO medicineDTO = null;

        validateMedicineId(medicineId);

        String queryString = "SELECT md.MEDI_ID, md.MEDI_NAME, md.MEDI_BRAND, md.MEDI_TYPE, md.MEDI_DESCRIPTION, \n" +
                "md.MEDI_COST, md.MEDI_DOSE, md.MEDI_DOSE_UOM, md.MEDI_MANUFACTURE_DATE, md.MEDI_EXPIRE_DATE, \n" +
                "md.MEDI_AVAILABLE_QUANTITY, md.MEDI_STAUS, md.CREATED_DATE, md.CREATED_USER_CODE, \n" +
                "md.MEDI_BRANCH_ID, md.LAST_MOD_DATE, md.LAST_MOD_USER_CODE, brand.CMRF_DESCRIPTION AS BRAND_NAME, \n" +
                "type.CMRF_DESCRIPTION AS TYPE_NAME, uom.CMRF_DESCRIPTION AS DOSE_UOM_NAME\n" +
                "FROM T_MS_MEDICINE md \n" +
                "LEFT JOIN T_RF_COMMON_REFERENCE brand ON md.MEDI_BRAND = brand.CMRF_CODE\n" +
                "LEFT JOIN T_RF_COMMON_REFERENCE type ON md.MEDI_TYPE = type.CMRF_CODE\n" +
                "LEFT JOIN T_RF_COMMON_REFERENCE uom ON md.MEDI_DOSE_UOM = uom.CMRF_CODE\n" +
                "WHERE md.MEDI_ID = :medicineId AND md.MEDI_STAUS = :status AND md.MEDI_BRANCH_ID IN (:branchIdList)";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("branchIdList", captureBranchIds());
        query.setParameter("medicineId", medicineId);

        List<Map<String,Object>> result = extractResultSet(query);

        if (result.size() == 0)
            return null;

        for (Map<String,Object> medicine : result) {

            medicineDTO = new MedicineDTO();

            createDTO(medicineDTO, medicine);
        }

        return medicineDTO;
    }

    private void validateMedicineId(Long medicineId) {
        if(medicineId == null)
            throw new NoRequiredInfoException("Medicine Id is Required");
    }

    private void validatePartyReferenceDetailsOnPersist(MedicineDTO medicineDTO) {

        if(medicineDTO.getBrand() != null)
            commonReferenceService
                    .getByCmrfCodeAndCmrtCode(MEDICINE_BRANDS.getValue(), medicineDTO.getBrand());

        if(medicineDTO.getType() != null)
            commonReferenceService
                    .getByCmrfCodeAndCmrtCode(MEDICINE_TYPES.getValue(), medicineDTO.getType());

        if(medicineDTO.getDoseUom() != null)
            commonReferenceService
                    .getByCmrfCodeAndCmrtCode(MEASUREMENT_TYPES.getValue(), medicineDTO.getDoseUom());
    }

    private void createDTO(MedicineDTO medicineDTO, Map<String,Object> medicine) {

        medicineDTO.setMedicineId(extractLongValue(String.valueOf(medicine.get("MEDI_ID"))));
        medicineDTO.setName(extractValue(String.valueOf(medicine.get("MEDI_NAME"))));
        medicineDTO.setBrand(extractValue(String.valueOf(medicine.get("MEDI_BRAND"))));
        medicineDTO.setBrandName(extractValue(String.valueOf(medicine.get("BRAND_NAME"))));
        medicineDTO.setType(extractValue(String.valueOf(medicine.get("MEDI_TYPE"))));
        medicineDTO.setTypeName(extractValue(String.valueOf(medicine.get("TYPE_NAME"))));
        medicineDTO.setDescription(extractValue(String.valueOf(medicine.get("MEDI_DESCRIPTION"))));
        medicineDTO.setCost(extractDecimalValue(String.valueOf(medicine.get("MEDI_COST"))));
        medicineDTO.setDose(extractDecimalValue(String.valueOf(medicine.get("MEDI_DOSE"))));
        medicineDTO.setDoseUom(extractValue(String.valueOf(medicine.get("MEDI_DOSE_UOM"))));
        medicineDTO.setDoseUomName(extractValue(String.valueOf(medicine.get("DOSE_UOM_NAME"))));
        medicineDTO.setManufactureDate(extractDate(String.valueOf(medicine.get("MEDI_MANUFACTURE_DATE"))));
        medicineDTO.setExpireDate(extractDate(String.valueOf(medicine.get("MEDI_EXPIRE_DATE"))));
        medicineDTO.setAvailableQty(extractIntegerValue(String.valueOf(medicine.get("MEDI_AVAILABLE_QUANTITY"))));
        medicineDTO.setStatus(extractShortValue(String.valueOf(medicine.get("MEDI_STAUS"))));
        medicineDTO.setCreatedDate(extractDateTime(String.valueOf(medicine.get("CREATED_DATE"))));
        medicineDTO.setCreatedUserCode(extractValue(String.valueOf(medicine.get("CREATED_USER_CODE"))));
        medicineDTO.setLastUpdatedDate(extractDateTime(String.valueOf(medicine.get("LAST_MOD_DATE"))));
        medicineDTO.setLastUpdatedUserCode(extractValue(String.valueOf(medicine.get("LAST_MOD_USER_CODE"))));
        medicineDTO.setBranchId(extractLongValue(String.valueOf(medicine.get("MEDI_BRANCH_ID"))));
    }
}
