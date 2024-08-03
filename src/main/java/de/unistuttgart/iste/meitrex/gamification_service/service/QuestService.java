package de.unistuttgart.iste.meitrex.gamification_service.service;

import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.*;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.mapper.QuestMapper;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.*;
import de.unistuttgart.iste.meitrex.generated.dto.Quest;
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

    private final static int passingPercentage = 80;

    /**
     * Creates a new empty QuestChainEntity for the course.
     *
     * @param courseUUID     the id of the new course that was just created
     */
    public void addCourse(UUID courseUUID) {
        QuestChainEntity questChainEntity = new QuestChainEntity();
        questChainEntity.setCourseUUID(courseUUID);
        questChainRepository.save(questChainEntity);
    }

    /**
     * Removes the questChain of the course and all userQuestChains of the courses users
     *
     * @param courseUUID     the id of the deleted course
     */
    public void deleteQuestChainAndUserQuestChainsOfCourse(UUID courseUUID) {
        QuestChainEntity courseQuestChain = questChainRepository.findByCourseUUID(courseUUID);
        questChainRepository.delete(courseQuestChain);

        List<UserQuestChainEntity> userQuestChainEntities = userQuestChainRepository.findByQuestChainUUID(courseQuestChain.getQuestChainUUID());
        userQuestChainRepository.deleteAll(userQuestChainEntities);
    }

    /**
     * Removes the quest that refers to the quiz
     *
     * @param courseUUID     the id of the course
     * @param quizUUID       the id of the deleted quiz
     */
    public void deleteQuestOfQuiz(UUID courseUUID, UUID quizUUID) {
        QuestChainEntity courseQuestChain = questChainRepository.findByCourseUUID(courseUUID);
        courseQuestChain.removeQuestOfQuiz(quizUUID);
        questChainRepository.save(courseQuestChain);

        List<UserQuestChainEntity> userQuestChainEntities = userQuestChainRepository.findByQuestChainUUID(courseQuestChain.getQuestChainUUID());
        for (UserQuestChainEntity userQuestChainEntity : userQuestChainEntities) {
            userQuestChainEntity.deleteQuest();
            userQuestChainRepository.save(userQuestChainEntity);
        }
    }

    /**
     * Removes the quest that refers to the fcs
     *
     * @param courseUUID             the id of the course
     * @param flashCardSetUUID       the id of the deleted fcs
     */
    public void deleteQuestOfFCS(UUID courseUUID, UUID flashCardSetUUID) {
        QuestChainEntity courseQuestChain = questChainRepository.findByCourseUUID(courseUUID);
        courseQuestChain.removeQuestOfFCS(flashCardSetUUID);
        questChainRepository.save(courseQuestChain);

        List<UserQuestChainEntity> userQuestChainEntities = userQuestChainRepository.findByQuestChainUUID(courseQuestChain.getQuestChainUUID());
        for (UserQuestChainEntity userQuestChainEntity : userQuestChainEntities) {
            userQuestChainEntity.deleteQuest();
            userQuestChainRepository.save(userQuestChainEntity);
        }
    }

    /**
     * Retrieves the quest for a user and the course depending on the level of the user at this questchain
     *
     * @param userUUID     the id of the user
     * @param courseUUID   the id of the course
     *
     * @return the Quest for the current level of the user at this course
     */
    public Quest getCurrentUserQuest(UUID userUUID, UUID courseUUID) {

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        if (questChainEntity == null) {
            return null;
        }

        UserQuestChainEntity userQuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), userUUID);
        if (userQuestChainEntity == null) {
            return null;
        }

        int userLevel = userQuestChainEntity.getUserLevel();

        if (userLevel < questChainEntity.getQuests().size()) {
            Quest quest = questMapper.questEntityToDto(questChainEntity.getQuests().get(userLevel));
            quest.setFinished(false);
            quest.setLevel(userLevel);
            return quest;
        }
        return new Quest(UUID.randomUUID(), null, null, true, "You finished all quests for this course!", userLevel);

    }

    /**
     * Retrieves the complete quest chain for the user at the course
     *
     * @param userUUID     the id of the user
     * @param courseUUID   the id of the course
     *
     * @return the UserQuestChain for the user at this course with the current level of the user
     */
    public UserQuestChain getUserQuestChain(UUID userUUID, UUID courseUUID) {

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        if (questChainEntity == null) {
            return null;
        }

        UserQuestChainEntity userQuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), userUUID);
        if (userQuestChainEntity == null) {
            return null;
        }

        return questMapper.userQuestChainEntityToDto(userQuestChainEntity, questChainEntity.getQuests());

    }

    /**
     * Deletes the userQuestChain for the user at the course
     *
     * @param userUUID     the id of the user
     * @param courseUUID   the id of the course
     */
    public void deleteUserQuestChain(UUID userUUID, UUID courseUUID) {
        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        if (questChainEntity == null) {
            return;
        }
        userQuestChainRepository.deleteByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), userUUID);
    }

    /**
     * Assigns the questchain of the course for a user, who just joined the course
     *
     * @param userUUID     the id of the user
     * @param courseUUID   the id of the course
     */
    public void assignQuestChainToUser(UUID userUUID, UUID courseUUID) {

        QuestChainEntity questChain = questChainRepository.findByCourseUUID(courseUUID);
        if (questChain == null) {
            return;
        }

        UserQuestChainEntity userQuestChain = new UserQuestChainEntity();
        userQuestChain.setQuestChainUUID(questChain.getQuestChainUUID());
        userQuestChain.setUserUUID(userUUID);
        userQuestChainRepository.save(userQuestChain);

    }

    /**
     * Creates a quest for the given course, which suggests the user to complete the quiz with 80% correct answers
     *
     * @param quizUUID     the id of the created quiz
     * @param name         the name of the quiz
     * @param courseUUID   the id of the course
     */
    public void createQuestForQuiz(UUID quizUUID, String name, UUID courseUUID) {

        QuestEntity quest = new QuestEntity();
        quest.setQuizUUID(quizUUID);
        quest.setDescription("Finish quiz " + name + " with at least " + passingPercentage + "% correct answers to unlock the next quest!");

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        questChainEntity.addQuest(quest);
        questChainRepository.save(questChainEntity);

    }

    /**
     * Creates a quest for the given course, which suggests the user to complete the flashcardset with 80% correct answers
     *
     * @param flashCardSetUUID     the id of the created flashcardset
     * @param name                 the name of the flashcardset
     * @param courseUUID           the id of the course
     */
    public void createQuestForFlashCardSet(UUID flashCardSetUUID, String name, UUID courseUUID) {

        QuestEntity quest = new QuestEntity();
        quest.setFlashCardSetUUID(flashCardSetUUID);
        quest.setDescription("Finish flashcardset " + name + " with at least " + passingPercentage + "% correct answers to unlock the next quest!");

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        questChainEntity.addQuest(quest);
        questChainRepository.save(questChainEntity);

    }

    /**
     * Retrieves the userQuestChain for the user at the course
     *
     * @param userUUID     the id of the user
     * @param courseUUID   the id of the course
     *
     * @return the UserQuestChainEntity for the user at this course
     */
    private UserQuestChainEntity findByUserUUIDAndCourseUUID(UUID userUUID, UUID courseUUID) {

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);

        if (questChainEntity == null) {
            return null;
        }
        return userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), userUUID);

    }

    /**
     * Marks the current quest of the user at this course as finished, if he got more than 80% of the answers correct
     * and the current quest points to the finished quiz
     *
     * @param userUUID             the id of the user
     * @param courseUUID           the id of the course, in which the quiz is
     * @param quizUUID             the id of the quiz
     * @param correctAnswers       the number of correct answers, the user got for this quiz
     * @param totalAnswers         the total number of questions in this quiz
     */
    public void markQuestAsFinishedIfPassedQuiz(UUID userUUID, UUID courseUUID, UUID quizUUID, int correctAnswers, int totalAnswers) {

        UserQuestChainEntity userQuestChainEntity = findByUserUUIDAndCourseUUID(userUUID, courseUUID);
        if (userQuestChainEntity == null) {
            return;
        }

        Quest currentUserQuest = getCurrentUserQuest(userUUID, courseUUID);
        int percentage = (correctAnswers * 100) / totalAnswers;
        if (quizUUID.equals(currentUserQuest.getQuizUUID()) && percentage > passingPercentage) {
            userQuestChainEntity.finishQuest();
        }

        userQuestChainRepository.save(userQuestChainEntity);

    }

    /**
     * Marks the current quest of the user at this course as finished, if he got more than 80% of the answers correct
     * and the current quest points to the finished flashcardset
     *
     * @param userUUID             the id of the user
     * @param courseUUID           the id of the course, in which the flashcardset is
     * @param flashCardSetUUID     the id of the flashcardset
     * @param correctAnswers       the number of correct answers, the user got for this flashcardset
     * @param totalAnswers         the total number of questions in this flashcardset
     */
    public void markQuestAsFinishedIfPassedFlashCardSet(UUID userUUID, UUID courseUUID, UUID flashCardSetUUID, int correctAnswers, int totalAnswers) {

        UserQuestChainEntity userQuestChainEntity = findByUserUUIDAndCourseUUID(userUUID, courseUUID);
        if (userQuestChainEntity == null) {
            return;
        }

        Quest currentUserQuest = getCurrentUserQuest(userUUID, courseUUID);
        int percentage = (correctAnswers * 100) / totalAnswers;
        if (flashCardSetUUID.equals(currentUserQuest.getFlashCardSetUUID()) && percentage > passingPercentage) {
            userQuestChainEntity.finishQuest();
        }

        userQuestChainRepository.save(userQuestChainEntity);

    }

}