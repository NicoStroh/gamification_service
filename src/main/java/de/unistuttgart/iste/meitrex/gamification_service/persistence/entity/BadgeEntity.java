package de.unistuttgart.iste.meitrex.gamification_service.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity(name = "Badge")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BadgeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID badgeUUID;

    private String name;

    private String description;

    private int passingPercentage;

    private UUID quizUUID;

    private UUID flashCardSetUUID;

}
