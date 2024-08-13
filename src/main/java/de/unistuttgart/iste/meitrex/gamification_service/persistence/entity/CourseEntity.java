package de.unistuttgart.iste.meitrex.gamification_service.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.*;

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

    @ElementCollection
    private List<Integer> requiredExpPerLevel;

    @ElementCollection
    private List<UUID> chapters;

    @ElementCollection
    private Set<UUID> content;

    public void addUser(UUID userUUID) {
        this.userUUIDs.add(userUUID);
    }

    public void removeUser(UUID userUUID) {
        if (this.userUUIDs != null) {
            this.userUUIDs.removeIf(userUUID::equals);
        }
    }

    public void addChapter(UUID chapterUUID) {
        if (this.requiredExpPerLevel == null) {
            this.requiredExpPerLevel = new ArrayList<>();
        }
        requiredExpPerLevel.add(requiredExpOfLevel(requiredExpPerLevel.size() + 1));

        if (this.chapters == null) {
            this.chapters = new LinkedList<>();
        }
        this.chapters.add(chapterUUID);
    }

    public int getLevelOfChapter(UUID chapterUUID) {
        if (this.chapters == null) {
            return -1;
        }
        return this.chapters.indexOf(chapterUUID);
    }

    public void addContent(UUID contentUUID) {
        if (this.content == null) {
            this.content = new HashSet<>();
        }
        this.content.add(contentUUID);
    }

    public void addQuiz(UUID quizUUID, UUID chapter) {
        int level = this.getLevelOfChapter(chapter);
        this.addContent(quizUUID);
        if (this.requiredExpPerLevel == null || this.requiredExpPerLevel.size() < level) {
            return;
        }
        this.requiredExpPerLevel.set(level, requiredExpPerLevel.get(level) + 50);
    }

    public void addFlashCardSet(UUID flashCardSet, UUID chapter) {
        int level = this.getLevelOfChapter(chapter);
        this.addContent(flashCardSet);
        if (this.requiredExpPerLevel == null || this.requiredExpPerLevel.size() < level) {
            return;
        }
        this.requiredExpPerLevel.set(level, requiredExpPerLevel.get(level) + 30);
    }

    public void removeQuiz(UUID quizUUID, UUID chapter) {
        int level = this.getLevelOfChapter(chapter);
        this.content.remove(quizUUID);
        if (this.requiredExpPerLevel == null || this.requiredExpPerLevel.size() < level) {
            return;
        }
        this.requiredExpPerLevel.set(level, requiredExpPerLevel.get(level) - 50);
    }

    public void removeFlashCardSet(UUID flashCardSet, UUID chapter) {
        int level = this.getLevelOfChapter(chapter);
        this.content.remove(flashCardSet);
        if (this.requiredExpPerLevel == null || this.requiredExpPerLevel.size() < level) {
            return;
        }
        this.requiredExpPerLevel.set(level, requiredExpPerLevel.get(level) - 30);
    }


    public int calculateLevelForExp(int exp) {
        if (this.requiredExpPerLevel == null) {
            return 0;
        }

        int currentLevel = 0;
        int accumulatedExp = 0;

        int i = 0;
        for (Integer requiredExp : requiredExpPerLevel) {
            accumulatedExp += requiredExp;

            if (exp < accumulatedExp) {
                break;
            }

            currentLevel = i + 1;
            i++;
        }

        return currentLevel;
    }

    public int calculateRemainingExpForCurrentLevel(int exp) {
        if (this.requiredExpPerLevel == null || exp == 0) {
            return 0;
        }

        int accumulatedExp = 0;

        for (Integer requiredExp : requiredExpPerLevel) {
            accumulatedExp += requiredExp;

            if (exp < accumulatedExp) {
                return requiredExp - (accumulatedExp - exp);
            }
        }

        return 0;
    }

    public int getRequiredExpOfLevel(int level) {
        if (this.requiredExpPerLevel == null
        || this.requiredExpPerLevel.isEmpty()
        || this.requiredExpPerLevel.size() < level
        || level < 0) {
            return 0;
        }
        return this.requiredExpPerLevel.get(level);
    }


    private static int requiredExpOfLevel(int level) {
        return 100 + (50 * level * level);
    }

}
