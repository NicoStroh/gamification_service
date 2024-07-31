package de.unistuttgart.iste.meitrex.gamification_service.service;

import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.PlayerTypeEntity;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.PlayerTypeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PlayerTypeService {

    private final PlayerTypeRepository playerTypeRepository;

    /**
     * Creates a new empty TestResult for the user.
     *
     * @param userUUID     the id of the new user that was just created
     * @return an empty PlayerTypeEntity, indicating that the user has
     *                  not yet taken the PlayerTypeTest
     */
    public PlayerTypeEntity createUser(final UUID userUUID) {

        PlayerTypeEntity entity = new PlayerTypeEntity(userUUID, false);
        return playerTypeRepository.save(entity);

    }

    /**
     * Saves the result of a BartleTest for a user.
     *
     * @param result        the users test result
     * @return a PlayerTypeEntity, representing the result of the users answers
     */
    public PlayerTypeEntity saveTestResult(final PlayerTypeEntity result) {
        return playerTypeRepository.save(result);
    }

    /**
     * Gets the player type percentages of a user.
     *
     * @param userUUID     the id of the user whose percentages are requested
     * @return a PlayerTypeEntity, representing the player types of the user
     */
    public Optional<PlayerTypeEntity> getPlayerTypeByUserUUID(UUID userUUID) {
        return playerTypeRepository.findByUserUUID(userUUID);
    }

}
