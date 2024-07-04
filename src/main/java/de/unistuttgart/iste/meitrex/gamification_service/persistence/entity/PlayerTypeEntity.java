package de.unistuttgart.iste.meitrex.gamification_service.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity(name = "PlayerType")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerTypeEntity {

    @Id
    private UUID userUUID;

    private int achieverPercentage;
    private int explorerPercentage;
    private int socializerPercentage;
    private int killerPercentage;

    public boolean isAchiever() {
        return achieverPercentage >= explorerPercentage
                && achieverPercentage >= socializerPercentage
                && achieverPercentage > killerPercentage;
    }

    public boolean isExplorer() {
        return explorerPercentage > achieverPercentage
                && explorerPercentage >= socializerPercentage
                && explorerPercentage > killerPercentage;
    }

    public boolean isSocializer() {
        return socializerPercentage > achieverPercentage
                && socializerPercentage > explorerPercentage
                && socializerPercentage > killerPercentage;
    }

    public boolean isKiller() {
        return killerPercentage >= achieverPercentage
                && killerPercentage >= explorerPercentage
                && killerPercentage >= socializerPercentage;
    }

}
