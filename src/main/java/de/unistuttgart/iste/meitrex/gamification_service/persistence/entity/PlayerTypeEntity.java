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

    private DominantPlayerType dominantPlayerType;

    public PlayerTypeEntity.DominantPlayerType dominantPlayerType() {

        if (killerPercentage >= socializerPercentage) {

            if (killerPercentage >= explorerPercentage) {

                if (killerPercentage >= achieverPercentage) {
                    return DominantPlayerType.Killer;
                } else {
                    return DominantPlayerType.Achiever;
                }

            } else {

                if (explorerPercentage >= achieverPercentage) {
                    return DominantPlayerType.Explorer;
                } else {
                    return DominantPlayerType.Achiever;
                }

            }

        } else {

            if (socializerPercentage >= explorerPercentage) {

                if (socializerPercentage >= achieverPercentage) {
                    return DominantPlayerType.Socializer;
                } else {
                    return DominantPlayerType.Achiever;
                }

            } else {

                if (explorerPercentage >= achieverPercentage) {
                    return DominantPlayerType.Explorer;
                } else {
                    return DominantPlayerType.Achiever;
                }

            }

        }

    }

    public boolean isAchiever() {
        return achieverPercentage > explorerPercentage
                && achieverPercentage > socializerPercentage
                && achieverPercentage > killerPercentage;
    }

    public boolean isExplorer() {
        return explorerPercentage >= achieverPercentage
                && explorerPercentage > socializerPercentage
                && explorerPercentage > killerPercentage;
    }

    public boolean isSocializer() {
        return socializerPercentage >= achieverPercentage
                && socializerPercentage >= explorerPercentage
                && socializerPercentage > killerPercentage;
    }

    public boolean isKiller() {
        return killerPercentage >= achieverPercentage
                && killerPercentage >= explorerPercentage
                && killerPercentage >= socializerPercentage;
    }

}
