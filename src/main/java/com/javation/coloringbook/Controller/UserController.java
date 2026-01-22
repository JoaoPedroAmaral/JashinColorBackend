package com.javation.coloringbook.Controller;

import com.javation.coloringbook.DTO.response.UserResponseDTO;
import com.javation.coloringbook.Entity.Users;
import com.javation.coloringbook.Service.UserService;
import com.javation.coloringbook.exceptions.BusinessRuleException;
import com.javation.coloringbook.exceptions.JsonMalformedException;
import lombok.RequiredArgsConstructor;
import org.cloudinary.json.JSONException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponseDTO> postUser (@RequestBody Users body){
        String email = body.getEmail();
        String password = body.getPassword();
        String regexEmail = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

        if (email == null || password == null) {
            throw new JsonMalformedException("email e password necessarios");
        } else if (email.isBlank() || password.isBlank()) {
            throw new BusinessRuleException("Não pode haver valores vazios");
        } else if (!email.matches(regexEmail)) {
            throw new BusinessRuleException("Email informado não é válido");
        }

        Users user = userService.createUser(body);

        return ResponseEntity.status(HttpStatus.CREATED).body(new UserResponseDTO(user));
    }

    @GetMapping("/id/{userId}")
    public ResponseEntity<UserResponseDTO> getUserById (@PathVariable long userId){
        Users user = userService.findUserById(userId);

        return ResponseEntity.ok(new UserResponseDTO(user));
    }

    @GetMapping("/email/{userEmail}")
    public ResponseEntity<UserResponseDTO> getUserByEmail(@PathVariable String userEmail){
        Users user = userService.findUserByEmail(userEmail);

        return ResponseEntity.ok(new UserResponseDTO(user));
    }

}
