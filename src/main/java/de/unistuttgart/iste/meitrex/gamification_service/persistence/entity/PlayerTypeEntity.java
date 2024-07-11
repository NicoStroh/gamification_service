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

    public enum DominantPlayerType {
        None,
        Achiever,
        Explorer,
        Socializer,
        Killer;
    }

    @Id
    private UUID userUUID;

    private int achieverPercentage;
    private int explorerPercentage;
    private int socializerPercentage;
    private int killerPercentage;

    public PlayerTypeEntity.DominantPlayerType dominantPlayerType() {

        if (achieverPercentage >= explorerPercentage) {

            if (achieverPercentage >= socializerPercentage) {

                if (achieverPercentage > killerPercentage) {
                    return DominantPlayerType.Achiever;
                } else {
                    return DominantPlayerType.Killer;
                }

            } else {

                if (socializerPercentage > killerPercentage) {
                    return DominantPlayerType.Socializer;
                } else {
                    return DominantPlayerType.Killer;
                }

            }

        } else {

            if (explorerPercentage >= socializerPercentage) {

                if (explorerPercentage > killerPercentage) {
                    return DominantPlayerType.Explorer;
                } else {
                    return DominantPlayerType.Killer;
                }

            } else {

                if (socializerPercentage > killerPercentage) {
                    return DominantPlayerType.Socializer;
                } else {
                    return DominantPlayerType.Killer;
                }

            }

        }

    }

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
