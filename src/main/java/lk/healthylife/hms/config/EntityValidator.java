package lk.healthylife.hms.config;

import lk.healthylife.hms.exception.DataNotFoundException;
import lk.healthylife.hms.exception.InvalidDataException;
import lk.healthylife.hms.exception.NoRequiredInfoException;
import lk.healthylife.hms.util.DateConversion;
import lk.healthylife.hms.util.constant.Constants;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.hibernate.query.internal.NativeQueryImpl;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class EntityValidator {

    @Autowired
    private LocalValidatorFactoryBean localValidatorFactoryBean;

    @Autowired
    private HttpServletRequest request;

    protected void validateEntity(Object dto) {
        Set<ConstraintViolation<Object>> violations = localValidatorFactoryBean.validate(dto);
        if (violations.stream().findFirst().isPresent()) {
            throw new DataNotFoundException(violations.stream().findFirst().get().getMessage());
        }
    }

    protected final List<Long> captureBranchIds() {

        final Object attribute = request.getAttribute(Constants.REQUEST_BRANCHES.getValue());

        if(attribute == null)
            throw new NoRequiredInfoException("Branch Id is missing");

        String branchesString = attribute.toString();

        if(Strings.isNullOrEmpty(branchesString))
            throw new NoRequiredInfoException("Branch Id is missing");

        String[] branchesArray = branchesString
                .replaceAll("\\[", "")
                .replaceAll("\\]", "")
                .replaceAll("\\s", "")
                .split(",");

        List<Long> branches = new ArrayList<>();

        for(String branch : branchesArray) {
            branches.add(Long.valueOf(branch));
        }

        log.info("Request came from branch {} ", branchesArray[0]);

        return branches;
    }

    protected final void validatePaginateIndexes(Integer page, Integer size) {

        if (page < 1)
            throw new InvalidDataException("Page should be a value greater than 0");

        if (size < 1)
            throw new InvalidDataException("Limit should be a value greater than 0");
    }

    protected final String getExceptionMessageChain(Throwable throwable) {
        List<String> result = new ArrayList<String>();

        while (throwable != null) {
            result.add(throwable.getMessage());
            throwable = throwable.getCause();
        }

        for(String errorMessage : result) {
            if(errorMessage.contains("duplicate key value")) {
                return errorMessage.substring(errorMessage.indexOf(")=") + 2);
            }
        }

        return result.get(0);
    }

    protected final String extractValue(String columnValue) {
        return columnValue == null || columnValue.equals("null") ? null : columnValue;
    }

    protected final Long extractLongValue(String columnValue) {
        return columnValue == null || columnValue.equals("null") ? null : Long.valueOf(columnValue);
    }

    protected final Short extractShortValue(String columnValue) {
        return columnValue == null || columnValue.equals("null") ? null : Short.valueOf(columnValue);
    }

    protected final Integer extractIntegerValue(String columnValue) {
        return columnValue == null || columnValue.equals("null") ? null : Integer.valueOf(columnValue);
    }

    protected final LocalDateTime extractDateTime(String columnValue) {
        return columnValue == null || columnValue.equals("null") ? null
                : DateConversion.convertStringToLocalDateTime(columnValue.split("\\.")[0]);
    }

    protected final LocalDate extractDate(String columnValue) {
        return columnValue == null || columnValue.equals("null") ? null
                : DateConversion.convertStringToLocalDateTime(columnValue.split("\\.")[0]).toLocalDate();
    }

    protected final BigDecimal extractDecimalValue(String columnValue) {
        return columnValue == null || columnValue.equals("null") ? null : BigDecimal.valueOf(Long.valueOf(columnValue).longValue());
    }

    protected final List<Map<String,Object>> extractResultSet(Query query) {
        NativeQueryImpl nativeQuery = (NativeQueryImpl) query;
        nativeQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
        return nativeQuery.getResultList();
    }
}
