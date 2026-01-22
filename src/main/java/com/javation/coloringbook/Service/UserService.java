package com.javation.coloringbook.Service;

import com.javation.coloringbook.Entity.Users;
import com.javation.coloringbook.Repository.UsersRepository;
import com.javation.coloringbook.exceptions.DuplicateUserException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional // Realiza o Rollback caso aja algum erro
public class UserService {
    private final UsersRepository usersRepository;

    public Users findUserById(Long id){
        return usersRepository.findById(id).orElseThrow(()-> new IllegalArgumentException("Usuario não encontrado"));
    }

    public Users findUserByEmail(String email){
        return usersRepository.findByEmail(email).orElseThrow(()-> new IllegalArgumentException("Email não encontrado!"));
    }

    public Users createUser(Users user){
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        user.setPassowdHash(encoder.encode(user.getPassword()));

        try {
            return usersRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateUserException();
        }
    }
}
