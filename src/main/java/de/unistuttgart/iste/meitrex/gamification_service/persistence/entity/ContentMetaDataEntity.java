package de.unistuttgart.iste.meitrex.gamification_service.persistence.entity;

import de.unistuttgart.iste.meitrex.generated.dto.SkillType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity(name = "ContentMetaData")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentMetaDataEntity {

    @Id
    private UUID contentUUID;

    private int skillPoints;

    private SkillType skillType;

    // Formula:
    // (correctAnswers / totalAnswers) * skillPoints * (skillLevel * 0,5)
    public double rewardOfFinishingContent(int correctAnswers, int totalAnswers) {
        double correctRatio = (double) correctAnswers / (double) totalAnswers;
        return (correctRatio * skillPoints * (skillType.ordinal() + 1)) / 2;
    }

}
