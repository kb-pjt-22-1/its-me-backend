package site.benepay.domain.notification.dto;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LocationUpdateRequestDto {

	@NotNull
	private Double latitude;

	@NotNull
	private Double longitude;
}
