package de.unistuttgart.iste.meitrex.gamification_service.service;

import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.PlayerTypeEntity;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.mapper.PlayerTypeMapper;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.PlayerTypeRepository;
import de.unistuttgart.iste.meitrex.generated.dto.PlayerType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PlayerTypeService {

    private final PlayerTypeMapper playerTypeMapper;
    private final PlayerTypeRepository playerTypeRepository;

    /**
     * Creates a new empty TestResult for the user.
     *
     * @param userUUID     the id of the new user that was just created
     * @return an empty PlayerType, indicating that the user has
     *                  not yet taken the PlayerTypeTest
     */
    public PlayerType createUser(final UUID userUUID) {

        PlayerTypeEntity entity = new PlayerTypeEntity(userUUID, false);
        return playerTypeMapper.entityToDto(playerTypeRepository.save(entity));

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
    public Optional<PlayerTypeEntity> getEntity(final UUID userUUID) {
        return playerTypeRepository.findById(userUUID);
    }

    public PlayerType createOrUpdatePlayerType(UUID userUUID, int achieverPercentage, int explorerPercentage, int socializerPercentage, int killerPercentage) {

        PlayerTypeEntity playerTypeEntity = new PlayerTypeEntity();
        playerTypeEntity.setUserUUID(userUUID);

        playerTypeEntity.setUserHasTakenTest(true);

        playerTypeEntity.setAchieverPercentage(achieverPercentage);
        playerTypeEntity.setExplorerPercentage(explorerPercentage);
        playerTypeEntity.setSocializerPercentage(socializerPercentage);
        playerTypeEntity.setKillerPercentage(killerPercentage);

        playerTypeEntity.setDominantPlayerType(playerTypeEntity.dominantPlayerType());

        return playerTypeMapper.entityToDto(playerTypeRepository.save(playerTypeEntity));

    }

    public Optional<PlayerTypeEntity> getPlayerTypeByUserUUID(UUID userUUID) {
        return playerTypeRepository.findByUserUUID(userUUID);
    }

}
