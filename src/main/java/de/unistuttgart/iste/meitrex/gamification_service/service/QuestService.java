package de.unistuttgart.iste.meitrex.gamification_service.service;

import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.*;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.mapper.QuestMapper;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.*;
import de.unistuttgart.iste.meitrex.generated.dto.Badge;
import de.unistuttgart.iste.meitrex.generated.dto.UserBadge;
import de.unistuttgart.iste.meitrex.generated.dto.UserQuest;
import de.unistuttgart.iste.meitrex.generated.dto.UserQuestChain;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class QuestService {

    private final QuestChainRepository questChainRepository;
    private final UserQuestChainRepository userQuestChainRepository;

    private final QuestMapper questMapper;

    public UserQuest getCurrentUserQuest(UUID userUUID, UUID courseUUID) {

        UserQuestChainEntity userQuestChainEntity = userQuestChainRepository.findByCourseUUIDAndUserUUID(courseUUID, userUUID);
        if (userQuestChainEntity == null) {
            return null;
        }
        return questMapper.userQuestEntityToDto(userQuestChainEntity.getCurrentUserQuest());

    }

    public UserQuestChain getUserQuestChain(UUID userUUID, UUID courseUUID) {
        return questMapper.userQuestChainEntityToDto(userQuestChainRepository.findByCourseUUIDAndUserUUID(courseUUID, userUUID));
    }

    public void assignQuestChainToUser(UUID userUUID, UUID courseUUID) {

        QuestChainEntity questChain = questChainRepository.findByCourseUUID(courseUUID);

        UserQuestChainEntity userQuestChain = new UserQuestChainEntity();
        userQuestChain.setQuestChainUUID(questChain.getQuestChainUUID());
        userQuestChain.setUserUUID(userUUID);
        userQuestChain.setCourseUUID(courseUUID);
        for (QuestEntity quest : questChain.getQuests()) {
            UserQuestEntity userQuest = new UserQuestEntity();
            userQuest.setQuestUUID(quest.getQuestUUID());
            userQuest.setUserUUID(userUUID);
            userQuest.setDescription(quest.getDescription());

            if (quest.getQuizUUID() != null) {
                userQuest.setQuizUUID(quest.getQuizUUID());
            } else if (quest.getFlashCardSetUUID() != null) {
                userQuest.setFlashCardSetUUID(quest.getFlashCardSetUUID());
            }

            userQuestChain.addUserQuest(userQuest);
        }
        userQuestChainRepository.save(userQuestChain);

    }

    public void createQuestForQuiz(UUID quizUUID, String name, UUID courseUUID) {

        QuestEntity quest = new QuestEntity();
        quest.setQuizUUID(quizUUID);
        quest.setDescription("Finish quiz " + name + " with at least 70% correct answers to unlock the next quest!");

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        questChainEntity.addQuest(quest);
        questChainRepository.save(questChainEntity);

    }

    public void createQuestForFlashCardSet(UUID flashCardSetUUID, String name, UUID courseUUID) {

        QuestEntity quest = new QuestEntity();
        quest.setFlashCardSetUUID(flashCardSetUUID);
        quest.setDescription("Finish Flashcardset " + name + " with at least 70% correct answers to unlock the next quest!");

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        questChainEntity.addQuest(quest);
        questChainRepository.save(questChainEntity);

    }

    public void markQuestAsFinishedIfPassedQuiz(UUID userUUID, UUID courseUUID, UUID quizUUID, int correctAnswers, int totalAnswers) {

        UserQuestChainEntity userQuestChainEntity = userQuestChainRepository.findByCourseUUIDAndUserUUID(courseUUID, userUUID);
        if (userQuestChainEntity == null) {
            return;
        }

        UserQuestEntity currentUserQuestEntity = userQuestChainEntity.getCurrentUserQuest();
        int percentage = (correctAnswers * 100) / totalAnswers;
        if (currentUserQuestEntity.getQuizUUID() == quizUUID && percentage > 70) {
            userQuestChainEntity.finishQuest();
        }

        userQuestChainRepository.save(userQuestChainEntity);

    }

    public void markQuestAsFinishedIfPassedFlashCardSet(UUID userUUID, UUID courseUUID, UUID flashCardSetUUID, int correctAnswers, int totalAnswers) {

        UserQuestChainEntity userQuestChainEntity = userQuestChainRepository.findByCourseUUIDAndUserUUID(courseUUID, userUUID);
        if (userQuestChainEntity == null) {
            return;
        }

        UserQuestEntity currentUserQuestEntity = userQuestChainEntity.getCurrentUserQuest();
        int percentage = (correctAnswers * 100) / totalAnswers;
        if (currentUserQuestEntity.getFlashCardSetUUID() == flashCardSetUUID && percentage > 70) {
            userQuestChainEntity.finishQuest();
        }

        userQuestChainRepository.save(userQuestChainEntity);

    }

}
