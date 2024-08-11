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

        TestUtils.createTestCourse(gamificationController,
                courseUUID,
                lecturerUUID,
                user1UUID,
                user2UUID,
                quizUUID,
                flashCardSetUUID);
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
                gamificationController.createFlashCardSet(flashCardSet, name, courseUUID, 0));

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
     * Tests the deletion of badges and quests associated with a flashCardSet.
     * <p>
     * This test verifies that when a flashCardSet is deleted, the corresponding badges, userBadges and quests are also removed
     * from the database. It checks that badges and quests related to other entities (e.g., quizzes) are not affected.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>The flashCardSet is deleted successfully.</li>
     *   <li>All badges associated with the flashCardSet are removed.</li>
     *   <li>Other badges and quests remain unaffected.</li>
     *   <li>The users' quest chain progress is not reset, because their currentQuest does not point to the flashCardSet.</li>
     * </ul>
     */
    @Test
    void deleteBadgesAndQuestOfFlashCardSetTest() {
        gamificationController.finishFlashCardSet(user1UUID, courseUUID, flashCardSetUUID, 5, 5, 0);
        gamificationController.finishQuiz(user2UUID, courseUUID, quizUUID, 5, 5, 0);

        assertEquals("FlashCardSet deleted.", gamificationController.deleteBadgesAndQuestOfFlashCardSet(flashCardSetUUID, courseUUID));

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
     * Tests the renaming of a flashCardSet.
     * <p>
     * This test verifies that when a flashCardSet is renamed, the descriptions of the associated badges and quests
     * are updated accordingly. The test checks that all related entities reflect the new flashCardSet name.
     * <p>
     * Expected Outcome:
     * <ul>
     *   <li>The flashCardSet name is changed successfully.</li>
     *   <li>All badges associated with the flashCardSet have their descriptions updated.</li>
     *   <li>The quest associated with the flashCardSet in the quest chain also has its description updated.</li>
     * </ul>
     */
    @Test
    void editFlashCardSetNameTest() {
        String newName = "New Name";
        assertEquals("Changed flashCardSet name!", gamificationController.editFlashCardSetName(flashCardSetUUID, courseUUID, newName));

        List<BadgeEntity> fcsBadges = badgeRepository.findByFlashCardSetUUID(flashCardSetUUID);
        assertEquals(3, fcsBadges.size());
        for (BadgeEntity badge : fcsBadges) {
            int passingPercentage = badge.getPassingPercentage();
            assertEquals(BadgeService.descriptionPart1 + passingPercentage + BadgeService.descriptionPart2 +
                    "flashCardSet " + newName + BadgeService.descriptionPart3, badge.getDescription());
        }

        QuestChainEntity questChainEntity = questChainRepository.findByCourseUUID(courseUUID);
        int index = questChainEntity.findIndexOfFlashcardSetQuest(flashCardSetUUID);
        QuestEntity quest = questChainEntity.getQuest(index);
        assertEquals(QuestService.descriptionPart1 + "flashCardSet " + newName +
                QuestService.descriptionPart2 + 80 + QuestService.descriptionPart3, quest.getDescription());
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
     * </ul>
     */
    @Test
    void finishFlashCardSetTest() {
        assertEquals("Finished flashCardSet!",
                gamificationController.finishFlashCardSet(lecturerUUID, courseUUID, flashCardSetUUID, 8, 10, 0));
        assertEquals("Finished flashCardSet!",
                gamificationController.finishFlashCardSet(user1UUID, courseUUID, flashCardSetUUID, 5, 10, 0));
        assertEquals("Finished flashCardSet!",
                gamificationController.finishFlashCardSet(lecturerUUID, courseUUID, flashCardSetUUID, 11, -1, 0));

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
    }

}