package lk.healthylife.hms.service;

import lk.healthylife.hms.dto.PaginatedEntity;
import lk.healthylife.hms.dto.PaymentDTO;

public interface PaymentService {

    PaymentDTO createPayment(PaymentDTO paymentDTO);

    PaymentDTO updatePayment(PaymentDTO paymentDTO);

    Boolean removePayment(Long paymentId, PaymentDTO paymentDTO);

    PaymentDTO getPaymentById(Long paymentId);

    PaginatedEntity patientSurgeryPaginatedSearch(String patientCode, Integer page, Integer size);
}
