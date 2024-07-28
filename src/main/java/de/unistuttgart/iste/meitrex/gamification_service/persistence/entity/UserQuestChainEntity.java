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
    private LinkedList<QuestEntity> quests;

    private UUID questChainUUID;

    private UUID userUUID;

    private int currentUserQuestIndex;

    private boolean finished;

    public void finishQuest() {
        if (currentUserQuestIndex < quests.size() - 1) {
            currentUserQuestIndex++;
        } else {
            finished = true;
        }
    }

    public QuestEntity getCurrentUserQuest() {
        if (this.quests != null && currentUserQuestIndex < quests.size() - 1) {
            return quests.get(currentUserQuestIndex);
        } else {
            return null;
        }
    }

    public void addQuest(QuestEntity quest) {
        if (this.quests == null) {
            this.quests = new LinkedList<QuestEntity>();
        }
        this.quests.add(quest);
    }

    public int length() {
        if (this.quests == null) {
            return 0;
        }
        return this.quests.size();
    }

}
