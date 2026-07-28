package site.benepay.domain.recommendation.dto.request;

public class NearbyBenefitStoreRequestDto {

    private Double latitude;
    private Double longitude;
    private Integer radiusMeters = 1000;
    private String categoryCode;
    private RecommendationSort sort = RecommendationSort.DISTANCE;
    private Integer limit = 20;

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Integer getRadiusMeters() {
        return radiusMeters;
    }

    public void setRadiusMeters(Integer radiusMeters) {
        this.radiusMeters = radiusMeters;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public RecommendationSort getSort() {
        return sort;
    }

    public void setSort(RecommendationSort sort) {
        this.sort = sort;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}
