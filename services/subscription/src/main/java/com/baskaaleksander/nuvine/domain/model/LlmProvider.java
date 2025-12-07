package com.baskaaleksander.nuvine.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.Id;

import java.util.UUID;

@Entity
@Table(name = "llm_providers")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LlmProvider {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String providerKey;

    private String displayName;
    private boolean active;

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL)
    private List<LlmModel> models;
}
