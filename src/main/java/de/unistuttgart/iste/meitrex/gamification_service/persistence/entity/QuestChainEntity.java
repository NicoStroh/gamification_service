package de.unistuttgart.iste.meitrex.gamification_service.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Entity(name = "QuestChain")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestChainEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID questChainUUID;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestEntity> quests;

    private UUID courseUUID;

    public void addQuest(QuestEntity quest) {
        if (this.quests == null) {
            this.quests = new LinkedList<QuestEntity>();
        }
        this.quests.add(quest);
    }

    public void removeQuestOfQuiz(UUID quizUUID) {
        if (this.quests != null) {
            this.quests.removeIf(quest -> quizUUID.equals(quest.getQuizUUID()));
        }
    }

    public void removeQuestOfFCS(UUID flashCardSetUUID) {
        if (this.quests != null) {
            this.quests.removeIf(quest -> flashCardSetUUID.equals(quest.getFlashCardSetUUID()));
        }
    }



}
