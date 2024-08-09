package de.unistuttgart.iste.gits.gamification_service.service;

import de.unistuttgart.iste.gits.gamification_service.test_utils.TestUtils;
import de.unistuttgart.iste.meitrex.gamification_service.GamificationApplication;
import de.unistuttgart.iste.meitrex.gamification_service.controller.GamificationController;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.PlayerTypeEntity;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.PlayerTypeTestQuestion;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.PlayerTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = GamificationApplication.class)
@Transactional
class PlayerTypeServiceTest {

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("testdb")
            .withUsername("root")
            .withPassword("root");

    @BeforeAll
    static void startContainer() {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private PlayerTypeRepository playerTypeRepository;

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
    void submitAnswerTest() {
        assertEquals("No test selected!", gamificationController.submitAnswer(0, true));

        gamificationController.test();
        assertEquals("Id out of bounds!", gamificationController.submitAnswer(10, true));
        assertEquals("Id out of bounds!", gamificationController.submitAnswer(-1, true));

        assertEquals("Answer submitted successfully!", gamificationController.submitAnswer(0, true));
    }

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

    @Test
    void evaluateTestTest() {

        UUID user1 = UUID.randomUUID();
        PlayerTypeEntity result1 = gamificationController.evaluateTest(user1);
        assertEquals(user1, result1.getUserUUID());
        assertFalse(result1.isUserHasTakenTest());
        assertEquals(PlayerTypeEntity.DominantPlayerType.None, result1.getDominantPlayerType());
        assertEquals(0, result1.getAchieverPercentage());
        assertEquals(0, result1.getExplorerPercentage());
        assertEquals(0, result1.getSocializerPercentage());
        assertEquals(0, result1.getKillerPercentage());


        UUID user2 = UUID.randomUUID();
        gamificationController.test();
        PlayerTypeEntity result2 = gamificationController.evaluateTest(user2);
        assertEquals(user2, result2.getUserUUID());
        assertFalse(result2.isUserHasTakenTest());
        assertEquals(PlayerTypeEntity.DominantPlayerType.None, result2.getDominantPlayerType());
        assertEquals(0, result2.getAchieverPercentage());
        assertEquals(0, result2.getExplorerPercentage());
        assertEquals(0, result2.getSocializerPercentage());
        assertEquals(0, result2.getKillerPercentage());


        gamificationController.submitAnswer(0, true);
        result2 = gamificationController.evaluateTest(user2);
        assertEquals(user2, result2.getUserUUID());
        assertTrue(result2.isUserHasTakenTest());
        assertNotEquals(PlayerTypeEntity.DominantPlayerType.None, result2.getDominantPlayerType());

        int sum = result2.getAchieverPercentage() + result2.getExplorerPercentage() + result2.getSocializerPercentage() + result2.getKillerPercentage();
        assertEquals(200, sum);
        
        assertTrue(0 <= result2.getAchieverPercentage());
        assertTrue(0 <= result2.getExplorerPercentage());
        assertTrue(0 <= result2.getSocializerPercentage());
        assertTrue(0 <= result2.getKillerPercentage());

        assertTrue(100 >= result2.getAchieverPercentage());
        assertTrue(100 >= result2.getExplorerPercentage());
        assertTrue(100 >= result2.getSocializerPercentage());
        assertTrue(100 >= result2.getKillerPercentage());

    }

}
