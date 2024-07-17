package de.unistuttgart.iste.meitrex.gamification_service.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity(name = "UserBadge")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBadgeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID userBadgeUUID;

    private UUID userUUID;

    private UUID badgeUUID;

    private boolean achieved;

    private String description;

    private int passingPercentage;

}
