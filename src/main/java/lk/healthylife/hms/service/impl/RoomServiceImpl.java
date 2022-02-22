package lk.healthylife.hms.service.impl;

import lk.healthylife.hms.config.AuditorAwareImpl;
import lk.healthylife.hms.config.EntityValidator;
import lk.healthylife.hms.dto.PaginatedEntity;
import lk.healthylife.hms.dto.RoomDTO;
import lk.healthylife.hms.exception.NoRequiredInfoException;
import lk.healthylife.hms.exception.OperationException;
import lk.healthylife.hms.service.CommonReferenceService;
import lk.healthylife.hms.service.DepartmentService;
import lk.healthylife.hms.service.RoomService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static lk.healthylife.hms.util.constant.CommonReferenceTypeCodes.ROOM_TYPES;
import static lk.healthylife.hms.util.constant.Constants.STATUS_ACTIVE;
import static lk.healthylife.hms.util.constant.Constants.STATUS_INACTIVE;

@Slf4j
@Service
public class RoomServiceImpl extends EntityValidator implements RoomService {

    private final CommonReferenceService commonReferenceService;
    private final DepartmentService departmentService;

    private final AuditorAwareImpl auditorAware;

    @PersistenceContext
    private EntityManager entityManager;

    public RoomServiceImpl(CommonReferenceService commonReferenceService,
                           DepartmentService departmentService, AuditorAwareImpl auditorAware) {
        this.commonReferenceService = commonReferenceService;
        this.departmentService = departmentService;
        this.auditorAware = auditorAware;
    }

    @Override
    public List<RoomDTO> getAllRoomsDropdown() {

        List<RoomDTO> roomDTOList = new ArrayList<>();

        final String queryString = "SELECT ROOM_ID, ROOM_NO FROM T_MS_ROOM \n" +
                "WHERE ROOM_STATUS = :status \n" +
                "AND ROOM_BRANCH_ID IN (:branchIdList)\n" +
                "ORDER BY CREATED_DATE";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("branchIdList", captureBranchIds());

        List<Map<String,Object>> result = extractResultSet(query);

        if(result.size() == 0)
            return Collections.emptyList();

        for (Map<String,Object> room : result) {

            RoomDTO roomDTO = new RoomDTO();

            roomDTO.setRoomId(extractLongValue(String.valueOf(room.get("ROOM_ID"))));
            roomDTO.setRoomNo(extractValue(String.valueOf(room.get("ROOM_NO"))));

            roomDTOList.add(roomDTO);
        }

        return roomDTOList;
    }

    @Transactional
    @Override
    public RoomDTO createRoom(RoomDTO roomDTO) {

        validateEntity(roomDTO);

        validatePartyReferenceDetailsOnPersist(roomDTO);

        try {
            final Query query = entityManager.createNativeQuery("INSERT INTO T_MS_ROOM (ROOM_NO, ROOM_TYPE, ROOM_PER_DAY_CHARGE,\n" +
                            "ROOM_DESCRIPTION, ROOM_BRANCH_ID, ROOM_STATUS, CREATED_DATE, CREATED_USER_CODE, ROOM_DEPARTMENT_CODE)\n" +
                            "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)")
                    .setParameter(1, roomDTO.getRoomNo())
                    .setParameter(2, roomDTO.getRoomType())
                    .setParameter(3, roomDTO.getPerDayCharge())
                    .setParameter(4, Strings.isNullOrEmpty(roomDTO.getDescription()) ? null : roomDTO.getDescription())
                    .setParameter(5, captureBranchIds().get(0))
                    .setParameter(6, STATUS_ACTIVE.getShortValue())
                    .setParameter(7, LocalDateTime.now())
                    .setParameter(8, auditorAware.getCurrentAuditor().get())
                    .setParameter(9, roomDTO.getDepartment().getDepartmentCode());

            query.executeUpdate();
        } catch (Exception e) {
            log.error("Error while persisting Room : " + e.getMessage());
            throw new OperationException("Error while persisting Room");
        }

        return getRoomByIdOrNo(null, roomDTO.getRoomNo());
    }

    @Transactional
    @Override
    public RoomDTO updateRoom(Long roomId, RoomDTO roomDTO) {

        validateRoomId(roomId);

        validateEntity(roomDTO);

        validatePartyReferenceDetailsOnPersist(roomDTO);

        final Query query = entityManager.createNativeQuery("UPDATE T_MS_ROOM SET ROOM_NO = :roomNo, ROOM_TYPE = :roomType, \n" +
                        "ROOM_PER_DAY_CHARGE = :charge, ROOM_DESCRIPTION = :description, LAST_MOD_DATE = :lastModDate,\n" +
                        "LAST_MOD_USER_CODE = :lastModUser, ROOM_DEPARTMENT_CODE = :departmentCode\n" +
                        "WHERE ROOM_STATUS = :status AND ROOM_ID = :roomId AND ROOM_BRANCH_ID IN (:branchIdList)")
                .setParameter("roomNo", roomDTO.getRoomNo())
                .setParameter("roomType", roomDTO.getRoomType())
                .setParameter("charge", roomDTO.getPerDayCharge())
                .setParameter("description", roomDTO.getDescription())
                .setParameter("lastModDate", LocalDateTime.now())
                .setParameter("lastModUser", auditorAware.getCurrentAuditor().get())
                .setParameter("status", STATUS_ACTIVE.getShortValue())
                .setParameter("roomId", roomId)
                .setParameter("departmentCode", roomDTO.getDepartment().getDepartmentCode())
                .setParameter("branchIdList", captureBranchIds());

        query.executeUpdate();

        return getRoomByIdOrNo(roomId, null);
    }

    @Transactional
    @Override
    public Boolean removeRoom(Long roomId) {

        validateRoomId(roomId);

        final Query query = entityManager.createNativeQuery("UPDATE T_MS_ROOM SET ROOM_STATUS = :statusInActive\n" +
                        "WHERE ROOM_STATUS = :statusActive AND ROOM_ID = :roomId AND ROOM_BRANCH_ID IN (:branchIdList)")
                .setParameter("statusInActive", STATUS_INACTIVE.getShortValue())
                .setParameter("roomId", roomId)
                .setParameter("statusActive", STATUS_ACTIVE.getShortValue())
                .setParameter("branchIdList", captureBranchIds());

        return query.executeUpdate() == 0 ? false : true;
    }

    @Override
    public PaginatedEntity roomPaginatedSearch(String roomNo, String roomType, Integer page, Integer size) {

        PaginatedEntity paginatedRoomList = null;
        List<RoomDTO> roomList = null;

        validatePaginateIndexes(page, size);
        page = page == 1 ? 0 : page;

        roomNo = roomNo.isEmpty() ? null : roomNo;
        roomType = roomType.isEmpty() ? null : roomType;

        String countQueryString = "SELECT COUNT(ROOM_ID)\n" +
                "FROM T_MS_ROOM\n" +
                "WHERE ROOM_STATUS = :status \n" +
                "AND ROOM_BRANCH_ID IN (:branchIdList)\n" +
                "AND (:roomNo IS NULL OR (:roomNo IS NOT NULL) AND ROOM_NO = :roomNo)\n" +
                "AND (:roomType IS NULL OR (:roomType IS NOT NULL) AND ROOM_TYPE = :roomType)";

        Query query = entityManager.createNativeQuery(countQueryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("roomNo", roomNo);
        query.setParameter("roomType", roomType);
        query.setParameter("branchIdList", captureBranchIds());

        final int selectedRecordCount = ((Number) query.getSingleResult()).intValue();

        query = null;

        final String queryString = "SELECT rm.ROOM_ID, rm.ROOM_NO, rm.ROOM_TYPE, rm.ROOM_PER_DAY_CHARGE, rm.ROOM_DESCRIPTION, \n" +
                "rm.ROOM_BRANCH_ID, rm.ROOM_STATUS, rm.CREATED_DATE, rm.CREATED_USER_CODE, rm.LAST_MOD_DATE,\n" +
                "rm.LAST_MOD_USER_CODE, type.CMRF_DESCRIPTION AS ROOM_TYPE_NAME, br.BRNH_NAME AS BRANCH_NAME, rm.ROOM_DEPARTMENT_CODE\n" +
                "FROM T_MS_ROOM rm \n" +
                "LEFT JOIN T_RF_COMMON_REFERENCE type ON rm.ROOM_TYPE = type.CMRF_CODE\n" +
                "INNER JOIN T_RF_BRANCH br ON rm.ROOM_BRANCH_ID = br.BRNH_ID\n" +
                "WHERE rm.ROOM_STATUS = :status \n" +
                "AND rm.ROOM_BRANCH_ID IN (:branchIdList)\n" +
                "AND (:roomNo IS NULL OR (:roomNo IS NOT NULL) AND rm.ROOM_NO = :roomNo)\n" +
                "AND (:roomType IS NULL OR (:roomType IS NOT NULL) AND rm.ROOM_TYPE = :roomType)\n" +
                "ORDER BY rm.CREATED_DATE OFFSET :page ROWS FETCH NEXT :size ROWS ONLY";

        query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("roomNo", roomNo);
        query.setParameter("roomType", roomType);
        query.setParameter("branchIdList", captureBranchIds());
        query.setParameter("size", size);
        query.setParameter("page", size * page);

        List<Map<String,Object>> result = extractResultSet(query);

        paginatedRoomList = new PaginatedEntity();
        roomList = new ArrayList<>();

        for (Map<String,Object> room : result) {

            RoomDTO roomDTO = new RoomDTO();

            createDTO(roomDTO, room);

            roomList.add(roomDTO);
        }

        paginatedRoomList
                .setTotalNoOfPages(selectedRecordCount == 0 ? 0 : selectedRecordCount < size ? 1 : selectedRecordCount / size);
        paginatedRoomList.setTotalNoOfRecords(Long.valueOf(selectedRecordCount));
        paginatedRoomList.setEntities(roomList);

        return paginatedRoomList;
    }

    public RoomDTO getRoomByIdOrNo(Long roomId, String roomNo) {

        RoomDTO roomDTO = null;

        String queryString = "SELECT r.ROOM_ID, r.ROOM_NO, r.ROOM_TYPE, r.ROOM_PER_DAY_CHARGE, r.ROOM_DESCRIPTION,\n" +
                "r.ROOM_BRANCH_ID, r.ROOM_STATUS, r.CREATED_DATE, r.CREATED_USER_CODE, r.LAST_MOD_DATE,\n" +
                "r.LAST_MOD_USER_CODE, type.CMRF_DESCRIPTION AS ROOM_TYPE_NAME, br.BRNH_NAME AS BRANCH_NAME, ROOM_DEPARTMENT_CODE\n" +
                "FROM T_MS_ROOM r\n" +
                "LEFT JOIN T_RF_COMMON_REFERENCE type ON r.ROOM_TYPE = type.CMRF_CODE\n" +
                "INNER JOIN T_RF_BRANCH br ON r.ROOM_BRANCH_ID = br.BRNH_ID\n" +
                "WHERE r.ROOM_STATUS = :status " +
                "AND (:roomNo IS NULL OR (:roomNo IS NOT NULL) AND r.ROOM_NO = :roomNo) ";

                if(roomId != null)
                    queryString += "AND r.ROOM_ID = :roomId ";

                queryString += "AND r.ROOM_BRANCH_ID IN (:branchIdList)";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("branchIdList", captureBranchIds());
        if(roomId != null)
            query.setParameter("roomId", roomId);
        query.setParameter("roomNo", roomNo);

        List<Map<String,Object>> result = extractResultSet(query);

        if (result.size() == 0)
            return null;

        for (Map<String,Object> room : result) {

            roomDTO = new RoomDTO();

            createDTO(roomDTO, room);
        }

        return roomDTO;
    }

    private void validatePartyReferenceDetailsOnPersist(RoomDTO roomDTO) {

        if(roomDTO.getRoomType() != null)
            commonReferenceService
                    .getByCmrfCodeAndCmrtCode(ROOM_TYPES.getValue(), roomDTO.getRoomType());
    }

    private void createDTO(RoomDTO roomDTO, Map<String,Object> room) {

        roomDTO.setRoomId(extractLongValue(String.valueOf(room.get("ROOM_ID"))));
        roomDTO.setRoomNo(extractValue(String.valueOf(room.get("ROOM_NO"))));
        roomDTO.setRoomType(extractValue(String.valueOf(room.get("ROOM_TYPE"))));
        roomDTO.setPerDayCharge(extractDecimalValue(String.valueOf(room.get("ROOM_PER_DAY_CHARGE"))));
        roomDTO.setDescription(extractValue(String.valueOf(room.get("ROOM_DESCRIPTION"))));
        roomDTO.setBranchId(extractLongValue(String.valueOf(room.get("ROOM_BRANCH_ID"))));
        roomDTO.setBranchName(extractValue(String.valueOf(room.get("BRANCH_NAME"))));
        roomDTO.setRoomTypeName(extractValue(String.valueOf(room.get("ROOM_TYPE_NAME"))));
        roomDTO.setStatus(extractShortValue(String.valueOf(room.get("ROOM_STATUS"))));
        roomDTO.setCreatedDate(extractDateTime(String.valueOf(room.get("CREATED_DATE"))));
        roomDTO.setCreatedUserCode(extractValue(String.valueOf(room.get("CREATED_USER_CODE"))));
        roomDTO.setLastUpdatedDate(extractDateTime(String.valueOf(room.get("LAST_MOD_DATE"))));
        roomDTO.setLastUpdatedUserCode(extractValue(String.valueOf(room.get("LAST_MOD_USER_CODE"))));
        roomDTO.setDepartment(departmentService.getDepartmentByCode(extractValue(String.valueOf(room.get("ROOM_DEPARTMENT_CODE")))));
    }

    private void validateRoomId(Long roomId) {
        if(roomId == null)
            throw new NoRequiredInfoException("Room Id is Required");
    }
}
