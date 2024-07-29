package de.unistuttgart.iste.meitrex.gamification_service.persistence.mapper;

import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.QuestEntity;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.UserQuestChainEntity;
import de.unistuttgart.iste.meitrex.generated.dto.Quest;
import de.unistuttgart.iste.meitrex.generated.dto.UserQuestChain;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class QuestMapper {

    private final ModelMapper modelMapper;

    public Quest questEntityToDto(QuestEntity questEntity) {
        Quest quest = new Quest();
        quest.setQuestUUID(questEntity.getQuestUUID());
        quest.setDescription(questEntity.getDescription());
        quest.setQuizUUID(questEntity.getQuizUUID());
        quest.setFlashCardSetUUID(questEntity.getFlashCardSetUUID());
        return quest;
    }

    public UserQuestChain userQuestChainEntityToDto(UserQuestChainEntity userQuestChainEntity, List<QuestEntity> quests) {

        UserQuestChain userQuestChain = new UserQuestChain();

        userQuestChain.setUserQuestChainUUID(userQuestChainEntity.getUserQuestChainUUID());
        userQuestChain.setQuestChainUUID(userQuestChainEntity.getQuestChainUUID());
        userQuestChain.setUserUUID(userQuestChainEntity.getUserUUID());
        int userLevel = userQuestChainEntity.getUserLevel();
        userQuestChain.setUserLevel(userLevel);
        userQuestChain.setFinished(userLevel >= quests.size());

        List<Quest> userQuests = new LinkedList<Quest>();
        int i = 0;
        for (QuestEntity userQuestEntity : quests) {
            Quest quest = questEntityToDto(userQuestEntity);
            quest.setFinished(i < userLevel);
            userQuests.add(quest);
            i++;
        }
        userQuestChain.setQuests(userQuests);

        return userQuestChain;

    }

}