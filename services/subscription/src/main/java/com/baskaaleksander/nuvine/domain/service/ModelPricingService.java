package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.ModelPricingResponse;
import com.baskaaleksander.nuvine.application.mapper.ModelPricingMapper;
import com.baskaaleksander.nuvine.domain.exception.ModelNotFoundException;
import com.baskaaleksander.nuvine.domain.model.LlmModel;
import com.baskaaleksander.nuvine.domain.model.ModelPricing;
import com.baskaaleksander.nuvine.infrastructure.persistence.LlmModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModelPricingService {

    private final LlmModelRepository llmModelRepository;
    private final ModelPricingMapper modelPricingMapper;

    public ModelPricing getModelPricing(String modelKey, String providerKey) {
        return llmModelRepository.findActiveModel(providerKey, modelKey, Instant.now())
                .map(LlmModel::getPricing)
                .orElseThrow(() -> new ModelNotFoundException("Model not found"));
    }

    public List<ModelPricingResponse> getAllActivePricing() {
        return llmModelRepository.findAllActiveModels(Instant.now())
                .stream()
                .map(modelPricingMapper::toResponse)
                .toList();
    }

    public BigDecimal calculateCost(String provider, String model,
                                    long inputTokens, long outputTokens) {
        ModelPricing pricing = getModelPricing(model, provider);

        BigDecimal inputCost = pricing.getInputPricePer1MTokens()
                .multiply(BigDecimal.valueOf(inputTokens))
                .divide(BigDecimal.valueOf(1_000_000), 8, RoundingMode.HALF_UP);

        BigDecimal outputCost = pricing.getOutputPricePer1MTokens()
                .multiply(BigDecimal.valueOf(outputTokens))
                .divide(BigDecimal.valueOf(1_000_000), 8, RoundingMode.HALF_UP);

        return inputCost.add(outputCost);
    }
}
