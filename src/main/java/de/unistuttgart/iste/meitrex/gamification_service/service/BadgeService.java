package de.unistuttgart.iste.meitrex.gamification_service.service;

import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.BadgeEntity;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.UserBadgeEntity;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.mapper.BadgeMapper;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.BadgeRepository;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.UserBadgeRepository;
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

    public List<BadgeEntity> getBadgesByCourseUUID(UUID courseUUID) {
        return badgeRepository.findByCourseUUID(courseUUID);
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

    public List<BadgeEntity> getBadgesByQuizUUID(UUID quizUUID) {
        return badgeRepository.findByQuizUUID(quizUUID);
    }

    public List<BadgeEntity> getBadgesByFlashCardSetUUID(UUID flashCardSetUUID) {
        return badgeRepository.findByFlashCardSetUUID(flashCardSetUUID);
    }


    public List<UserBadge> markBadgesAsAchievedIfPassedQuiz(UUID userUUID, UUID quizUUID, int correctAnswers, int totalAnswers) {

        List<UserBadge> userBadges = new LinkedList<UserBadge>();

        int percentage = (correctAnswers * 100) / totalAnswers;
        List<BadgeEntity> quizBadges = this.getBadgesByQuizUUID(quizUUID);
        for (BadgeEntity quizBadge : quizBadges) {
            if (percentage > quizBadge.getPassingPercentage()) {
                userBadges.add(markBadgeAsAchieved(userUUID, quizBadge.getBadgeUUID()));
            }
        }

        return userBadges;
    }

    public List<UserBadge> markBadgesAsAchievedIfPassedFlashCardSet(UUID userUUID, UUID flashCardSetUUID, int correctAnswers, int totalAnswers) {

        List<UserBadge> userBadges = new LinkedList<UserBadge>();

        int percentage = (correctAnswers * 100) / totalAnswers;
        List<BadgeEntity> flashCardSetBadges = this.getBadgesByFlashCardSetUUID(flashCardSetUUID);
        for (BadgeEntity flashCardSetBadge : flashCardSetBadges) {
            if (percentage > flashCardSetBadge.getPassingPercentage()) {
                userBadges.add(markBadgeAsAchieved(userUUID, flashCardSetBadge.getBadgeUUID()));
            }
        }

        return userBadges;
    }

    public UserBadge assignBadgeToUser(UUID userUUID, BadgeEntity badge) {
        UserBadgeEntity userBadgeEntity = new UserBadgeEntity();
        userBadgeEntity.setUserUUID(userUUID);
        userBadgeEntity.setBadgeUUID(badge.getBadgeUUID());
        userBadgeEntity.setAchieved(false);

        UserBadge userBadge = badgeMapper.userBadgeEntityToDto(userBadgeRepository.save(userBadgeEntity));
        userBadge.setDescription(badge.getDescription());
        userBadge.setPassingPercentage(badge.getPassingPercentage());

        return userBadge;
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

    public void createBadgeForQuiz(UUID quizUUID, String name, int passingPercentage, UUID courseUUID, CourseService courseService) {

        BadgeEntity badgeEntity = new BadgeEntity();
        badgeEntity.setDescription("At least " + passingPercentage + "% of your answers for the quiz " + name + " are correct.");
        badgeEntity.setPassingPercentage(passingPercentage);
        badgeEntity.setQuizUUID(quizUUID);
        badgeEntity.setCourseUUID(courseUUID);

        badgeEntity = badgeRepository.save(badgeEntity);
        courseService.addBadgeForCourseAndUsers(courseUUID, badgeEntity, this);

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

    public void createBadgeForFlashCardSet(UUID flashCardSetId, String name, int passingPercentage, UUID courseUUID, CourseService courseService) {

        BadgeEntity badgeEntity = new BadgeEntity();
        badgeEntity.setDescription("At least " + passingPercentage + "% of your answers for the flashcardSet " + name + " are correct.");
        badgeEntity.setPassingPercentage(passingPercentage);
        badgeEntity.setFlashCardSetUUID(flashCardSetId);
        badgeEntity.setCourseUUID(courseUUID);

        badgeEntity = badgeRepository.save(badgeEntity);
        courseService.addBadgeForCourseAndUsers(courseUUID, badgeEntity, this);

    }

}
