package cl.duoc.mesatech.lanzador.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Reciclado del SecurityConfig de la EP1 (MSProyect), ahora con Entra ID SIEMPRE habilitado.
 * - Todo /api/** exige un access token válido de Entra ID con el scope access_as_user (si no: 401).
 * - Disparar solicitudes exige además el App Role configurado, por defecto Admin (si no: 403).
 * Sin CORS: React llama a este backend a través del proxy de desarrollo (mismo origen).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.csrf(csrf -> csrf.disable())
			.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/public/**").permitAll()
				.requestMatchers("/api/**").hasAuthority("SCOPE_access_as_user")
				.anyRequest().authenticated())
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(entraIdConverter())));
		return http.build();
	}

	/** scp -> SCOPE_xxx y roles -> ROLE_xxx (App Roles de Entra ID). */
	private Converter<Jwt, AbstractAuthenticationToken> entraIdConverter() {
		JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
		return jwt -> {
			Collection<GrantedAuthority> authorities = new ArrayList<>(scopes.convert(jwt));
			List<String> roles = jwt.getClaimAsStringList("roles");
			if (roles != null) {
				roles.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
			}
			String nombre = jwt.getClaimAsString("preferred_username");
			return new JwtAuthenticationToken(jwt, authorities, nombre != null ? nombre : jwt.getSubject());
		};
	}
}
