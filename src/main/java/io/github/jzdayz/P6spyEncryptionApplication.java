package io.github.jzdayz;

import io.github.jzdayz.bean.Test;
import io.github.jzdayz.jdbc.CustomDataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;

/**
 *  1.针对插入修改和删除，需要解析sql，应为不会提供resultSet，然后针对prepareStatement的各种set方法去处理
 *  2.针对查询，需要处理statement和prepareStatement的resultSet的各种get方法，然后根据ResultSet的ResultSetMetaData处理当前字段
 */
@SpringBootApplication
public class P6spyEncryptionApplication {

	public static void main(String[] args) {
		try (
				ConfigurableApplicationContext context = SpringApplication.run(P6spyEncryptionApplication.class, args)
				){
			DataSource ds = context.getBean(DataSource.class);
			JdbcTemplate jdbcTemplate = new JdbcTemplate(new CustomDataSource(ds));
			List<Test> list = jdbcTemplate.query("select * from test where id = ?", new BeanPropertyRowMapper<>(Test.class), 1);
			System.out.println(list);
		}

	}


}
