package org.springframework.data.jpa.repository.support;

import static org.assertj.core.api.Assertions.*;

import jakarta.persistence.EntityManagerFactory;

import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.hibernate.dialect.PostgreSQL91Dialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.domain.sample.QUser;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = FetchableFluentQueryByPredicateIntegrationTests.Config.class)
@Transactional
public class FetchableFluentQueryByPredicateIntegrationTests {

	@Autowired FetchableFluentRepository repository;

	@BeforeEach
	void setUp() {
		repository.saveAndFlush(new User("Bilbo", "Baggins", "riddles@tolkien.com"));
	}

	@Test
	void projectionsOnDtoClassesShouldHaveAReducedProjectionInTheQuery() {

		List<UserDto> users = repository.findBy(QUser.user.firstname.eq("Bilbo"), p -> p //
				.project("firstname") //
				.as(UserDto.class) //
				.all());

		assertThat(users).extracting(UserDto::getFirstname).containsExactly("Bilbo");
	}

	@Test
	void projectionsOnEntitiesShouldHaveAReducedProjectionInTheQuery() {

		List<User> users = repository.findBy(QUser.user.firstname.eq("Bilbo"), p -> p //
				.project("firstname") //
				.all());

		assertThat(users).extracting(User::getFirstname).containsExactly("Bilbo");
	}

	public interface FetchableFluentRepository extends JpaRepository<User, Long>, QuerydslPredicateExecutor<User> {

	}

	public interface UserDto {

		String getFirstname();
	}

	@EnableJpaRepositories(considerNestedRepositories = true, basePackageClasses = FetchableFluentRepository.class, //
			includeFilters = @ComponentScan.Filter(value = { FetchableFluentRepository.class },
					type = FilterType.ASSIGNABLE_TYPE))
	@EnableTransactionManagement
	static class Config {

		@Bean(initMethod = "start", destroyMethod = "stop")
		public PostgreSQLContainer<?> postgresContainer() {

			return new PostgreSQLContainer<>("postgres:9.6.12") //
					.withUsername("postgres");
		}

		@Bean
		public DataSource dataSource(PostgreSQLContainer<?> container) {

			PGSimpleDataSource dataSource = new PGSimpleDataSource();
			dataSource.setUrl(container.getJdbcUrl());
			dataSource.setUser(container.getUsername());
			dataSource.setPassword(container.getPassword());
			return dataSource;
		}

		// @Bean(initMethod = "start", destroyMethod = "stop")
		// public MySQLContainer<?> mysqlContainer() {
		//
		// return new MySQLContainer<>("mysql:8.0.24") //
		// .withUsername("test") //
		// .withPassword("test") //
		// .withConfigurationOverride("");
		// }
		//
		// @Bean
		// public DataSource dataSource(MySQLContainer<?> container) {
		//
		// MysqlDataSource dataSource = new MysqlDataSource();
		// dataSource.setUrl(container.getJdbcUrl());
		// dataSource.setUser(container.getUsername());
		// dataSource.setPassword(container.getPassword());
		// return dataSource;
		// }

		@Bean
		public AbstractEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {

			LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
			factoryBean.setDataSource(dataSource);
			factoryBean.setPersistenceUnitRootLocation("simple-persistence");
			factoryBean.setPersistenceUnitName("spring-data-jpa");
			factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

			Properties properties = new Properties();
			properties.setProperty("hibernate.hbm2ddl.auto", "create");
			properties.setProperty("hibernate.dialect", PostgreSQL91Dialect.class.getCanonicalName());
			// properties.setProperty("hibernate.dialect", MySQL8Dialect.class.getCanonicalName());
			factoryBean.setJpaProperties(properties);

			return factoryBean;
		}

		@Bean
		PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
			return new JpaTransactionManager(entityManagerFactory);
		}
	}
}
