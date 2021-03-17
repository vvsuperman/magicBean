package com.furiousTidy.magicbean;

import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.retry.annotation.EnableRetry;


@SpringBootApplication
@EnableRetry
//@MapperScan("com.furiousTidy.magicbean.dbutil.mapper.*")
public class MagicbeanApplication {
	private static final Logger logger = LoggerFactory.getLogger(MagicbeanApplication.class);

	public static void main(String[] args) {

		SpringApplication.run(MagicbeanApplication.class, args);
		logger.info("magicbean start success");
	}
}
