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

    private UUID courseUUID;
    private UUID lecturerUUID;
    private UUID user1UUID;
    private UUID user2UUID;
    private UUID quizUUID;
    private UUID flashCardSetUUID;

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
    void addCourseTest() {

        UUID course = UUID.randomUUID();
        UUID lecturer = UUID.randomUUID();
        assertEquals("Added course.", gamificationController.addCourse(course, lecturer));

        Optional<CourseEntity> courseEntity = courseRepository.findById(course);
        assertEquals(1, courseRepository.findAll().size());
        assertTrue(courseEntity.isPresent());
        assertEquals(1, courseEntity.get().getUserUUIDs().size());

        assertEquals(0, badgeRepository.findAll().size());
        assertEquals(0, userBadgeRepository.findAll().size());

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(course);
        assertEquals(1, questChainRepository.findAll().size());
        assertNotNull(questChainEntity);
        assertEquals(course, questChainEntity.getCourseUUID());
        assertTrue(questChainEntity.getQuests().isEmpty());

        UserQuestChainEntity userQuestChainEntity = userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), lecturer);
        assertEquals(1, userQuestChainRepository.findAll().size());
        assertNotNull(userQuestChainEntity);
        assertEquals(lecturer, userQuestChainEntity.getUserUUID());
        assertEquals(questChainEntity.getQuestChainUUID(), userQuestChainEntity.getQuestChainUUID());
        assertEquals(0, userQuestChainEntity.getUserLevel());

    }

    @Test
    void addUserToCourseTest() {
        // First, create course
        UUID course = UUID.randomUUID();
        UUID lecturer = UUID.randomUUID();
        gamificationController.addCourse(course, lecturer);

        // Then, add user to it
        UUID user1 = UUID.randomUUID();
        assertEquals("Added user to course.", gamificationController.addUserToCourse(user1, course));

        Optional<CourseEntity> courseEntity = courseRepository.findById(course);
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getUserUUIDs().size());

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(course);
        assertEquals(1, questChainRepository.findAll().size());
        assertNotNull(questChainEntity);
        assertTrue(questChainEntity.getQuests().isEmpty());

        UserQuestChainEntity userQuestChainEntity1 =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), lecturer);
        UserQuestChainEntity userQuestChainEntity2 =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), user1);
        assertEquals(2, userQuestChainRepository.findAll().size());
        assertNotNull(userQuestChainEntity1);
        assertNotNull(userQuestChainEntity2);
        assertEquals(0, userQuestChainEntity1.getUserLevel());
        assertEquals(0, userQuestChainEntity2.getUserLevel());
        assertEquals(lecturer, userQuestChainEntity1.getUserUUID());
        assertEquals(user1, userQuestChainEntity2.getUserUUID());
        assertEquals(questChainEntity.getQuestChainUUID(), userQuestChainEntity1.getQuestChainUUID());
        assertEquals(questChainEntity.getQuestChainUUID(), userQuestChainEntity2.getQuestChainUUID());


        // Create quiz in course
        UUID quiz = UUID.randomUUID();
        gamificationController.createQuiz(quiz, "Quiz 1", course);

        List<BadgeEntity> allBadges = badgeRepository.findAll();
        assertEquals(3, allBadges.size());
        for (BadgeEntity badge : allBadges) {
            int passingPercentage = badge.getPassingPercentage();
            assertEquals(course, badge.getCourseUUID());
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
            assertTrue(lecturer.equals(userBadge.getUserUUID()) || user1.equals(userBadge.getUserUUID()));
            assertFalse(userBadge.isAchieved());
        }


        // Second, add second user to already existing course with already existing badges and quests
        UUID user2 = UUID.randomUUID();
        assertEquals("Added user to course.", gamificationController.addUserToCourse(user2, course));

        courseEntity = courseRepository.findById(course);
        assertTrue(courseEntity.isPresent());
        assertEquals(3, courseEntity.get().getUserUUIDs().size());

        allUserBadges = userBadgeRepository.findAll();
        assertEquals(9, allUserBadges.size());
        for (UserBadgeEntity userBadge : allUserBadges) {
            assertNotNull(userBadge.getUserBadgeUUID());
            assertTrue(badgeRepository.findById(userBadge.getBadgeUUID()).isPresent());
            assertTrue(lecturer.equals(userBadge.getUserUUID())
                    || user1.equals(userBadge.getUserUUID())
                    || user2.equals(userBadge.getUserUUID()));
            assertFalse(userBadge.isAchieved());
        }

        questChainEntity = questChainRepository.findByCourseUUID(course);
        assertEquals(1, questChainEntity.getQuests().size());

        UserQuestChainEntity userQuestChainEntity3 =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), user2);
        assertEquals(3, userQuestChainRepository.findAll().size());
        assertNotNull(userQuestChainEntity3);
        assertEquals(0, userQuestChainEntity3.getUserLevel());

    }

    @Test
    void deleteBadgesAndQuestsOfCourseTest() {
        createTestCourse();

        assertEquals("Course deleted.", gamificationController.deleteBadgesAndQuestsOfCourse(courseUUID));

        assertEquals(0, courseRepository.findAll().size());
        assertEquals(0, badgeRepository.findAll().size());
        assertEquals(0, userBadgeRepository.findAll().size());
        assertEquals(0, questChainRepository.findAll().size());
        assertEquals(0, userQuestChainRepository.findAll().size());
    }

    @Test
    void getCoursesUserBadgesTest() {
        createTestCourse();

        List<UserBadge> lecturersUserBadges = gamificationController.getCoursesUserBadges(courseUUID, lecturerUUID);
        List<UserBadge> user1UserBadges = gamificationController.getCoursesUserBadges(courseUUID, user1UUID);

        gamificationController.finishQuiz(user2UUID, courseUUID, quizUUID, 5, 5);
        gamificationController.finishFlashCardSet(user2UUID, courseUUID, flashCardSetUUID, 5, 5);
        List<UserBadge> user2UserBadges = gamificationController.getCoursesUserBadges(courseUUID, user2UUID);

        assertEquals(6, lecturersUserBadges.size());
        assertEquals(6, user1UserBadges.size());
        assertEquals(6, user2UserBadges.size());

        for (UserBadge badge : lecturersUserBadges) {
            assertTrue(badgeRepository.findById(badge.getBadgeUUID()).isPresent());
            assertEquals(lecturerUUID, badge.getUserUUID());
            assertTrue(badge.getDescription().equals(BadgeService.descriptionPart1 + badge.getPassingPercentage()
                            + BadgeService.descriptionPart2 + "quiz Quiz 1" + BadgeService.descriptionPart3) ||
                       badge.getDescription().equals(BadgeService.descriptionPart1 + badge.getPassingPercentage()
                               + BadgeService.descriptionPart2 + "flashCardSet FCS 1" + BadgeService.descriptionPart3)
                    );
            assertFalse(badge.getAchieved());
        }

        for (UserBadge badge : user1UserBadges) {
            assertTrue(badgeRepository.findById(badge.getBadgeUUID()).isPresent());
            assertEquals(user1UUID, badge.getUserUUID());
            assertTrue(badge.getDescription().equals(BadgeService.descriptionPart1 + badge.getPassingPercentage()
                    + BadgeService.descriptionPart2 + "quiz Quiz 1" + BadgeService.descriptionPart3) ||
                    badge.getDescription().equals(BadgeService.descriptionPart1 + badge.getPassingPercentage()
                            + BadgeService.descriptionPart2 + "flashCardSet FCS 1" + BadgeService.descriptionPart3)
            );
            assertFalse(badge.getAchieved());
        }

        for (UserBadge badge : user2UserBadges) {
            assertTrue(badgeRepository.findById(badge.getBadgeUUID()).isPresent());
            assertEquals(user2UUID, badge.getUserUUID());
            assertTrue(badge.getDescription().equals(BadgeService.descriptionPart1 + badge.getPassingPercentage()
                    + BadgeService.descriptionPart2 + "quiz Quiz 1" + BadgeService.descriptionPart3) ||
                    badge.getDescription().equals(BadgeService.descriptionPart1 + badge.getPassingPercentage()
                            + BadgeService.descriptionPart2 + "flashCardSet FCS 1" + BadgeService.descriptionPart3)
            );
            assertTrue(badge.getAchieved());
        }
    }

    @Test
    void getCurrentUserQuestTest() {
        createTestCourse();

        gamificationController.finishQuiz(lecturerUUID, courseUUID, quizUUID, 5, 5);
        Quest lecturerQuest = gamificationController.getCurrentUserQuest(lecturerUUID, courseUUID);

        gamificationController.finishFlashCardSet(user1UUID, courseUUID, flashCardSetUUID, 5, 5);
        Quest user1Quest = gamificationController.getCurrentUserQuest(user1UUID, courseUUID);

        gamificationController.finishQuiz(user2UUID, courseUUID, quizUUID, 5, 5);
        gamificationController.finishFlashCardSet(user2UUID, courseUUID, flashCardSetUUID, 5, 5);
        Quest user2Quest = gamificationController.getCurrentUserQuest(user2UUID, courseUUID);

        assertFalse(lecturerQuest.getFinished());
        assertNull(lecturerQuest.getQuizUUID());
        assertEquals(flashCardSetUUID, lecturerQuest.getFlashCardSetUUID());
        assertEquals(1, lecturerQuest.getLevel());
        assertEquals(QuestService.descriptionPart1 + "flashCardSet FCS 1" +
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
        createTestCourse();

        gamificationController.finishQuiz(lecturerUUID, courseUUID, quizUUID, 5, 5);
        UserQuestChain lecturerQuestChain = gamificationController.getUserQuestChain(lecturerUUID, courseUUID);

        gamificationController.finishFlashCardSet(user1UUID, courseUUID, flashCardSetUUID, 5, 5);
        UserQuestChain user1QuestChain = gamificationController.getUserQuestChain(user1UUID, courseUUID);

        gamificationController.finishQuiz(user2UUID, courseUUID, quizUUID, 5, 5);
        gamificationController.finishFlashCardSet(user2UUID, courseUUID, flashCardSetUUID, 5, 5);
        UserQuestChain user2QuestChain = gamificationController.getUserQuestChain(user2UUID, courseUUID);

        assertTrue(questChainRepository.findById(lecturerQuestChain.getQuestChainUUID()).isPresent());
        assertEquals(lecturerUUID, lecturerQuestChain.getUserUUID());
        assertNotNull(lecturerQuestChain.getQuests());
        assertEquals(2, lecturerQuestChain.getQuests().size());
        assertEquals(1, lecturerQuestChain.getUserLevel());
        assertFalse(lecturerQuestChain.getFinished());

        assertTrue(questChainRepository.findById(user1QuestChain.getQuestChainUUID()).isPresent());
        assertEquals(user1UUID, user1QuestChain.getUserUUID());
        assertNotNull(user1QuestChain.getQuests());
        assertEquals(2, user1QuestChain.getQuests().size());
        assertEquals(0, user1QuestChain.getUserLevel());
        assertFalse(user1QuestChain.getFinished());

        assertTrue(questChainRepository.findById(user2QuestChain.getQuestChainUUID()).isPresent());
        assertEquals(user2UUID, user2QuestChain.getUserUUID());
        assertNotNull(user2QuestChain.getQuests());
        assertEquals(2, user2QuestChain.getQuests().size());
        assertEquals(2, user2QuestChain.getUserLevel());
        assertTrue(user2QuestChain.getFinished());
    }

    @Test
    void removeUserFromCourseTest() {
        createTestCourse();

        assertEquals("Removed user from course.", gamificationController.removeUserFromCourse(lecturerUUID, courseUUID));

        Optional<CourseEntity> course = courseRepository.findById(courseUUID);
        assertEquals(1, courseRepository.findAll().size());
        assertTrue(course.isPresent());
        assertEquals(2, course.get().getUserUUIDs().size());
        assertTrue(course.get().getUserUUIDs().contains(user1UUID));
        assertTrue(course.get().getUserUUIDs().contains(user2UUID));

        List<UserBadgeEntity> allUserBadges = userBadgeRepository.findAll();
        assertEquals(12, allUserBadges.size());
        for (UserBadgeEntity userBadge : allUserBadges) {
            assertNotEquals(lecturerUUID, userBadge.getUserUUID());
        }

        List<UserQuestChainEntity> allUserQuests = userQuestChainRepository.findAll();
        assertEquals(2, allUserQuests.size());
        for (UserQuestChainEntity userQuestChain : allUserQuests) {
            assertNotEquals(lecturerUUID, userQuestChain.getUserUUID());
        }

    }

}