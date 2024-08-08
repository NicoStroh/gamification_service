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

    @Test
    void getCoursesUserBadgesTest(@Argument UUID courseUUID, @Argument UUID userUUID) {
        return badgeService.getUserBadgesByCourseUUID(courseUUID, userUUID);
    }

    @Test
    void getCurrentUserQuestTest(@Argument UUID userUUID, @Argument UUID courseUUID) {
        return questService.getCurrentUserQuest(userUUID, courseUUID);
    }

    @Test
    public UserQuestChain getUserQuestChainTest(@Argument UUID userUUID, @Argument UUID courseUUID) {
        return questService.getUserQuestChain(userUUID, courseUUID);
    }

    @Test
    void removeUserFromCourseTest(@Argument UUID userUUID, @Argument UUID courseUUID) {
        courseService.removeUserFromCourse(userUUID, courseUUID);
        badgeService.deleteUserBadgesOfCourse(userUUID, courseUUID);
        questService.deleteUserQuestChain(userUUID, courseUUID);
        return "Removed user from course.";
    }
    */

}