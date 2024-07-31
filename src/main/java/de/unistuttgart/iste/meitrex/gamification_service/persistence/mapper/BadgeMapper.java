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

    public UserBadge userBadgeEntityToDto(UserBadgeEntity userBadgeEntity, BadgeEntity badgeEntity) {
        UserBadge userBadge = new UserBadge();
        userBadge.setUserBadgeUUID(userBadgeEntity.getUserBadgeUUID());
        userBadge.setUserUUID(userBadgeEntity.getUserUUID());
        userBadge.setBadgeUUID(userBadgeEntity.getBadgeUUID());
        userBadge.setAchieved(userBadgeEntity.isAchieved());

        userBadge.setDescription(badgeEntity.getDescription());
        userBadge.setPassingPercentage(badgeEntity.getPassingPercentage());
        return userBadge;
    }

}