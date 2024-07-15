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

        PlayerTypeEntity playerTypeEntity = new PlayerTypeEntity();
        playerTypeEntity.setUserUUID(userUUID);

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
