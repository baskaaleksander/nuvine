package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.domain.model.EmbeddedChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class VectorStorageService {

    public void upsert(List<EmbeddedChunk> chunks) {

    }
}
