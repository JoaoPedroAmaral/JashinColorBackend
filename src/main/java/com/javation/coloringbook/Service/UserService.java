package com.javation.coloringbook.Service;

import com.javation.coloringbook.Exception.UserNotFoundException;
import com.javation.coloringbook.Entity.Users;
import com.javation.coloringbook.Repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UsersRepository usersRepository;

    @Transactional(readOnly = true)
    public Users findUserById(Long id) {
        log.debug("Finding user by ID: {}", id);
        return usersRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Users findUserByEmail(String email) {
        log.debug("Finding user by email: {}", email);
        return usersRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Email não encontrado: " + email));
    }

    @Transactional
    public Users createUser(Users user) {
        log.info("Creating new user with email: {}", user.getEmail());
        
        if (usersRepository.findByEmail(user.getEmail()).isPresent()) {
            log.warn("Attempt to create duplicate user: {}", user.getEmail());
            throw new IllegalArgumentException("Email já está em uso");
        }
        
        user.setCreateAt(LocalDateTime.now());
        Users savedUser = usersRepository.save(user);
        log.info("User created successfully with ID: {}", savedUser.getId());
        return savedUser;
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return usersRepository.findByEmail(email).isPresent();
    }
}
