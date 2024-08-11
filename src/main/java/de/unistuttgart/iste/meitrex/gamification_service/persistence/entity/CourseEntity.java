package de.unistuttgart.iste.meitrex.gamification_service.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity(name = "Course")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseEntity {

    @Id
    private UUID courseUUID;

    @ElementCollection
    private Set<UUID> userUUIDs;

    private int numberOfLevels;

    @ElementCollection
    private List<Integer> requiredExpPerLevel;

    public void addUser(UUID userUUID) {
        this.userUUIDs.add(userUUID);
    }

    public void removeUser(UUID userUUID) {
        if (this.userUUIDs != null) {
            this.userUUIDs.removeIf(userUUID::equals);
        }
    }

    public void addLevel() {
        numberOfLevels++;
        if (this.requiredExpPerLevel == null) {
            this.requiredExpPerLevel = new ArrayList<>();
        }
        requiredExpPerLevel.add(requiredExpOfLevel(numberOfLevels));
    }

    public void addQuiz(int level) {
        if (this.requiredExpPerLevel == null || this.requiredExpPerLevel.size() < level) {
            return;
        }
        this.requiredExpPerLevel.set(level, requiredExpPerLevel.get(level) + quizExp);
    }

    public void addFlashCardSet(int level) {
        if (this.requiredExpPerLevel == null || this.requiredExpPerLevel.size() < level) {
            return;
        }
        this.requiredExpPerLevel.set(level, requiredExpPerLevel.get(level) + flashCardSetExp);
    }

    public int calculateLevelForExp(int exp) {
        if (this.requiredExpPerLevel == null) {
            return 0;
        }

        int currentLevel = 0;
        int accumulatedExp = 0;

        for (int i = 0; i < requiredExpPerLevel.size(); i++) {
            accumulatedExp += requiredExpPerLevel.get(i);

            if (exp < accumulatedExp) {
                break;
            }

            currentLevel = i + 1;
        }

        return currentLevel;
    }

    public int calculateRemainingExpForCurrentLevel(int exp) {
        if (this.requiredExpPerLevel == null) {
            return 0;
        }

        int accumulatedExp = 0;

        for (Integer integer : requiredExpPerLevel) {
            accumulatedExp += integer;

            if (exp < accumulatedExp) {
                return accumulatedExp - exp;
            }
        }

        return 0;
    }



    private static int quizExp = 50;
    private static int flashCardSetExp = 30;

    private static int requiredExpOfLevel(int level) {
        return 100 + (50 * level * level);
    }

    public static int rewardOfFinishedQuiz(int level, int correctAnswers, int totalAnswers) {
        return quizExp * level * (correctAnswers / totalAnswers);
    }

    public static int rewardOfFinishedFlashCardSet(int level, int correctAnswers, int totalAnswers) {
        return flashCardSetExp * level * (correctAnswers / totalAnswers);
    }

}
