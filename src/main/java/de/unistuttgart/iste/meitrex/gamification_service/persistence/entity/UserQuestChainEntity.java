package de.unistuttgart.iste.meitrex.gamification_service.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.LinkedList;
import java.util.UUID;

@Entity(name = "UserQuestChain")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserQuestChainEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID userQuestChainUUID;

    private UUID questChainUUID;

    private UUID userUUID;

    private int userLevel;

    public void finishQuest() {
        userLevel++;
    }

    public void decreaseUserLevel() {
        if (userLevel <= 0) {
            return;
        }
        userLevel--;
    }

}
