package com.example.secrets.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.secrets.dto.CreateUserDto;
import com.example.secrets.dto.LoginUserDto;
import com.example.secrets.dto.RecoveryJwtTokenDto;
import com.example.secrets.service.UserService;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<RecoveryJwtTokenDto> authenticateUser(@RequestBody LoginUserDto loginUserDto) {
        RecoveryJwtTokenDto token = userService.authenticateUser(loginUserDto);
        return new ResponseEntity<>(token, HttpStatus.OK);
    }

    // Solicitação de código OTP (Etapa 2)
    @PostMapping("/auth/request-code")
    public ResponseEntity<Void> requestCode(@RequestBody java.util.Map<String, String> request) {
        String email = request.get("email");
        userService.solicitarCodigoAcesso(email);
        return ResponseEntity.ok().build(); // Retorna 200 OK sem falar o código no JSON por segurança
    }

    @PostMapping
    public ResponseEntity<Void> createUser(@RequestBody CreateUserDto createUserDto) {
        userService.createUser(createUserDto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/test/customer")
    public ResponseEntity<String> testCustomer() {
        return new ResponseEntity<>("Acesso permitido: Perfil CUSTOMER.", HttpStatus.OK);
    }

    @GetMapping("/test/administrator")
    public ResponseEntity<String> testAdministrator() {
        return new ResponseEntity<>("Acesso permitido: Perfil ADMINISTRATOR.", HttpStatus.OK);
    }
}