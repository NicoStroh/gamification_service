package de.unistuttgart.iste.meitrex.gamification_service.service;

import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.BadgeEntity;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.CourseEntity;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.UserBadgeEntity;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.mapper.BadgeMapper;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.BadgeRepository;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.CourseRepository;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.UserBadgeRepository;
import de.unistuttgart.iste.meitrex.generated.dto.Badge;
import de.unistuttgart.iste.meitrex.generated.dto.UserBadge;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BadgeService {

    private final BadgeMapper badgeMapper;

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final CourseRepository courseRepository;


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

    public void addUserToCourse(UUID userUUID, UUID courseUUID) {
        List<Badge> badges = getBadgesByCourseUUID(courseUUID);
        for (Badge badge : badges) {
            assignBadgeToUser(userUUID, badge.getBadgeUUID());
        }

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        if (courseEntity.isPresent()) {
            CourseEntity course = courseEntity.get();
            course.addUser(userUUID);
            courseRepository.save(course);
        }
    }

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

        return badgeMapper.userBadgeEntityToDto(userBadgeRepository.save(userBadge));
    }

    public UserBadge markBadgeAsAchieved(UUID userUUID, UUID badgeUUID) {
        UserBadgeEntity userBadge = userBadgeRepository.findByUserUUIDAndBadgeUUID(userUUID, badgeUUID);
        userBadge.setAchieved(true);
        return badgeMapper.userBadgeEntityToDto(userBadgeRepository.save(userBadge));
    }

    public Badge addBadgeForCourseAndUsers(UUID courseUUID, BadgeEntity badgeEntity) {

        CourseEntity courseEntity = courseRepository.findById(courseUUID).orElseThrow(() -> new RuntimeException("Course not found"));
        courseEntity.addBadge(badgeEntity.getBadgeUUID());

        for (UUID userUUID : courseEntity.getUserUUIDs()) {
            assignBadgeToUser(userUUID, badgeEntity.getBadgeUUID());
        }
        courseRepository.save(courseEntity);

        badgeEntity = badgeRepository.save(badgeEntity);
        return badgeMapper.badgeEntityToDto(badgeEntity);

    }

    public Badge createBadgeForQuiz(UUID quizUUID, String name, String description, int passingPercentage, UUID courseUUID) {

        BadgeEntity badgeEntity = new BadgeEntity();
        badgeEntity.setName(name);
        badgeEntity.setDescription(description);
        badgeEntity.setPassingPercentage(passingPercentage);
        badgeEntity.setQuizUUID(quizUUID);

        return addBadgeForCourseAndUsers(courseUUID, badgeEntity);
    }

    public Badge createBadgeForFlashCardSet(UUID flashCardSetId, String name, String description, int passingPercentage, UUID courseUUID) {

        BadgeEntity badgeEntity = new BadgeEntity();
        badgeEntity.setName(name);
        badgeEntity.setDescription(description);
        badgeEntity.setPassingPercentage(passingPercentage);
        badgeEntity.setFlashCardSetUUID(flashCardSetId);

        return addBadgeForCourseAndUsers(courseUUID, badgeEntity);
    }

}
