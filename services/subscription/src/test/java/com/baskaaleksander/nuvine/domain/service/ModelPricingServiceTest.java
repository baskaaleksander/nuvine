package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.TestFixtures;
import com.baskaaleksander.nuvine.application.dto.ModelPricingResponse;
import com.baskaaleksander.nuvine.application.mapper.ModelPricingMapper;
import com.baskaaleksander.nuvine.domain.exception.ModelNotFoundException;
import com.baskaaleksander.nuvine.domain.model.LlmModel;
import com.baskaaleksander.nuvine.domain.model.ModelPricing;
import com.baskaaleksander.nuvine.infrastructure.persistence.LlmModelRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ModelPricingService")
class ModelPricingServiceTest {

    @Mock
    private LlmModelRepository llmModelRepository;

    @Mock
    private ModelPricingMapper modelPricingMapper;

    @InjectMocks
    private ModelPricingService modelPricingService;

    @Nested
    @DisplayName("getModelPricing")
    class GetModelPricing {

        @Test
        @DisplayName("should return pricing when model is active")
        void getModelPricing_activeModel_returnsPricing() {
            String modelKey = TestFixtures.DEFAULT_MODEL_KEY;
            String providerKey = TestFixtures.DEFAULT_PROVIDER_KEY;
            ModelPricing expectedPricing = TestFixtures.modelPricing();
            LlmModel model = TestFixtures.llmModel()
                    .pricing(expectedPricing)
                    .build();

            when(llmModelRepository.findActiveModel(eq(providerKey), eq(modelKey), any(Instant.class)))
                    .thenReturn(Optional.of(model));

            ModelPricing result = modelPricingService.getModelPricing(modelKey, providerKey);

            assertThat(result).isEqualTo(expectedPricing);
            assertThat(result.getInputPricePer1MTokens()).isEqualTo(BigDecimal.valueOf(30.00));
            assertThat(result.getOutputPricePer1MTokens()).isEqualTo(BigDecimal.valueOf(60.00));
            assertThat(result.getCurrency()).isEqualTo("USD");

            verify(llmModelRepository).findActiveModel(eq(providerKey), eq(modelKey), any(Instant.class));
        }

        @Test
        @DisplayName("should throw ModelNotFoundException when model not found")
        void getModelPricing_modelNotFound_throwsModelNotFoundException() {
            String modelKey = "non-existent-model";
            String providerKey = "unknown-provider";

            when(llmModelRepository.findActiveModel(eq(providerKey), eq(modelKey), any(Instant.class)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> modelPricingService.getModelPricing(modelKey, providerKey))
                    .isInstanceOf(ModelNotFoundException.class)
                    .hasMessageContaining("Model not found");
        }
    }

    @Nested
    @DisplayName("getAllActivePricing")
    class GetAllActivePricing {

        @Test
        @DisplayName("should return all active models pricing")
        void getAllActivePricing_returnsAllActiveModels() {
            LlmModel gpt4Model = TestFixtures.llmModel()
                    .modelKey("gpt-4")
                    .displayName("GPT-4")
                    .build();
            LlmModel gpt35Model = TestFixtures.freeModel()
                    .modelKey("gpt-3.5-turbo")
                    .displayName("GPT-3.5 Turbo")
                    .build();
            List<LlmModel> activeModels = List.of(gpt4Model, gpt35Model);

            ModelPricingResponse gpt4Response = new ModelPricingResponse(
                    "openai", "gpt-4", "GPT-4", gpt4Model.getPricing()
            );
            ModelPricingResponse gpt35Response = new ModelPricingResponse(
                    "openai", "gpt-3.5-turbo", "GPT-3.5 Turbo", gpt35Model.getPricing()
            );

            when(llmModelRepository.findAllActiveModels(any(Instant.class)))
                    .thenReturn(activeModels);
            when(modelPricingMapper.toResponse(gpt4Model)).thenReturn(gpt4Response);
            when(modelPricingMapper.toResponse(gpt35Model)).thenReturn(gpt35Response);

            List<ModelPricingResponse> results = modelPricingService.getAllActivePricing();

            assertThat(results).hasSize(2);
            assertThat(results).containsExactly(gpt4Response, gpt35Response);

            verify(llmModelRepository).findAllActiveModels(any(Instant.class));
            verify(modelPricingMapper).toResponse(gpt4Model);
            verify(modelPricingMapper).toResponse(gpt35Model);
        }

        @Test
        @DisplayName("should return empty list when no active models exist")
        void getAllActivePricing_noActiveModels_returnsEmptyList() {
            when(llmModelRepository.findAllActiveModels(any(Instant.class)))
                    .thenReturn(List.of());

            List<ModelPricingResponse> results = modelPricingService.getAllActivePricing();

            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("calculateCost")
    class CalculateCost {

        @Test
        @DisplayName("should calculate correct cost for valid token counts")
        void calculateCost_validCounts_calculatesCorrectly() {
            String provider = TestFixtures.DEFAULT_PROVIDER_KEY;
            String model = TestFixtures.DEFAULT_MODEL_KEY;
            long inputTokens = 1000L;
            long outputTokens = 500L;

            ModelPricing pricing = TestFixtures.modelPricing();
            LlmModel llmModel = TestFixtures.llmModel()
                    .pricing(pricing)
                    .build();

            when(llmModelRepository.findActiveModel(eq(provider), eq(model), any(Instant.class)))
                    .thenReturn(Optional.of(llmModel));

            BigDecimal result = modelPricingService.calculateCost(provider, model, inputTokens, outputTokens);

            BigDecimal expectedInputCost = BigDecimal.valueOf(30.00)
                    .multiply(BigDecimal.valueOf(1000))
                    .divide(BigDecimal.valueOf(1_000_000), 8, java.math.RoundingMode.HALF_UP);
            BigDecimal expectedOutputCost = BigDecimal.valueOf(60.00)
                    .multiply(BigDecimal.valueOf(500))
                    .divide(BigDecimal.valueOf(1_000_000), 8, java.math.RoundingMode.HALF_UP);
            BigDecimal expectedTotal = expectedInputCost.add(expectedOutputCost);

            assertThat(result).isEqualByComparingTo(expectedTotal);
            assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(0.06));
        }

        @Test
        @DisplayName("should return zero cost for zero tokens")
        void calculateCost_zeroTokens_returnsZero() {
            String provider = TestFixtures.DEFAULT_PROVIDER_KEY;
            String model = TestFixtures.DEFAULT_MODEL_KEY;
            long inputTokens = 0L;
            long outputTokens = 0L;

            ModelPricing pricing = TestFixtures.modelPricing();
            LlmModel llmModel = TestFixtures.llmModel()
                    .pricing(pricing)
                    .build();

            when(llmModelRepository.findActiveModel(eq(provider), eq(model), any(Instant.class)))
                    .thenReturn(Optional.of(llmModel));

            BigDecimal result = modelPricingService.calculateCost(provider, model, inputTokens, outputTokens);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should calculate cost correctly for large token counts")
        void calculateCost_largeTokenCounts_calculatesCorrectly() {
            String provider = TestFixtures.DEFAULT_PROVIDER_KEY;
            String model = TestFixtures.DEFAULT_MODEL_KEY;
            long inputTokens = 1_000_000L;
            long outputTokens = 500_000L;

            ModelPricing pricing = TestFixtures.modelPricing();
            LlmModel llmModel = TestFixtures.llmModel()
                    .pricing(pricing)
                    .build();

            when(llmModelRepository.findActiveModel(eq(provider), eq(model), any(Instant.class)))
                    .thenReturn(Optional.of(llmModel));

            BigDecimal result = modelPricingService.calculateCost(provider, model, inputTokens, outputTokens);

            assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(60.00));
        }
    }
}
