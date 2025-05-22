package com.javation.coloringbook.Controller;

import com.javation.coloringbook.Entity.Users;
import com.javation.coloringbook.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/user/id/{userId}")
    public ResponseEntity<Users> getUserById (@PathVariable long userId){
        Users user = userService.findUserById(userId);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/user/email/{userEmail}")
    public ResponseEntity<Users> getUserByEmail(@PathVariable String userEmail){
        Users user = userService.findUserByEmail(userEmail);
        return ResponseEntity.ok(user);
    }
}
