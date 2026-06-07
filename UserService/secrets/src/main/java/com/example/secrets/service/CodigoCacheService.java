package com.example.secrets.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class CodigoCacheService {

    // Registro interno para armazenar o código e o tempo de expiração
    private record CacheEntry(String codigo, LocalDateTime expiracao) {}

    // ConcurrentHashMap recomendado para o cache em memória
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    // Armazena o código associado ao e-mail com validade de 5 minutos
    public void salvarCodigo(String email, String codigo) {
        cache.put(email, new CacheEntry(codigo, LocalDateTime.now().plusMinutes(5)));
    }

    // Recupera o código se ainda estiver válido
    public String obterCodigo(String email) {
        CacheEntry entry = cache.get(email);
        if (entry != null && LocalDateTime.now().isBefore(entry.expiracao())) {
            return entry.codigo();
        }
        if (entry != null) {
            cache.remove(email); // Remove se já expirou
        }
        return null;
    }

    // @Scheduled roda a cada 1 minuto (60000ms) para limpar códigos expirados da memória
    @Scheduled(fixedRate = 60000)
    public void limparCacheExpirado() {
        cache.forEach((email, entry) -> {
            if (LocalDateTime.now().isAfter(entry.expiracao())) {
                cache.remove(email);
            }
        });
    }
}