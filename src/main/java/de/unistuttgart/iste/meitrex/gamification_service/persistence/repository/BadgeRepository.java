package de.unistuttgart.iste.meitrex.gamification_service.persistence.repository;

import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.BadgeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BadgeRepository extends JpaRepository<BadgeEntity, UUID> {

    List<BadgeEntity> findByQuizUUID(UUID quizUUID);
    List<BadgeEntity> findByFlashCardSetUUID(UUID flashCardSetUUID);
    List<BadgeEntity> findByCourseUUID(UUID courseUUID);

}
