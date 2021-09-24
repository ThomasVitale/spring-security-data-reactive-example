package com.thomasvitale.springsecuritydatareactiveexample;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.spel.spi.EvaluationContextExtension;
import org.springframework.data.spel.spi.ReactiveEvaluationContextExtension;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.data.repository.query.SecurityEvaluationContextExtension;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class SpringSecurityDataReactiveExampleApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringSecurityDataReactiveExampleApplication.class, args);
	}

	@Bean
	ReactiveEvaluationContextExtension securityExtension(){
		return new ReactiveEvaluationContextExtension() {

			@Override
			public String getExtensionId() {
				return "security";
			}

			@Override
			public Mono<? extends EvaluationContextExtension> getExtension() {
				return ReactiveSecurityContextHolder.getContext()
						.map(SecurityContext::getAuthentication)
						.map(SecurityEvaluationContextExtension::new);
			}
		};
	}

	@Bean
	SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
		return http
				.csrf().disable()
				.authorizeExchange(authorize -> authorize.anyExchange().authenticated())
				.httpBasic(Customizer.withDefaults())
				.build();
	}

	@Bean
	ReactiveUserDetailsService userDetailsService() {
		var isabelle = User.withUsername("isabelle")
				.password("password")
				.authorities("user")
				.build();

		var bjorn = User.withUsername("bjorn")
				.password("password")
				.authorities("user")
				.build();

		return new MapReactiveUserDetailsService(isabelle, bjorn);
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return NoOpPasswordEncoder.getInstance();
	}
}

@RestController
@RequestMapping("/books")
class BookController {
	private final BookRepository bookRepository;

	BookController(BookRepository bookRepository) {
		this.bookRepository = bookRepository;
	}

	@GetMapping
	public Flux<Book> getBooksByCreatedUser() {
		return bookRepository.findBooksForCurrentUser();
	}

	@GetMapping("/test")
	public Flux<Book> getBooksByCreatedUserTest() {
		return bookRepository.findBooksForUser(null);
	}

	@PostMapping
	public Mono<Book> addBook(@RequestBody Book book) {
		return bookRepository.save(book);
	}
}

@Configuration
@EnableR2dbcAuditing
class DataConfig {
	@Bean
	ReactiveAuditorAware<String> auditorAware() {
		return () -> ReactiveSecurityContextHolder.getContext()
				.map(SecurityContext::getAuthentication)
				.filter(Authentication::isAuthenticated)
				.map(Authentication::getName);
	}
}

interface BookRepository extends ReactiveCrudRepository<Book,Long> {
	@Query("select * from Book b where b.user = ?#{authentication.name}")
	Flux<Book> findBooksForCurrentUser();

	@Query("{ 'user': ?#{authentication.name} }")
	Flux<Book> findBooksForUser(String user);
}

record Book (
	@Id
	Long id,
	String name,
	@CreatedBy
	String user
){}
