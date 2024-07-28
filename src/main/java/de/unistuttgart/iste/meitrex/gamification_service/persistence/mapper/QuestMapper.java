package de.unistuttgart.iste.meitrex.gamification_service.persistence.mapper;

import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.QuestEntity;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.UserQuestChainEntity;
import de.unistuttgart.iste.meitrex.generated.dto.Quest;
import de.unistuttgart.iste.meitrex.generated.dto.UserQuestChain;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedList;

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

    public UserQuestChain userQuestChainEntityToDto(UserQuestChainEntity userQuestChainEntity) {

        UserQuestChain userQuestChain = new UserQuestChain();

        userQuestChain.setUserQuestChainUUID(userQuestChainEntity.getUserQuestChainUUID());
        userQuestChain.setQuestChainUUID(userQuestChainEntity.getQuestChainUUID());
        userQuestChain.setUserUUID(userQuestChainEntity.getUserUUID());
        userQuestChain.setCurrentUserQuestIndex(userQuestChainEntity.getCurrentUserQuestIndex());
        userQuestChain.setFinished(userQuestChainEntity.isFinished());

        LinkedList<Quest> userQuests = new LinkedList<Quest>();
        int i = 0;
        for (QuestEntity userQuestEntity : userQuestChainEntity.getQuests()) {
            Quest quest = questEntityToDto(userQuestEntity);
            quest.setFinished(i < userQuestChain.getCurrentUserQuestIndex());
            userQuests.add(quest);
            i++;
        }
        userQuestChain.setQuests(userQuests);

        return userQuestChain;

    }

}