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
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;

    private final BadgeMapper badgeMapper;

    private static final int bronzePassingPercentage = 50;
    private static final int silverPassingPercentage = 70;
    private static final int goldPassingPercentage = 90;

    public List<Badge> getBadgesByCourseUUID(UUID courseUUID) {
        List<BadgeEntity> entities = badgeRepository.findByCourseUUID(courseUUID);
        return entities.stream()
                .map(badgeMapper::badgeEntityToDto)
                .toList();
    }

    public List<UserBadge> getUserBadgesByCourseUUID(UUID courseUUID, UUID userUUID) {

        List<BadgeEntity> badgeEntities = badgeRepository.findByCourseUUID(courseUUID);
        List<UserBadgeEntity> userBadgeEntities = new LinkedList<UserBadgeEntity>();

        for (BadgeEntity badgeEntity : badgeEntities) {
            userBadgeEntities.add(userBadgeRepository.findByUserUUIDAndBadgeUUID(userUUID, badgeEntity.getBadgeUUID()));
        }
        return userBadgeEntities.stream()
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


    public List<UserBadge> markBadgesAsAchievedIfPassedQuiz(UUID userUUID, UUID quizUUID, int correctAnswers, int totalAnswers) {

        List<UserBadge> userBadges = new LinkedList<UserBadge>();

        int percentage = (correctAnswers * 100) / totalAnswers;
        List<Badge> quizBadges = this.getBadgesByQuizUUID(quizUUID);
        for (Badge quizBadge : quizBadges) {
            if (percentage > quizBadge.getPassingPercentage()) {
                userBadges.add(markBadgeAsAchieved(userUUID, quizBadge.getBadgeUUID()));
            }
        }

        return userBadges;
    }

    public List<UserBadge> markBadgesAsAchievedIfPassedFlashCardSet(UUID userUUID, UUID flashCardSetUUID, int correctAnswers, int totalAnswers) {

        List<UserBadge> userBadges = new LinkedList<UserBadge>();

        int percentage = (correctAnswers * 100) / totalAnswers;
        List<Badge> flashCardSetBadges = this.getBadgesByFlashCardSetUUID(flashCardSetUUID);
        for (Badge flashCardSetBadge : flashCardSetBadges) {
            if (percentage > flashCardSetBadge.getPassingPercentage()) {
                userBadges.add(markBadgeAsAchieved(userUUID, flashCardSetBadge.getBadgeUUID()));
            }
        }

        return userBadges;
    }

    public UserBadge assignBadgeToUser(UUID userUUID, UUID badgeUUID) {
        BadgeEntity badge = badgeRepository.findById(badgeUUID).orElseThrow(() -> new RuntimeException("Badge not found"));

        UserBadgeEntity userBadge = new UserBadgeEntity();
        userBadge.setUserUUID(userUUID);
        userBadge.setBadgeUUID(badge.getBadgeUUID());
        userBadge.setAchieved(false);
        userBadge.setDescription(badge.getDescription());
        userBadge.setPassingPercentage(badge.getPassingPercentage());

        return badgeMapper.userBadgeEntityToDto(userBadgeRepository.save(userBadge));
    }

    public UserBadge markBadgeAsAchieved(UUID userUUID, UUID badgeUUID) {
        UserBadgeEntity userBadge = userBadgeRepository.findByUserUUIDAndBadgeUUID(userUUID, badgeUUID);
        userBadge.setAchieved(true);
        return badgeMapper.userBadgeEntityToDto(userBadgeRepository.save(userBadge));
    }

    public void createBadgesForQuiz(UUID quizUUID,
                                           String name,
                                           UUID courseUUID,
                                           CourseService courseService) {
        // 50% Badge
        createBadgeForQuiz(quizUUID, name, bronzePassingPercentage, courseUUID, courseService);
        // 70% Badge
        createBadgeForQuiz(quizUUID, name, silverPassingPercentage, courseUUID, courseService);
        // 90% Badge
        createBadgeForQuiz(quizUUID, name, goldPassingPercentage, courseUUID, courseService);
    }

    public Badge createBadgeForQuiz(UUID quizUUID, String name, int passingPercentage, UUID courseUUID, CourseService courseService) {

        BadgeEntity badgeEntity = new BadgeEntity();
        badgeEntity.setDescription("At least " + passingPercentage + "% of your answers for the quiz " + name + " are correct.");
        badgeEntity.setPassingPercentage(passingPercentage);
        badgeEntity.setQuizUUID(quizUUID);
        badgeEntity.setCourseUUID(courseUUID);

        badgeEntity = badgeRepository.save(badgeEntity);
        return courseService.addBadgeForCourseAndUsers(courseUUID, badgeEntity, this);

    }

    public void createBadgesForFlashCardSet(UUID flashCardSetUUID,
                                                   String name,
                                                   UUID courseUUID,
                                                   CourseService courseService) {
        // 50% Badge
        createBadgeForFlashCardSet(flashCardSetUUID, name, bronzePassingPercentage, courseUUID, courseService);
        // 70% Badge
        createBadgeForFlashCardSet(flashCardSetUUID, name, silverPassingPercentage, courseUUID, courseService);
        // 90% Badge
        createBadgeForFlashCardSet(flashCardSetUUID, name, goldPassingPercentage, courseUUID, courseService);
    }

    public Badge createBadgeForFlashCardSet(UUID flashCardSetId, String name, int passingPercentage, UUID courseUUID, CourseService courseService) {

        BadgeEntity badgeEntity = new BadgeEntity();
        badgeEntity.setDescription("At least " + passingPercentage + "% of your answers for the flashcardSet " + name + " are correct.");
        badgeEntity.setPassingPercentage(passingPercentage);
        badgeEntity.setFlashCardSetUUID(flashCardSetId);
        badgeEntity.setCourseUUID(courseUUID);

        badgeEntity = badgeRepository.save(badgeEntity);
        return courseService.addBadgeForCourseAndUsers(courseUUID, badgeEntity, this);

    }

}
