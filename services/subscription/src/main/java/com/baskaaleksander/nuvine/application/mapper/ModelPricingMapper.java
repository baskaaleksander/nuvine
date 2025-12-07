package com.baskaaleksander.nuvine.application.mapper;

import com.baskaaleksander.nuvine.application.dto.ModelPricingResponse;
import com.baskaaleksander.nuvine.domain.model.LlmModel;
import org.springframework.stereotype.Component;

@Component
public class ModelPricingMapper {

    public ModelPricingResponse toResponse(LlmModel model) {
        return new ModelPricingResponse(
                model.getProvider().getProviderKey(),
                model.getModelKey(),
                model.getDisplayName(),
                model.getPricing()
        );
    }
}
