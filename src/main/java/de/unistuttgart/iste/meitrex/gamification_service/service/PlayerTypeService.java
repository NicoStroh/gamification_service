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

    public PlayerType createOrUpdatePlayerType(UUID userUUID, int achieverPercentage, int explorerPercentage, int socializerPercentage, int killerPercentage) {
        Optional<PlayerTypeEntity> existingPlayerType = playerTypeRepository.findByUserUUID(userUUID);
        PlayerTypeEntity playerType;

        if (existingPlayerType.isPresent()) {
            playerType = existingPlayerType.get();
        } else {
            playerType = new PlayerTypeEntity();
            playerType.setUserUUID(userUUID);
        }

        playerType.setAchieverPercentage(achieverPercentage);
        playerType.setExplorerPercentage(explorerPercentage);
        playerType.setSocializerPercentage(socializerPercentage);
        playerType.setKillerPercentage(killerPercentage);

        return playerTypeMapper.entityToDto(playerTypeRepository.save(playerType));
    }

    public PlayerType getPlayerTypeByUserUUID(UUID userUUID) {
        Optional<PlayerTypeEntity> entity = playerTypeRepository.findByUserUUID(userUUID);
        if (entity.isPresent()) {
            return playerTypeMapper.entityToDto(entity.get());
        }
        PlayerType playerType = new PlayerType();
        playerType.setUserUUID(userUUID);
        return playerType;
    }


}
