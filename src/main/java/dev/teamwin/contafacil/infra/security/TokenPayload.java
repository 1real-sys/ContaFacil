package dev.teamwin.contafacil.infra.security;

public record TokenPayload(String email, Long id, String username) {
}
