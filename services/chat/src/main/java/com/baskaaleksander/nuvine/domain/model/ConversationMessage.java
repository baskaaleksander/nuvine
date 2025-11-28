//package com.baskaaleksander.nuvine.domain.model;
//
//import jakarta.persistence.*;
//import lombok.*;
//
//import java.util.UUID;
//
//@AllArgsConstructor
//@NoArgsConstructor
//@Getter
//@Setter
//@Builder
//@Entity
//@Table(name = "conversation_message")
//public class ConversationMessage {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.UUID)
//    private UUID id;
//
//    @Column(nullable = false)
//    private UUID conversationId;
//
//    @Column(nullable = false)
//    private String content;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    private ConversationRole role;
//
//    @Column(nullable = false)
//    private String modelUsed;
//
//
//    private int tokensIn;
//
//    private int tokensOut;
//
//    private double cost;
//}
