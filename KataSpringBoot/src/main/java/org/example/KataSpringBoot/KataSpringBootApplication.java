/*
 * The MIT License, Copyright (c) 2011-2025 Marcel Schneider
 * for details see License.txt
 */
package org.example.KataSpringBoot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.context.ApplicationContext;

@SpringBootApplication
@RestController
public class KataSpringBootApplication {

	private static ApplicationContext ctx;

	public static void main(String[] args) {

		ctx = SpringApplication.run(KataSpringBootApplication.class, args);
	}

	@GetMapping("/hello")
	public String hello(@RequestParam(value = "name", defaultValue = "World") String name) {

		var greetingService = ctx.getBean(GreetingService.class);
		return greetingService.greet(name);
	}
}
