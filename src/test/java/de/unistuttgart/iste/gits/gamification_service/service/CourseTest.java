package de.unistuttgart.iste.gits.gamification_service.service;

import de.unistuttgart.iste.gits.gamification_service.test_utils.TestUtils;
import de.unistuttgart.iste.meitrex.gamification_service.GamificationApplication;
import de.unistuttgart.iste.meitrex.gamification_service.controller.GamificationController;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.*;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.*;
import de.unistuttgart.iste.meitrex.gamification_service.service.*;
import de.unistuttgart.iste.meitrex.generated.dto.Quest;
import de.unistuttgart.iste.meitrex.generated.dto.SkillType;
import de.unistuttgart.iste.meitrex.generated.dto.UserBadge;
import de.unistuttgart.iste.meitrex.generated.dto.UserQuestChain;
import org.junit.jupiter.api.BeforeEach;
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

/**
 * Unit tests for the course functionalities in the GamificationService.
 * <p>
 * This test class contains unit tests to verify the functionality of the course-related operations
 * within the GamificationController. It includes tests for adding a course, adding a user to a course,
 * removing a user from a course, deleting a course, retrieving the courses userBadges of a course member,
 * their current quest, or their total questChain.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = GamificationApplication.class)
@Transactional // Each test method runs in a transaction that is rolled back after the test completes
class CourseTest {

    // Required to run tests for the repositories
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("testdb")
            .withUsername("root")
            .withPassword("root");

    /**
     * Starts the PostgreSQL container before all tests are executed.
     * <p>
     * This method is responsible for initializing the PostgreSQL container which is required
     * to run the repository tests.
     */
    @BeforeAll
    static void startContainer() {
        postgres.start();
    }

    /**
     * Configures the database properties for the tests.
     * <p>
     * This method sets the necessary database connection properties dynamically using the PostgreSQL container.
     *
     * @param registry the registry to add the dynamic properties to
     */
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
    private BloomLevelRepository bloomLevelRepository;

    @Autowired
    private ContentMetaDataRepository contentMetaDataRepository;

    @Autowired
    private GamificationController gamificationController;


    private UUID courseUUID;
    private UUID lecturerUUID;
    private UUID user1UUID;
    private UUID user2UUID;
    private UUID quizUUID;
    private UUID flashCardSetUUID;
    private UUID chapterUUID;

    /**
     * Sets up a test course before each test.
     * <p>
     * This method initializes the necessary UUIDs and creates a test course with 3 users, 1 quiz,
     * and 1 flashCardSet using the {@code TestUtils.createTestCourse} method.
     */
    @BeforeEach
    void createTestCourse() {
        this.courseUUID = UUID.randomUUID();
        this.lecturerUUID = UUID.randomUUID();
        this.user1UUID = UUID.randomUUID();
        this.user2UUID = UUID.randomUUID();
        this.quizUUID = UUID.randomUUID();
        this.flashCardSetUUID = UUID.randomUUID();
        this.chapterUUID = UUID.randomUUID();

        TestUtils.createTestCourse(gamificationController,
                courseUUID,
                lecturerUUID,
                user1UUID,
                user2UUID,
                quizUUID,
                flashCardSetUUID,
                chapterUUID);
    }

    /**
     * Tests the setup of a test course {@code createTestCourse} and verifies that all related entities
     * (course, badges, quests, etc.) are correctly created and associated.
     * <p>
     * This test ensures that:
     * <ul>
     *   <li>The course exists and contains the correct users (lecturer, user1, user2).</li>
     *   <li>All badges related to the course and its components (quiz, flashcard set) are correctly created.</li>
     *   <li>All user badges are associated with the correct users and marked as not achieved.</li>
     *   <li>The quest chain and quests are properly linked to the course and the content.</li>
     *   <li>Each user has their own quest chain with the correct initial settings.</li>
     *   <li>The contentMetaData of the quiz and the flashCardSet is correct.</li>
     * </ul>
     */
    @Test
    void createTestCourseTest() {
        assertEquals(1, courseRepository.count());
        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());

        assertEquals(3, courseEntity.get().getUserUUIDs().size());
        assertTrue(courseEntity.get().getUserUUIDs().contains(lecturerUUID));
        assertTrue(courseEntity.get().getUserUUIDs().contains(user1UUID));
        assertTrue(courseEntity.get().getUserUUIDs().contains(user2UUID));

        assertEquals(1, courseEntity.get().getChapters().size());
        assertTrue(courseEntity.get().getChapters().contains(chapterUUID));

        assertEquals(2, courseEntity.get().getContent().size());
        assertTrue(courseEntity.get().getContent().contains(flashCardSetUUID));
        assertTrue(courseEntity.get().getContent().contains(quizUUID));

        assertEquals(6, badgeRepository.count());
        for (BadgeEntity badgeEntity : badgeRepository.findAll()) {
            assertEquals(courseUUID, badgeEntity.getCourseUUID());
            assertTrue(quizUUID.equals(badgeEntity.getQuizUUID())
                    || flashCardSetUUID.equals(badgeEntity.getFlashCardSetUUID()));
            assertTrue(50 == badgeEntity.getPassingPercentage()
            || 70 == badgeEntity.getPassingPercentage()
            || 90 == badgeEntity.getPassingPercentage());
            assertTrue(badgeEntity.getDescription().equals(BadgeService.descriptionPart1 +
                    badgeEntity.getPassingPercentage() + BadgeService.descriptionPart2 + "quiz Quiz 1" + BadgeService.descriptionPart3) ||
                    badgeEntity.getDescription().equals(BadgeService.descriptionPart1 +
                    badgeEntity.getPassingPercentage() + BadgeService.descriptionPart2 + "flashCardSet FCS 1" + BadgeService.descriptionPart3));
        }

        assertEquals(18, userBadgeRepository.count());
        for (UserBadgeEntity userBadgeEntity : userBadgeRepository.findAll()) {
            assertTrue(badgeRepository.existsById(userBadgeEntity.getBadgeUUID()));
            assertTrue(lecturerUUID.equals(userBadgeEntity.getUserUUID())
            || user1UUID.equals(userBadgeEntity.getUserUUID())
            || user2UUID.equals(userBadgeEntity.getUserUUID()));
            assertFalse(userBadgeEntity.isAchieved());
        }

        assertEquals(1, questChainRepository.count());
        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertNotNull(questChainEntity);
        assertEquals(courseUUID, questChainEntity.getCourseUUID());
        assertEquals(2, questChainEntity.size());

        QuestEntity firstQuest = questChainEntity.getQuest(0);
        assertEquals(quizUUID, firstQuest.getQuizUUID());
        assertNull(firstQuest.getFlashCardSetUUID());
        assertEquals(QuestService.descriptionPart1 + "quiz Quiz 1" + QuestService.descriptionPart2
                + QuestService.passingPercentage + QuestService.descriptionPart3, firstQuest.getDescription());

        QuestEntity secondQuest = questChainEntity.getQuest(1);
        assertEquals(flashCardSetUUID, secondQuest.getFlashCardSetUUID());
        assertNull(secondQuest.getQuizUUID());
        assertEquals(QuestService.descriptionPart1 + "flashCardSet FCS 1" + QuestService.descriptionPart2
                + QuestService.passingPercentage + QuestService.descriptionPart3, secondQuest.getDescription());

        assertEquals(3, userQuestChainRepository.count());
        for (UserQuestChainEntity userQuestChainEntity : userQuestChainRepository.findAll()) {
            assertTrue(questChainRepository.existsById(userQuestChainEntity.getQuestChainUUID()));
            assertTrue(lecturerUUID.equals(userQuestChainEntity.getUserUUID())
            || user1UUID.equals(userQuestChainEntity.getUserUUID())
            || user2UUID.equals(userQuestChainEntity.getUserUUID()));
            assertEquals(0, userQuestChainEntity.getUserLevel());
        }

        List<BloomLevelEntity> bloomLevelEntities = bloomLevelRepository.findAll();
        assertEquals(3, bloomLevelEntities.size());
        BloomLevelEntity lecturerBloomLevel = bloomLevelRepository.findByUserUUIDAndCourseUUID(lecturerUUID, courseUUID);
        BloomLevelEntity user1BloomLevel = bloomLevelRepository.findByUserUUIDAndCourseUUID(user1UUID, courseUUID);
        BloomLevelEntity user2BloomLevel = bloomLevelRepository.findByUserUUIDAndCourseUUID(user2UUID, courseUUID);
        assertNotNull(lecturerBloomLevel);
        assertNotNull(user1BloomLevel);
        assertNotNull(user2BloomLevel);
        assertEquals(0, lecturerBloomLevel.getCollectedExp());
        assertEquals(0, user1BloomLevel.getCollectedExp());
        assertEquals(0, user2BloomLevel.getCollectedExp());

        assertEquals(2, contentMetaDataRepository.findAll().size());
        Optional<ContentMetaDataEntity> flashCardSetMetaData = contentMetaDataRepository.findById(flashCardSetUUID);
        Optional<ContentMetaDataEntity> quizMetaData = contentMetaDataRepository.findById(quizUUID);

        assertTrue(flashCardSetMetaData.isPresent());
        assertEquals(SkillType.APPLY, flashCardSetMetaData.get().getSkillType());
        assertEquals(60, flashCardSetMetaData.get().getSkillPoints());

        assertTrue(quizMetaData.isPresent());
        assertEquals(SkillType.ANALYSE, quizMetaData.get().getSkillType());
        assertEquals(50, quizMetaData.get().getSkillPoints());
    }

    /**
     * Tests the addition of a new course and verifies that the course, along with its quest chain
     * and the questChains for the users, is correctly created.
     * <p>
     * This test ensures that:
     * <ul>
     *   <li>A new course is successfully added to the repository.</li>
     *   <li>The course is associated with the correct lecturer.</li>
     *   <li>An empty quest chain is created for the course.</li>
     *   <li>The lecturer has a corresponding user quest chain with initial settings.</li>
     *   <li>The bloomLevel of the lecturer is initialized correctly.</li>
     * </ul>
     */
    @Test
    void addCourseTest() {

        UUID course = UUID.randomUUID();
        UUID lecturer = UUID.randomUUID();
        UUID chapter = UUID.randomUUID();
        assertEquals("Added course.", gamificationController.addCourse(course, lecturer, new LinkedList<>(List.of(chapter))));

        Optional<CourseEntity> courseEntity = courseRepository.findById(course);
        assertEquals(2, courseRepository.findAll().size());
        assertTrue(courseEntity.isPresent());
        assertEquals(1, courseEntity.get().getUserUUIDs().size());
        assertTrue(courseEntity.get().getUserUUIDs().contains(lecturer));

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(course);
        assertEquals(2, questChainRepository.findAll().size());
        assertNotNull(questChainEntity);
        assertEquals(course, questChainEntity.getCourseUUID());
        assertEquals(0, questChainEntity.size());

        UserQuestChainEntity userQuestChainEntity = userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), lecturer);
        assertEquals(4, userQuestChainRepository.findAll().size());
        assertNotNull(userQuestChainEntity);
        assertEquals(lecturer, userQuestChainEntity.getUserUUID());
        assertEquals(questChainEntity.getQuestChainUUID(), userQuestChainEntity.getQuestChainUUID());
        assertEquals(0, userQuestChainEntity.getUserLevel());

        List<BloomLevelEntity> bloomLevelEntities = bloomLevelRepository.findAll();
        assertEquals(4, bloomLevelEntities.size());
        BloomLevelEntity lecturerBloomLevel = bloomLevelRepository.findByUserUUIDAndCourseUUID(lecturer, course);
        assertNotNull(lecturerBloomLevel);
        assertEquals(0, lecturerBloomLevel.getCollectedExp());
    }

    /**
     * Tests the addition of a new user to an existing course and verifies that the user and their
     * associated entities (badges, quest chain) are created correctly.
     * <p>
     * This test ensures that:
     * <ul>
     *   <li>A new user is successfully added to an existing course.</li>
     *   <li>The course is updated to include the new user.</li>
     *   <li>The new user has a quest chain associated with the course.</li>
     *   <li>The new user is assigned the correct badges for the course, all marked as not achieved.</li>
     *   <li>The bloomLevel of the joined user is initialized correctly.</li>
     * </ul>
     */
    @Test
    void addUserToCourseTest() {
        UUID user = UUID.randomUUID();
        assertEquals("Added user to course.", gamificationController.addUserToCourse(user, courseUUID));

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(4, courseEntity.get().getUserUUIDs().size());

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        UserQuestChainEntity userQuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), user);
        assertEquals(4, userQuestChainRepository.findAll().size());
        assertNotNull(userQuestChainEntity);
        assertEquals(0, userQuestChainEntity.getUserLevel());
        assertEquals(user, userQuestChainEntity.getUserUUID());
        assertEquals(questChainEntity.getQuestChainUUID(), userQuestChainEntity.getQuestChainUUID());

        List<UserBadgeEntity> allUserBadges = userBadgeRepository.findByUserUUID(user);
        assertEquals(6, allUserBadges.size());
        for (UserBadgeEntity userBadge : allUserBadges) {
            assertTrue(badgeRepository.findById(userBadge.getBadgeUUID()).isPresent());
            assertEquals(user, userBadge.getUserUUID());
            assertFalse(userBadge.isAchieved());
        }

        List<BloomLevelEntity> bloomLevelEntities = bloomLevelRepository.findAll();
        assertEquals(4, bloomLevelEntities.size());
        BloomLevelEntity lecturerBloomLevel = bloomLevelRepository.findByUserUUIDAndCourseUUID(user, courseUUID);
        assertNotNull(lecturerBloomLevel);
        assertEquals(0, lecturerBloomLevel.getCollectedExp());
    }

    /**
     * Tests the deletion of a course along with all associated badges, quests, and related user entities.
     * <p>
     * This test ensures that:
     * <ul>
     *   <li>The specified course is successfully deleted from the repository.</li>
     *   <li>All badges, user badges, quest chains, and user quest chains associated with the course are also deleted.</li>
     *   <li>No remaining entities related to the course exist in their respective repositories.</li>
     * </ul>
     */
    @Test
    void deleteBadgesAndQuestsOfCourseTest() {
        assertEquals("Course deleted.", gamificationController.deleteBadgesAndQuestsOfCourse(courseUUID));

        assertFalse(courseRepository.findById(courseUUID).isPresent());

        assertEquals(0, courseRepository.findAll().size());
        assertEquals(0, badgeRepository.findAll().size());
        assertEquals(0, userBadgeRepository.findAll().size());
        assertEquals(0, questChainRepository.findAll().size());
        assertEquals(0, userQuestChainRepository.findAll().size());
        assertEquals(0, bloomLevelRepository.findAll().size());
        assertEquals(0, contentMetaDataRepository.findAll().size());
    }

    /**
     * Tests the retrieval of a users badges for a course.
     * <p>
     * This test verifies that a user can successfully retrieve all badges associated with them.
     * It checks that the correct number of badges is returned, each badge is associated with the
     * correct user, and that none of the badges have been achieved yet.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>The user retrieves exactly six badges.</li>
     *   <li>All badges are associated with the correct user.</li>
     *   <li>None of the badges have been marked as achieved yet.</li>
     * </ul>
     */
    @Test
    void getCoursesUserBadgesTest() {
        List<UserBadge> lecturersUserBadges = gamificationController.getCoursesUserBadges(courseUUID, lecturerUUID);
        List<UserBadge> user1UserBadges = gamificationController.getCoursesUserBadges(courseUUID, user1UUID);

        gamificationController.finishQuiz(user2UUID, courseUUID, quizUUID, 5, 5, chapterUUID);
        gamificationController.finishFlashCardSet(user2UUID, courseUUID, flashCardSetUUID, 5, 5, chapterUUID);
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
                               + BadgeService.descriptionPart2 + "flashCardSet FCS 1" + BadgeService.descriptionPart3));
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

    /**
     * Tests the retrieval of the current quest for different users after completing various tasks.
     * <p>
     * This test verifies that the current quest for each user is correctly updated and retrieved
     * after they complete quizzes and flashcard sets. It checks the state of the quest, including
     * whether it is finished, the associated quiz or flashcard set UUIDs, the user's level, and
     * the quest description.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>The lecturer's current quest is not finished, is associated with the flashcard set,
     *   and has a level of 1 with the correct description.</li>
     *   <li>User1's current quest is not finished, is associated with the quiz,
     *   and has a level of 0 with the correct description.</li>
     *   <li>User2's current quest is finished, is not associated with any quiz or flashcard set,
     *   and has a level of 2 with the completion message.</li>
     * </ul>
     */
    @Test
    void getCurrentUserQuestTest() {
        gamificationController.finishQuiz(lecturerUUID, courseUUID, quizUUID, 5, 5, chapterUUID);
        Quest lecturerQuest = gamificationController.getCurrentUserQuest(lecturerUUID, courseUUID);

        gamificationController.finishFlashCardSet(user1UUID, courseUUID, flashCardSetUUID, 5, 5, chapterUUID);
        Quest user1Quest = gamificationController.getCurrentUserQuest(user1UUID, courseUUID);

        gamificationController.finishQuiz(user2UUID, courseUUID, quizUUID, 5, 5, chapterUUID);
        gamificationController.finishFlashCardSet(user2UUID, courseUUID, flashCardSetUUID, 5, 5, chapterUUID);
        Quest user2Quest = gamificationController.getCurrentUserQuest(user2UUID, courseUUID);

        assertFalse(lecturerQuest.getFinished());
        assertNull(lecturerQuest.getQuizUUID());
        assertEquals(flashCardSetUUID, lecturerQuest.getFlashCardSetUUID());
        assertEquals(1, lecturerQuest.getLevel());
        assertEquals(QuestService.descriptionPart1 + "flashCardSet FCS 1" +
                QuestService.descriptionPart2 + 80 + QuestService.descriptionPart3, lecturerQuest.getDescription());

        assertFalse(user1Quest.getFinished());
        assertEquals(quizUUID, user1Quest.getQuizUUID());
        assertNull(user1Quest.getFlashCardSetUUID());
        assertEquals(0, user1Quest.getLevel());
        assertEquals(QuestService.descriptionPart1 + "quiz Quiz 1" +
                QuestService.descriptionPart2 + 80 + QuestService.descriptionPart3, user1Quest.getDescription());

        assertTrue(user2Quest.getFinished());
        assertNull(user2Quest.getQuizUUID());
        assertNull(user2Quest.getFlashCardSetUUID());
        assertEquals(2, user2Quest.getLevel());
        assertEquals("You finished all quests for this course!", user2Quest.getDescription());

    }

    /**
     * Tests the retrieval of a users questChain.
     * <p>
     * This test ensures that a user can successfully retrieve their associated questChain.
     * It verifies that the correct number of quests is returned and that each userQuestChain
     * contains the expected number of quests.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>The user retrieves exactly one quest chain.</li>
     *   <li>The quest chain contains two quests.</li>
     *   <li>The achieved quests have been marked as achieved.</li>
     * </ul>
     */
    @Test
    void getUserQuestChainTest() {
        gamificationController.finishQuiz(lecturerUUID, courseUUID, quizUUID, 5, 5, chapterUUID);
        UserQuestChain lecturerQuestChain = gamificationController.getUserQuestChain(lecturerUUID, courseUUID);

        gamificationController.finishFlashCardSet(user1UUID, courseUUID, flashCardSetUUID, 5, 5, chapterUUID);
        UserQuestChain user1QuestChain = gamificationController.getUserQuestChain(user1UUID, courseUUID);

        gamificationController.finishQuiz(user2UUID, courseUUID, quizUUID, 5, 5, chapterUUID);
        gamificationController.finishFlashCardSet(user2UUID, courseUUID, flashCardSetUUID, 5, 5, chapterUUID);
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

    /**
     * Tests the removal of a user from a course and ensures that related data is correctly updated.
     * <p>
     * This test checks if a user can be successfully removed from a course and verifies that the course's
     * user list is updated accordingly. It also ensures that any associated user badges and quest chains
     * are removed from the database for that user.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>The method returns a confirmation message indicating that the user was removed from the course.</li>
     *   <li>The course still exists in the repository, but its user list no longer includes the removed user.</li>
     *   <li>The total number of users in the course is reduced accordingly.</li>
     *   <li>All badges and quest chains associated with the removed user are deleted from the repositories.</li>
     *   <li>The bloomLevel of the user for the course is deleted.</li>
     * </ul>
     */
    @Test
    void removeUserFromCourseTest() {
        assertEquals("Removed user from course.", gamificationController.removeUserFromCourse(lecturerUUID, courseUUID));

        Optional<CourseEntity> course = courseRepository.findById(courseUUID);
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

        assertEquals(2, bloomLevelRepository.count());
        assertNull(bloomLevelRepository.findByUserUUIDAndCourseUUID(lecturerUUID, courseUUID));

    }

}