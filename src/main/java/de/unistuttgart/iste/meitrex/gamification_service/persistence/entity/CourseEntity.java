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
        requiredExpPerLevel.add(0);

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

    public boolean addContent(UUID contentUUID, UUID chapter, int skillPoints) {
        int level = this.getLevelOfChapter(chapter);
        if (level < 0) {
            return false;
        }

        this.addContent(contentUUID);
        if (this.requiredExpPerLevel == null || this.requiredExpPerLevel.size() < level) {
            return false;
        }
        this.requiredExpPerLevel.set(level, requiredExpPerLevel.get(level) + (skillPoints / 2));
        return true;
    }

    public void removeContent(UUID contentUUID, UUID chapter, int skillPoints) {
        int level = this.getLevelOfChapter(chapter);
        this.content.remove(contentUUID);
        if (this.requiredExpPerLevel == null || this.requiredExpPerLevel.size() < level) {
            return;
        }
        this.requiredExpPerLevel.set(level, requiredExpPerLevel.get(level) - (skillPoints / 2));
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

        return exp - accumulatedExp;
    }

    public int getRequiredExpOfLevel(int level) {
        if (this.requiredExpPerLevel == null
        || this.requiredExpPerLevel.isEmpty()
        || level < 0) {
            return 0;
        } else if (level >= this.requiredExpPerLevel.size()) {
            return Integer.MAX_VALUE;
        }
        return this.requiredExpPerLevel.get(level);
    }

}
