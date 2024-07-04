package de.unistuttgart.iste.meitrex.gamification_service.persistence.mapper;

import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.PlayerTypeEntity;
import de.unistuttgart.iste.meitrex.generated.dto.PlayerType;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlayerTypeMapper {

    private final ModelMapper modelMapper;

    public PlayerType entityToDto(PlayerTypeEntity playerTypeEntity) {
        return modelMapper.map(playerTypeEntity, PlayerType.class);
    }

    public PlayerTypeEntity dtoToEntity(PlayerType playerType) {
        return modelMapper.map(playerType, PlayerTypeEntity.class);
    }

}