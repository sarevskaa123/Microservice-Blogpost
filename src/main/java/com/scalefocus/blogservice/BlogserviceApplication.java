package com.scalefocus.blogservice;

import com.scalefocus.blogservice.config.ApplicationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ApplicationProperties.class)
public class BlogserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BlogserviceApplication.class, args);
	}

}
