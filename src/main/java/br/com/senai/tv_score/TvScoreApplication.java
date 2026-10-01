package br.com.senai.tv_score;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;


@SpringBootApplication
public class TvScoreApplication {
	public static void main(String[] args) {
		SpringApplication.run(TvScoreApplication.class, args);
	}

}
