package de.unistuttgart.iste.meitrex.gamification_service.persistence.repository;

import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.UserQuestChainEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserQuestChainRepository extends JpaRepository<UserQuestChainEntity, UUID> {

    List<UserQuestChainEntity> findByQuestChainUUID(UUID questChainUUID);
    UserQuestChainEntity findByQuestChainUUIDAndUserUUID(UUID questChainUUID, UUID userUUID);
    void deleteByQuestChainUUIDAndUserUUID(UUID questChainUUID, UUID userUUID);

}
