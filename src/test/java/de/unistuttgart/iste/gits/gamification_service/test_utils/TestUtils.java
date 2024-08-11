package de.unistuttgart.iste.gits.gamification_service.test_utils;

import de.unistuttgart.iste.meitrex.gamification_service.controller.GamificationController;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.*;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.BadgeRepository;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.CourseRepository;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.PlayerTypeRepository;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.QuestChainRepository;
import de.unistuttgart.iste.meitrex.gamification_service.service.BadgeService;
import de.unistuttgart.iste.meitrex.gamification_service.service.QuestService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TestUtils {

    /**
     * Helper method which creates some users and saves their playertypes to the repository.
     *
     * @param repo The repository to save the entities to.
     * @return Returns the created playertypes.
     */
    public static List<PlayerTypeEntity> populatePlayerTypeRepository(PlayerTypeRepository repo) {

        List<PlayerTypeEntity> playerTypes = new ArrayList<>();

        int numQuestions = 10;
        int numCombinations = 1 << numQuestions;
        for (int i = 0; i < numCombinations; i++) {
            PlayerTypeTest test = new PlayerTypeTest();

            boolean[] booleans = new boolean[numQuestions];
            // Convert the number to a boolean array
            for (int j = 0; j < numQuestions; j++) {
                booleans[j] = (i & (1 << j)) != 0;
                test.setAnswer(j, booleans[j]);
            }

            UUID userUUID = UUID.randomUUID();
            PlayerTypeEntity playerType = test.evaluateTest(userUUID);
            repo.save(playerType);
            playerTypes.add(playerType);
        }
        return playerTypes;
    }

    /**
     * Helper method which creates a test couse with 3 users, 1 quiz and 1 flashcardSet and
     * the corresponding badges and quests.
     *
     * @param gamificationController  The controller which holds all the repositories and services.
     * @param courseUUID              The id of the course.
     * @param lecturerUUID            The id of the course creator.
     * @param user1                   The first user to join the course before the creation of the quiz and the fcs.
     * @param user2                   The second user to join the course after the creation of the quiz and the fcs.
     * @param quizUUID                The id of the quiz to create.
     * @param fcsUUID                 The id of the flashcardSet to create.
     */
    public static void createTestCourse(GamificationController gamificationController,
                                        UUID courseUUID,
                                        UUID lecturerUUID,
                                        UUID user1,
                                        UUID user2,
                                        UUID quizUUID,
                                        UUID fcsUUID) {
        gamificationController.addCourse(courseUUID, lecturerUUID);
        gamificationController.addUserToCourse(user1, courseUUID);
        gamificationController.createQuiz(quizUUID, "Quiz 1", courseUUID, 0);
        gamificationController.createFlashCardSet(fcsUUID, "FCS 1", courseUUID, 0);
        gamificationController.addUserToCourse(user2, courseUUID);
    }

}
