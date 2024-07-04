package de.unistuttgart.iste.meitrex.gamification_service.service;

import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.BadgeEntity;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.UserBadgeEntity;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.mapper.BadgeMapper;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.BadgeRepository;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.UserBadgeRepository;
import de.unistuttgart.iste.meitrex.generated.dto.Badge;
import de.unistuttgart.iste.meitrex.generated.dto.UserBadge;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BadgeService {

    private final BadgeMapper badgeMapper;

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;


    public List<UserBadge> getUserBadges(UUID userUUID) {
        List<UserBadgeEntity> entities = userBadgeRepository.findByUserUUID(userUUID);
        return entities.stream()
                .map(badgeMapper::userBadgeEntityToDto)
                .toList();
    }

    public List<UserBadge> getAchievedBadges(UUID userUUID) {
        List<UserBadgeEntity> entities = userBadgeRepository.findByUserUUIDAndAchieved(userUUID, true);
        return entities.stream()
                .map(badgeMapper::userBadgeEntityToDto)
                .toList();
    }


    public List<Badge> getBadgesByQuizUUID(UUID quizUUID) {
        List<BadgeEntity> entities = badgeRepository.findByQuizUUID(quizUUID);
        return entities.stream()
                .map(badgeMapper::badgeEntityToDto)
                .toList();
    }

    public List<Badge> getBadgesByFlashCardSetUUID(UUID flashCardSetUUID) {
        List<BadgeEntity> entities = badgeRepository.findByFlashCardSetUUID(flashCardSetUUID);
        return entities.stream()
                .map(badgeMapper::badgeEntityToDto)
                .toList();
    }


    public void assignBadgeToUser(UUID userUUID, UUID badgeUUID) {
        BadgeEntity badge = badgeRepository.findById(badgeUUID).orElseThrow(() -> new RuntimeException("Badge not found"));

        UserBadgeEntity userBadge = new UserBadgeEntity();
        userBadge.setUserUUID(userUUID);
        userBadge.setBadgeUUID(badge.getBadgeUUID());
        userBadge.setAchieved(false);

        userBadgeRepository.save(userBadge);
    }

    public void markBadgeAsAchieved(UUID userUUID, UUID badgeUUID) {
        UserBadgeEntity userBadge = userBadgeRepository.findByUserUUIDAndBadgeUUID(userUUID, badgeUUID);
        userBadge.setAchieved(true);
        userBadgeRepository.save(userBadge);
    }

    public Badge createBadgeForQuiz(UUID quizUUID, String name, String description, int passingPercentage) {
        BadgeEntity badgeEntity = new BadgeEntity();
        badgeEntity.setName(name);
        badgeEntity.setDescription(description);
        badgeEntity.setPassingPercentage(passingPercentage);
        badgeEntity.setQuizUUID(quizUUID);

        badgeEntity = badgeRepository.save(badgeEntity);
        return badgeMapper.badgeEntityToDto(badgeEntity);
    }

    public Badge createBadgeForFlashCardSet(UUID flashCardSetId, String name, String description, int passingPercentage) {
        BadgeEntity badgeEntity = new BadgeEntity();
        badgeEntity.setName(name);
        badgeEntity.setDescription(description);
        badgeEntity.setPassingPercentage(passingPercentage);
        badgeEntity.setFlashCardSetUUID(flashCardSetId);

        badgeEntity = badgeRepository.save(badgeEntity);
        return badgeMapper.badgeEntityToDto(badgeEntity);
    }

}
