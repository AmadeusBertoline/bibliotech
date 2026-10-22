package com.bibliotech.bibliotech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EnableSpringDataWebSupport 
public class BibliotechApplication {

	public static void main(String[] args) {
		SpringApplication.run(BibliotechApplication.class, args);
	}

}
