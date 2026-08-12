package site.benepay.domain.recommendation.engine;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.PostConstruct;

/**
 * recommendation-params.json(월 1회 수준으로 갱신, CsvProcessing/export_params.py 산출물)을
 * 시작 시점에 한 번 읽어 메모리에 들고 있는다. 파일을 교체하면 재배포로 반영된다 - 이 정도
 * 갱신 주기에는 핫리로드/캐싱까지는 과설계라 안 함.
 */
@Component
public class RecommendationParamsLoader {

	private static final String RESOURCE_PATH = "recommendation/recommendation-params.json";

	private final ObjectMapper objectMapper;
	private RecommendationParams params;

	public RecommendationParamsLoader(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@PostConstruct
	void load() {
		// 공유 ObjectMapper 빈을 그대로 configure()하면 다른 곳에도 영향을 주므로,
		// 이 파싱에만 unknown-property 허용을 적용한 reader를 새로 만들어 쓴다.
		try (InputStream in = new ClassPathResource(RESOURCE_PATH).getInputStream()) {
			this.params = objectMapper.reader()
				.without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				.readValue(in, RecommendationParams.class);
		} catch (IOException e) {
			throw new IllegalStateException(
				"추천 파라미터 파일(" + RESOURCE_PATH + ")을 읽지 못했습니다", e);
		}
	}

	public RecommendationParams params() {
		return params;
	}
}
