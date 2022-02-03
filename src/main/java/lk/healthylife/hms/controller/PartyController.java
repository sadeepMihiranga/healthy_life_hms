package lk.healthylife.hms.controller;

import lk.healthylife.hms.dto.PartyDTO;
import lk.healthylife.hms.response.SuccessResponse;
import lk.healthylife.hms.response.SuccessResponseHandler;
import lk.healthylife.hms.service.PartyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@CrossOrigin
@RestController
@RequestMapping("/api/parties")
public class PartyController {

    private final PartyService partyService;

    public PartyController(PartyService partyService) {
        this.partyService = partyService;
    }

    @GetMapping(path = "/search")
    public ResponseEntity<SuccessResponse> getCustomersByNamePaginated(@RequestParam(name = "name", required = false) String name,
                                                                       @RequestParam(name = "partyType", required = false) String partyType,
                                                                       @RequestParam(name = "page", required = true) Integer page,
                                                                       @RequestParam(name = "size", required = true) Integer size) {
        return SuccessResponseHandler.generateResponse(partyService.partyPaginatedSearch(name, partyType, page, size));
    }

    @PostMapping
    public ResponseEntity<SuccessResponse> insertParty(@RequestBody PartyDTO partyDTO) {
        return SuccessResponseHandler.generateResponse(partyService.createParty(partyDTO));
    }

    @GetMapping("/{partyCode}")
    public ResponseEntity<SuccessResponse> getPartyByPartyId(@PathVariable("partyCode") String partyCode) {
        return SuccessResponseHandler.generateResponse(partyService.getPartyByPartyCode(partyCode));
    }

    @PutMapping("/{partyCode}")
    public ResponseEntity<SuccessResponse> updateParty(@PathVariable("partyCode") String partyCode,
                                                       @RequestBody PartyDTO partyDTO) throws IOException {
        return SuccessResponseHandler.generateResponse(partyService.updateParty(partyCode, partyDTO));
    }

    @DeleteMapping("/{partyCode}")
    public ResponseEntity<SuccessResponse> removeParty(@PathVariable("partyCode") String partyCode) {
        return SuccessResponseHandler.generateResponse(partyService.removeParty(partyCode));
    }
}
