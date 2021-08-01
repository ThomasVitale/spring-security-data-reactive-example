package com.thomasvitale.springsecuritydatareactiveexample;

import reactor.core.publisher.Mono;

import org.springframework.data.spel.spi.EvaluationContextExtension;
import org.springframework.data.spel.spi.ReactiveEvaluationContextExtension;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.data.repository.query.SecurityEvaluationContextExtension;

public class ReactiveSecurityEvaluationContextExtension implements ReactiveEvaluationContextExtension {

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

}
