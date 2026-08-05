package site.benepay.domain.merchant.vo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MerchantBrandTest {

    private MerchantBrand.MerchantBrandBuilder validBuilder() {
        return MerchantBrand.builder()
                .brandId(1L)
                .brandCode("B001")
                .brandName("브랜드A")
                .brandLogo("logo-url");
    }

    @Test
    void builderCreatesMerchantBrandWithAllFields() {
        MerchantBrand brand = validBuilder().build();

        assertThat(brand.getBrandId()).isEqualTo(1L);
        assertThat(brand.getBrandCode()).isEqualTo("B001");
        assertThat(brand.getBrandName()).isEqualTo("브랜드A");
        assertThat(brand.getBrandLogo()).isEqualTo("logo-url");
    }

    @Test
    void builderAllowsNullBrandIdAndBrandLogo() {
        MerchantBrand brand = validBuilder().brandId(null).brandLogo(null).build();

        assertThat(brand.getBrandId()).isNull();
        assertThat(brand.getBrandLogo()).isNull();
    }

    @Test
    void builderRejectsNullBrandCode() {
        assertThatThrownBy(() -> validBuilder().brandCode(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void builderRejectsNullBrandName() {
        assertThatThrownBy(() -> validBuilder().brandName(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void setBrandLogoUpdatesValue() {
        MerchantBrand brand = validBuilder().build();

        brand.setBrandLogo("new-logo-url");

        assertThat(brand.getBrandLogo()).isEqualTo("new-logo-url");
    }
}
