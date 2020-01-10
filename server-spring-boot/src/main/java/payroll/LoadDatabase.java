package payroll;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
class LoadDatabase {

	@Bean
	CommandLineRunner initDatabase(ItemRepository repository) {
		return args -> {
			log.info("Preloading " + repository.save(new Item("Bilbo Baggins", "false")));
			log.info("Preloading " + repository.save(new Item("Frodo Baggins", "false")));
		};
	}
}
