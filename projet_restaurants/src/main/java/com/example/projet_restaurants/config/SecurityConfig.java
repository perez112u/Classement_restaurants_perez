package com.example.projet_restaurants.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Cette classe lit la liste de rôles du JWT et la convertit en authorities Spring.
     * (C'est juste une passerelle entre le format Keycloak et le format attendu par Spring.)
     */
    static class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            String clientId = jwt.getClaimAsString("azp"); // => "coursm2"

            // Récupère l'objet "resource_access" dans le JWT (la ou il y a les roles)
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (clientId == null) return List.of();

            Object clientBlock = resourceAccess.get(clientId);
            if (!(clientBlock instanceof Map<?, ?> cb)) return List.of();

            // Lis la liste des rôles ["USER","ADMIN"]
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) ((Map<String, Object>) cb).getOrDefault("roles", List.of());

            // Transforme chaque rôle en "ROLE_<ROLE>" pour Spring (ex: "ADMIN" -> "ROLE_ADMIN")
            return roles.stream()
                    .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 2) CONFIGURATEUR JWT
     *
     * - On branche notre convertisseur de rôles ci-dessus pour que les @PreAuthorize(hasRole(...))
     *   fonctionnent.
     * - On choisit aussi quel "nom" sera renvoyé par authentication.getName().
     *   Par défaut, Spring utiliserait "sub" (un id opaque). Ici, on préfère "preferred_username".
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        // -> ajoute les authorities "ROLE_USER", "ROLE_ADMIN" à partir du JWT de Keycloak
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());

        // permet d'avoir un nom plus humain que le sub
        converter.setPrincipalClaimName("preferred_username");

        return converter;
    }

    /** Sécurité HTTP : Swagger en libre accès, tout le reste nécessite un JWT. */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // route publique pour le swagger : (generation du yaml)
                        .requestMatchers(
                                "/v3/api-docs.yaml"
                        ).permitAll()
                        // tout est public (sauf méthodes avec @PreAuthorize)
                        .anyRequest().permitAll()
                )
                // roles, et nom d'utilisateur definis via  notre converter
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                // pour que @preAuthorize soit activé
                .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
