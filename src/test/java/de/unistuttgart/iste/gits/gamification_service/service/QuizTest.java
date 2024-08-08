package de.unistuttgart.iste.gits.gamification_service.service;

import de.unistuttgart.iste.gits.gamification_service.test_utils.TestUtils;
import de.unistuttgart.iste.meitrex.gamification_service.GamificationApplication;
import de.unistuttgart.iste.meitrex.gamification_service.controller.GamificationController;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.*;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.*;
import de.unistuttgart.iste.meitrex.gamification_service.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = GamificationApplication.class)
@Transactional
class QuizTest {

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


    @Test
    void createQuizTest() {

        UUID courseUUID = UUID.randomUUID();
        UUID lecturerUUID = UUID.randomUUID();
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        TestUtils.createTestCourse(gamificationController, courseUUID, lecturerUUID, user1, user2,
                UUID.randomUUID(), UUID.randomUUID());

        UUID quizUUID = UUID.randomUUID();
        String name = "Quiz 2";
        assertEquals("Created quiz successfully.",
                gamificationController.createQuiz(quizUUID, name, courseUUID));

        assertEquals(9, badgeRepository.findAll().size());
        List<BadgeEntity> quizBadges = badgeRepository.findByQuizUUID(quizUUID);
        List<UserBadgeEntity> quizUserBadges = new LinkedList<>();
        assertEquals(3, quizBadges.size());
        for (BadgeEntity badge : quizBadges) {
            int passingPercentage = badge.getPassingPercentage();
            assertEquals(courseUUID, badge.getCourseUUID());
            assertTrue(50 == passingPercentage || 70 == passingPercentage || 90 == passingPercentage);
            assertEquals(quizUUID, badge.getQuizUUID());
            assertNull(badge.getFlashCardSetUUID());
            assertEquals(BadgeService.descriptionPart1 + passingPercentage + BadgeService.descriptionPart2 +
                    "quiz " + name + BadgeService.descriptionPart3, badge.getDescription());

            quizUserBadges.addAll(userBadgeRepository.findByBadgeUUID(badge.getBadgeUUID()));
        }

        assertEquals(27, userBadgeRepository.findAll().size());
        assertEquals(9, quizUserBadges.size());
        for (UserBadgeEntity userBadge : quizUserBadges) {
            assertNotNull(userBadge.getUserBadgeUUID());
            assertTrue(badgeRepository.findById(userBadge.getBadgeUUID()).isPresent());
            assertTrue(lecturerUUID.equals(userBadge.getUserUUID())
                    || user1.equals(userBadge.getUserUUID())
                    || user2.equals(userBadge.getUserUUID()));
            assertFalse(userBadge.isAchieved());
        }

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertEquals(1, questChainRepository.findAll().size());
        assertNotNull(questChainEntity);
        assertEquals(3, questChainEntity.getQuests().size());

        UserQuestChainEntity userQuestChainEntity1 = userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), lecturerUUID);
        UserQuestChainEntity userQuestChainEntity2 = userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), user1);
        UserQuestChainEntity userQuestChainEntity3 = userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), user2);
        assertEquals(3, userQuestChainRepository.findAll().size());
        assertNotNull(userQuestChainEntity1);
        assertNotNull(userQuestChainEntity2);
        assertNotNull(userQuestChainEntity3);
        assertEquals(0, userQuestChainEntity1.getUserLevel());
        assertEquals(0, userQuestChainEntity2.getUserLevel());
        assertEquals(0, userQuestChainEntity3.getUserLevel());

    }



    /*
    @Test
    void deleteBadgesAndQuestOfQuizTest(@Argument UUID quizUUID, @Argument UUID courseUUID) {
        badgeService.deleteBadgesAndUserBadgesOfQuiz(quizUUID);
        questService.deleteQuestOfQuiz(courseUUID, quizUUID);
        return "Quiz deleted.";
    }

    @Test
    void editQuizNameTest(@Argument UUID quizUUID,
                          @Argument UUID courseUUID,
                          @Argument String name) {
        badgeService.changeQuizName(quizUUID, name);
        questService.changeQuizName(quizUUID, courseUUID, name);
        return "Changed quiz name!";
    }

    @Test
    void finishQuizTest(@Argument UUID userUUID,
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