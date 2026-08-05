package site.benepay.domain.merchant.vo;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MerchantTest {

    private Merchant.MerchantBuilder validBuilder() {
        return Merchant.builder()
                .merchantId(1L)
                .categoryCode("5812")
                .brandId(1L)
                .merchantCode("M001")
                .merchantName("테스트 식당")
                .address("서울시 강남구")
                .latitude(BigDecimal.valueOf(37.5))
                .longitude(BigDecimal.valueOf(127.0))
                .phone("02-000-0000");
    }

    @Test
    void builderCreatesMerchantWithAllFields() {
        Merchant merchant = validBuilder().build();

        assertThat(merchant.getMerchantId()).isEqualTo(1L);
        assertThat(merchant.getCategoryCode()).isEqualTo("5812");
        assertThat(merchant.getBrandId()).isEqualTo(1L);
        assertThat(merchant.getMerchantCode()).isEqualTo("M001");
        assertThat(merchant.getMerchantName()).isEqualTo("테스트 식당");
        assertThat(merchant.getAddress()).isEqualTo("서울시 강남구");
        assertThat(merchant.getLatitude()).isEqualByComparingTo(BigDecimal.valueOf(37.5));
        assertThat(merchant.getLongitude()).isEqualByComparingTo(BigDecimal.valueOf(127.0));
        assertThat(merchant.getPhone()).isEqualTo("02-000-0000");
    }

    @Test
    void builderAllowsNullMerchantIdAndPhone() {
        Merchant merchant = validBuilder().merchantId(null).phone(null).build();

        assertThat(merchant.getMerchantId()).isNull();
        assertThat(merchant.getPhone()).isNull();
    }

    @Test
    void builderRejectsNullCategoryCode() {
        assertThatThrownBy(() -> validBuilder().categoryCode(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void builderRejectsNullBrandId() {
        assertThatThrownBy(() -> validBuilder().brandId(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void builderRejectsNullMerchantCode() {
        assertThatThrownBy(() -> validBuilder().merchantCode(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void builderRejectsNullMerchantName() {
        assertThatThrownBy(() -> validBuilder().merchantName(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void builderRejectsNullAddress() {
        assertThatThrownBy(() -> validBuilder().address(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void builderRejectsNullLatitude() {
        assertThatThrownBy(() -> validBuilder().latitude(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void builderRejectsNullLongitude() {
        assertThatThrownBy(() -> validBuilder().longitude(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void setAddressRejectsNull() {
        Merchant merchant = validBuilder().build();

        assertThatThrownBy(() -> merchant.setAddress(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void setAddressUpdatesValue() {
        Merchant merchant = validBuilder().build();

        merchant.setAddress("서울시 서초구");

        assertThat(merchant.getAddress()).isEqualTo("서울시 서초구");
    }

    @Test
    void setPhoneAllowsNull() {
        Merchant merchant = validBuilder().build();

        merchant.setPhone(null);

        assertThat(merchant.getPhone()).isNull();
    }
}
