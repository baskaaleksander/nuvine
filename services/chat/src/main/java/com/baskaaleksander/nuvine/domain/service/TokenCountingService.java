package com.baskaaleksander.nuvine.domain.service;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;
import org.springframework.stereotype.Service;

@Service
public class TokenCountingService {

    private final EncodingRegistry encodingRegistry = Encodings.newDefaultEncodingRegistry();
    private final Encoding encoding = encodingRegistry.getEncodingForModel(ModelType.GPT_4_TURBO);


    public int count(String rawText) {
        return encoding.countTokens(rawText);
    }
}
