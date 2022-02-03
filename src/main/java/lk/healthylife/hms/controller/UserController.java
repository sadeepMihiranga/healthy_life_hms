package lk.healthylife.hms.controller;

import lk.healthylife.hms.dto.UserDTO;
import lk.healthylife.hms.entity.TMsRole;
import lk.healthylife.hms.response.SuccessResponse;
import lk.healthylife.hms.response.SuccessResponseHandler;
import lk.healthylife.hms.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<SuccessResponse> getAllUsers() {
        return SuccessResponseHandler.generateResponse(userService.getAllUsers());
    }

    @GetMapping("/search")
    public ResponseEntity<SuccessResponse> getPaginatedUsers(@RequestParam(name = "username", required = false) String username,
                                                             @RequestParam(name = "partyCode", required = false) String partyCode,
                                                             @RequestParam(name = "page", required = true) Integer page,
                                                             @RequestParam(name = "size", required = true) Integer size) {
        return SuccessResponseHandler.generateResponse(userService.userPaginatedSearch(username, partyCode, page, size));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<SuccessResponse> getAUser(@PathVariable("userId") Long userId) {
        return SuccessResponseHandler.generateResponse(userService.getUserById(userId));
    }

    @PostMapping
    public ResponseEntity<SuccessResponse> createAUser(@RequestBody UserDTO userDTO) {
        return SuccessResponseHandler.generateResponse(userService.createUser(userDTO));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<SuccessResponse> removeUser(@PathVariable("userId") Long userId) {
        return SuccessResponseHandler.generateResponse(userService.removeUserById(userId));
    }

    @PostMapping("/{userId}/roles/assign")
    public ResponseEntity<?> assignRoleToUser(@PathVariable("userId") Long userId, @RequestBody List<String> roles) {
        return SuccessResponseHandler.generateResponse(userService.assignRoleToUser(userId, roles));
    }

    @PostMapping("/role")
    public ResponseEntity<TMsRole> createARole(@RequestBody TMsRole role) {
        return ResponseEntity.ok().body(userService.createRole(role));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<SuccessResponse> updateUser(@PathVariable("userId") Long userId, @RequestBody UserDTO userDTO) {
        return SuccessResponseHandler.generateResponse(userService.updateUser(userId, userDTO));
    }
}
