package lk.healthylife.hms.controller;

import lk.healthylife.hms.dto.MedicineDTO;
import lk.healthylife.hms.response.SuccessResponse;
import lk.healthylife.hms.response.SuccessResponseHandler;
import lk.healthylife.hms.service.MedicineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@CrossOrigin
@RestController
@RequestMapping("/api/medicine")
public class MedicineController {

    private final MedicineService medicineService;

    public MedicineController(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    @PostMapping
    public ResponseEntity<SuccessResponse> insertMedicine(@RequestBody MedicineDTO medicineDTO) {
        return SuccessResponseHandler.generateResponse(medicineService.insertMedicine(medicineDTO));
    }

    @GetMapping("/{medicineId}")
    public ResponseEntity<SuccessResponse> getRoomById(@PathVariable("medicineId") Long medicineId) {
        return SuccessResponseHandler.generateResponse(medicineService.getMedicineById(medicineId));
    }

    @GetMapping(path = "/search")
    public ResponseEntity<SuccessResponse> searchRoomsPaginated(@RequestParam(name = "name", required = false) String name,
                                                                @RequestParam(name = "brand", required = false) String brand,
                                                                @RequestParam(name = "type", required = false) String type,
                                                                @RequestParam(name = "page", required = true) Integer page,
                                                                @RequestParam(name = "size", required = true) Integer size) {
        return SuccessResponseHandler.generateResponse(medicineService.medicinePaginatedSearch(name, brand, type, page, size));
    }

    @PutMapping("/{medicineId}")
    public ResponseEntity<SuccessResponse> updateRoom(@PathVariable("medicineId") Long medicineId,
                                                      @RequestBody MedicineDTO medicineDTO) throws IOException {
        return SuccessResponseHandler.generateResponse(medicineService.updateMedicine(medicineId, medicineDTO));
    }

    @DeleteMapping("/{medicineId}")
    public ResponseEntity<SuccessResponse> removeRoom(@PathVariable("medicineId") Long medicineId) {
        return SuccessResponseHandler.generateResponse(medicineService.removeMedicine(medicineId));
    }
}
