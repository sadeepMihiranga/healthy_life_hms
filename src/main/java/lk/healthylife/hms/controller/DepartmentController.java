package lk.healthylife.hms.controller;

import lk.healthylife.hms.dto.DepartmentDTO;
import lk.healthylife.hms.response.SuccessResponse;
import lk.healthylife.hms.response.SuccessResponseHandler;
import lk.healthylife.hms.service.DepartmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@CrossOrigin
@RestController
@RequestMapping("/api/department")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping(path = "/search")
    public ResponseEntity<SuccessResponse> searchDepartmentsPaginated(@RequestParam(name = "departmentCode", required = false) String departmentCode,
                                                                      @RequestParam(name = "departmentName", required = false) String departmentName,
                                                                      @RequestParam(name = "page", required = true) Integer page,
                                                                      @RequestParam(name = "size", required = true) Integer size) {
        return SuccessResponseHandler.generateResponse(departmentService.departmentPaginatedSearch(departmentCode, departmentName, page, size));
    }

    @PostMapping
    public ResponseEntity<SuccessResponse> insertDepartment(@RequestBody DepartmentDTO departmentDTO) {
        return SuccessResponseHandler.generateResponse(departmentService.createDepartment(departmentDTO));
    }

    @GetMapping("/{departmentCode}")
    public ResponseEntity<SuccessResponse> getDepartmentByCode(@PathVariable("departmentCode") String departmentCode) {
        return SuccessResponseHandler.generateResponse(departmentService.getDepartmentByCode(departmentCode));
    }

    @PutMapping("/{departmentCode}")
    public ResponseEntity<SuccessResponse> updateParty(@PathVariable("departmentCode") String departmentCode,
                                                       @RequestBody DepartmentDTO departmentDTO) throws IOException {
        return SuccessResponseHandler.generateResponse(departmentService.updateDepartment(departmentCode, departmentDTO));
    }

    @DeleteMapping("/{departmentCode}")
    public ResponseEntity<SuccessResponse> removeParty(@PathVariable("departmentCode") String departmentCode) {
        return SuccessResponseHandler.generateResponse(departmentService.removeDepartment(departmentCode));
    }
}
