package com.example.service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.data.annotation.Id
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.rsocket.RSocketSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.Instant
import java.util.stream.Stream

@SpringBootApplication
class ServiceApplication {

	@Bean
	fun routes(rr: ReservationRepository) = router {
		GET("/reservations") {
			ServerResponse.ok().body(rr.findAll())
		}
	}

	private fun user(user: String) = User.withDefaultPasswordEncoder().username(user).password("pw").roles("USER").build()

/*
	@Bean
	fun authentication() = MapReactiveUserDetailsService(user("jlong"), user("rwinch"))
*/
/*

	@Bean
	fun rsocketAuthorization(rs: RSocketSecurity) = rs
			.simpleAuthentication(Customizer.withDefaults())
			.authorizePayload { auth ->
				auth
						.route("greetings").authenticated()
						.anyRequest().permitAll()
			}
			.build()

	@Bean
	fun httpAuthorization(http: ServerHttpSecurity) =
			http
					.httpBasic(Customizer.withDefaults())
					.authorizeExchange { auth ->
						auth
								.pathMatchers("/reservations").authenticated()
								.anyExchange().permitAll()
					}
					.build()
*/

	@Bean
	fun initializer(rr: ReservationRepository) = ApplicationListener<ApplicationReadyEvent> {

		val write = Flux
				.just("Dr. Syer", "Cornelia", "Madhura", "Violetta", "Stéphane", "Ria", "Josh")
				.map { Reservation(null, it) }
				.flatMap { rr.save(it) }

		rr
				.deleteAll()
				.thenMany(write)
				.thenMany(rr.findAll())
				.subscribe { println("wrote $it") }
	}
}

data class GreetingRequest(val name: String)
data class GreetingResponse(val message: String)

@Controller
class GreetingService {

	@MessageMapping("greetings")
	fun greet() = ReactiveSecurityContextHolder.getContext().map { it.authentication.name }.map { GreetingRequest(it) }.flatMapMany { greet(it) }


	fun greet(request: GreetingRequest) = Flux
			.fromStream(Stream.generate {
				GreetingResponse("Hello ${request.name} @  ${Instant.now()}!")
			})
			.delayElements(Duration.ofSeconds(1))
}

fun main(args: Array<String>) {
	runApplication<ServiceApplication>(*args)
}


interface ReservationRepository : ReactiveCrudRepository<Reservation, Int>

data class Reservation(@Id val id: Int?, val name: String)