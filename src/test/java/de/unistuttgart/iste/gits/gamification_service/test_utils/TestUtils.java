package de.unistuttgart.iste.gits.gamification_service.test_utils;

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


}
