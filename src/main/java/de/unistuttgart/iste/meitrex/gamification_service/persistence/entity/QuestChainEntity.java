package de.unistuttgart.iste.meitrex.gamification_service.persistence.entity;

import de.unistuttgart.iste.meitrex.gamification_service.service.QuestService;
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

    public int size() {
        if (this.quests == null) {
            return 0;
        }
        return quests.size();
    }

    public QuestEntity getQuest(int index) {
        return quests.get(index);
    }

    public void addQuest(QuestEntity quest) {
        if (this.quests == null) {
            this.quests = new LinkedList<QuestEntity>();
        }
        this.quests.add(quest);
    }

    public void changeNameOfQuiz(UUID quizUUID, String newName) {
        if (this.quests == null) {
            return;
        }
        for (QuestEntity quest : this.quests) {
            if (quizUUID.equals(quest.getQuizUUID())) {
                quest.setDescription(QuestService.descriptionPart1 + "quiz " + newName +
                        QuestService.descriptionPart2 + QuestService.passingPercentage + QuestService.descriptionPart3);
                break;
            }
        }
    }

    public void changeNameOfFlashCardSet(UUID flashCardSetUUID, String newName) {
        if (this.quests == null) {
            return;
        }
        for (QuestEntity quest : this.quests) {
            if (flashCardSetUUID.equals(quest.getFlashCardSetUUID())) {
                quest.setDescription(QuestService.descriptionPart1 + "flashCardSet " + newName +
                        QuestService.descriptionPart2 + QuestService.passingPercentage + QuestService.descriptionPart3);
                break;
            }
        }
    }

    public int findIndexOfQuizQuest(UUID quizUUID) {
        if (this.quests != null) {
            int i = 0;
            for (QuestEntity quest : this.quests) {
                if (quizUUID.equals(quest.getQuizUUID())) {
                    return i;
                }
                i++;
            }
        }
        return -1;
    }

    public int findIndexOfFlashcardSetQuest(UUID flashCardSetUUID) {
        if (this.quests != null) {
            int i = 0;
            for (QuestEntity quest : this.quests) {
                if (flashCardSetUUID.equals(quest.getFlashCardSetUUID())) {
                    return i;
                }
                i++;
            }
        }
        return -1;
    }

    public int removeQuestOfQuiz(UUID quizUUID) {
        if (this.quests != null) {
            int indexOfQuizQuest = this.findIndexOfQuizQuest(quizUUID);
            if (0 <= indexOfQuizQuest && indexOfQuizQuest < this.quests.size()) {
                this.quests.remove(indexOfQuizQuest);
            }
            return indexOfQuizQuest;
        }
        return -1;
    }

    public int removeQuestOfFCS(UUID flashCardSetUUID) {
        if (this.quests != null) {
            int indexOfFCSQuest = this.findIndexOfFlashcardSetQuest(flashCardSetUUID);
            if (0 <= indexOfFCSQuest && indexOfFCSQuest < this.quests.size()) {
                this.quests.remove(indexOfFCSQuest);
            }
            return indexOfFCSQuest;
        }
        return -1;
    }



}
