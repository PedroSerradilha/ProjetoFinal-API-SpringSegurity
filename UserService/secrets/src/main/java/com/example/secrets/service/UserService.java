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

    @Transactional
    public void createUser(CreateUserDto createUserDto) {
        // 1. Define a role padrão usando o seu Enum existente
        com.example.secrets.enums.RoleName rolePadrao = com.example.secrets.enums.RoleName.ROLE_CUSTOMER;
        
        // 2. Tenta buscar a Role correspondente no banco para evitar duplicações
        Role finalRole;
        try {
            finalRole = entityManager.createQuery(
                    "SELECT r FROM Role r WHERE r.name = :name", Role.class)
                    .setParameter("name", rolePadrao)
                    .getSingleResult();
        } catch (Exception e) {
            finalRole = Role.builder()
                    .name(rolePadrao)
                    .build();
        }

        // 3. Monta e salva o usuário no banco
        User newUser = User.builder()
                .email(createUserDto.email())
                .password(securityConfiguration.passwordEncoder().encode(createUserDto.password()))
                .roles(List.of(finalRole)) 
                .build();

        User userSalvo = userRepository.save(newUser); // Guarda o usuário salvo com o ID gerado

        try {
            // Cria o UUID com base no ID do usuário para bater com o formato do DTO
            UUID userIdUuid = new UUID(0L, userSalvo.getId());

            // Monta o DTO com a mensagem de boas-vindas do cadastro
            com.example.secrets.dto.EmailDto emailDto = new com.example.secrets.dto.EmailDto(
                    userIdUuid,
                    userSalvo.getEmail(),
                    "Cadastro Realizado com Sucesso!",
                    "Olá! Seja muito bem-vindo ao sistema. Sua conta foi criada com sucesso!"
            );

            // Publica no CloudAMQP para o EmailService capturar do outro lado
            userProducer.publishEmailMessage(emailDto);
            System.out.println("Mensagem de boas-vindas enviada para a fila com sucesso!");
        } catch (Exception e) {
            System.err.println("Falha ao enviar mensagem para a fila do RabbitMQ: " + e.getMessage());
        }
    }

    // MÉTODO SOLICITAR CÓDIGO (Mantido idêntico ao seu)
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

    public boolean validarCodigoCache(String email, String codigoDigitado) {
        String codigoCorreto = codigoCacheService.obterCodigo(email);
        
        if (codigoCorreto == null) {
            return false; // Código expirou ou não existe
        }
        
        return codigoCorreto.equals(codigoDigitado);
    }

    @Transactional
    public User updateProfile(String email, com.example.secrets.dto.UpdateProfileDto dto) {
        // 1. Busca o usuário pelo e-mail
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado com o e-mail: " + email));

        // 2. Atualiza o nome (Conforme requisito do campo 'name' na entidade)
        user.setName(dto.getName());

        // 3. Converte a String recebida do Frontend ("ROLE_CUSTOMER" ou "ROLE_ADMINISTRATOR") para o seu Enum real
        com.example.secrets.enums.RoleName roleEnum = com.example.secrets.enums.RoleName.valueOf(dto.getRole());

        // 4. Busca a Role correspondente no banco de dados usando a mesma estrutura limpa que você já usa no createUser
        Role finalRole;
        try {
            finalRole = entityManager.createQuery(
                    "SELECT r FROM Role r WHERE r.name = :name", Role.class)
                    .setParameter("name", roleEnum)
                    .getSingleResult();
        } catch (Exception e) {
            // Caso a role ainda não exista fisicamente na tabela do banco, cria dinamicamente
            finalRole = Role.builder()
                    .name(roleEnum)
                    .build();
            entityManager.persist(finalRole);
        }

        // 5. Substitui as roles antigas pela nova selecionada (critério de uma única role por usuário)
        user.getRoles().clear();
        user.getRoles().add(finalRole);

        // 6. Salva as mudanças e retorna o objeto sincronizado
        return userRepository.save(user);
    }
}