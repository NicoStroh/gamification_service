package de.unistuttgart.iste.meitrex.gamification_service.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity(name = "BloomLevel")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BloomLevelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID bloomLevelUUID;

    private UUID userUUID;

    private UUID courseUUID;

    private int collectedExp;

    public int addExp(int exp) {
        collectedExp += exp;
        return collectedExp;
    }

}
