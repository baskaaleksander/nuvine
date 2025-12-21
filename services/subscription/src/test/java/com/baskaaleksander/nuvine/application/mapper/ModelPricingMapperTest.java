package com.baskaaleksander.nuvine.application.mapper;

import com.baskaaleksander.nuvine.TestFixtures;
import com.baskaaleksander.nuvine.application.dto.ModelPricingResponse;
import com.baskaaleksander.nuvine.domain.model.LlmModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ModelPricingMapper")
class ModelPricingMapperTest {

    private ModelPricingMapper modelPricingMapper;

    @BeforeEach
    void setUp() {
        modelPricingMapper = new ModelPricingMapper();
    }

    @Test
    @DisplayName("toResponse maps all fields correctly for paid model")
    void toResponse_mapsAllFields_forPaidModel() {
        LlmModel model = TestFixtures.llmModel().build();

        ModelPricingResponse response = modelPricingMapper.toResponse(model);

        assertThat(response.provider()).isEqualTo(model.getProvider().getProviderKey());
        assertThat(response.model()).isEqualTo(model.getModelKey());
        assertThat(response.displayName()).isEqualTo(model.getDisplayName());
        assertThat(response.pricing()).isNotNull();
        assertThat(response.pricing().getInputPricePer1MTokens())
                .isEqualTo(model.getPricing().getInputPricePer1MTokens());
        assertThat(response.pricing().getOutputPricePer1MTokens())
                .isEqualTo(model.getPricing().getOutputPricePer1MTokens());
        assertThat(response.pricing().getCurrency())
                .isEqualTo(model.getPricing().getCurrency());
    }

    @Test
    @DisplayName("toResponse maps all fields correctly for free model")
    void toResponse_mapsAllFields_forFreeModel() {
        LlmModel model = TestFixtures.freeModel().build();

        ModelPricingResponse response = modelPricingMapper.toResponse(model);

        assertThat(response.provider()).isEqualTo(model.getProvider().getProviderKey());
        assertThat(response.model()).isEqualTo(model.getModelKey());
        assertThat(response.displayName()).isEqualTo(model.getDisplayName());
        assertThat(response.pricing()).isNotNull();
        assertThat(response.pricing().getInputPricePer1MTokens())
                .isEqualTo(model.getPricing().getInputPricePer1MTokens());
        assertThat(response.pricing().getOutputPricePer1MTokens())
                .isEqualTo(model.getPricing().getOutputPricePer1MTokens());
    }
}
