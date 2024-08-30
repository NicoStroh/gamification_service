package de.unistuttgart.iste.gits.gamification_service.service;

import de.unistuttgart.iste.gits.gamification_service.test_utils.TestUtils;
import de.unistuttgart.iste.meitrex.gamification_service.GamificationApplication;
import de.unistuttgart.iste.meitrex.gamification_service.controller.GamificationController;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.*;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.*;
import de.unistuttgart.iste.meitrex.gamification_service.service.BadgeService;
import de.unistuttgart.iste.meitrex.gamification_service.service.QuestService;
import de.unistuttgart.iste.meitrex.generated.dto.BloomLevel;
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
class BloomLevelTest {

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
     * Tests the creation of a new chapter.
     * <p>
     * This test verifies the creation of a chapter within a course and checks that the required exp for a
     * new level are created correctly
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>A new chapter is created successfully.</li>
     *   <li>The index of the chapter is 1.</li>
     *   <li>The required exp for the level of the chapter increased are 300.</li>
     * </ul>
     */
    @Test
    void addChapterTest() {
        UUID chapter = UUID.randomUUID();
        assertEquals("Added chapter to course.", gamificationController.addChapter(courseUUID, chapter));

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getChapters().size());
        assertEquals(1, courseEntity.get().getLevelOfChapter(chapter));

        assertEquals(2, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(0, courseEntity.get().getRequiredExpOfLevel(1));
    }

    /**
     * Tests the addition of an already existing chapter.
     * <p>
     * This test verifies that the chapter is not added again to the course.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>A new chapter is created successfully.</li>
     *   <li>The index of the chapter is 1.</li>
     *   <li>The required exp for the level of the chapter increased are 300.</li>
     * </ul>
     */
    @Test
    void addExistingChapterTest() {

        assertEquals("Chapter already in course", gamificationController.addChapter(courseUUID, chapterUUID));

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(1, courseEntity.get().getChapters().size());

        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
    }

    /**
     * Tests the addition of a new chapter to a course that does not exist.
     * <p>
     * This test verifies that the chapter can not be added, since the course does not exist
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>The course is not found.</li>
     *   <li>The only existing course is the previously created one.</li>
     *   <li>The existing course does still contain just one chapter.</li>
     * </ul>
     */
    @Test
    void addChapterToNotExistingCourseTest() {

        UUID course = UUID.randomUUID();
        UUID chapter = UUID.randomUUID();
        assertEquals("Course not found.", gamificationController.addChapter(course, chapter));

        assertEquals(1, courseRepository.count());

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(1, courseEntity.get().getChapters().size());

    }

    /**
     * Tests the retrieval of the bloom level of the users.
     * <p>
     * This test verifies that the bloom levels are what one would expect.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>The lecturer is level 1 and there are no limits for level 2, since it is the highest level.</li>
     *   <li>The user1 is at level 0 and has 50 experience points.</li>
     * </ul>
     */
    @Test
    void getUsersBloomLevelTest() {
        gamificationController.finishQuiz(lecturerUUID, courseUUID, quizUUID, 10, 10, chapterUUID);
        gamificationController.finishQuiz(lecturerUUID, courseUUID, quizUUID, 10, 10, chapterUUID);
        gamificationController.finishQuiz(lecturerUUID, courseUUID, quizUUID, 10, 10, chapterUUID);

        gamificationController.finishQuiz(user1UUID, courseUUID, quizUUID, 5, 10, chapterUUID);

        BloomLevel lecturerBloomLevel = gamificationController.getUsersBloomLevel(lecturerUUID, courseUUID);
        BloomLevel user1BloomLevel = gamificationController.getUsersBloomLevel(user1UUID, courseUUID);

        assertNotNull(lecturerBloomLevel);
        assertEquals(1, lecturerBloomLevel.getLevel());
        assertEquals(300, lecturerBloomLevel.getTotalExp());
        assertEquals(245, lecturerBloomLevel.getExpForCurrentLevel());
        assertEquals(Integer.MAX_VALUE, lecturerBloomLevel.getRequiredExpForCurrentLevel());

        assertNotNull(user1BloomLevel);
        assertEquals(0, user1BloomLevel.getLevel());
        assertEquals(50, user1BloomLevel.getTotalExp());
        assertEquals(50, user1BloomLevel.getExpForCurrentLevel());
        assertEquals(55, user1BloomLevel.getRequiredExpForCurrentLevel());
    }

    /**
     * Tests the retrieval of the bloom level of a user for a not existing course.
     * <p>
     * This test verifies that the bloom level is the default value.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>The lecturers bloom level is default.</li>
     * </ul>
     */
    @Test
    void getUsersBloomLevelForNotExistingCourseTest() {
        UUID course = UUID.randomUUID();
        assertEquals(new BloomLevel(), gamificationController.getUsersBloomLevel(lecturerUUID, course));
    }

    /**
     * Tests the retrieval of the bloom level of a user for a course where the user is not contained.
     * <p>
     * This test verifies that the bloom level is the default value.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>The users bloom level is default.</li>
     * </ul>
     */
    @Test
    void getUsersBloomLevelForUserNotInCourseTest() {
        UUID user = UUID.randomUUID();
        assertEquals(new BloomLevel(), gamificationController.getUsersBloomLevel(user, courseUUID));
    }

}
