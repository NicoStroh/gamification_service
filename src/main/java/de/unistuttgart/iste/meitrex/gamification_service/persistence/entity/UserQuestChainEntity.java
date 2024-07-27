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

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private LinkedList<UserQuestEntity> userQuests;

    private UUID questChainUUID;

    private UUID userUUID;

    private UUID courseUUID;

    private int currentUserQuestIndex;

    private boolean finished;

    public void finishQuest() {
        if (currentUserQuestIndex < userQuests.size() - 1) {
            getCurrentUserQuest().setFinished(true);
            currentUserQuestIndex++;
        } else {
            finished = true;
        }
    }

    public UserQuestEntity getCurrentUserQuest() {
        if (userQuests != null && currentUserQuestIndex < userQuests.size() - 1) {
            return userQuests.get(currentUserQuestIndex);
        } else {
            return null;
        }
    }

    public void addUserQuest(UserQuestEntity userQuest) {
        if (this.userQuests == null) {
            this.userQuests = new LinkedList<UserQuestEntity>();
        }
        this.userQuests.add(userQuest);
    }

    public int length() {
        if (this.userQuests == null) {
            return 0;
        }
        return this.userQuests.size();
    }

}
