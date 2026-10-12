package com.utn.atenea;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

	static final ApplicationModules modules = ApplicationModules.of(AteneaApplication.class);

	@Test
	void verificaLaEstructuraModular() {
		modules.verify();
	}

	@Test
	void imprimeLosModulos() {
		modules.forEach(System.out::println);
	}
}
