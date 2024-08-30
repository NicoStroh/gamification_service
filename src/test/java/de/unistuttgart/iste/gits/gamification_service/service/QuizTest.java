package de.unistuttgart.iste.gits.gamification_service.service;

import de.unistuttgart.iste.gits.gamification_service.test_utils.TestUtils;
import de.unistuttgart.iste.meitrex.gamification_service.GamificationApplication;
import de.unistuttgart.iste.meitrex.gamification_service.controller.GamificationController;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.*;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.*;
import de.unistuttgart.iste.meitrex.gamification_service.service.BadgeService;
import de.unistuttgart.iste.meitrex.gamification_service.service.QuestService;
import de.unistuttgart.iste.meitrex.generated.dto.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Unit tests for the Quiz functionalities in the GamificationService.
 * <p>
 * This test class contains unit tests to verify the functionality of the Quiz-related operations
 * within the GamificationController. It includes tests for creating quizzes, deleting quiz-related badges
 * and quests, editing quiz names, and finishing quizzes.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = GamificationApplication.class)
@Transactional // Each test method runs in a transaction that is rolled back after the test completes
class QuizTest {

    // Required to run tests for the repositories using Testcontainers
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
    private CourseRepository courseRepository;

    @Autowired
    private BadgeRepository badgeRepository;

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
     * Tests the creation of a new quiz.
     * <p>
     * This test verifies the creation of a quiz within a course and checks that the appropriate badges
     * are created and associated with the quiz. It also checks that the badges are properly linked to the users
     * of the course, and that the quest chain is updated accordingly.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>A new quiz is created successfully.</li>
     *   <li>The quiz is added to the content of the course.</li>
     *   <li>The required exp for the level of the chapter increased by 50.</li>
     *   <li>Three badges are associated with the new quiz.</li>
     *   <li>Each badge has the correct description and properties.</li>
     *   <li>Each user has the appropriate user badges initialized but not achieved.</li>
     *   <li>The quest chain is updated to include a new quest related to the quiz.</li>
     * </ul>
     */
    @Test
    void createQuizTest() {
        UUID quiz = UUID.randomUUID();
        String name = "Quiz 2";
        assertEquals("Created quiz successfully.",
                gamificationController.createQuiz(quiz, name, courseUUID, chapterUUID, 45, List.of(SkillType.REMEMBER)));

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(3, courseEntity.get().getContent().size());
        assertTrue(courseEntity.get().getContent().contains(quiz));
        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(77, courseEntity.get().getRequiredExpOfLevel(0));

        assertEquals(9, badgeRepository.count());
        List<BadgeEntity> quizBadges = badgeRepository.findByQuizUUID(quiz);
        assertEquals(3, quizBadges.size());

        List<UserBadgeEntity> quizUserBadges = new LinkedList<>();
        for (BadgeEntity badge : quizBadges) {
            int passingPercentage = badge.getPassingPercentage();
            assertEquals(courseUUID, badge.getCourseUUID());
            assertTrue(50 == passingPercentage || 70 == passingPercentage || 90 == passingPercentage);
            assertEquals(quiz, badge.getQuizUUID());
            assertNull(badge.getFlashCardSetUUID());
            assertEquals(BadgeService.descriptionPart1 + passingPercentage + BadgeService.descriptionPart2 +
                    "quiz " + name + BadgeService.descriptionPart3, badge.getDescription());

            quizUserBadges.addAll(userBadgeRepository.findByBadgeUUID(badge.getBadgeUUID()));
        }

        assertEquals(27, userBadgeRepository.count());
        assertEquals(9, quizUserBadges.size());
        for (UserBadgeEntity userBadge : quizUserBadges) {
            assertNotNull(userBadge.getUserBadgeUUID());
            assertTrue(badgeRepository.findById(userBadge.getBadgeUUID()).isPresent());
            assertTrue(lecturerUUID.equals(userBadge.getUserUUID())
                    || user1UUID.equals(userBadge.getUserUUID())
                    || user2UUID.equals(userBadge.getUserUUID()));
            assertFalse(userBadge.isAchieved());
        }

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertEquals(1, questChainRepository.count());
        assertNotNull(questChainEntity);
        assertEquals(3, questChainEntity.size());
        assertTrue(questChainEntity.getQuests().stream().anyMatch(
                questEntity -> { return quiz.equals(questEntity.getQuizUUID());}));

    }

    /**
     * Tests the creation of a quiz, that already exists.
     * <p>
     * This test verifies the creation of a quiz, that already exists and that the other repositories are
     * not changed
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>The quiz is not created again.</li>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void createQuizForExistingQuizTest() {

        String name = "FCS 2";
        assertEquals("Error at creating quiz.",
                gamificationController.createQuiz(quizUUID, name, courseUUID, chapterUUID,
                        70, List.of(SkillType.REMEMBER)));

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getContent().size());
        assertTrue(courseEntity.get().getContent().contains(quizUUID));
        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(55, courseEntity.get().getRequiredExpOfLevel(0));

        assertEquals(6, badgeRepository.count());
        List<BadgeEntity> quizBadges = badgeRepository.findByQuizUUID(quizUUID);
        assertEquals(3, quizBadges.size());

        List<UserBadgeEntity> quizUserBadges = new LinkedList<>();
        for (BadgeEntity badge : quizBadges) {
            quizUserBadges.addAll(userBadgeRepository.findByBadgeUUID(badge.getBadgeUUID()));
        }

        assertEquals(18, userBadgeRepository.count());
        assertEquals(9, quizUserBadges.size());
        for (UserBadgeEntity userBadge : quizUserBadges) {
            assertNotNull(userBadge.getUserBadgeUUID());
            assertTrue(badgeRepository.findById(userBadge.getBadgeUUID()).isPresent());
            assertFalse(userBadge.isAchieved());
        }

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertEquals(1, questChainRepository.count());
        assertNotNull(questChainEntity);
        assertEquals(2, questChainEntity.size());
        assertTrue(questChainEntity.getQuests().stream().anyMatch(
                questEntity -> { return quizUUID.equals(questEntity.getQuizUUID());}));


    }

    /**
     * Tests the addition of a quiz, to a course, that does not exist.
     * <p>
     * This test verifies that the addition of a quiz, to a course, that does not exist, fails.
     * There should also be no changes to the repositories
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>The quiz is not created.</li>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void createQuizForNotExistingCourseTest() {

        UUID course = UUID.randomUUID();
        UUID quiz = UUID.randomUUID();
        String name = "FCS 2";
        assertEquals("Error at creating quiz.",
                gamificationController.createQuiz(quiz, name, course, chapterUUID,
                        70, List.of(SkillType.REMEMBER)));

        Optional<CourseEntity> courseEntity = courseRepository.findById(course);
        assertFalse(courseEntity.isPresent());

        assertEquals(6, badgeRepository.count());
        List<BadgeEntity> quizBadges = badgeRepository.findByQuizUUID(quiz);
        assertEquals(0, quizBadges.size());
        assertEquals(18, userBadgeRepository.count());

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(course);
        assertEquals(1, questChainRepository.count());
        assertNull(questChainEntity);

    }

    /**
     * Tests the addition of a quiz, to a chapter, that does not exist.
     * <p>
     * This test verifies that the addition of a quiz, to a chapter, that does not exist, fails.
     * There should also be no changes to the repositories
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>The quiz is not created.</li>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void createQuizForNotExistingChapterTest() {

        UUID chapter = UUID.randomUUID();
        UUID quiz = UUID.randomUUID();
        String name = "FCS 2";
        assertEquals("Error at creating quiz.",
                gamificationController.createQuiz(quiz, name, courseUUID, chapter,
                        70, List.of(SkillType.REMEMBER)));

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getContent().size());
        assertFalse(courseEntity.get().getContent().contains(quiz));
        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(55, courseEntity.get().getRequiredExpOfLevel(0));

        assertEquals(6, badgeRepository.count());
        List<BadgeEntity> quizBadges = badgeRepository.findByQuizUUID(quiz);
        assertEquals(0, quizBadges.size());
        assertEquals(18, userBadgeRepository.count());

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertEquals(1, questChainRepository.count());
        assertNotNull(questChainEntity);
        assertEquals(2, questChainEntity.size());
        assertFalse(questChainEntity.getQuests().stream().anyMatch(
                questEntity -> { return quiz.equals(questEntity.getQuizUUID());}));


    }

    /**
     * Tests the addition of a quiz for an invalid value of skillPoints.
     * <p>
     * This test verifies that the addition of a quiz, for skillPoints < 0 or > 100</>
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>The quiz is not created.</li>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void createQuizForSkillPointsOutOfRangeTest() {

        UUID quiz = UUID.randomUUID();
        String name = "FCS 2";
        assertEquals("Error at creating quiz.",
                gamificationController.createQuiz(quiz, name, courseUUID, chapterUUID,
                        -1, List.of(SkillType.REMEMBER)));

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getContent().size());
        assertFalse(courseEntity.get().getContent().contains(quiz));
        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(55, courseEntity.get().getRequiredExpOfLevel(0));

        assertEquals(6, badgeRepository.count());
        List<BadgeEntity> quizBadges = badgeRepository.findByQuizUUID(quiz);
        assertEquals(0, quizBadges.size());
        assertEquals(18, userBadgeRepository.count());

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertEquals(1, questChainRepository.count());
        assertNotNull(questChainEntity);
        assertEquals(2, questChainEntity.size());
        assertFalse(questChainEntity.getQuests().stream().anyMatch(
                questEntity -> { return quiz.equals(questEntity.getQuizUUID());}));




        assertEquals("Error at creating quiz.",
                gamificationController.createQuiz(quiz, name, courseUUID, chapterUUID,
                        101, List.of(SkillType.REMEMBER)));

        courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getContent().size());
        assertFalse(courseEntity.get().getContent().contains(quiz));
        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(55, courseEntity.get().getRequiredExpOfLevel(0));

        assertEquals(6, badgeRepository.count());
        quizBadges = badgeRepository.findByQuizUUID(quiz);
        assertEquals(0, quizBadges.size());
        assertEquals(18, userBadgeRepository.count());

        questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertEquals(1, questChainRepository.count());
        assertNotNull(questChainEntity);
        assertEquals(2, questChainEntity.size());
        assertFalse(questChainEntity.getQuests().stream().anyMatch(
                questEntity -> { return quiz.equals(questEntity.getQuizUUID());}));


    }

    /**
     * Tests the addition of a quiz for an invalid value of skillTypes.
     * <p>
     * This test verifies that the addition of a quiz, for skillPoints null or an empty list</>
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>The quiz is not created.</li>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void createQuizForEmptySkillTypesTest() {

        UUID quiz = UUID.randomUUID();
        String name = "FCS 2";
        assertEquals("Error at creating quiz.",
                gamificationController.createQuiz(quiz, name, courseUUID, chapterUUID,
                        70, null));

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getContent().size());
        assertFalse(courseEntity.get().getContent().contains(quiz));
        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(55, courseEntity.get().getRequiredExpOfLevel(0));

        assertEquals(6, badgeRepository.count());
        List<BadgeEntity> quizBadges = badgeRepository.findByQuizUUID(quiz);
        assertEquals(0, quizBadges.size());
        assertEquals(18, userBadgeRepository.count());

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertEquals(1, questChainRepository.count());
        assertNotNull(questChainEntity);
        assertEquals(2, questChainEntity.size());
        assertFalse(questChainEntity.getQuests().stream().anyMatch(
                questEntity -> { return quiz.equals(questEntity.getQuizUUID());}));




        assertEquals("Error at creating quiz.",
                gamificationController.createQuiz(quiz, name, courseUUID, chapterUUID,
                        70, new LinkedList<>()));

        courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getContent().size());
        assertFalse(courseEntity.get().getContent().contains(quiz));
        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(55, courseEntity.get().getRequiredExpOfLevel(0));

        assertEquals(6, badgeRepository.count());
        quizBadges = badgeRepository.findByQuizUUID(quiz);
        assertEquals(0, quizBadges.size());
        assertEquals(18, userBadgeRepository.count());

        questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertEquals(1, questChainRepository.count());
        assertNotNull(questChainEntity);
        assertEquals(2, questChainEntity.size());
        assertFalse(questChainEntity.getQuests().stream().anyMatch(
                questEntity -> { return quiz.equals(questEntity.getQuizUUID());}));


    }

    /**
     * Tests the deletion of badges and quests associated with a quiz.
     * <p>
     * This test verifies that when a quiz is deleted, the corresponding badges, userBadges and quests are also removed
     * from the database. It checks that badges and quests related to other entities (e.g., flashcard sets) are not affected.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>The quiz is deleted successfully.</li>
     *   <li>The quiz is removed from the content of the course.</li>
     *   <li>The required exp for the level of the chapter decreased by 50.</li>
     *   <li>All badges associated with the quiz are removed.</li>
     *   <li>Other badges and quests remain unaffected.</li>
     *   <li>The users' quest chain progress is reset accordingly.</li>
     * </ul>
     */
    @Test
    void deleteBadgesAndQuestOfQuizTest() {
        gamificationController.finishFlashCardSet(user1UUID, courseUUID, flashCardSetUUID, 5, 5, chapterUUID);
        gamificationController.finishQuiz(user2UUID, courseUUID, quizUUID, 5, 5, chapterUUID);

        assertEquals("Quiz deleted.", gamificationController.deleteBadgesAndQuestOfQuiz(quizUUID, courseUUID, chapterUUID));

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(1, courseEntity.get().getContent().size());
        assertFalse(courseEntity.get().getContent().contains(quizUUID));
        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(30, courseEntity.get().getRequiredExpOfLevel(0));

        List<BadgeEntity> allBadges = badgeRepository.findAll();
        assertEquals(3, allBadges.size());
        for(BadgeEntity badge : allBadges) {
            assertNotEquals(quizUUID, badge.getQuizUUID());
        }

        List<UserBadgeEntity> allUserBadges = userBadgeRepository.findAll();
        assertEquals(9, allUserBadges.size());

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertNotNull(questChainEntity);
        assertEquals(1, questChainEntity.size());
        for (QuestEntity quest : questChainEntity.getQuests()) {
            assertNotEquals(quizUUID, quest.getQuizUUID());
        }

        UserQuestChainEntity lecturerQuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), lecturerUUID);
        UserQuestChainEntity user1QuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), user1UUID);
        UserQuestChainEntity user2QuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), user2UUID);
        assertEquals(3, userQuestChainRepository.count());
        assertEquals(0, lecturerQuestChainEntity.getUserLevel());
        assertEquals(0, user1QuestChainEntity.getUserLevel());
        assertEquals(0, user2QuestChainEntity.getUserLevel());

    }

    /**
     * Tests the deletion of badges and quests associated with a quiz, that does not exist.
     * <p>
     * This test verifies that when a quiz, that does not exist, is tried to be, that badges and quests
     * related to other entities (e.g., quizzes) are not affected.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void deleteBadgesAndQuestOfNotExistingQuizTest() {
        gamificationController.finishFlashCardSet(user1UUID, courseUUID, flashCardSetUUID, 5, 5, chapterUUID);
        gamificationController.finishQuiz(user2UUID, courseUUID, quizUUID, 5, 5, chapterUUID);

        UUID quiz = UUID.randomUUID();
        assertEquals("Error at deleting quiz.", gamificationController.deleteBadgesAndQuestOfQuiz(quiz, courseUUID, chapterUUID));

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getContent().size());
        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(55, courseEntity.get().getRequiredExpOfLevel(0));

        List<BadgeEntity> allBadges = badgeRepository.findAll();
        assertEquals(6, allBadges.size());

        List<UserBadgeEntity> allUserBadges = userBadgeRepository.findAll();
        assertEquals(18, allUserBadges.size());

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertNotNull(questChainEntity);
        assertEquals(2, questChainEntity.size());

        UserQuestChainEntity lecturerQuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), lecturerUUID);
        UserQuestChainEntity user1QuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), user1UUID);
        UserQuestChainEntity user2QuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), user2UUID);
        assertEquals(3, userQuestChainRepository.count());
        assertEquals(0, lecturerQuestChainEntity.getUserLevel());
        assertEquals(0, user1QuestChainEntity.getUserLevel());
        assertEquals(1, user2QuestChainEntity.getUserLevel());

    }

    /**
     * Tests the deletion of badges and quests associated with a quiz, but with a not existing course id.
     * <p>
     * This test verifies that when a quiz, is tried to be deleted for a not existing course,
     * that badges and quests related to other entities (e.g., quizzes) are not affected.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void deleteBadgesAndQuestOfQuizForNotExistingCourseTest() {
        gamificationController.finishFlashCardSet(user1UUID, courseUUID, flashCardSetUUID, 5, 5, chapterUUID);
        gamificationController.finishQuiz(user2UUID, courseUUID, quizUUID, 5, 5, chapterUUID);

        UUID course = UUID.randomUUID();
        assertEquals("Error at deleting quiz.", gamificationController.deleteBadgesAndQuestOfQuiz(
                quizUUID, course, chapterUUID));

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getContent().size());
        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(55, courseEntity.get().getRequiredExpOfLevel(0));

        List<BadgeEntity> allBadges = badgeRepository.findAll();
        assertEquals(6, allBadges.size());

        List<UserBadgeEntity> allUserBadges = userBadgeRepository.findAll();
        assertEquals(18, allUserBadges.size());

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertNotNull(questChainEntity);
        assertEquals(2, questChainEntity.size());

        UserQuestChainEntity lecturerQuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), lecturerUUID);
        UserQuestChainEntity user1QuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), user1UUID);
        UserQuestChainEntity user2QuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), user2UUID);
        assertEquals(3, userQuestChainRepository.count());
        assertEquals(0, lecturerQuestChainEntity.getUserLevel());
        assertEquals(0, user1QuestChainEntity.getUserLevel());
        assertEquals(1, user2QuestChainEntity.getUserLevel());

    }

    /**
     * Tests the deletion of badges and quests associated with a quiz, but with a wrong course id.
     * <p>
     * This test verifies that when a quiz, is tried to be deleted for the wrong course,
     * that badges and quests related to other entities (e.g., quizzes) are not affected.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void deleteBadgesAndQuestOfQuizForWrongCourseTest() {
        gamificationController.finishFlashCardSet(user1UUID, courseUUID, flashCardSetUUID, 5, 5, chapterUUID);
        gamificationController.finishQuiz(user2UUID, courseUUID, quizUUID, 5, 5, chapterUUID);

        UUID course = UUID.randomUUID();
        gamificationController.addCourse(course, UUID.randomUUID(), List.of(UUID.randomUUID()));

        assertEquals("Error at deleting quiz.", gamificationController.deleteBadgesAndQuestOfQuiz(
                quizUUID, course, chapterUUID));

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getContent().size());
        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(55, courseEntity.get().getRequiredExpOfLevel(0));

        List<BadgeEntity> allBadges = badgeRepository.findAll();
        assertEquals(6, allBadges.size());

        List<UserBadgeEntity> allUserBadges = userBadgeRepository.findAll();
        assertEquals(18, allUserBadges.size());

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertNotNull(questChainEntity);
        assertEquals(2, questChainEntity.size());

        UserQuestChainEntity lecturerQuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), lecturerUUID);
        UserQuestChainEntity user1QuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), user1UUID);
        UserQuestChainEntity user2QuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), user2UUID);
        assertEquals(4, userQuestChainRepository.count());
        assertEquals(0, lecturerQuestChainEntity.getUserLevel());
        assertEquals(0, user1QuestChainEntity.getUserLevel());
        assertEquals(1, user2QuestChainEntity.getUserLevel());

    }

    /**
     * Tests the deletion of badges and quests associated with a quiz, but with a wrong chapter id.
     * <p>
     * This test verifies that when a quiz, is tried to be deleted for a course, that is not in the course,
     * that badges and quests related to other entities (e.g., quizzes) are not affected.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void deleteBadgesAndQuestOfQuizForNotExistingChapterTest() {
        gamificationController.finishFlashCardSet(user1UUID, courseUUID, flashCardSetUUID, 5, 5, chapterUUID);
        gamificationController.finishQuiz(user2UUID, courseUUID, quizUUID, 5, 5, chapterUUID);

        UUID chapter = UUID.randomUUID();
        assertEquals("Error at deleting quiz.", gamificationController.deleteBadgesAndQuestOfQuiz(
                quizUUID, courseUUID, chapter));

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getContent().size());
        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(55, courseEntity.get().getRequiredExpOfLevel(0));

        List<BadgeEntity> allBadges = badgeRepository.findAll();
        assertEquals(6, allBadges.size());

        List<UserBadgeEntity> allUserBadges = userBadgeRepository.findAll();
        assertEquals(18, allUserBadges.size());

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertNotNull(questChainEntity);
        assertEquals(2, questChainEntity.size());

        UserQuestChainEntity lecturerQuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), lecturerUUID);
        UserQuestChainEntity user1QuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), user1UUID);
        UserQuestChainEntity user2QuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), user2UUID);
        assertEquals(3, userQuestChainRepository.count());
        assertEquals(0, lecturerQuestChainEntity.getUserLevel());
        assertEquals(0, user1QuestChainEntity.getUserLevel());
        assertEquals(1, user2QuestChainEntity.getUserLevel());

    }

    /**
     * Tests the changing of a quizs data.
     * <p>
     * This test verifies that when a quizs data is changed, the descriptions of the associated badges and quests
     * are updated accordingly. The test checks that all related entities reflect the new quiz name.
     * The quizs metadata is updated as well.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>The quiz name and metadata is changed successfully.</li>
     *   <li>All badges associated with the quiz have their descriptions updated.</li>
     *   <li>The quest associated with the quiz in the quest chain also has its description updated.</li>
     *   <li>The metadata of the quiz is changed accordingly.</li>
     * </ul>
     */
    @Test
    void editQuizTest() {
        String newName = "New Name";
        assertEquals("Changed quiz data!", gamificationController.editQuiz(quizUUID,
                courseUUID, chapterUUID, newName, 55, List.of(SkillType.UNDERSTAND)));

        List<BadgeEntity> quizBadges = badgeRepository.findByQuizUUID(quizUUID);
        assertEquals(3, quizBadges.size());
        for (BadgeEntity badge : quizBadges) {
            int passingPercentage = badge.getPassingPercentage();
            assertEquals(BadgeService.descriptionPart1 + passingPercentage + BadgeService.descriptionPart2 +
                    "quiz " + newName + BadgeService.descriptionPart3, badge.getDescription());
        }

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        int index = questChainEntity.findIndexOfQuizQuest(quizUUID);
        QuestEntity quest = questChainEntity.getQuest(index);
        assertEquals(QuestService.descriptionPart1 + "quiz " + newName +
                QuestService.descriptionPart2 + 80 + QuestService.descriptionPart3, quest.getDescription());

        Optional<ContentMetaDataEntity> quizMetaData = contentMetaDataRepository.findById(quizUUID);

        assertTrue(quizMetaData.isPresent());
        assertEquals(SkillType.UNDERSTAND, quizMetaData.get().getSkillType());
        assertEquals(55, quizMetaData.get().getSkillPoints());
    }

    /**
     * Tests the editing of a quiz, that does not exist.
     * <p>
     * This test verifies that trying to edit a quiz that does not exist, fails and that the metadata is not changed.
     * <p>
     * Expected Outcome:
     * <ul>
     *
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void editQuizForNotExistingQuizTest() {

        UUID quiz = UUID.randomUUID();
        String name = "New name";
        assertEquals("Error at editing quiz.",
                gamificationController.editQuiz(quiz, courseUUID, chapterUUID, name,55,
                        List.of(SkillType.UNDERSTAND)));

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getContent().size());
        assertFalse(courseEntity.get().getContent().contains(quiz));

        List<BadgeEntity> quizBadges = badgeRepository.findByQuizUUID(quiz);
        assertEquals(0, quizBadges.size());
        assertEquals(6, badgeRepository.count());
        assertEquals(18, userBadgeRepository.count());

        assertEquals(2, contentMetaDataRepository.count());
        Optional<ContentMetaDataEntity> quizMetaData = contentMetaDataRepository.findById(quiz);
        assertTrue(quizMetaData.isEmpty());

    }

    /**
     * Tests the editing of a quiz for a course, that does not exist.
     * <p>
     * This test verifies that the editing of a quiz for a course, that does not exist, fails.
     * There should also be no changes to the repositories.
     * <p>
     *
     * Expected Outcome:
     * <ul>
     *   <li>The quiz is not edited.</li>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void editQuizForNotExistingCourseTest() {

        UUID course = UUID.randomUUID();
        String name = "FCS 2";
        assertEquals("Error at editing quiz.",
                gamificationController.editQuiz(quizUUID, course, chapterUUID, name,
                        70, List.of(SkillType.REMEMBER)));

        Optional<CourseEntity> courseEntity = courseRepository.findById(course);
        assertFalse(courseEntity.isPresent());

        assertEquals(6, badgeRepository.count());
        List<BadgeEntity> quizBadges = badgeRepository.findByQuizUUID(quizUUID);
        assertEquals(3, quizBadges.size());
        assertEquals(18, userBadgeRepository.count());

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(course);
        assertEquals(1, questChainRepository.count());
        assertNull(questChainEntity);

    }

    /**
     * Tests the editing of a quiz for a course, that does not contain the quiz.
     * <p>
     * This test verifies that the editing of a quiz for the wrong course, fails.
     * There should also be no changes to the repositories.
     * <p>
     *
     * Expected Outcome:
     * <ul>
     *   <li>The quiz is not edited.</li>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void editQuizForWrongCourseTest() {

        UUID course = UUID.randomUUID();
        gamificationController.addCourse(courseUUID, UUID.randomUUID(), List.of(UUID.randomUUID()));

        String name = "FCS 2";
        assertEquals("Error at editing quiz.",
                gamificationController.editQuiz(quizUUID, course, chapterUUID, name,
                        70, List.of(SkillType.REMEMBER)));

        Optional<CourseEntity> courseEntity = courseRepository.findById(course);
        assertFalse(courseEntity.isPresent());

        assertEquals(6, badgeRepository.count());
        List<BadgeEntity> quizBadges = badgeRepository.findByQuizUUID(quizUUID);
        assertEquals(3, quizBadges.size());
        assertEquals(18, userBadgeRepository.count());

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(course);
        assertEquals(1, questChainRepository.count());
        assertNull(questChainEntity);

    }

    /**
     * Tests the editing of a quiz for the wrong chapter id, that does not exist.
     * <p>
     * This test verifies that the editing of a quiz for a wrong chapter fails.
     * There should also be no changes to the repositories.
     * <p>
     *
     * Expected Outcome:
     * <ul>
     *   <li>The quiz is not edited.</li>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void editQuizForNotExistingChapterTest() {

        UUID chapter = UUID.randomUUID();
        String name = "FCS 2";
        assertEquals("Error at editing quiz.",
                gamificationController.editQuiz(quizUUID, courseUUID, chapter, name,
                        70, List.of(SkillType.REMEMBER)));

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());

        assertEquals(6, badgeRepository.count());
        List<BadgeEntity> quizBadges = badgeRepository.findByQuizUUID(quizUUID);
        assertEquals(3, quizBadges.size());
        assertEquals(18, userBadgeRepository.count());

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertEquals(1, questChainRepository.count());
        assertNotNull(questChainEntity);

    }

    /**
     * Tests the editing of a quiz for invalid values of skillPoints.
     * <p>
     * This test verifies that the editing of a quiz, for skillPoints < 0 or > 100 fails
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>The quiz is not edited.</li>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void editQuizForSkillPointsOutOfRangeTest() {

        String name = "FCS 2";
        assertEquals("Error at editing quiz.",
                gamificationController.editQuiz(quizUUID, courseUUID, chapterUUID, name,
                        -1, List.of(SkillType.UNDERSTAND)));

        Optional<ContentMetaDataEntity> contentMetaDataEntity = contentMetaDataRepository.findById(quizUUID);
        assertTrue(contentMetaDataEntity.isPresent());
        assertEquals(quizUUID, contentMetaDataEntity.get().getContentUUID());
        assertEquals(50, contentMetaDataEntity.get().getSkillPoints());
        assertEquals(SkillType.ANALYSE, contentMetaDataEntity.get().getSkillType());

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getContent().size());
        assertTrue(courseEntity.get().getContent().contains(quizUUID));
        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(55, courseEntity.get().getRequiredExpOfLevel(0));

        assertEquals(6, badgeRepository.count());
        List<BadgeEntity> quizBadges = badgeRepository.findByQuizUUID(quizUUID);
        assertEquals(3, quizBadges.size());
        assertEquals(18, userBadgeRepository.count());

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertEquals(1, questChainRepository.count());
        assertNotNull(questChainEntity);
        assertEquals(2, questChainEntity.size());
        assertTrue(questChainEntity.getQuests().stream().anyMatch(
                questEntity -> { return quizUUID.equals(questEntity.getQuizUUID());}));


        assertEquals("Error at editing quiz.",
                gamificationController.editQuiz(quizUUID, courseUUID, chapterUUID, name,
                        101, List.of(SkillType.APPLY)));

        contentMetaDataEntity = contentMetaDataRepository.findById(quizUUID);
        assertTrue(contentMetaDataEntity.isPresent());
        assertEquals(quizUUID, contentMetaDataEntity.get().getContentUUID());
        assertEquals(50, contentMetaDataEntity.get().getSkillPoints());
        assertEquals(SkillType.ANALYSE, contentMetaDataEntity.get().getSkillType());

        courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getContent().size());
        assertTrue(courseEntity.get().getContent().contains(quizUUID));
        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(55, courseEntity.get().getRequiredExpOfLevel(0));

        assertEquals(6, badgeRepository.count());
        quizBadges = badgeRepository.findByQuizUUID(quizUUID);
        assertEquals(3, quizBadges.size());
        assertEquals(18, userBadgeRepository.count());

        questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertEquals(1, questChainRepository.count());
        assertNotNull(questChainEntity);
        assertEquals(2, questChainEntity.size());
        assertTrue(questChainEntity.getQuests().stream().anyMatch(
                questEntity -> { return quizUUID.equals(questEntity.getQuizUUID());}));


    }

    /**
     * Tests the editing of a quiz for an invalid value of skillTypes.
     * <p>
     * This test verifies that the editing of a quiz, for skillPoints null or an empty list</>
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>The quiz is not edited.</li>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void editQuizForEmptySkillTypesTest() {

        String name = "FCS 2";
        assertEquals("Error at editing quiz.",
                gamificationController.editQuiz(quizUUID, courseUUID, chapterUUID, name,
                        65, null));

        Optional<ContentMetaDataEntity> contentMetaDataEntity = contentMetaDataRepository.findById(quizUUID);
        assertTrue(contentMetaDataEntity.isPresent());
        assertEquals(quizUUID, contentMetaDataEntity.get().getContentUUID());
        assertEquals(50, contentMetaDataEntity.get().getSkillPoints());
        assertEquals(SkillType.ANALYSE, contentMetaDataEntity.get().getSkillType());

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getContent().size());
        assertTrue(courseEntity.get().getContent().contains(quizUUID));
        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(55, courseEntity.get().getRequiredExpOfLevel(0));

        assertEquals(6, badgeRepository.count());
        List<BadgeEntity> quizBadges = badgeRepository.findByQuizUUID(quizUUID);
        assertEquals(3, quizBadges.size());
        assertEquals(18, userBadgeRepository.count());

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertEquals(1, questChainRepository.count());
        assertNotNull(questChainEntity);
        assertEquals(2, questChainEntity.size());
        assertTrue(questChainEntity.getQuests().stream().anyMatch(
                questEntity -> { return quizUUID.equals(questEntity.getQuizUUID());}));


        assertEquals("Error at editing quiz.",
                gamificationController.editQuiz(quizUUID, courseUUID, chapterUUID, name,
                        65, List.of()));

        contentMetaDataEntity = contentMetaDataRepository.findById(quizUUID);
        assertTrue(contentMetaDataEntity.isPresent());
        assertEquals(quizUUID, contentMetaDataEntity.get().getContentUUID());
        assertEquals(50, contentMetaDataEntity.get().getSkillPoints());
        assertEquals(SkillType.ANALYSE, contentMetaDataEntity.get().getSkillType());

        courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getContent().size());
        assertTrue(courseEntity.get().getContent().contains(quizUUID));
        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(55, courseEntity.get().getRequiredExpOfLevel(0));

        assertEquals(6, badgeRepository.count());
        quizBadges = badgeRepository.findByQuizUUID(quizUUID);
        assertEquals(3, quizBadges.size());
        assertEquals(18, userBadgeRepository.count());

        questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertEquals(1, questChainRepository.count());
        assertNotNull(questChainEntity);
        assertEquals(2, questChainEntity.size());
        assertTrue(questChainEntity.getQuests().stream().anyMatch(
                questEntity -> { return quizUUID.equals(questEntity.getQuizUUID());}));


    }

    /**
     * Tests the completion of a quiz by different users.
     * <p>
     * This test verifies that when users complete a quiz, their progress is updated correctly in the
     * badges and quest chain entities. The test checks that the correct badges are marked as achieved
     * based on the users' quiz performance.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>Users complete the quiz successfully.</li>
     *   <li>Badges are marked as achieved or not based on the user's performance.</li>
     *   <li>The quest chain reflects the user's progress, updating their levels accordingly.</li>
     *   <li>The users receive experience points.</li>
     * </ul>
     */
    @Test
    void finishQuizTest() {
        assertEquals("Finished quiz!",
                gamificationController.finishQuiz(lecturerUUID, courseUUID, quizUUID, 8, 10, chapterUUID));
        assertEquals("Finished quiz!",
                gamificationController.finishQuiz(user1UUID, courseUUID, quizUUID, 5, 10, chapterUUID));
        assertEquals("Finished quiz!",
                gamificationController.finishQuiz(user2UUID, courseUUID, quizUUID, 2, 10, chapterUUID));

        List<BadgeEntity> quizBadges = badgeRepository.findByQuizUUID(quizUUID);
        assertEquals(3, quizBadges.size());
        List<UserBadgeEntity> lecturerFCSBadges = new ArrayList<UserBadgeEntity>();
        List<UserBadgeEntity> user1FCSBadges = new ArrayList<UserBadgeEntity>();
        List<UserBadgeEntity> user2FCSBadges = new ArrayList<UserBadgeEntity>();
        for (BadgeEntity badge : quizBadges) {
            lecturerFCSBadges.add(userBadgeRepository.findByUserUUIDAndBadgeUUID(lecturerUUID, badge.getBadgeUUID()));
            user1FCSBadges.add(userBadgeRepository.findByUserUUIDAndBadgeUUID(user1UUID, badge.getBadgeUUID()));
            user2FCSBadges.add(userBadgeRepository.findByUserUUIDAndBadgeUUID(user2UUID, badge.getBadgeUUID()));
        }
        assertEquals(3, lecturerFCSBadges.size());
        assertEquals(3, user1FCSBadges.size());
        assertEquals(3, user2FCSBadges.size());

        for (UserBadgeEntity userBadge : lecturerFCSBadges) {
            Optional<BadgeEntity> optionalBadge = badgeRepository.findById(userBadge.getBadgeUUID());
            assertTrue(optionalBadge.isPresent());
            BadgeEntity badge = optionalBadge.get();
            assertTrue(50 == badge.getPassingPercentage()
                    || 70 == badge.getPassingPercentage()
                    || 90 == badge.getPassingPercentage());
            switch (badge.getPassingPercentage()) {
                case 50, 70:
                    assertTrue(userBadge.isAchieved());
                    break;
                case 90:
                    assertFalse(userBadge.isAchieved());
                    break;
            }
        }

        for (UserBadgeEntity userBadge : user1FCSBadges) {
            Optional<BadgeEntity> optionalBadge = badgeRepository.findById(userBadge.getBadgeUUID());
            assertTrue(optionalBadge.isPresent());
            BadgeEntity badge = optionalBadge.get();
            assertTrue(50 == badge.getPassingPercentage()
                    || 70 == badge.getPassingPercentage()
                    || 90 == badge.getPassingPercentage());
            switch (badge.getPassingPercentage()) {
                case 50:
                    assertTrue(userBadge.isAchieved());
                    break;
                case 70, 90:
                    assertFalse(userBadge.isAchieved());
                    break;
            }
        }

        for (UserBadgeEntity userBadge : user2FCSBadges) {
            assertFalse(userBadge.isAchieved());
        }

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        UserQuestChainEntity lecturerQuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), lecturerUUID);
        UserQuestChainEntity user1QuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), user1UUID);
        UserQuestChainEntity user2QuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), user2UUID);
        assertEquals(1, lecturerQuestChainEntity.getUserLevel());
        assertEquals(0, user1QuestChainEntity.getUserLevel());
        assertEquals(0, user2QuestChainEntity.getUserLevel());

        BloomLevelEntity lecturerBloomLevel = bloomLevelRepository.findByUserUUIDAndCourseUUID(lecturerUUID, courseUUID);
        BloomLevelEntity user1BloomLevel = bloomLevelRepository.findByUserUUIDAndCourseUUID(user1UUID, courseUUID);
        BloomLevelEntity user2BloomLevel = bloomLevelRepository.findByUserUUIDAndCourseUUID(user2UUID, courseUUID);
        assertNotNull(lecturerBloomLevel);
        assertNotNull(user1BloomLevel);
        assertNotNull(user2BloomLevel);
        assertEquals(80, lecturerBloomLevel.getCollectedExp());
        assertEquals(50, user1BloomLevel.getCollectedExp());
        assertEquals(20, user2BloomLevel.getCollectedExp());

    }

    /**
     * Tests the completion of a quiz by a users for an invalid number of correct answers.
     * <p>
     * This test verifies that when users complete a quiz with an invalid number of correct answers,
     * their progress is not updated correctly in the badges and quest chain entities.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>No changes for badges, quests, or the users Bloom Level.</li>
     * </ul>
     */
    @Test
    void finishQuizForCorrectAnswersOutOfBoundsTest() {
        assertEquals("Error at finishing quiz.",
                gamificationController.finishQuiz(lecturerUUID, courseUUID, quizUUID, 11, -1, chapterUUID));

        List<BadgeEntity> quizBadges = badgeRepository.findByQuizUUID(quizUUID);
        assertEquals(3, quizBadges.size());
        List<UserBadgeEntity> lecturerFCSBadges = new ArrayList<UserBadgeEntity>();
        for (BadgeEntity badge : quizBadges) {
            lecturerFCSBadges.add(userBadgeRepository.findByUserUUIDAndBadgeUUID(lecturerUUID, badge.getBadgeUUID()));
        }
        assertEquals(3, lecturerFCSBadges.size());

        for (UserBadgeEntity userBadge : lecturerFCSBadges) {
            assertFalse(userBadge.isAchieved());
        }

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        UserQuestChainEntity lecturerQuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), lecturerUUID);
        assertNotNull(lecturerQuestChainEntity);
        assertEquals(0, lecturerQuestChainEntity.getUserLevel());

        BloomLevelEntity lecturerBloomLevel = bloomLevelRepository.findByUserUUIDAndCourseUUID(lecturerUUID, courseUUID);
        assertNotNull(lecturerBloomLevel);
        assertEquals(0, lecturerBloomLevel.getCollectedExp());



        assertEquals("Error at finishing quiz.",
                gamificationController.finishQuiz(lecturerUUID, courseUUID, quizUUID, 11, -1, chapterUUID));

        lecturerFCSBadges = new ArrayList<UserBadgeEntity>();
        for (BadgeEntity badge : quizBadges) {
            lecturerFCSBadges.add(userBadgeRepository.findByUserUUIDAndBadgeUUID(lecturerUUID, badge.getBadgeUUID()));
        }
        assertEquals(3, lecturerFCSBadges.size());

        for (UserBadgeEntity userBadge : lecturerFCSBadges) {
            assertFalse(userBadge.isAchieved());
        }

        questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        lecturerQuestChainEntity = userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), lecturerUUID);
        assertNotNull(lecturerQuestChainEntity);
        assertEquals(0, lecturerQuestChainEntity.getUserLevel());

        lecturerBloomLevel = bloomLevelRepository.findByUserUUIDAndCourseUUID(lecturerUUID, courseUUID);
        assertNotNull(lecturerBloomLevel);
        assertEquals(0, lecturerBloomLevel.getCollectedExp());

    }

    /**
     * Tests the completion of a quiz, that does not exist.
     * <p>
     * This test verifies that when users complete a quiz, that does not exist, their progress is not updated
     * in the badges and quest chain entities.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void finishNotExistingQuizTest() {

        UUID quiz = UUID.randomUUID();
        assertEquals("Error at finishing quiz.",
                gamificationController.finishQuiz(lecturerUUID, courseUUID, quiz, 8, 10, chapterUUID));

        List<BadgeEntity> quizBadges = badgeRepository.findByQuizUUID(quiz);
        assertEquals(0, quizBadges.size());
        List<UserBadgeEntity> lecturerFCSBadges = new ArrayList<UserBadgeEntity>();
        for (BadgeEntity badge : quizBadges) {
            lecturerFCSBadges.add(userBadgeRepository.findByUserUUIDAndBadgeUUID(lecturerUUID, badge.getBadgeUUID()));
        }
        assertEquals(0, lecturerFCSBadges.size());

        for (UserBadgeEntity userBadge : lecturerFCSBadges) {
            assertFalse(userBadge.isAchieved());
        }

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        UserQuestChainEntity lecturerQuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), lecturerUUID);
        assertNotNull(lecturerQuestChainEntity);
        assertEquals(0, lecturerQuestChainEntity.getUserLevel());

        BloomLevelEntity lecturerBloomLevel = bloomLevelRepository.findByUserUUIDAndCourseUUID(lecturerUUID, courseUUID);
        assertNotNull(lecturerBloomLevel);
        assertEquals(0, lecturerBloomLevel.getCollectedExp());

    }

    /**
     * Tests the completion of a quiz, which is in a course, where the user is not present.
     * <p>
     * This test verifies that when users tries to complete a quiz, where it has no access to,
     * their progress is not updated in the badges and quest chain entities.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void finishQuizForUserNotInCourseTest() {

        UUID user = UUID.randomUUID();
        assertEquals("Error at finishing quiz.",
                gamificationController.finishQuiz(user, courseUUID, quizUUID, 8, 10, chapterUUID));

        List<BadgeEntity> quizBadges = badgeRepository.findByQuizUUID(quizUUID);
        assertEquals(3, quizBadges.size());
        for (BadgeEntity badge : quizBadges) {
            assertNull(userBadgeRepository.findByUserUUIDAndBadgeUUID(user, badge.getBadgeUUID()));
        }

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        UserQuestChainEntity userQuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), user);
        assertNull(userQuestChainEntity);

        BloomLevelEntity lecturerBloomLevel = bloomLevelRepository.findByUserUUIDAndCourseUUID(user, courseUUID);
        assertNull(lecturerBloomLevel);

    }

    /**
     * Tests the completion of a quiz, with a course, that does not exist and the wrong course id.
     * <p>
     * This test verifies that when users complete a quiz, with a course, that does not exist, or
     * the wrong course id, their progress is not updated in the badges and quest chain entities.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void finishQuizForWrongCourseTest() {

        UUID course = UUID.randomUUID();
        assertEquals("Error at finishing quiz.",
                gamificationController.finishQuiz(lecturerUUID, course, quizUUID, 8, 10, chapterUUID));

        List<BadgeEntity> quizBadges = badgeRepository.findByQuizUUID(quizUUID);
        assertEquals(3, quizBadges.size());
        List<UserBadgeEntity> lecturerFCSBadges = new ArrayList<UserBadgeEntity>();
        for (BadgeEntity badge : quizBadges) {
            lecturerFCSBadges.add(userBadgeRepository.findByUserUUIDAndBadgeUUID(lecturerUUID, badge.getBadgeUUID()));
        }

        for (UserBadgeEntity userBadge : lecturerFCSBadges) {
            assertNotNull(userBadge);
            assertFalse(userBadge.isAchieved());
        }

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(course);
        assertNull(questChainEntity);

        BloomLevelEntity lecturerBloomLevel = bloomLevelRepository.findByUserUUIDAndCourseUUID(lecturerUUID, course);
        assertNull(lecturerBloomLevel);



        gamificationController.addCourse(course, lecturerUUID, List.of(UUID.randomUUID()));
        assertEquals("Error at finishing quiz.",
                gamificationController.finishQuiz(lecturerUUID, course, quizUUID, 8, 10, chapterUUID));

        quizBadges = badgeRepository.findByQuizUUID(quizUUID);
        assertEquals(3, quizBadges.size());
        lecturerFCSBadges = new ArrayList<UserBadgeEntity>();
        for (BadgeEntity badge : quizBadges) {
            lecturerFCSBadges.add(userBadgeRepository.findByUserUUIDAndBadgeUUID(lecturerUUID, badge.getBadgeUUID()));
        }

        for (UserBadgeEntity userBadge : lecturerFCSBadges) {
            assertNotNull(userBadge);
            assertFalse(userBadge.isAchieved());
        }

        questChainEntity = questChainRepository.findByCourseUUID(course);
        UserQuestChainEntity lecturerQuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), lecturerUUID);
        assertNotNull(questChainEntity);
        assertNotNull(lecturerQuestChainEntity);
        assertEquals(0, lecturerQuestChainEntity.getUserLevel());

        lecturerBloomLevel = bloomLevelRepository.findByUserUUIDAndCourseUUID(lecturerUUID, course);
        assertNotNull(lecturerBloomLevel);
        assertEquals(0, lecturerBloomLevel.getCollectedExp());

    }

    /**
     * Tests the completion of a quiz, for a chapter, that does not exist.
     * <p>
     * This test verifies that when users complete a quiz, in a chapter, that does not exist,
     * their progress is not updated in the badges and quest chain entities.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void finishNotExistingQuizForNotExistingChapterTest() {

        UUID chapter = UUID.randomUUID();
        assertEquals("Error at finishing quiz.",
                gamificationController.finishQuiz(lecturerUUID, courseUUID, quizUUID, 8, 10, chapter));

        List<BadgeEntity> quizBadges = badgeRepository.findByQuizUUID(quizUUID);
        assertEquals(3, quizBadges.size());
        List<UserBadgeEntity> lecturerFCSBadges = new ArrayList<UserBadgeEntity>();
        for (BadgeEntity badge : quizBadges) {
            lecturerFCSBadges.add(userBadgeRepository.findByUserUUIDAndBadgeUUID(lecturerUUID, badge.getBadgeUUID()));
        }
        assertEquals(3, lecturerFCSBadges.size());

        for (UserBadgeEntity userBadge : lecturerFCSBadges) {
            assertNotNull(userBadge);
            assertFalse(userBadge.isAchieved());
        }

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        UserQuestChainEntity lecturerQuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), lecturerUUID);
        assertNotNull(lecturerQuestChainEntity);
        assertEquals(0, lecturerQuestChainEntity.getUserLevel());

        BloomLevelEntity lecturerBloomLevel = bloomLevelRepository.findByUserUUIDAndCourseUUID(lecturerUUID, courseUUID);
        assertNotNull(lecturerBloomLevel);
        assertEquals(0, lecturerBloomLevel.getCollectedExp());

    }

}
