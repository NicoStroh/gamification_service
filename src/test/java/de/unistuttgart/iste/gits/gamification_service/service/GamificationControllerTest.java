package de.unistuttgart.iste.gits.gamification_service.service;

import de.unistuttgart.iste.gits.gamification_service.test_utils.TestUtils;
import de.unistuttgart.iste.meitrex.gamification_service.GamificationApplication;
import de.unistuttgart.iste.meitrex.gamification_service.controller.GamificationController;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.*;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.mapper.BadgeMapper;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.mapper.QuestMapper;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.*;
import de.unistuttgart.iste.meitrex.gamification_service.service.*;
import org.junit.jupiter.api.BeforeEach;
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


    @BeforeEach
    void setGamificationController() {
        PlayerTypeService playerTypeService = new PlayerTypeService(playerTypeRepository);
        CourseService courseService = new CourseService(courseRepository);
        BadgeService badgeService = new BadgeService(badgeRepository, userBadgeRepository, new BadgeMapper());
        QuestService questService = new QuestService(questChainRepository, userQuestChainRepository, new QuestMapper());
        this.gamificationController = new GamificationController(playerTypeService, courseService, badgeService, questService);
    }


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

    /*

    @Test
    void addUserToCourseTest(@Argument UUID userUUID, @Argument UUID courseUUID) {
        courseService.addUserToCourse(userUUID, courseUUID);
        badgeService.assignCoursesBadgesToUser(courseUUID, userUUID);
        questService.assignQuestChainToUser(userUUID, courseUUID);
        return "Added user to course.";
    }

    @Test
    void createFlashCardSetTest(@Argument UUID flashCardSetUUID,
                                     @Argument String name,
                                     @Argument UUID courseUUID) {
        badgeService.createBadgesForFlashCardSet(flashCardSetUUID, name, courseUUID, courseService.getCoursesUsers(courseUUID));
        questService.createQuestForFlashCardSet(flashCardSetUUID, name, courseUUID);
        return "Created Flashcardset successfully.";
    }

    @Test
    void createQuizTest(@Argument UUID quizUUID,
                             @Argument String name,
                             @Argument UUID courseUUID) {
        badgeService.createBadgesForQuiz(quizUUID, name, courseUUID, courseService.getCoursesUsers(courseUUID));
        questService.createQuestForQuiz(quizUUID, name, courseUUID);
        return "Created quiz successfully.";
    }

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

    @Test
    void testTest() {
        this.test = new PlayerTypeTest();
        return this.test.getQuestions();
    }

    @Test
    void userHasTakenTestTest(@Argument UUID userUUID) {

        Optional<PlayerTypeEntity> entity = playerTypeService.getPlayerTypeByUserUUID(userUUID);
        if (entity.isEmpty()) {
            // User is not present in playertype_database
            playerTypeService.createUser(userUUID);
            return false;
        }
        return entity.get().isUserHasTakenTest();

    }

    @Test
    void usersDominantPlayerTypeTest(@Argument UUID userUUID) {
        Optional<PlayerTypeEntity> playerType = playerTypeService.getPlayerTypeByUserUUID(userUUID);
        if (playerType.isPresent() && playerType.get().getDominantPlayerType() != null) {
            return playerType.get().getDominantPlayerType();
        } else {
            return PlayerTypeEntity.DominantPlayerType.None;
        }
    }
    */

}