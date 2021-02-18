package com.furiousTidy.magicbean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;


@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class MagicbeanApplication {
	private static final Logger logger = LoggerFactory.getLogger(MagicbeanApplication.class);

	public static void main(String[] args) {

		SpringApplication.run(MagicbeanApplication.class, args);
		logger.info("magicbean start success");
	}

}
