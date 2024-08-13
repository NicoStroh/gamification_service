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

    public int rewardOfFinishingContent(int defaultReward, int correctAnswers, int totalAnswers, int level) {
        int reward = (level * defaultReward * correctAnswers) / totalAnswers;
        int skillReward = (skillPoints * (skillType.ordinal() + 1)) / 10;
        return (reward * skillReward) / 10;
    }

}
