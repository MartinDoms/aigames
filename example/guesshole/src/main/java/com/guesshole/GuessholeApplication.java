package com.guesshole;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
@EnableR2dbcRepositories
public class GuessholeApplication {

	private static final Logger log = LoggerFactory.getLogger(GuessholeApplication.class);

	public static void main(String[] args) {
		log.info("Starting...");
		
		new SpringApplicationBuilder(GuessholeApplication.class)
						.run(args);
	}
}
