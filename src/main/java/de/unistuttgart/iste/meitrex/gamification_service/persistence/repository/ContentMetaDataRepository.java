package de.unistuttgart.iste.meitrex.gamification_service.persistence.repository;

import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.BloomLevelEntity;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.ContentMetaDataEntity;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.UserBadgeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentMetaDataRepository extends JpaRepository<ContentMetaDataEntity, UUID> {

}
