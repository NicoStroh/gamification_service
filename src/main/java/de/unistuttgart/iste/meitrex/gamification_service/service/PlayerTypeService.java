package de.unistuttgart.iste.meitrex.gamification_service.service;

import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.PlayerTypeEntity;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.PlayerTypeTest;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.PlayerTypeTestQuestion;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.PlayerTypeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PlayerTypeService {

    private final PlayerTypeRepository playerTypeRepository;

    private PlayerTypeTest test;

    /**
     * Evaluates and saves the result of a BartleTest for a user.
     *
     * @param userUUID        the users UUID
     * @return a PlayerTypeEntity, representing the result of the users answers
     */
    public PlayerTypeEntity evaluateTest(UUID userUUID) {
        if (this.test != null && !this.test.justCreated) {
            PlayerTypeEntity playerTypeEntity = this.test.evaluateTest(userUUID);
            return playerTypeRepository.save(playerTypeEntity);
        }
        return new PlayerTypeEntity(userUUID, false);
    }

    /**
     * Creates an instance of a BartleTest.
     *
     * @return a PlayerTypeEntity, representing the result of the users answers
     */
    public PlayerTypeTestQuestion[] test() {
        this.test = new PlayerTypeTest();
        return this.test.getQuestions();
    }

    /**
     * Saves the answer for a BartleTest.
     *
     * @param questionId        the id of the question
     * @param answer            the selected answer
     *
     * @return a PlayerTypeEntity, representing the result of the users answers
     */
    public String submitAnswer(int questionId, boolean answer) {
        if (this.test != null && 0 <= questionId && questionId < this.test.length()) {
            this.test.setAnswer(questionId, answer);
            return "Answer submitted successfully!";
        } else if (this.test == null) {
            return "No test selected!";
        } else if (0 >= questionId || questionId >= this.test.length()) {
            return "Id out of bounds!";
        }
        return "Unknown error occurred";
    }

    /**
     * Retrieves whether the user has already taken the BartleTest.
     *
     * @param userUUID        the users UUID
     *
     * @return has the user taken the test
     */
    public boolean userHasTakenTest(UUID userUUID) {

        Optional<PlayerTypeEntity> entity = playerTypeRepository.findByUserUUID(userUUID);;
        if (entity.isEmpty()) {
            // User is not present in playertype_database
            return false;
        }
        return entity.get().isUserHasTakenTest();

    }

    /**
     * Gets the dominant player type of a user.
     *
     * @param userUUID     the id of the user whose dominant player type is requested
     * @return the dominant player type
     */
    public PlayerTypeEntity.DominantPlayerType usersDominantPlayerType(@Argument UUID userUUID) {
        Optional<PlayerTypeEntity> playerType = playerTypeRepository.findByUserUUID(userUUID);
        if (playerType.isPresent() && playerType.get().getDominantPlayerType() != null) {
            return playerType.get().getDominantPlayerType();
        } else {
            return PlayerTypeEntity.DominantPlayerType.None;
        }
    }

}
