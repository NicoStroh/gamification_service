package de.unistuttgart.iste.gits.gamification_service.service;

import de.unistuttgart.iste.gits.gamification_service.test_utils.TestUtils;
import de.unistuttgart.iste.meitrex.gamification_service.GamificationApplication;
import de.unistuttgart.iste.meitrex.gamification_service.controller.GamificationController;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.*;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.*;
import de.unistuttgart.iste.meitrex.gamification_service.service.*;
import de.unistuttgart.iste.meitrex.generated.dto.Quest;
import de.unistuttgart.iste.meitrex.generated.dto.UserBadge;
import de.unistuttgart.iste.meitrex.generated.dto.UserQuestChain;
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
class CourseTest {

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
    private CourseRepository courseRepository;

    @Autowired
    private QuestChainRepository questChainRepository;

    @Autowired
    private UserBadgeRepository userBadgeRepository;

    @Autowired
    private UserQuestChainRepository userQuestChainRepository;

    @Autowired
    private GamificationController gamificationController;


    @Test
    void addCourseTest() {

        UUID courseUUID = UUID.randomUUID();
        UUID lecturerUUID = UUID.randomUUID();
        assertEquals("Added course.", gamificationController.addCourse(courseUUID, lecturerUUID));

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertEquals(1, courseRepository.findAll().size());
        assertTrue(courseEntity.isPresent());
        assertEquals(1, courseEntity.get().getUserUUIDs().size());

        assertEquals(0, badgeRepository.findAll().size());
        assertEquals(0, userBadgeRepository.findAll().size());

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertEquals(1, questChainRepository.findAll().size());
        assertNotNull(questChainEntity);
        assertTrue(questChainEntity.getQuests().isEmpty());

        UserQuestChainEntity userQuestChainEntity = userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), lecturerUUID);
        assertEquals(1, userQuestChainRepository.findAll().size());
        assertNotNull(userQuestChainEntity);
        assertEquals(0, userQuestChainEntity.getUserLevel());

    }

    @Test
    void addUserToCourseTest() {

        // First, create course
        UUID courseUUID = UUID.randomUUID();
        UUID lecturerUUID = UUID.randomUUID();
        gamificationController.addCourse(courseUUID, lecturerUUID);

        // Then, add user to it
        UUID user1 = UUID.randomUUID();
        assertEquals("Added user to course.", gamificationController.addUserToCourse(user1, courseUUID));

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertEquals(1, courseRepository.findAll().size());
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getUserUUIDs().size());

        assertEquals(0, badgeRepository.findAll().size());
        assertEquals(0, userBadgeRepository.findAll().size());

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertEquals(1, questChainRepository.findAll().size());
        assertNotNull(questChainEntity);
        assertTrue(questChainEntity.getQuests().isEmpty());

        UserQuestChainEntity userQuestChainEntity1 = userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), lecturerUUID);
        UserQuestChainEntity userQuestChainEntity2 = userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), user1);
        assertEquals(2, userQuestChainRepository.findAll().size());
        assertNotNull(userQuestChainEntity1);
        assertNotNull(userQuestChainEntity2);
        assertEquals(0, userQuestChainEntity1.getUserLevel());
        assertEquals(0, userQuestChainEntity2.getUserLevel());


        // Create quiz in course
        UUID quiz = UUID.randomUUID();
        gamificationController.createQuiz(quiz, "Quiz 1", courseUUID);

        List<BadgeEntity> allBadges = badgeRepository.findAll();
        assertEquals(3, allBadges.size());
        for (BadgeEntity badge : allBadges) {
            int passingPercentage = badge.getPassingPercentage();
            assertEquals(courseUUID, badge.getCourseUUID());
            assertTrue(50 == passingPercentage || 70 == passingPercentage || 90 == passingPercentage);
            assertEquals(quiz, badge.getQuizUUID());
            assertNull(badge.getFlashCardSetUUID());
            assertEquals(BadgeService.descriptionPart1 + passingPercentage + BadgeService.descriptionPart2 + "quiz Quiz 1"
                    + BadgeService.descriptionPart3, badge.getDescription());
        }

        List<UserBadgeEntity> allUserBadges = userBadgeRepository.findAll();
        assertEquals(6, allUserBadges.size());
        for (UserBadgeEntity userBadge : allUserBadges) {
            assertNotNull(userBadge.getUserBadgeUUID());
            assertTrue(badgeRepository.findById(userBadge.getBadgeUUID()).isPresent());
            assertTrue(lecturerUUID.equals(userBadge.getUserUUID()) || user1.equals(userBadge.getUserUUID()));
            assertFalse(userBadge.isAchieved());
        }


        // Second, add second user to already existing course with already existin badges and quests
        UUID user2 = UUID.randomUUID();
        assertEquals("Added user to course.", gamificationController.addUserToCourse(user2, courseUUID));

        courseEntity = courseRepository.findById(courseUUID);
        assertEquals(1, courseRepository.findAll().size());
        assertTrue(courseEntity.isPresent());
        assertEquals(3, courseEntity.get().getUserUUIDs().size());

        allBadges = badgeRepository.findAll();
        assertEquals(3, allBadges.size());

        allUserBadges = userBadgeRepository.findAll();
        assertEquals(9, allUserBadges.size());
        for (UserBadgeEntity userBadge : allUserBadges) {
            assertNotNull(userBadge.getUserBadgeUUID());
            assertTrue(badgeRepository.findById(userBadge.getBadgeUUID()).isPresent());
            assertTrue(lecturerUUID.equals(userBadge.getUserUUID())
                    || user1.equals(userBadge.getUserUUID())
                    || user2.equals(userBadge.getUserUUID()));
            assertFalse(userBadge.isAchieved());
        }

        questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertEquals(1, questChainRepository.findAll().size());
        assertNotNull(questChainEntity);
        assertEquals(1, questChainEntity.getQuests().size());

        userQuestChainEntity1 = userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), lecturerUUID);
        userQuestChainEntity2 = userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), user1);
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
    void deleteBadgesAndQuestsOfCourseTest(@Argument UUID courseUUID) {
        courseService.deleteCourse(courseUUID);
        badgeService.deleteBadgesAndUserBadgesOfCourse(courseUUID);
        questService.deleteQuestChainAndUserQuestChainsOfCourse(courseUUID);
        return "Course deleted.";
    }
    */

    @Test
    void getCoursesUserBadgesTest() {
        UUID courseUUID = UUID.randomUUID();
        UUID lecturerUUID = UUID.randomUUID();
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        UUID quizUUID = UUID.randomUUID();
        UUID flashcardSetUUID = UUID.randomUUID();

        TestUtils.createTestCourse(gamificationController,
                courseUUID,
                lecturerUUID,
                user1,
                user2,
                quizUUID,
                flashcardSetUUID);

        List<UserBadge> lecturersUserBadges = gamificationController.getCoursesUserBadges(courseUUID, lecturerUUID);
        List<UserBadge> user1UserBadges = gamificationController.getCoursesUserBadges(courseUUID, user1);

        gamificationController.finishQuiz(user2, courseUUID, quizUUID, 5, 5);
        gamificationController.finishFlashCardSet(user2, courseUUID, flashcardSetUUID, 5, 5);
        List<UserBadge> user2UserBadges = gamificationController.getCoursesUserBadges(courseUUID, user2);

        assertEquals(6, lecturersUserBadges.size());
        assertEquals(6, user1UserBadges.size());
        assertEquals(6, user2UserBadges.size());

        for (UserBadge badge : lecturersUserBadges) {
            assertTrue(badgeRepository.findById(badge.getBadgeUUID()).isPresent());
            assertEquals(lecturerUUID, badge.getUserUUID());
            assertTrue(badge.getDescription().equals(BadgeService.descriptionPart1 + badge.getPassingPercentage()
                            + BadgeService.descriptionPart2 + "quiz Quiz 1" + BadgeService.descriptionPart3) ||
                       badge.getDescription().equals(BadgeService.descriptionPart1 + badge.getPassingPercentage()
                               + BadgeService.descriptionPart2 + "flashcardSet FCS 1" + BadgeService.descriptionPart3)
                    );
            assertFalse(badge.getAchieved());
        }

        for (UserBadge badge : user1UserBadges) {
            assertTrue(badgeRepository.findById(badge.getBadgeUUID()).isPresent());
            assertEquals(user1, badge.getUserUUID());
            assertTrue(badge.getDescription().equals(BadgeService.descriptionPart1 + badge.getPassingPercentage()
                    + BadgeService.descriptionPart2 + "quiz Quiz 1" + BadgeService.descriptionPart3) ||
                    badge.getDescription().equals(BadgeService.descriptionPart1 + badge.getPassingPercentage()
                            + BadgeService.descriptionPart2 + "flashcardSet FCS 1" + BadgeService.descriptionPart3)
            );
            assertFalse(badge.getAchieved());
        }

        for (UserBadge badge : user2UserBadges) {
            assertTrue(badgeRepository.findById(badge.getBadgeUUID()).isPresent());
            assertEquals(user2, badge.getUserUUID());
            assertTrue(badge.getDescription().equals(BadgeService.descriptionPart1 + badge.getPassingPercentage()
                    + BadgeService.descriptionPart2 + "quiz Quiz 1" + BadgeService.descriptionPart3) ||
                    badge.getDescription().equals(BadgeService.descriptionPart1 + badge.getPassingPercentage()
                            + BadgeService.descriptionPart2 + "flashcardSet FCS 1" + BadgeService.descriptionPart3)
            );
            assertTrue(badge.getAchieved());
        }
    }

    @Test
    void getCurrentUserQuestTest() {
        UUID courseUUID = UUID.randomUUID();
        UUID lecturerUUID = UUID.randomUUID();
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        UUID quizUUID = UUID.randomUUID();
        UUID flashcardSetUUID = UUID.randomUUID();

        TestUtils.createTestCourse(gamificationController,
                courseUUID,
                lecturerUUID,
                user1,
                user2,
                quizUUID,
                flashcardSetUUID);

        gamificationController.finishQuiz(lecturerUUID, courseUUID, quizUUID, 5, 5);
        Quest lecturerQuest = gamificationController.getCurrentUserQuest(lecturerUUID, courseUUID);

        gamificationController.finishFlashCardSet(user1, courseUUID, flashcardSetUUID, 5, 5);
        Quest user1Quest = gamificationController.getCurrentUserQuest(user1, courseUUID);

        gamificationController.finishQuiz(user2, courseUUID, quizUUID, 5, 5);
        gamificationController.finishFlashCardSet(user2, courseUUID, flashcardSetUUID, 5, 5);
        Quest user2Quest = gamificationController.getCurrentUserQuest(user2, courseUUID);

        assertFalse(lecturerQuest.getFinished());
        assertNull(lecturerQuest.getQuizUUID());
        assertEquals(flashcardSetUUID, lecturerQuest.getFlashCardSetUUID());
        assertEquals(1, lecturerQuest.getLevel());
        assertEquals(QuestService.descriptionPart1 + "flashcardSet FCS 1" +
                QuestService.descriptionPart2 + 80 + QuestService.descriptionPart3, lecturerQuest.getDescription());

        assertFalse(user1Quest.getFinished());
        assertNull(user1Quest.getFlashCardSetUUID());
        assertEquals(quizUUID, user1Quest.getQuizUUID());
        assertEquals(0, user1Quest.getLevel());
        assertEquals(QuestService.descriptionPart1 + "quiz Quiz 1" +
                QuestService.descriptionPart2 + 80 + QuestService.descriptionPart3, user1Quest.getDescription());

        assertTrue(user2Quest.getFinished());
        assertNull(user2Quest.getQuizUUID());
        assertNull(user2Quest.getFlashCardSetUUID());
        assertEquals(2, user2Quest.getLevel());
        assertEquals("You finished all quests for this course!", user2Quest.getDescription());

    }

    @Test
    void getUserQuestChainTest() {
        UUID courseUUID = UUID.randomUUID();
        UUID lecturerUUID = UUID.randomUUID();
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        UUID quizUUID = UUID.randomUUID();
        UUID flashcardSetUUID = UUID.randomUUID();

        TestUtils.createTestCourse(gamificationController,
                courseUUID,
                lecturerUUID,
                user1,
                user2,
                quizUUID,
                flashcardSetUUID);

        gamificationController.finishQuiz(lecturerUUID, courseUUID, quizUUID, 5, 5);
        UserQuestChain lecturerQuestChain = gamificationController.getUserQuestChain(lecturerUUID, courseUUID);

        gamificationController.finishFlashCardSet(user1, courseUUID, flashcardSetUUID, 5, 5);
        UserQuestChain user1QuestChain = gamificationController.getUserQuestChain(user1, courseUUID);

        gamificationController.finishQuiz(user2, courseUUID, quizUUID, 5, 5);
        gamificationController.finishFlashCardSet(user2, courseUUID, flashcardSetUUID, 5, 5);
        UserQuestChain user2QuestChain = gamificationController.getUserQuestChain(user2, courseUUID);

        assertTrue(questChainRepository.findById(lecturerQuestChain.getQuestChainUUID()).isPresent());
        assertEquals(lecturerUUID, lecturerQuestChain.getUserUUID());
        assertNotNull(lecturerQuestChain.getQuests());
        assertEquals(2, lecturerQuestChain.getQuests().size());
        assertEquals(1, lecturerQuestChain.getUserLevel());
        assertFalse(lecturerQuestChain.getFinished());

        assertTrue(questChainRepository.findById(user1QuestChain.getQuestChainUUID()).isPresent());
        assertEquals(user1, user1QuestChain.getUserUUID());
        assertNotNull(user1QuestChain.getQuests());
        assertEquals(2, user1QuestChain.getQuests().size());
        assertEquals(0, user1QuestChain.getUserLevel());
        assertFalse(user1QuestChain.getFinished());

        assertTrue(questChainRepository.findById(user2QuestChain.getQuestChainUUID()).isPresent());
        assertEquals(user2, user2QuestChain.getUserUUID());
        assertNotNull(user2QuestChain.getQuests());
        assertEquals(2, user2QuestChain.getQuests().size());
        assertEquals(2, user2QuestChain.getUserLevel());
        assertTrue(user2QuestChain.getFinished());
    }

/*
    @Test
    void removeUserFromCourseTest(@Argument UUID userUUID, @Argument UUID courseUUID) {
        courseService.removeUserFromCourse(userUUID, courseUUID);
        badgeService.deleteUserBadgesOfCourse(userUUID, courseUUID);
        questService.deleteUserQuestChain(userUUID, courseUUID);
        return "Removed user from course.";
    }
    */

}