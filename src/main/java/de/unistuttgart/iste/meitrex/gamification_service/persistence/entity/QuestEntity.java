package de.unistuttgart.iste.meitrex.gamification_service.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity(name = "Quest")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID questUUID;

    private UUID quizUUID;

    private UUID flashCardSetUUID;

    private String description;

}
