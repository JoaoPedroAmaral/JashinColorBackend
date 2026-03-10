package com.javation.coloringbook.Controller;

import com.javation.coloringbook.Entity.Users;
import com.javation.coloringbook.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<Users> postUser (@RequestBody Users body){
        Users user = userService.createUser(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Users> getUserById(@PathVariable Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = authentication.getName();
        Users currentUser = userService.findUserByEmail(currentEmail);

        if (!currentUser.getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Users user = userService.findUserById(userId);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/email/{userEmail}")
    public ResponseEntity<Users> getUserByEmail(@PathVariable String userEmail){
        Users user = userService.findUserByEmail(userEmail);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/me")
    public ResponseEntity<Users> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        Users user = userService.findUserByEmail(email);
        return ResponseEntity.ok(user);
    }
}
