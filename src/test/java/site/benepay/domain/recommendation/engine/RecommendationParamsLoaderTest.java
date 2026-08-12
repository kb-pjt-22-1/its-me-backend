package site.benepay.domain.recommendation.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

class RecommendationParamsLoaderTest {

	private static void invokeLoad(RecommendationParamsLoader loader) throws ReflectiveOperationException {
		Method load = RecommendationParamsLoader.class.getDeclaredMethod("load");
		load.setAccessible(true);
		try {
			load.invoke(loader);
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			throw e;
		}
	}

	@Test
	void loadReadsTheBundledParamsFileFromClasspath() throws Exception {
		RecommendationParamsLoader loader = new RecommendationParamsLoader(new ObjectMapper());

		invokeLoad(loader);

		assertThat(loader.params()).isNotNull();
		assertThat(loader.params().typicalPaymentAmount()).isNotEmpty();
	}

	@Test
	void loadWrapsReadFailureInIllegalStateException() throws Exception {
		ObjectMapper objectMapper = mock(ObjectMapper.class);
		ObjectReader reader = mock(ObjectReader.class);
		when(objectMapper.reader()).thenReturn(reader);
		when(reader.without(any(DeserializationFeature.class))).thenReturn(reader);
		when(reader.readValue(any(InputStream.class), eq(RecommendationParams.class)))
			.thenThrow(new IOException("boom"));

		RecommendationParamsLoader loader = new RecommendationParamsLoader(objectMapper);

		assertThatThrownBy(() -> invokeLoad(loader))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("recommendation-params.json")
			.hasCauseInstanceOf(IOException.class);
	}
}
