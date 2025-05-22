package com.javation.coloringbook.Service;

import com.javation.coloringbook.Entity.Users;
import com.javation.coloringbook.Repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UsersRepository usersRepository;

    public Users findUserById(Long id){
        return usersRepository.findById(id).orElseThrow(()-> new IllegalArgumentException("Usuario não encontrado"));
    }

    public Users findUserByEmail(String email){
        return usersRepository.findByEmail(email).orElseThrow(()-> new IllegalArgumentException("Email não encontrado!"));
    }

    public Users createUser(Users user){
        return usersRepository.save(user);
    }
}
