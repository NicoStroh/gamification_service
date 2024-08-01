package de.unistuttgart.iste.meitrex.gamification_service.persistence.repository;

import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.UserBadgeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadgeEntity, UUID> {

    List<UserBadgeEntity> findByBadgeUUID(UUID badgeUUID);
    UserBadgeEntity findByUserUUIDAndBadgeUUID(UUID userUUID, UUID badgeUUID);
    void deleteAllByBadgeUUID(UUID badgeUUID);
    void deleteAllByUserUUID(UUID userUUID);
    void deleteByUserUUIDAndBadgeUUID(UUID userUUID, UUID badgeUUID);

}
