package com.example.secrets.email;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/emails")
public class EmailRestController {

    @PostMapping("/send")
    public ResponseEntity<String> sendEmailFake() {
        // Estrutura base mockada para futura integração de envio de e-mails
        return new ResponseEntity<>("Email Service: Simulação de envio executada com sucesso.", HttpStatus.OK);
    }
}
