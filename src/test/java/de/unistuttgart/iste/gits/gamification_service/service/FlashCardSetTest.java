package de.unistuttgart.iste.gits.gamification_service.service;

import de.unistuttgart.iste.gits.gamification_service.test_utils.TestUtils;
import de.unistuttgart.iste.meitrex.gamification_service.GamificationApplication;
import de.unistuttgart.iste.meitrex.gamification_service.controller.GamificationController;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.*;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.*;
import de.unistuttgart.iste.meitrex.gamification_service.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = GamificationApplication.class)
@Transactional
class FlashCardSetTest {

    // Required to run tests for the repositories
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("testdb")
            .withUsername("root")
            .withPassword("root");

    // Required to run tests for the repositories
    @BeforeAll
    static void startContainer() {
        postgres.start();
    }

    // Required to run tests for the repositories
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private BadgeRepository badgeRepository;

    @Autowired
    private QuestChainRepository questChainRepository;

    @Autowired
    private UserBadgeRepository userBadgeRepository;

    @Autowired
    private UserQuestChainRepository userQuestChainRepository;

    @Autowired
    private GamificationController gamificationController;

    private UUID courseUUID;
    private UUID lecturerUUID;
    private UUID user1UUID;
    private UUID user2UUID;
    private UUID quizUUID;
    private UUID flashCardSetUUID;

    @BeforeEach
    void createTestCourse() {
        this.courseUUID = UUID.randomUUID();
        this.lecturerUUID = UUID.randomUUID();
        this.user1UUID = UUID.randomUUID();
        this.user2UUID = UUID.randomUUID();
        this.quizUUID = UUID.randomUUID();
        this.flashCardSetUUID = UUID.randomUUID();

        TestUtils.createTestCourse(gamificationController,
                courseUUID,
                lecturerUUID,
                user1UUID,
                user2UUID,
                quizUUID,
                flashCardSetUUID);
    }


    @Test
    void createFlashCardSetTest() {
        UUID flashCardSet = UUID.randomUUID();
        String name = "FCS 2";
        assertEquals("Created flashCardSet successfully.",
                gamificationController.createFlashCardSet(flashCardSet, name, courseUUID));

        assertEquals(9, badgeRepository.findAll().size());
        List<BadgeEntity> fcsBadges = badgeRepository.findByFlashCardSetUUID(flashCardSet);
        assertEquals(3, fcsBadges.size());

        List<UserBadgeEntity> fcsUserBadges = new LinkedList<>();
        for (BadgeEntity badge : fcsBadges) {
            int passingPercentage = badge.getPassingPercentage();
            assertEquals(courseUUID, badge.getCourseUUID());
            assertTrue(50 == passingPercentage || 70 == passingPercentage || 90 == passingPercentage);
            assertEquals(flashCardSet, badge.getFlashCardSetUUID());
            assertNull(badge.getQuizUUID());
            assertEquals(BadgeService.descriptionPart1 + passingPercentage + BadgeService.descriptionPart2 +
                    "flashCardSet " + name + BadgeService.descriptionPart3, badge.getDescription());

            fcsUserBadges.addAll(userBadgeRepository.findByBadgeUUID(badge.getBadgeUUID()));
        }

        assertEquals(27, userBadgeRepository.findAll().size());
        assertEquals(9, fcsUserBadges.size());
        for (UserBadgeEntity userBadge : fcsUserBadges) {
            assertNotNull(userBadge.getUserBadgeUUID());
            assertTrue(badgeRepository.findById(userBadge.getBadgeUUID()).isPresent());
            assertTrue(lecturerUUID.equals(userBadge.getUserUUID())
                    || user1UUID.equals(userBadge.getUserUUID())
                    || user2UUID.equals(userBadge.getUserUUID()));
            assertFalse(userBadge.isAchieved());
        }

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertEquals(1, questChainRepository.findAll().size());
        assertNotNull(questChainEntity);
        assertEquals(3, questChainEntity.getQuests().size());

    }

    @Test
    void deleteBadgesAndQuestOfFlashCardSetTest() {
        gamificationController.finishFlashCardSet(user1UUID, courseUUID, flashCardSetUUID, 5, 5);
        gamificationController.finishQuiz(user2UUID, courseUUID, quizUUID, 5, 5);

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        UserQuestChainEntity user1QuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), user1UUID);
        UserQuestChainEntity user2QuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), user2UUID);
        assertEquals(0, user1QuestChainEntity.getUserLevel());
        assertEquals(1, user2QuestChainEntity.getUserLevel());

        assertEquals("FlashCardSet deleted.", gamificationController.deleteBadgesAndQuestOfFlashCardSet(flashCardSetUUID, courseUUID));

        List<BadgeEntity> allBadges = badgeRepository.findAll();
        assertEquals(3, allBadges.size());
        for(BadgeEntity badge : allBadges) {
            assertNotEquals(flashCardSetUUID, badge.getFlashCardSetUUID());
        }

        List<UserBadgeEntity> allUserBadges = userBadgeRepository.findAll();
        assertEquals(9, allUserBadges.size());

        questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertNotNull(questChainEntity);
        assertEquals(1, questChainEntity.getQuests().size());
        for (QuestEntity quest : questChainEntity.getQuests()) {
            assertNotEquals(flashCardSetUUID, quest.getFlashCardSetUUID());
        }

        UserQuestChainEntity lecturerQuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), lecturerUUID);
        user1QuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), user1UUID);
        user2QuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), user2UUID);
        assertEquals(3, userQuestChainRepository.findAll().size());
        assertEquals(0, lecturerQuestChainEntity.getUserLevel());
        assertEquals(0, user1QuestChainEntity.getUserLevel());
        assertEquals(1, user2QuestChainEntity.getUserLevel());

    }

    @Test
    void editFlashCardSetNameTest() {
        String newName = "New Name";
        assertEquals("Changed flashCardSet name!", gamificationController.editFlashCardSetName(flashCardSetUUID, courseUUID, newName));

        List<BadgeEntity> fcsBadges = badgeRepository.findByFlashCardSetUUID(quizUUID);
        for (BadgeEntity badge : fcsBadges) {
            int passingPercentage = badge.getPassingPercentage();
            assertEquals(BadgeService.descriptionPart1 + passingPercentage + BadgeService.descriptionPart2 +
                    "flashCardSet " + newName + BadgeService.descriptionPart3, badge.getDescription());
        }

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        int index = questChainEntity.findIndexOfFlashcardSetQuest(flashCardSetUUID);
        QuestEntity quest = questChainEntity.getQuests().get(index);
        assertEquals(QuestService.descriptionPart1 + "flashCardSet " + newName +
                QuestService.descriptionPart2 + 80 + QuestService.descriptionPart3, quest.getDescription());
    }

    /*
    @Test
    void finishFlashCardSetTest(@Argument UUID userUUID,
                        @Argument UUID courseUUID,
                        @Argument UUID quizUUID,
                        @Argument int correctAnswers,
                        @Argument int totalAnswers) {
        badgeService.markBadgesAsAchievedIfPassedQuiz(userUUID, quizUUID, correctAnswers, totalAnswers);
        questService.markQuestAsFinishedIfPassedQuiz(userUUID, courseUUID, quizUUID, correctAnswers, totalAnswers);
        return "Finished quiz!";
    }
    */

}