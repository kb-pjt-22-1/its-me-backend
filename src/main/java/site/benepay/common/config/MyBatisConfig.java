package site.benepay.common.config;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Configuration
@MapperScan(basePackages = {
	"site.benepay.domain.user.mapper",
	"site.benepay.domain.card.mapper",

	"site.benepay.domain.merchant.mapper",
	"site.benepay.common.crypto.mapper",
	"site.benepay.domain.recommendation.mapper"

})
public class MyBatisConfig {

	@Bean
	public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
		SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
		factoryBean.setDataSource(dataSource);
		factoryBean.setMapperLocations(
			new PathMatchingResourcePatternResolver().getResources("classpath:mybatis/mapper/*.xml"));

		factoryBean.setTypeAliasesPackage("site.benepay.domain.user.vo,site.benepay.domain.recommendation.model");

		// snake_case → camelCase 자동 매핑 설정
		org.apache.ibatis.session.Configuration mybatisConfiguration =
			new org.apache.ibatis.session.Configuration();

		mybatisConfiguration.setMapUnderscoreToCamelCase(true);

		factoryBean.setConfiguration(mybatisConfiguration);

		return factoryBean.getObject();
	}

	@Bean
	public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
		return new SqlSessionTemplate(sqlSessionFactory);
	}
}
