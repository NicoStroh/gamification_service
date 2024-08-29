package de.unistuttgart.iste.gits.gamification_service.service;

import de.unistuttgart.iste.gits.gamification_service.test_utils.TestUtils;
import de.unistuttgart.iste.meitrex.gamification_service.GamificationApplication;
import de.unistuttgart.iste.meitrex.gamification_service.controller.GamificationController;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.*;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.*;
import de.unistuttgart.iste.meitrex.gamification_service.service.*;
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
 * Unit tests for the FlashCardSet functionalities in the GamificationService.
 * <p>
 * This test class contains unit tests to verify the functionality of the flashCardSet-related operations
 * within the GamificationController. It includes tests for creating flashCardSets, deleting flashCardSet-related badges
 * and quests, editing flashCardSet names, and finishing flashCardSets.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = GamificationApplication.class)
@Transactional // Each test method runs in a transaction that is rolled back after the test completes
class FlashCardSetTest {

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
     * Tests the creation of a new flashCardSet.
     * <p>
     * This test verifies the creation of a flashCardSet within a course and checks that the appropriate badges
     * are created and associated with the flashCardSet. It also checks that the badges are properly linked to the users
     * of the course, and that the quest chain is updated accordingly.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>A new flashCardSet is created successfully.</li>
     *   <li>The flashCardSet is added to the content of the course.</li>
     *   <li>The required exp for the level of the chapter increased by 30.</li>
     *   <li>Three badges are associated with the new flashCardSet.</li>
     *   <li>Each badge has the correct description and properties.</li>
     *   <li>Each user has the appropriate user badges initialized but not achieved.</li>
     *   <li>The quest chain is updated to include a new quest related to the flashCardSet.</li>
     * </ul>
     */
    @Test
    void createFlashCardSetTest() {
        UUID flashCardSet = UUID.randomUUID();
        String name = "FCS 2";
        assertEquals("Created flashCardSet successfully.",
                gamificationController.createFlashCardSet(flashCardSet, name, courseUUID, chapterUUID,
                        70, List.of(SkillType.REMEMBER)));

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(3, courseEntity.get().getContent().size());
        assertTrue(courseEntity.get().getContent().contains(flashCardSet));
        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(260, courseEntity.get().getRequiredExpOfLevel(0));

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
        assertEquals(3, questChainEntity.size());
        assertTrue(questChainEntity.getQuests().stream().anyMatch(
                questEntity -> { return flashCardSet.equals(questEntity.getFlashCardSetUUID());}));


    }

    /**
     * Tests the creation of a flashCardSet, that already exists.
     * <p>
     * This test verifies the creation of a flashCardSet, that already exists and that the other repositories are
     * not changed
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>The flashCardSet is not created again.</li>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void createFlashCardSetForExistingFlashCardSetTest() {

        String name = "FCS 2";
        assertEquals("Error at creating flashCardSet.",
                gamificationController.createFlashCardSet(flashCardSetUUID, name, courseUUID, chapterUUID,
                        70, List.of(SkillType.REMEMBER)));

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getContent().size());
        assertTrue(courseEntity.get().getContent().contains(flashCardSetUUID));
        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(230, courseEntity.get().getRequiredExpOfLevel(0));

        assertEquals(6, badgeRepository.findAll().size());
        List<BadgeEntity> fcsBadges = badgeRepository.findByFlashCardSetUUID(flashCardSetUUID);
        assertEquals(3, fcsBadges.size());

        List<UserBadgeEntity> fcsUserBadges = new LinkedList<>();
        for (BadgeEntity badge : fcsBadges) {
            fcsUserBadges.addAll(userBadgeRepository.findByBadgeUUID(badge.getBadgeUUID()));
        }

        assertEquals(18, userBadgeRepository.findAll().size());
        assertEquals(9, fcsUserBadges.size());
        for (UserBadgeEntity userBadge : fcsUserBadges) {
            assertNotNull(userBadge.getUserBadgeUUID());
            assertTrue(badgeRepository.findById(userBadge.getBadgeUUID()).isPresent());
            assertFalse(userBadge.isAchieved());
        }

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertEquals(1, questChainRepository.findAll().size());
        assertNotNull(questChainEntity);
        assertEquals(2, questChainEntity.size());
        assertTrue(questChainEntity.getQuests().stream().anyMatch(
                questEntity -> { return flashCardSetUUID.equals(questEntity.getFlashCardSetUUID());}));


    }

    /**
     * Tests the addition of a flashCardSet, to a course, that does not exist.
     * <p>
     * This test verifies that the addition of a flashCardSet, to a course, that does not exist, fails.
     * There should also be no changes to the repositories
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>The flashCardSet is not created.</li>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void createFlashCardSetForNotExistingCourseTest() {

        UUID course = UUID.randomUUID();
        UUID flashCardSet = UUID.randomUUID();
        String name = "FCS 2";
        assertEquals("Error at creating flashCardSet.",
                gamificationController.createFlashCardSet(flashCardSet, name, course, chapterUUID,
                        70, List.of(SkillType.REMEMBER)));

        Optional<CourseEntity> courseEntity = courseRepository.findById(course);
        assertFalse(courseEntity.isPresent());

        assertEquals(6, badgeRepository.findAll().size());
        List<BadgeEntity> fcsBadges = badgeRepository.findByFlashCardSetUUID(flashCardSet);
        assertEquals(0, fcsBadges.size());
        assertEquals(18, userBadgeRepository.findAll().size());

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(course);
        assertEquals(1, questChainRepository.findAll().size());
        assertNull(questChainEntity);

    }

    /**
     * Tests the addition of a flashCardSet, to a chapter, that does not exist.
     * <p>
     * This test verifies that the addition of a flashCardSet, to a chapter, that does not exist, fails.
     * There should also be no changes to the repositories
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>The flashCardSet is not created.</li>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void createFlashCardSetForNotExistingChapterTest() {

        UUID chapter = UUID.randomUUID();
        UUID flashCardSet = UUID.randomUUID();
        String name = "FCS 2";
        assertEquals("Error at creating flashCardSet.",
                gamificationController.createFlashCardSet(flashCardSet, name, courseUUID, chapter,
                        70, List.of(SkillType.REMEMBER)));

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getContent().size());
        assertFalse(courseEntity.get().getContent().contains(flashCardSet));
        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(230, courseEntity.get().getRequiredExpOfLevel(0));

        assertEquals(6, badgeRepository.findAll().size());
        List<BadgeEntity> fcsBadges = badgeRepository.findByFlashCardSetUUID(flashCardSet);
        assertEquals(0, fcsBadges.size());
        assertEquals(18, userBadgeRepository.findAll().size());

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertEquals(1, questChainRepository.findAll().size());
        assertNotNull(questChainEntity);
        assertEquals(2, questChainEntity.size());
        assertFalse(questChainEntity.getQuests().stream().anyMatch(
                questEntity -> { return flashCardSet.equals(questEntity.getFlashCardSetUUID());}));


    }

    /**
     * Tests the addition of a flashCardSet for an invalid value of skillPoints.
     * <p>
     * This test verifies that the addition of a flashCardSet, for skillPoints < 0 or > 100 fails.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>The flashCardSet is not created.</li>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void createFlashCardSetForSkillPointsOutOfRangeTest() {

        UUID flashCardSet = UUID.randomUUID();
        String name = "FCS 2";
        assertEquals("Error at creating flashCardSet.",
                gamificationController.createFlashCardSet(flashCardSet, name, courseUUID, chapterUUID,
                        -1, List.of(SkillType.REMEMBER)));

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getContent().size());
        assertFalse(courseEntity.get().getContent().contains(flashCardSet));
        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(230, courseEntity.get().getRequiredExpOfLevel(0));

        assertEquals(6, badgeRepository.findAll().size());
        List<BadgeEntity> fcsBadges = badgeRepository.findByFlashCardSetUUID(flashCardSet);
        assertEquals(0, fcsBadges.size());
        assertEquals(18, userBadgeRepository.findAll().size());

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertEquals(1, questChainRepository.findAll().size());
        assertNotNull(questChainEntity);
        assertEquals(2, questChainEntity.size());
        assertFalse(questChainEntity.getQuests().stream().anyMatch(
                questEntity -> { return flashCardSet.equals(questEntity.getFlashCardSetUUID());}));




        assertEquals("Error at creating flashCardSet.",
                gamificationController.createFlashCardSet(flashCardSet, name, courseUUID, chapterUUID,
                        101, List.of(SkillType.REMEMBER)));

        courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getContent().size());
        assertFalse(courseEntity.get().getContent().contains(flashCardSet));
        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(230, courseEntity.get().getRequiredExpOfLevel(0));

        assertEquals(6, badgeRepository.findAll().size());
        fcsBadges = badgeRepository.findByFlashCardSetUUID(flashCardSet);
        assertEquals(0, fcsBadges.size());
        assertEquals(18, userBadgeRepository.findAll().size());

        questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertEquals(1, questChainRepository.findAll().size());
        assertNotNull(questChainEntity);
        assertEquals(2, questChainEntity.size());
        assertFalse(questChainEntity.getQuests().stream().anyMatch(
                questEntity -> { return flashCardSet.equals(questEntity.getFlashCardSetUUID());}));


    }

    /**
     * Tests the addition of a flashCardSet for an invalid value of skillTypes.
     * <p>
     * This test verifies that the addition of a flashCardSet, for skillPoints null or an empty list</>
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>The flashCardSet is not created.</li>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void createFlashCardSetForEmptySkillTypesTest() {

        UUID flashCardSet = UUID.randomUUID();
        String name = "FCS 2";
        assertEquals("Error at creating flashCardSet.",
                gamificationController.createFlashCardSet(flashCardSet, name, courseUUID, chapterUUID,
                        70, null));

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getContent().size());
        assertFalse(courseEntity.get().getContent().contains(flashCardSet));
        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(230, courseEntity.get().getRequiredExpOfLevel(0));

        assertEquals(6, badgeRepository.findAll().size());
        List<BadgeEntity> fcsBadges = badgeRepository.findByFlashCardSetUUID(flashCardSet);
        assertEquals(0, fcsBadges.size());
        assertEquals(18, userBadgeRepository.findAll().size());

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertEquals(1, questChainRepository.findAll().size());
        assertNotNull(questChainEntity);
        assertEquals(2, questChainEntity.size());
        assertFalse(questChainEntity.getQuests().stream().anyMatch(
                questEntity -> { return flashCardSet.equals(questEntity.getFlashCardSetUUID());}));




        assertEquals("Error at creating flashCardSet.",
                gamificationController.createFlashCardSet(flashCardSet, name, courseUUID, chapterUUID,
                        70, new LinkedList<>()));

        courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getContent().size());
        assertFalse(courseEntity.get().getContent().contains(flashCardSet));
        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(230, courseEntity.get().getRequiredExpOfLevel(0));

        assertEquals(6, badgeRepository.findAll().size());
        fcsBadges = badgeRepository.findByFlashCardSetUUID(flashCardSet);
        assertEquals(0, fcsBadges.size());
        assertEquals(18, userBadgeRepository.findAll().size());

        questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertEquals(1, questChainRepository.findAll().size());
        assertNotNull(questChainEntity);
        assertEquals(2, questChainEntity.size());
        assertFalse(questChainEntity.getQuests().stream().anyMatch(
                questEntity -> { return flashCardSet.equals(questEntity.getFlashCardSetUUID());}));


    }


    /**
     * Tests the deletion of badges and quests associated with a flashCardSet.
     * <p>
     * This test verifies that when a flashCardSet is deleted, the corresponding badges, userBadges and quests are also removed
     * from the database. It checks that badges and quests related to other entities (e.g., quizzes) are not affected.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>The flashCardSet is deleted successfully.</li>
     *   <li>The flashCardSet is removed from the content of the course.</li>
     *   <li>The required exp for the level of the chapter decreased by 30.</li>
     *   <li>All badges associated with the flashCardSet are removed.</li>
     *   <li>Other badges and quests remain unaffected.</li>
     *   <li>The users' quest chain progress is not reset, because their currentQuest does not point to the flashCardSet.</li>
     * </ul>
     */
    @Test
    void deleteBadgesAndQuestOfFlashCardSetTest() {
        gamificationController.finishFlashCardSet(user1UUID, courseUUID, flashCardSetUUID, 5, 5, chapterUUID);
        gamificationController.finishQuiz(user2UUID, courseUUID, quizUUID, 5, 5, chapterUUID);

        assertEquals("FlashCardSet deleted.", gamificationController.deleteBadgesAndQuestOfFlashCardSet(flashCardSetUUID, courseUUID, chapterUUID));

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(1, courseEntity.get().getContent().size());
        assertFalse(courseEntity.get().getContent().contains(flashCardSetUUID));
        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(200, courseEntity.get().getRequiredExpOfLevel(0));

        List<BadgeEntity> allBadges = badgeRepository.findAll();
        assertEquals(3, allBadges.size());
        for(BadgeEntity badge : allBadges) {
            assertNotEquals(flashCardSetUUID, badge.getFlashCardSetUUID());
        }

        List<UserBadgeEntity> allUserBadges = userBadgeRepository.findAll();
        assertEquals(9, allUserBadges.size());

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertNotNull(questChainEntity);
        assertEquals(1, questChainEntity.size());
        for (QuestEntity quest : questChainEntity.getQuests()) {
            assertNotEquals(flashCardSetUUID, quest.getFlashCardSetUUID());
        }

        UserQuestChainEntity lecturerQuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), lecturerUUID);
        UserQuestChainEntity user1QuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), user1UUID);
        UserQuestChainEntity user2QuestChainEntity =
                userQuestChainRepository.findByQuestChainUUIDAndUserUUID(questChainEntity.getQuestChainUUID(), user2UUID);
        assertEquals(3, userQuestChainRepository.findAll().size());
        assertEquals(0, lecturerQuestChainEntity.getUserLevel());
        assertEquals(0, user1QuestChainEntity.getUserLevel());
        assertEquals(1, user2QuestChainEntity.getUserLevel());

    }

    /**
     * Tests the deletion of badges and quests associated with a flashCardSet, that does not exist.
     * <p>
     * This test verifies that when a flashCardSet, that does not exist, is tried to be, that badges and quests
     * related to other entities (e.g., quizzes) are not affected.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void deleteBadgesAndQuestOfNotExistingFlashCardSetTest() {
        gamificationController.finishFlashCardSet(user1UUID, courseUUID, flashCardSetUUID, 5, 5, chapterUUID);
        gamificationController.finishQuiz(user2UUID, courseUUID, quizUUID, 5, 5, chapterUUID);

        UUID flashCardSet = UUID.randomUUID();
        assertEquals("Error at deleting flashCardSet.", gamificationController.deleteBadgesAndQuestOfFlashCardSet(flashCardSet, courseUUID, chapterUUID));

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getContent().size());
        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(230, courseEntity.get().getRequiredExpOfLevel(0));

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
        assertEquals(3, userQuestChainRepository.findAll().size());
        assertEquals(0, lecturerQuestChainEntity.getUserLevel());
        assertEquals(0, user1QuestChainEntity.getUserLevel());
        assertEquals(1, user2QuestChainEntity.getUserLevel());

    }

    /**
     * Tests the deletion of badges and quests associated with a flashCardSet, but with a not existing course id.
     * <p>
     * This test verifies that when a flashCardSet, is tried to be deleted for a not existing course,
     * that badges and quests related to other entities (e.g., quizzes) are not affected.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void deleteBadgesAndQuestOfFlashCardSetForNotExistingCourseTest() {
        gamificationController.finishFlashCardSet(user1UUID, courseUUID, flashCardSetUUID, 5, 5, chapterUUID);
        gamificationController.finishQuiz(user2UUID, courseUUID, quizUUID, 5, 5, chapterUUID);

        UUID course = UUID.randomUUID();
        assertEquals("Error at deleting flashCardSet.", gamificationController.deleteBadgesAndQuestOfFlashCardSet(
                flashCardSetUUID, course, chapterUUID));

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getContent().size());
        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(230, courseEntity.get().getRequiredExpOfLevel(0));

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
        assertEquals(3, userQuestChainRepository.findAll().size());
        assertEquals(0, lecturerQuestChainEntity.getUserLevel());
        assertEquals(0, user1QuestChainEntity.getUserLevel());
        assertEquals(1, user2QuestChainEntity.getUserLevel());

    }

    /**
     * Tests the deletion of badges and quests associated with a flashCardSet, but with a wrong course id.
     * <p>
     * This test verifies that when a flashCardSet, is tried to be deleted for the wrong course,
     * that badges and quests related to other entities (e.g., quizzes) are not affected.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void deleteBadgesAndQuestOfFlashCardSetForWrongCourseTest() {
        gamificationController.finishFlashCardSet(user1UUID, courseUUID, flashCardSetUUID, 5, 5, chapterUUID);
        gamificationController.finishQuiz(user2UUID, courseUUID, quizUUID, 5, 5, chapterUUID);

        UUID course = UUID.randomUUID();
        gamificationController.addCourse(course, UUID.randomUUID(), List.of(UUID.randomUUID()));

        assertEquals("Error at deleting flashCardSet.", gamificationController.deleteBadgesAndQuestOfFlashCardSet(
                flashCardSetUUID, course, chapterUUID));

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getContent().size());
        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(230, courseEntity.get().getRequiredExpOfLevel(0));

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
        assertEquals(4, userQuestChainRepository.findAll().size());
        assertEquals(0, lecturerQuestChainEntity.getUserLevel());
        assertEquals(0, user1QuestChainEntity.getUserLevel());
        assertEquals(1, user2QuestChainEntity.getUserLevel());

    }

    /**
     * Tests the deletion of badges and quests associated with a flashCardSet, but with a wrong chapter id.
     * <p>
     * This test verifies that when a flashCardSet, is tried to be deleted for a course, that is not in the course,
     * that badges and quests related to other entities (e.g., quizzes) are not affected.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void deleteBadgesAndQuestOfFlashCardSetForNotExistingChapterTest() {
        gamificationController.finishFlashCardSet(user1UUID, courseUUID, flashCardSetUUID, 5, 5, chapterUUID);
        gamificationController.finishQuiz(user2UUID, courseUUID, quizUUID, 5, 5, chapterUUID);

        UUID chapter = UUID.randomUUID();
        assertEquals("Error at deleting flashCardSet.", gamificationController.deleteBadgesAndQuestOfFlashCardSet(
                flashCardSetUUID, courseUUID, chapter));

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getContent().size());
        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(230, courseEntity.get().getRequiredExpOfLevel(0));

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
        assertEquals(3, userQuestChainRepository.findAll().size());
        assertEquals(0, lecturerQuestChainEntity.getUserLevel());
        assertEquals(0, user1QuestChainEntity.getUserLevel());
        assertEquals(1, user2QuestChainEntity.getUserLevel());

    }

    /**
     * Tests the changing of a flashCardSets data.
     * <p>
     * This test verifies that when a flashCardSets data is changed, the descriptions of the associated badges and quests
     * are updated accordingly. The test checks that all related entities reflect the new flashCardSet name.
     * The flashCardSets metadata is updated as well.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>The flashCardSet name and metadata is changed successfully.</li>
     *   <li>All badges associated with the flashCardSet have their descriptions updated.</li>
     *   <li>The quest associated with the flashCardSet in the quest chain also has its description updated.</li>
     *   <li>The metadata of the flashCardSet is changed accordingly.</li>
     * </ul>
     */
    @Test
    void editFlashCardSetTest() {
        String newName = "New Name";
        assertEquals("Changed flashCardSet data!", gamificationController.editFlashCardSet(flashCardSetUUID,
                courseUUID, newName, 55, List.of(SkillType.UNDERSTAND)));

        List<BadgeEntity> fcsBadges = badgeRepository.findByFlashCardSetUUID(flashCardSetUUID);
        assertEquals(3, fcsBadges.size());
        for (BadgeEntity badge : fcsBadges) {
            int passingPercentage = badge.getPassingPercentage();
            assertEquals(BadgeService.descriptionPart1 + passingPercentage + BadgeService.descriptionPart2 +
                    "flashCardSet " + newName + BadgeService.descriptionPart3, badge.getDescription());
        }

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        int index = questChainEntity.findIndexOfFlashCardSetQuest(flashCardSetUUID);
        QuestEntity quest = questChainEntity.getQuest(index);
        assertEquals(QuestService.descriptionPart1 + "flashCardSet " + newName +
                QuestService.descriptionPart2 + 80 + QuestService.descriptionPart3, quest.getDescription());

        Optional<ContentMetaDataEntity> flashCardSetMetaData = contentMetaDataRepository.findById(flashCardSetUUID);

        assertTrue(flashCardSetMetaData.isPresent());
        assertEquals(SkillType.UNDERSTAND, flashCardSetMetaData.get().getSkillType());
        assertEquals(55, flashCardSetMetaData.get().getSkillPoints());
    }

    /**
     * Tests the editing of a flashCardSet, that does not exist.
     * <p>
     * This test verifies that trying to edit a flashCardSet that does not exist, fails and that the metadata is not changed.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void editFlashCardSetForNotExistingFlashCardSetTest() {

        UUID flashCardSet = UUID.randomUUID();
        String name = "New name";
        assertEquals("Error at editing flashCardSet.",
                gamificationController.editFlashCardSet(flashCardSet, courseUUID, name,55,
                        List.of(SkillType.UNDERSTAND)));

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getContent().size());
        assertFalse(courseEntity.get().getContent().contains(flashCardSet));

        List<BadgeEntity> fcsBadges = badgeRepository.findByFlashCardSetUUID(flashCardSet);
        assertEquals(0, fcsBadges.size());
        assertEquals(6, badgeRepository.count());
        assertEquals(18, userBadgeRepository.count());

        assertEquals(2, contentMetaDataRepository.count());
        Optional<ContentMetaDataEntity> flashCardSetMetaData = contentMetaDataRepository.findById(flashCardSet);
        assertTrue(flashCardSetMetaData.isEmpty());

    }

    /**
     * Tests the editing of a flashCardSet for a course, that does not exist.
     * <p>
     * This test verifies that the editing of a flashCardSet for a course, that does not exist, fails.
     * There should also be no changes to the repositories.
     * <p>
     *
     * Expected Outcome:
     * <ul>
     *   <li>The flashCardSet is not edited.</li>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void editFlashCardSetForNotExistingCourseTest() {

        UUID course = UUID.randomUUID();
        String name = "FCS 2";
        assertEquals("Error at editing flashCardSet.",
                gamificationController.editFlashCardSet(flashCardSetUUID, course, name,
                        70, List.of(SkillType.REMEMBER)));

        Optional<CourseEntity> courseEntity = courseRepository.findById(course);
        assertFalse(courseEntity.isPresent());

        assertEquals(6, badgeRepository.findAll().size());
        List<BadgeEntity> fcsBadges = badgeRepository.findByFlashCardSetUUID(flashCardSetUUID);
        assertEquals(3, fcsBadges.size());
        assertEquals(18, userBadgeRepository.findAll().size());

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(course);
        assertEquals(1, questChainRepository.findAll().size());
        assertNull(questChainEntity);

    }

    /**
     * Tests the editing of a flashCardSet for a course, that does not contain the flashCardSet.
     * <p>
     * This test verifies that the editing of a flashCardSet for the wrong course, fails.
     * There should also be no changes to the repositories.
     * <p>
     *
     * Expected Outcome:
     * <ul>
     *   <li>The flashCardSet is not edited.</li>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void editFlashCardSetForWrongCourseTest() {

        UUID course = UUID.randomUUID();
        gamificationController.addCourse(courseUUID, UUID.randomUUID(), List.of(UUID.randomUUID()));

        String name = "FCS 2";
        assertEquals("Error at editing flashCardSet.",
                gamificationController.editFlashCardSet(flashCardSetUUID, course, name,
                        70, List.of(SkillType.REMEMBER)));

        Optional<CourseEntity> courseEntity = courseRepository.findById(course);
        assertFalse(courseEntity.isPresent());

        assertEquals(6, badgeRepository.findAll().size());
        List<BadgeEntity> fcsBadges = badgeRepository.findByFlashCardSetUUID(flashCardSetUUID);
        assertEquals(3, fcsBadges.size());
        assertEquals(18, userBadgeRepository.findAll().size());

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(course);
        assertEquals(1, questChainRepository.findAll().size());
        assertNull(questChainEntity);

    }

    /**
     * Tests the editing of a flashCardSet for invalid values of skillPoints.
     * <p>
     * This test verifies that the editing of a flashCardSet, for skillPoints < 0 or > 100 fails
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>The flashCardSet is not edited.</li>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void editFlashCardSetForSkillPointsOutOfRangeTest() {

        String name = "FCS 2";
        assertEquals("Error at editing flashCardSet.",
                gamificationController.editFlashCardSet(flashCardSetUUID, courseUUID, name,
                        -1, List.of(SkillType.UNDERSTAND)));

        Optional<ContentMetaDataEntity> contentMetaDataEntity = contentMetaDataRepository.findById(flashCardSetUUID);
        assertTrue(contentMetaDataEntity.isPresent());
        assertEquals(flashCardSetUUID, contentMetaDataEntity.get().getContentUUID());
        assertEquals(60, contentMetaDataEntity.get().getSkillPoints());
        assertEquals(SkillType.APPLY, contentMetaDataEntity.get().getSkillType());

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getContent().size());
        assertTrue(courseEntity.get().getContent().contains(flashCardSetUUID));
        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(230, courseEntity.get().getRequiredExpOfLevel(0));

        assertEquals(6, badgeRepository.findAll().size());
        List<BadgeEntity> fcsBadges = badgeRepository.findByFlashCardSetUUID(flashCardSetUUID);
        assertEquals(3, fcsBadges.size());
        assertEquals(18, userBadgeRepository.findAll().size());

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertEquals(1, questChainRepository.findAll().size());
        assertNotNull(questChainEntity);
        assertEquals(2, questChainEntity.size());
        assertTrue(questChainEntity.getQuests().stream().anyMatch(
                questEntity -> { return flashCardSetUUID.equals(questEntity.getFlashCardSetUUID());}));


        assertEquals("Error at editing flashCardSet.",
                gamificationController.editFlashCardSet(flashCardSetUUID, courseUUID, name,
                        101, List.of(SkillType.APPLY)));

        contentMetaDataEntity = contentMetaDataRepository.findById(flashCardSetUUID);
        assertTrue(contentMetaDataEntity.isPresent());
        assertEquals(flashCardSetUUID, contentMetaDataEntity.get().getContentUUID());
        assertEquals(60, contentMetaDataEntity.get().getSkillPoints());
        assertEquals(SkillType.APPLY, contentMetaDataEntity.get().getSkillType());

        courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getContent().size());
        assertTrue(courseEntity.get().getContent().contains(flashCardSetUUID));
        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(230, courseEntity.get().getRequiredExpOfLevel(0));

        assertEquals(6, badgeRepository.findAll().size());
        fcsBadges = badgeRepository.findByFlashCardSetUUID(flashCardSetUUID);
        assertEquals(3, fcsBadges.size());
        assertEquals(18, userBadgeRepository.findAll().size());

        questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertEquals(1, questChainRepository.findAll().size());
        assertNotNull(questChainEntity);
        assertEquals(2, questChainEntity.size());
        assertTrue(questChainEntity.getQuests().stream().anyMatch(
                questEntity -> { return flashCardSetUUID.equals(questEntity.getFlashCardSetUUID());}));


    }

    /**
     * Tests the editing of a flashCardSet for an invalid value of skillTypes.
     * <p>
     * This test verifies that the editing of a flashCardSet, for skillPoints null or an empty list</>
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>The flashCardSet is not edited.</li>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void editFlashCardSetForEmptySkillTypesTest() {

        String name = "FCS 2";
        assertEquals("Error at editing flashCardSet.",
                gamificationController.editFlashCardSet(flashCardSetUUID, courseUUID, name,
                        65, null));

        Optional<ContentMetaDataEntity> contentMetaDataEntity = contentMetaDataRepository.findById(flashCardSetUUID);
        assertTrue(contentMetaDataEntity.isPresent());
        assertEquals(flashCardSetUUID, contentMetaDataEntity.get().getContentUUID());
        assertEquals(60, contentMetaDataEntity.get().getSkillPoints());
        assertEquals(SkillType.APPLY, contentMetaDataEntity.get().getSkillType());

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getContent().size());
        assertTrue(courseEntity.get().getContent().contains(flashCardSetUUID));
        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(230, courseEntity.get().getRequiredExpOfLevel(0));

        assertEquals(6, badgeRepository.findAll().size());
        List<BadgeEntity> fcsBadges = badgeRepository.findByFlashCardSetUUID(flashCardSetUUID);
        assertEquals(3, fcsBadges.size());
        assertEquals(18, userBadgeRepository.findAll().size());

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertEquals(1, questChainRepository.findAll().size());
        assertNotNull(questChainEntity);
        assertEquals(2, questChainEntity.size());
        assertTrue(questChainEntity.getQuests().stream().anyMatch(
                questEntity -> { return flashCardSetUUID.equals(questEntity.getFlashCardSetUUID());}));


        assertEquals("Error at editing flashCardSet.",
                gamificationController.editFlashCardSet(flashCardSetUUID, courseUUID, name,
                        65, List.of()));

        contentMetaDataEntity = contentMetaDataRepository.findById(flashCardSetUUID);
        assertTrue(contentMetaDataEntity.isPresent());
        assertEquals(flashCardSetUUID, contentMetaDataEntity.get().getContentUUID());
        assertEquals(60, contentMetaDataEntity.get().getSkillPoints());
        assertEquals(SkillType.APPLY, contentMetaDataEntity.get().getSkillType());

        courseEntity = courseRepository.findById(courseUUID);
        assertTrue(courseEntity.isPresent());
        assertEquals(2, courseEntity.get().getContent().size());
        assertTrue(courseEntity.get().getContent().contains(flashCardSetUUID));
        assertEquals(1, courseEntity.get().getRequiredExpPerLevel().size());
        assertEquals(230, courseEntity.get().getRequiredExpOfLevel(0));

        assertEquals(6, badgeRepository.findAll().size());
        fcsBadges = badgeRepository.findByFlashCardSetUUID(flashCardSetUUID);
        assertEquals(3, fcsBadges.size());
        assertEquals(18, userBadgeRepository.findAll().size());

        questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        assertEquals(1, questChainRepository.findAll().size());
        assertNotNull(questChainEntity);
        assertEquals(2, questChainEntity.size());
        assertTrue(questChainEntity.getQuests().stream().anyMatch(
                questEntity -> { return flashCardSetUUID.equals(questEntity.getFlashCardSetUUID());}));


    }

    /**
     * Tests the completion of a flashCardSet by different users.
     * <p>
     * This test verifies that when users complete a flashCardSet, their progress is updated correctly in the
     * badges and quest chain entities. The test checks that the correct badges are marked as achieved
     * based on the users' flashCardSet performance.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>Users complete the flashCardSet successfully.</li>
     *   <li>Badges are marked as achieved or not based on the user's performance.</li>
     *   <li>The quest chain reflects the user's progress, not updating their levels accordingly,
     *   as their current quest is not completing the flashCardSet.</li>
     *   <li>The users receive experience points.</li>
     * </ul>
     */
    @Test
    void finishFlashCardSetTest() {
        assertEquals("Finished flashCardSet!",
                gamificationController.finishFlashCardSet(lecturerUUID, courseUUID, flashCardSetUUID, 8, 10, chapterUUID));
        assertEquals("Finished flashCardSet!",
                gamificationController.finishFlashCardSet(user1UUID, courseUUID, flashCardSetUUID, 5, 10, chapterUUID));
        assertEquals("Finished flashCardSet!",
                gamificationController.finishFlashCardSet(user2UUID, courseUUID, flashCardSetUUID, 2, 10, chapterUUID));

        List<BadgeEntity> fcsBadges = badgeRepository.findByFlashCardSetUUID(flashCardSetUUID);
        assertEquals(3, fcsBadges.size());
        List<UserBadgeEntity> lecturerFCSBadges = new ArrayList<UserBadgeEntity>();
        List<UserBadgeEntity> user1FCSBadges = new ArrayList<UserBadgeEntity>();
        List<UserBadgeEntity> user2FCSBadges = new ArrayList<UserBadgeEntity>();
        for (BadgeEntity badge : fcsBadges) {
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
        assertEquals(0, lecturerQuestChainEntity.getUserLevel());
        assertEquals(0, user1QuestChainEntity.getUserLevel());
        assertEquals(0, user2QuestChainEntity.getUserLevel());

        BloomLevelEntity lecturerBloomLevel = bloomLevelRepository.findByUserUUIDAndCourseUUID(lecturerUUID, courseUUID);
        BloomLevelEntity user1BloomLevel = bloomLevelRepository.findByUserUUIDAndCourseUUID(user1UUID, courseUUID);
        BloomLevelEntity user2BloomLevel = bloomLevelRepository.findByUserUUIDAndCourseUUID(user2UUID, courseUUID);
        assertNotNull(lecturerBloomLevel);
        assertNotNull(user1BloomLevel);
        assertNotNull(user2BloomLevel);
        assertEquals(43, lecturerBloomLevel.getCollectedExp());
        assertEquals(27, user1BloomLevel.getCollectedExp());
        assertEquals(10, user2BloomLevel.getCollectedExp());

    }

    /**
     * Tests the completion of a flashCardSet by a users for an invalid number of correct answers.
     * <p>
     * This test verifies that when users complete a flashCardSet with an invalid number of correct answers,
     * their progress is not updated correctly in the badges and quest chain entities.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>No changes for badges, quests, or the users Bloom Level.</li>
     * </ul>
     */
    @Test
    void finishFlashCardSetForCorrectAnswersOutOfBoundsTest() {
        assertEquals("Error at finishing flashCardSet.",
                gamificationController.finishFlashCardSet(lecturerUUID, courseUUID, flashCardSetUUID, 11, -1, chapterUUID));

        List<BadgeEntity> fcsBadges = badgeRepository.findByFlashCardSetUUID(flashCardSetUUID);
        assertEquals(3, fcsBadges.size());
        List<UserBadgeEntity> lecturerFCSBadges = new ArrayList<UserBadgeEntity>();
        for (BadgeEntity badge : fcsBadges) {
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



        assertEquals("Error at finishing flashCardSet.",
                gamificationController.finishFlashCardSet(lecturerUUID, courseUUID, flashCardSetUUID, 11, -1, chapterUUID));

        lecturerFCSBadges = new ArrayList<UserBadgeEntity>();
        for (BadgeEntity badge : fcsBadges) {
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
     * Tests the completion of a flashCardSet, that does not exist.
     * <p>
     * This test verifies that when users complete a flashCardSet, that does not exist, their progress is not updated
     * in the badges and quest chain entities.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void finishNotExistingFlashCardSetTest() {

        UUID flashCardSet = UUID.randomUUID();
        assertEquals("Error at finishing flashCardSet.",
                gamificationController.finishFlashCardSet(lecturerUUID, courseUUID, flashCardSet, 8, 10, chapterUUID));

        List<BadgeEntity> fcsBadges = badgeRepository.findByFlashCardSetUUID(flashCardSet);
        assertEquals(0, fcsBadges.size());
        List<UserBadgeEntity> lecturerFCSBadges = new ArrayList<UserBadgeEntity>();
        for (BadgeEntity badge : fcsBadges) {
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
     * Tests the completion of a flashCardSet, which is in a course, where the user is not present.
     * <p>
     * This test verifies that when users tries to complete a flashCardSet, where it has no access to,
     * their progress is not updated in the badges and quest chain entities.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void finishFlashCardSetForUserNotInCourseTest() {

        UUID user = UUID.randomUUID();
        assertEquals("Error at finishing flashCardSet.",
                gamificationController.finishFlashCardSet(user, courseUUID, flashCardSetUUID, 8, 10, chapterUUID));

        List<BadgeEntity> fcsBadges = badgeRepository.findByFlashCardSetUUID(flashCardSetUUID);
        assertEquals(3, fcsBadges.size());
        for (BadgeEntity badge : fcsBadges) {
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
     * Tests the completion of a flashCardSet, with a course, that does not exist and the wrong course id.
     * <p>
     * This test verifies that when users complete a flashCardSet, with a course, that does not exist, or
     * the wrong course id, their progress is not updated in the badges and quest chain entities.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void finishFlashCardSetForWrongCourseTest() {

        UUID course = UUID.randomUUID();
        assertEquals("Error at finishing flashCardSet.",
                gamificationController.finishFlashCardSet(lecturerUUID, course, flashCardSetUUID, 8, 10, chapterUUID));

        List<BadgeEntity> fcsBadges = badgeRepository.findByFlashCardSetUUID(flashCardSetUUID);
        assertEquals(3, fcsBadges.size());
        List<UserBadgeEntity> lecturerFCSBadges = new ArrayList<UserBadgeEntity>();
        for (BadgeEntity badge : fcsBadges) {
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
        assertEquals("Error at finishing flashCardSet.",
                gamificationController.finishFlashCardSet(lecturerUUID, course, flashCardSetUUID, 8, 10, chapterUUID));

        fcsBadges = badgeRepository.findByFlashCardSetUUID(flashCardSetUUID);
        assertEquals(3, fcsBadges.size());
        lecturerFCSBadges = new ArrayList<UserBadgeEntity>();
        for (BadgeEntity badge : fcsBadges) {
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
     * Tests the completion of a flashCardSet, for a chapter, that does not exist.
     * <p>
     * This test verifies that when users complete a flashCardSet, in a chapter, that does not exist,
     * their progress is not updated in the badges and quest chain entities.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>No changes in the repositories.</li>
     * </ul>
     */
    @Test
    void finishNotExistingFlashCardSetForNotExistingChapterTest() {

        UUID chapter = UUID.randomUUID();
        assertEquals("Error at finishing flashCardSet.",
                gamificationController.finishFlashCardSet(lecturerUUID, courseUUID, flashCardSetUUID, 8, 10, chapter));

        List<BadgeEntity> fcsBadges = badgeRepository.findByFlashCardSetUUID(flashCardSetUUID);
        assertEquals(3, fcsBadges.size());
        List<UserBadgeEntity> lecturerFCSBadges = new ArrayList<UserBadgeEntity>();
        for (BadgeEntity badge : fcsBadges) {
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