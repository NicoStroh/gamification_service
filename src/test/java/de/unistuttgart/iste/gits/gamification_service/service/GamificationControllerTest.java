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
class GamificationControllerTest {

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
    private PlayerTypeRepository playerTypeRepository;

    @Autowired
    private QuestChainRepository questChainRepository;

    @Autowired
    private UserBadgeRepository userBadgeRepository;

    @Autowired
    private UserQuestChainRepository userQuestChainRepository;

    @Autowired
    private GamificationController gamificationController;


    @Test
    void testEvaluationTest() {
        TestUtils.populatePlayerTypeRepository(playerTypeRepository);

        List<PlayerTypeEntity> playerTypeEntityList = playerTypeRepository.findAll();
        assertEquals(1024, playerTypeEntityList.size());
        for (PlayerTypeEntity playerTypeEntity : playerTypeEntityList) {
            assertTrue(playerTypeEntity.isUserHasTakenTest());
            assertNotNull(playerTypeEntity.getDominantPlayerType());
            assertNotEquals(PlayerTypeEntity.DominantPlayerType.None, playerTypeEntity.getDominantPlayerType());

            int sum = playerTypeEntity.getAchieverPercentage() + playerTypeEntity.getExplorerPercentage() + playerTypeEntity.getSocializerPercentage() + playerTypeEntity.getKillerPercentage();
            assertEquals(200, sum);
            assertTrue(0 <= playerTypeEntity.getAchieverPercentage());
            assertTrue(0 <= playerTypeEntity.getExplorerPercentage());
            assertTrue(0 <= playerTypeEntity.getSocializerPercentage());
            assertTrue(0 <= playerTypeEntity.getKillerPercentage());

            assertTrue(100 >= playerTypeEntity.getAchieverPercentage());
            assertTrue(100 >= playerTypeEntity.getExplorerPercentage());
            assertTrue(100 >= playerTypeEntity.getSocializerPercentage());
            assertTrue(100 >= playerTypeEntity.getKillerPercentage());
        }

    }

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

    @Test
    void createFlashCardSetTest() {

        UUID courseUUID = UUID.randomUUID();
        UUID lecturerUUID = UUID.randomUUID();
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        TestUtils.createTestCourse(gamificationController, courseUUID, lecturerUUID, user1, user2,
                UUID.randomUUID(), UUID.randomUUID());

        UUID flashCardSetUUID = UUID.randomUUID();
        String name = "FCS 2";
        assertEquals("Created Flashcardset successfully.",
                gamificationController.createFlashCardSet(flashCardSetUUID, name, courseUUID));

        assertEquals(9, badgeRepository.findAll().size());
        List<BadgeEntity> fcsBadges = badgeRepository.findByFlashCardSetUUID(flashCardSetUUID);
        List<UserBadgeEntity> fcsUserBadges = new LinkedList<>();
        assertEquals(3, fcsBadges.size());
        for (BadgeEntity badge : fcsBadges) {
            int passingPercentage = badge.getPassingPercentage();
            assertEquals(courseUUID, badge.getCourseUUID());
            assertTrue(50 == passingPercentage || 70 == passingPercentage || 90 == passingPercentage);
            assertEquals(flashCardSetUUID, badge.getFlashCardSetUUID());
            assertNull(badge.getQuizUUID());
            assertEquals(BadgeService.descriptionPart1 + passingPercentage + BadgeService.descriptionPart2 +
                    "flashcardSet " + name + BadgeService.descriptionPart3, badge.getDescription());

            fcsUserBadges.addAll(userBadgeRepository.findByBadgeUUID(badge.getBadgeUUID()));
        }

        assertEquals(27, userBadgeRepository.findAll().size());
        assertEquals(9, fcsUserBadges.size());
        for (UserBadgeEntity userBadge : fcsUserBadges) {
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
    void deleteBadgesAndQuestsOfCourseTest(@Argument UUID courseUUID) {
        courseService.deleteCourse(courseUUID);
        badgeService.deleteBadgesAndUserBadgesOfCourse(courseUUID);
        questService.deleteQuestChainAndUserQuestChainsOfCourse(courseUUID);
        return "Course deleted.";
    }

    @Test
    void deleteBadgesAndQuestOfFlashCardSetTest(@Argument UUID flashcardSetUUID, @Argument UUID courseUUID) {
        badgeService.deleteBadgesAndUserBadgesOfFCS(flashcardSetUUID);
        questService.deleteQuestOfFCS(courseUUID, flashcardSetUUID);
        return "FlashcardSet deleted.";
    }

    @Test
    void deleteBadgesAndQuestOfQuizTest(@Argument UUID quizUUID, @Argument UUID courseUUID) {
        badgeService.deleteBadgesAndUserBadgesOfQuiz(quizUUID);
        questService.deleteQuestOfQuiz(courseUUID, quizUUID);
        return "Quiz deleted.";
    }

    @Test
    void editFlashcardSetNameTest(@Argument UUID flashcardSetUUID,
                                       @Argument UUID courseUUID,
                                       @Argument String name) {
        badgeService.changeFlashCardSetName(flashcardSetUUID, name);
        questService.changeFlashcardSetName(flashcardSetUUID, courseUUID, name);
        return "Changed flashcardset name!";
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
    void evaluateTestTest(@Argument UUID userUUID) {

        if (this.test != null && !this.test.justCreated) {
            PlayerTypeEntity playerTypeEntity = this.test.evaluateTest(userUUID);
            return playerTypeService.saveTestResult(playerTypeEntity);
        }
        return new PlayerTypeEntity(userUUID, false);

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

    @Test
    void finishFlashCardSetTest(@Argument UUID userUUID,
                                     @Argument UUID courseUUID,
                                     @Argument UUID flashCardSetUUID,
                                     @Argument int correctAnswers,
                                     @Argument int totalAnswers) {
        badgeService.markBadgesAsAchievedIfPassedFlashCardSet(userUUID, flashCardSetUUID, correctAnswers, totalAnswers);
        questService.markQuestAsFinishedIfPassedFlashCardSet(userUUID, courseUUID, flashCardSetUUID, correctAnswers, totalAnswers);
        return "Finished FlashCardSet!";
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

    @Test
    void submitAnswerTest(@Argument int questionId, @Argument boolean answer) {

        if (this.test != null) {
            this.test.setAnswer(questionId, answer);
            return "Answer submitted successfully!";
        }
        return "No test selected!";

    }
    */

    @Test
    void testTest() {
        PlayerTypeTestQuestion[] questions = gamificationController.test();
        assertNotNull(questions);
        assertEquals(10, questions.length);

        int i = 0;
        assertEquals(i, questions[i].getId());
        assertEquals("Are you interested in the Bloom's Taxonomy level of other students?", questions[i].getText());
        assertEquals("Yes", questions[i].getOption0());
        assertEquals("No", questions[i].getOption1());
        assertFalse(questions[i].getSelectedOption());


        i = 1;
        assertEquals(i, questions[i].getId());
        assertEquals("Would you like to see which position you have on a leaderboard?", questions[i].getText());
        assertEquals("Yes", questions[i].getOption0());
        assertEquals("No", questions[i].getOption1());
        assertFalse(questions[i].getSelectedOption());


        i = 2;
        assertEquals(i, questions[i].getId());
        assertEquals("Are you interested in who has gathered the most experience points in the month?", questions[i].getText());
        assertEquals("Yes", questions[i].getOption0());
        assertEquals("No", questions[i].getOption1());
        assertFalse(questions[i].getSelectedOption());


        i = 3;
        assertEquals(i, questions[i].getId());
        assertEquals("Do you like to collect experience points?", questions[i].getText());
        assertEquals("Yes", questions[i].getOption0());
        assertEquals("No", questions[i].getOption1());
        assertFalse(questions[i].getSelectedOption());


        i = 4;
        assertEquals(i, questions[i].getId());
        assertEquals("Is a user profile important for you?", questions[i].getText());
        assertEquals("Yes", questions[i].getOption0());
        assertEquals("No", questions[i].getOption1());
        assertFalse(questions[i].getSelectedOption());


        i = 5;
        assertEquals(i, questions[i].getId());
        assertEquals("Do you like to display badges or achievements in your user profile?", questions[i].getText());
        assertEquals("Yes", questions[i].getOption0());
        assertEquals("No", questions[i].getOption1());
        assertFalse(questions[i].getSelectedOption());


        i = 6;
        assertEquals(i, questions[i].getId());
        assertEquals("Do you like to have a level system?", questions[i].getText());
        assertEquals("Yes", questions[i].getOption0());
        assertEquals("No", questions[i].getOption1());
        assertFalse(questions[i].getSelectedOption());


        i = 7;
        assertEquals(i, questions[i].getId());
        assertEquals("Do you like to customize your avatar/user profile with for example clothes, hats, ...?", questions[i].getText());
        assertEquals("Yes", questions[i].getOption0());
        assertEquals("No", questions[i].getOption1());
        assertFalse(questions[i].getSelectedOption());


        i = 8;
        assertEquals(i, questions[i].getId());
        assertEquals("Do you like to unlock new or hidden content?", questions[i].getText());
        assertEquals("Yes", questions[i].getOption0());
        assertEquals("No", questions[i].getOption1());
        assertFalse(questions[i].getSelectedOption());


        i = 9;
        assertEquals(i, questions[i].getId());
        assertEquals("If you have the choice to beat an end boss in a team or alone, what would you choose?", questions[i].getText());
        assertEquals("Fighting in a team", questions[i].getOption0());
        assertEquals("Fighting alone", questions[i].getOption1());
        assertFalse(questions[i].getSelectedOption());
    }

    @Test
    void userHasTakenTestTest() {
        UUID user1 = UUID.randomUUID();
        gamificationController.evaluateTest(user1);
        assertFalse(gamificationController.userHasTakenTest(user1));

        gamificationController.submitAnswer(0, true);
        assertFalse(gamificationController.userHasTakenTest(user1));

        gamificationController.test();
        gamificationController.submitAnswer(0, true);
        gamificationController.evaluateTest(user1);
        assertTrue(gamificationController.userHasTakenTest(user1));


        UUID user2 = UUID.randomUUID();
        gamificationController.test();
        gamificationController.submitAnswer(0, true);
        assertFalse(gamificationController.userHasTakenTest(user2));


        UUID user3 = UUID.randomUUID();
        gamificationController.test();
        assertFalse(gamificationController.userHasTakenTest(user3));


        UUID user4 = UUID.randomUUID();
        assertFalse(gamificationController.userHasTakenTest(user4));
    }

    @Test
    void usersDominantPlayerTypeTest() {
        UUID user1 = UUID.randomUUID();
        gamificationController.test();
        gamificationController.submitAnswer(0, true);
        gamificationController.evaluateTest(user1);

        PlayerTypeEntity.DominantPlayerType dominantPlayerType1 = gamificationController.usersDominantPlayerType(user1);
        assertNotNull(dominantPlayerType1);
        assertNotEquals(PlayerTypeEntity.DominantPlayerType.None, dominantPlayerType1);


        UUID user2 = UUID.randomUUID();
        gamificationController.test();
        gamificationController.submitAnswer(0, true);

        PlayerTypeEntity.DominantPlayerType dominantPlayerType2 = gamificationController.usersDominantPlayerType(user2);
        assertEquals(PlayerTypeEntity.DominantPlayerType.None, dominantPlayerType2);


        UUID user3 = UUID.randomUUID();
        gamificationController.test();

        PlayerTypeEntity.DominantPlayerType dominantPlayerType3 = gamificationController.usersDominantPlayerType(user3);
        assertEquals(PlayerTypeEntity.DominantPlayerType.None, dominantPlayerType3);


        UUID user4 = UUID.randomUUID();

        PlayerTypeEntity.DominantPlayerType dominantPlayerType4 = gamificationController.usersDominantPlayerType(user4);
        assertEquals(PlayerTypeEntity.DominantPlayerType.None, dominantPlayerType4);


        UUID user5 = UUID.randomUUID();
        gamificationController.evaluateTest(user5);

        PlayerTypeEntity.DominantPlayerType dominantPlayerType5 = gamificationController.usersDominantPlayerType(user5);
        assertEquals(PlayerTypeEntity.DominantPlayerType.None, dominantPlayerType5);
    }

}