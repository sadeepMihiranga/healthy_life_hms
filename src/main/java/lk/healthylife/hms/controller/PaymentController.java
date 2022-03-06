package lk.healthylife.hms.controller;

import lk.healthylife.hms.dto.PaymentDTO;
import lk.healthylife.hms.response.SuccessResponse;
import lk.healthylife.hms.response.SuccessResponseHandler;
import lk.healthylife.hms.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping(path = "/search")
    public ResponseEntity<SuccessResponse> searchPaymentPaginated(@RequestParam(name = "patientCode", required = false) String patientCode,
                                                                  @RequestParam(name = "page", required = true) Integer page,
                                                                  @RequestParam(name = "size", required = true) Integer size) {
        return SuccessResponseHandler.generateResponse(paymentService.patientSurgeryPaginatedSearch(patientCode, page, size));
    }

    @PostMapping
    public ResponseEntity<SuccessResponse> insertPayment(@RequestBody PaymentDTO paymentDTO) {
        return SuccessResponseHandler.generateResponse(paymentService.createPayment(paymentDTO));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<SuccessResponse> getPaymentByPaymentId(@PathVariable("paymentId") Long paymentId) {
        return SuccessResponseHandler.generateResponse(paymentService.getPaymentById(paymentId));
    }

    @DeleteMapping("/{paymentId}")
    public ResponseEntity<SuccessResponse> removeParty(@PathVariable("paymentId") Long paymentId, @RequestBody PaymentDTO paymentDTO) {
        return SuccessResponseHandler.generateResponse(paymentService.removePayment(paymentId, paymentDTO));
    }
}
