package de.unistuttgart.iste.meitrex.gamification_service.persistence.repository;

import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.QuestChainEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestChainRepository extends JpaRepository<QuestChainEntity, UUID> {

    QuestChainEntity findByCourseUUID(UUID courseUUID);

}
