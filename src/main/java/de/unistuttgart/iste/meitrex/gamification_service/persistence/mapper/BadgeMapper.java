package de.unistuttgart.iste.meitrex.gamification_service.persistence.mapper;

import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.BadgeEntity;
import de.unistuttgart.iste.meitrex.generated.dto.UserBadge;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.UserBadgeEntity;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BadgeMapper {

    private final ModelMapper modelMapper;

    public UserBadge userBadgeEntityToDto(UserBadgeEntity userBadgeEntity) {
        return modelMapper.map(userBadgeEntity, UserBadge.class);
    }

    public UserBadgeEntity dtoToUserBadgeEntity(UserBadge userBadge) {
        return modelMapper.map(userBadge, UserBadgeEntity.class);
    }

}