package de.unistuttgart.iste.meitrex.gamification_service.persistence.mapper;

import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.UserQuestChainEntity;
import de.unistuttgart.iste.meitrex.generated.dto.UserQuest;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.UserQuestEntity;
import de.unistuttgart.iste.meitrex.generated.dto.UserQuestChain;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedList;

@Component
@RequiredArgsConstructor
public class QuestMapper {

    private final ModelMapper modelMapper;

    public UserQuest userQuestEntityToDto(UserQuestEntity userQuestEntity) {
        return modelMapper.map(userQuestEntity, UserQuest.class);
    }

    public UserQuestEntity dtoToUserQuestEntity(UserQuest userQuest) {
        return modelMapper.map(userQuest, UserQuestEntity.class);
    }

    public UserQuestChain userQuestChainEntityToDto(UserQuestChainEntity userQuestChainEntity) {
        UserQuestChain userQuestChain = new UserQuestChain();

        userQuestChain.setUserQuestChainUUID(userQuestChainEntity.getUserQuestChainUUID());
        userQuestChain.setQuestChainUUID(userQuestChainEntity.getQuestChainUUID());
        userQuestChain.setCourseUUID(userQuestChainEntity.getCourseUUID());
        userQuestChain.setCurrentUserQuestIndex(userQuestChainEntity.getCurrentUserQuestIndex());
        userQuestChain.setFinished(userQuestChainEntity.isFinished());

        LinkedList<UserQuest> userQuests = new LinkedList<UserQuest>();
        for (UserQuestEntity userQuestEntity : userQuestChainEntity.getUserQuests()) {
            userQuests.add(userQuestEntityToDto(userQuestEntity));
        }
        userQuestChain.setUserQuests(userQuests);

        return userQuestChain;
    }

}