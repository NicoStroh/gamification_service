package de.unistuttgart.iste.meitrex.gamification_service.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity(name = "UserQuest")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserQuestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID userQuestUUID;

    private UUID userUUID;

    private UUID questUUID;

    private UUID quizUUID;

    private UUID flashCardSetUUID;

    private boolean finished;

    private String description;

}
