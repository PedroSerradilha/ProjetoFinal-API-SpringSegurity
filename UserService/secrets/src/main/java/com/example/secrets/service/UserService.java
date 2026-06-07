package com.example.secrets.service;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.secrets.dto.CreateUserDto;
import com.example.secrets.dto.EmailDto;
import com.example.secrets.dto.LoginUserDto;
import com.example.secrets.dto.RecoveryJwtTokenDto;
import com.example.secrets.entity.Role;
import com.example.secrets.entity.User;
import com.example.secrets.producer.UserProducer;
import com.example.secrets.repository.UserRepository;
import com.example.secrets.security.config.SecurityConfiguration;
import com.example.secrets.security.service.JwtTokenService;
import com.example.secrets.security.service.UserDetailsImpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class UserService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityConfiguration securityConfiguration;

    @Autowired
    private CodigoCacheService codigoCacheService;

    @Autowired
    private UserProducer userProducer;

    @PersistenceContext
    private EntityManager entityManager; // Usado para buscar a Role existente de forma limpa

    public RecoveryJwtTokenDto authenticateUser(LoginUserDto loginUserDto) {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                new UsernamePasswordAuthenticationToken(loginUserDto.email(), loginUserDto.password());

        Authentication authentication = authenticationManager.authenticate(usernamePasswordAuthenticationToken);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        return new RecoveryJwtTokenDto(jwtTokenService.generateToken(userDetails));
    }

    public void createUser(CreateUserDto createUserDto) {
        User newUser = User.builder()
                .email(createUserDto.email())
                .password(securityConfiguration.passwordEncoder().encode(createUserDto.password()))
                .roles(List.of(Role.builder().name(createUserDto.role()).build()))
                .build();

        userRepository.save(newUser);
    }

    // MÉTODO AJUSTADO PARA SEGUIR O ROTEIRO COMPLETAMENTE À RISCA
    @Transactional
    public void solicitarCodigoAcesso(String email) {
        // 1. Busca o usuário ou cria um temporário caso não exista (com ROLE_CUSTOMER)
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            
            // Busca a ROLE_CUSTOMER existente no banco para evitar o erro de duplicação
            Role customerRole;
            try {
                customerRole = entityManager.createQuery(
                        "SELECT r FROM Role r WHERE r.name = :name", Role.class)
                        .setParameter("name", com.example.secrets.enums.RoleName.ROLE_CUSTOMER)
                        .getSingleResult();
            } catch (Exception e) {
                // Caso seja a primeira vez rodando o sistema e não exista no banco, ele cria
                customerRole = Role.builder()
                        .name(com.example.secrets.enums.RoleName.ROLE_CUSTOMER)
                        .build();
            }

            User tempUser = User.builder()
                    .email(email)
                    .password(securityConfiguration.passwordEncoder().encode(UUID.randomUUID().toString()))
                    .roles(List.of(customerRole)) // Adiciona a role ROLE_CUSTOMER exigida!
                    .build();
            return userRepository.save(tempUser);
        });

        // 2. Gera um código aleatório de 6 dígitos
        String codigo = String.format("%06d", new Random().nextInt(1000000));

        // 3. Salva no Cache em memória por 5 minutos
        codigoCacheService.salvarCodigo(email, codigo);

        // 4. Converte o ID Long para UUID fictício para cumprir a exigência do EmailDto
        // Usamos o ID numérico para gerar um UUID constante (ex: ID 1 vira 00000000-0000-0000-0000-000000000001)
        UUID uuidFormatado = new UUID(0L, user.getId());

        // 5. Monta o DTO exatamente com a estrutura pedida (userId como UUID)
        EmailDto emailDto = new EmailDto(
                uuidFormatado, // Enviando o UUID exigido pelo roteiro!
                email,
                "Seu código de acesso",
                "Olá! Use o seguinte código de verificação para acessar o sistema: " + codigo
        );

        // 6. Publica a mensagem na fila do RabbitMQ
        userProducer.publishEmailMessage(emailDto);
    }
}