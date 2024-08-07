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

    public PlayerTypeEntity(UUID userUUID, boolean userHasTakenTest) {

        this.userUUID = userUUID;
        this.userHasTakenTest = userHasTakenTest;

    }

    public PlayerTypeEntity(UUID userUUID,
                            int achieverPercentage, int explorerPercentage,
                            int socializerPercentage, int killerPercentage) {

        this.userUUID = userUUID;
        this.userHasTakenTest = true;
        this.achieverPercentage = achieverPercentage;
        this.explorerPercentage = explorerPercentage;
        this.socializerPercentage = socializerPercentage;
        this.killerPercentage = killerPercentage;
        this.dominantPlayerType = this.dominantPlayerType();

    }

    @Id
    private UUID userUUID;

    private boolean userHasTakenTest;

    private int achieverPercentage;
    private int explorerPercentage;
    private int socializerPercentage;
    private int killerPercentage;

    private DominantPlayerType dominantPlayerType;

    private PlayerTypeEntity.DominantPlayerType dominantPlayerType() {

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
        return this.dominantPlayerType == DominantPlayerType.Achiever;
    }

    public boolean isExplorer() {
        return this.dominantPlayerType == DominantPlayerType.Explorer;
    }

    public boolean isSocializer() {
        return this.dominantPlayerType == DominantPlayerType.Socializer;
    }

    public boolean isKiller() {
        return this.dominantPlayerType == DominantPlayerType.Killer;
    }

    public void increaseAchieverPercentage(int increase) {
        this.achieverPercentage += increase;
    }

    public void increaseExplorerPercentage(int increase) {
        this.explorerPercentage += increase;
    }

    public void increaseSocializerPercentage(int increase) {
        this.socializerPercentage += increase;
    }

    public void increaseKillerPercentage(int increase) {
        this.killerPercentage += increase;
    }

}
