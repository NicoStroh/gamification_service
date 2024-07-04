package de.unistuttgart.iste.meitrex.gamification_service.persistence.mapper;

import de.unistuttgart.iste.meitrex.generated.dto.Badge;
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

    public Badge badgeEntityToDto(BadgeEntity badgeEntity) {
        return modelMapper.map(badgeEntity, Badge.class);
    }

    public BadgeEntity dtoToBadgeEntity(Badge badge) {
        return modelMapper.map(badge, BadgeEntity.class);
    }

    public UserBadge userBadgeEntityToDto(UserBadgeEntity userBadgeEntity) {
        return modelMapper.map(userBadgeEntity, UserBadge.class);
    }

    public UserBadgeEntity dtoToUserBadgeEntity(UserBadge userBadge) {
        return modelMapper.map(userBadge, UserBadgeEntity.class);
    }

}