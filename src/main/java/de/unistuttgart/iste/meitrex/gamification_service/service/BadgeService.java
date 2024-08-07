package de.unistuttgart.iste.meitrex.gamification_service.service;

import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.BadgeEntity;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.CourseEntity;
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

    public static final int bronzePassingPercentage = 50;
    public static final int silverPassingPercentage = 70;
    public static final int goldPassingPercentage = 90;

    public static final String descriptionPart1 = "At least ";
    public static final String descriptionPart2 = "% of your answers for the ";
    public static final String descriptionPart3 = " are correct.";

    /**
     * Retrieves the complete list of badges of the course
     *
     * @param courseUUID   the id of the course
     *
     * @return a List of BadgeEntitys, which contains all Badges of this course
     */
    public List<BadgeEntity> getBadgesByCourseUUID(UUID courseUUID) {
        return badgeRepository.findByCourseUUID(courseUUID);
    }

    /**
     * Retrieves the complete list of userBadges for the course and the user
     *
     * @param courseUUID   the id of the course
     * @param userUUID     the id of the user
     *
     * @return a List of UserBadges, which contains all UserBadges of this course, that refer to the user
     */
    public List<UserBadge> getUserBadgesByCourseUUID(UUID courseUUID, UUID userUUID) {

        List<BadgeEntity> badgeEntities = badgeRepository.findByCourseUUID(courseUUID);
        List<UserBadge> userBadges = new LinkedList<UserBadge>();

        for (BadgeEntity badgeEntity : badgeEntities) {
            UserBadgeEntity userBadgeEntity = userBadgeRepository.findByUserUUIDAndBadgeUUID(userUUID, badgeEntity.getBadgeUUID());
            if (userBadgeEntity != null) {
                userBadges.add(badgeMapper.userBadgeEntityToDto(userBadgeEntity, badgeEntity));
            }
        }
        return userBadges;

    }

    /**
     * Retrieves the complete list of badges that refer to the quiz
     *
     * @param quizUUID   the id of the quiz
     *
     * @return a List of BadgeEntity, which contains all Badges that refer to the quiz
     */
    public List<BadgeEntity> getBadgesByQuizUUID(UUID quizUUID) {
        return badgeRepository.findByQuizUUID(quizUUID);
    }

    /**
     * Retrieves the complete list of badges that refer to the flashcardset
     *
     * @param flashCardSetUUID   the id of the flashcardset
     *
     * @return a List of BadgeEntity, which contains all Badges that refer to the flashcardset
     */
    public List<BadgeEntity> getBadgesByFlashCardSetUUID(UUID flashCardSetUUID) {
        return badgeRepository.findByFlashCardSetUUID(flashCardSetUUID);
    }

    /**
     * Assigns all the courses badges to the user
     *
     * @param courseUUID     the id of the course
     * @param userUUID       the id of the user, who joined the course
     */
    public void assignCoursesBadgesToUser(UUID courseUUID, UUID userUUID) {

        List<BadgeEntity> badges = getBadgesByCourseUUID(courseUUID);
        for (BadgeEntity badge : badges) {
            assignBadgeToUser(userUUID, badge.getBadgeUUID());
        }

    }

    /**
     * Removes all badges from the badgeRepository of the course and all corresponding userBadges
     *
     * @param courseUUID   the id of the deleted course
     */
    public void deleteBadgesAndUserBadgesOfCourse(UUID courseUUID) {
        List<BadgeEntity> courseBadges = this.deleteBadgesOfCourse(courseUUID);
        this.deleteUserBadges(courseBadges);
    }

    /**
     * Removes all badges from the badgeRepository of the quiz and all corresponding userBadges
     *
     * @param quizUUID   the id of the deleted quiz
     */
    public void deleteBadgesAndUserBadgesOfQuiz(UUID quizUUID) {
        List<BadgeEntity> quizBadges = this.deleteBadgesOfQuiz(quizUUID);
        this.deleteUserBadges(quizBadges);
    }

    /**
     * Removes all badges from the badgeRepository of the fcs and all corresponding userBadges
     *
     * @param flashcardSetUUID   the id of the deleted fcs
     */
    public void deleteBadgesAndUserBadgesOfFCS(UUID flashcardSetUUID) {
        List<BadgeEntity> fcsBadges = this.deleteBadgesOfFCS(flashcardSetUUID);
        this.deleteUserBadges(fcsBadges);
    }

    /**
     * Removes all badges from the badgeRepository of the course
     *
     * @param courseUUID   the id of the deleted course
     *
     * @return a List of BadgeEntitys, which contains all Badges of this course
     */
    public List<BadgeEntity> deleteBadgesOfCourse(UUID courseUUID) {
        List<BadgeEntity> courseBadges = badgeRepository.findByCourseUUID(courseUUID);
        badgeRepository.deleteAll(courseBadges);
        return courseBadges;
    }

    /**
     * Removes all badges from the badgeRepository of the quiz
     *
     * @param quizUUID   the id of the deleted course
     *
     * @return a List of BadgeEntitys, which contains all Badges of this quiz
     */
    public List<BadgeEntity> deleteBadgesOfQuiz(UUID quizUUID) {
        List<BadgeEntity> quizBadges = badgeRepository.findByQuizUUID(quizUUID);
        badgeRepository.deleteAll(quizBadges);
        return quizBadges;
    }

    /**
     * Removes all badges from the badgeRepository of the fcs
     *
     * @param flashcardSetUUID   the id of the deleted fcs
     *
     * @return a List of BadgeEntitys, which contains all Badges of this fcs
     */
    public List<BadgeEntity> deleteBadgesOfFCS(UUID flashcardSetUUID) {
        List<BadgeEntity> fcsBadges = badgeRepository.findByFlashCardSetUUID(flashcardSetUUID);
        badgeRepository.deleteAll(fcsBadges);
        return fcsBadges;
    }

    /**
     * Removes all userBadges from the userBadgeRepository that refer to the given badges
     *
     * @param badgeEntities   the deleted badges
     */
    public void deleteUserBadges(List<BadgeEntity> badgeEntities) {
        for (BadgeEntity badgeEntity : badgeEntities) {
            userBadgeRepository.deleteAllByBadgeUUID(badgeEntity.getBadgeUUID());
        }
    }

    /**
     * Change the name of the flahscardset in its Badges
     *
     * @param flashcardSetUUID   the id of fcs
     * @param name               the new name of the fcs
     */
    public void changeFlashCardSetName(UUID flashcardSetUUID, String name) {
        List<BadgeEntity> fcsBadges = badgeRepository.findByFlashCardSetUUID(flashcardSetUUID);
        for (BadgeEntity fcsBadge : fcsBadges) {
            fcsBadge.setDescription(descriptionPart1 + fcsBadge.getPassingPercentage() + descriptionPart2 + "flashcardSet " + name + descriptionPart3);
            badgeRepository.save(fcsBadge);
        }
    }

    /**
     * Change the name of the quiz in its Badges
     *
     * @param quizUUID   the id of quiz
     * @param name       the new name of the quiz
     */
    public void changeQuizName(UUID quizUUID, String name) {
        List<BadgeEntity> quizBadges = badgeRepository.findByQuizUUID(quizUUID);
        for (BadgeEntity quizBadge : quizBadges) {
            quizBadge.setDescription(descriptionPart1 + quizBadge.getPassingPercentage() + descriptionPart2 + "quiz " + name + descriptionPart3);
            badgeRepository.save(quizBadge);
        }
    }

    /**
     * Marks the UserBadge of the user as achieved, if he got more answers correct than the passingPercentage of the
     * Badges, that refer to the quiz
     *
     * @param userUUID             the id of the user
     * @param quizUUID             the id of the quiz
     * @param correctAnswers       the number of correct answers, the user got for this quiz
     * @param totalAnswers         the total number of questions in this quiz
     */
    public void markBadgesAsAchievedIfPassedQuiz(UUID userUUID, UUID quizUUID, int correctAnswers, int totalAnswers) {

        int percentage = (correctAnswers * 100) / totalAnswers;
        List<BadgeEntity> quizBadges = this.getBadgesByQuizUUID(quizUUID);
        for (BadgeEntity quizBadge : quizBadges) {
            if (percentage > quizBadge.getPassingPercentage()) {
                markBadgeAsAchieved(userUUID, quizBadge.getBadgeUUID());
            }
        }

    }

    /**
     * Marks the UserBadge of the user as achieved, if he got more answers correct than the passingPercentage of the
     * Badges, that refer to the flashcardset
     *
     * @param userUUID             the id of the user
     * @param flashCardSetUUID     the id of the flashcardset
     * @param correctAnswers       the number of correct answers, the user got for this flashcardset
     * @param totalAnswers         the total number of questions in this flashcardset
     */
    public void markBadgesAsAchievedIfPassedFlashCardSet(UUID userUUID, UUID flashCardSetUUID, int correctAnswers, int totalAnswers) {

        int percentage = (correctAnswers * 100) / totalAnswers;
        List<BadgeEntity> flashCardSetBadges = this.getBadgesByFlashCardSetUUID(flashCardSetUUID);
        for (BadgeEntity flashCardSetBadge : flashCardSetBadges) {
            if (percentage > flashCardSetBadge.getPassingPercentage()) {
                markBadgeAsAchieved(userUUID, flashCardSetBadge.getBadgeUUID());
            }
        }

    }

    /**
     * Assigns the Badge for the user and saves it in the userBadgeRepository as not achieved
     *
     * @param userUUID             the id of the user
     * @param badgeUUID            the id of the badge
     */
    public void assignBadgeToUser(UUID userUUID, UUID badgeUUID) {
        UserBadgeEntity userBadgeEntity = new UserBadgeEntity();
        userBadgeEntity.setUserUUID(userUUID);
        userBadgeEntity.setBadgeUUID(badgeUUID);
        userBadgeEntity.setAchieved(false);
        userBadgeRepository.save(userBadgeEntity);
    }

    /**
     * Deletes all the userBadges of this course
     *
     * @param userUUID               the id of the user who left the course
     * @param courseUUID             the id of the course
     */
    public void deleteUserBadgesOfCourse(UUID userUUID, UUID courseUUID) {
        List<BadgeEntity> badgeEntities = badgeRepository.findByCourseUUID(courseUUID);

        for (BadgeEntity badgeEntity : badgeEntities) {
            userBadgeRepository.deleteByUserUUIDAndBadgeUUID(userUUID, badgeEntity.getBadgeUUID());
        }
    }

    /**
     * Marks the userBadge as achieved and saves it in the userBadgeRepository
     *
     * @param userUUID             the id of the user
     * @param badgeUUID            the id of the badge
     */
    public void markBadgeAsAchieved(UUID userUUID, UUID badgeUUID) {
        UserBadgeEntity userBadge = userBadgeRepository.findByUserUUIDAndBadgeUUID(userUUID, badgeUUID);
        userBadge.setAchieved(true);
        userBadgeRepository.save(userBadge);
    }

    /**
     * Creates 3 Badges for the new created flashcardset, which suggests the user to complete the flashcardset with 50,
     * 70 and 90% correct answers
     *
     * @param flashCardSetUUID     the id of the created flashcardset
     * @param name                 the name of the flashcardset
     * @param courseUUID           the id of the course
     * @param coursesUsers         the UUIDs of the members of the course
     */
    public List<BadgeEntity> createBadgesForFlashCardSet(UUID flashCardSetUUID,
                                                   String name,
                                                   UUID courseUUID,
                                                   Set<UUID> coursesUsers) {
        List<BadgeEntity> badgeEntities = new LinkedList<>();

        // 50% Badge
        badgeEntities.add(createBadgeForFlashCardSet(flashCardSetUUID, name, bronzePassingPercentage, courseUUID, coursesUsers));
        // 70% Badge
        badgeEntities.add(createBadgeForFlashCardSet(flashCardSetUUID, name, silverPassingPercentage, courseUUID, coursesUsers));
        // 90% Badge
        badgeEntities.add(createBadgeForFlashCardSet(flashCardSetUUID, name, goldPassingPercentage, courseUUID, coursesUsers));

        return badgeEntities;
    }

    /**
     * Creates a Badge for the new flashcardset and assigns it to all the users of the course
     *
     * @param flashCardSetUUID     the id of the created flashcardset
     * @param name                 the name of the flashcardset
     * @param passingPercentage    the required percentage to get this badge
     * @param courseUUID           the id of the course
     * @param coursesUsers         the UUIDs of the members of the course
     */
    public BadgeEntity createBadgeForFlashCardSet(UUID flashCardSetUUID, String name, int passingPercentage, UUID courseUUID, Set<UUID> coursesUsers) {

        BadgeEntity badgeEntity = new BadgeEntity();
        badgeEntity.setDescription(descriptionPart1 + passingPercentage + descriptionPart2 + "flashcardSet " + name + descriptionPart3);
        badgeEntity.setPassingPercentage(passingPercentage);
        badgeEntity.setFlashCardSetUUID(flashCardSetUUID);
        badgeEntity.setCourseUUID(courseUUID);

        badgeEntity = badgeRepository.save(badgeEntity);

        for (UUID userUUID : coursesUsers) {
            assignBadgeToUser(userUUID, badgeEntity.getBadgeUUID());
        }
        return badgeEntity;

    }

    /**
     * Creates 3 Badges for the new created quiz, which suggests the user to complete the quiz with 50,
     * 70 and 90% correct answers
     *
     * @param quizUUID             the id of the created quiz
     * @param name                 the name of the quiz
     * @param courseUUID           the id of the course
     * @param coursesUsers         the UUIDs of the members of the course
     */
    public List<BadgeEntity> createBadgesForQuiz(UUID quizUUID,
                                    String name,
                                    UUID courseUUID,
                                    Set<UUID> coursesUsers) {
        List<BadgeEntity> badgeEntities = new LinkedList<>();

        // 50% Badge
        badgeEntities.add(createBadgeForQuiz(quizUUID, name, bronzePassingPercentage, courseUUID, coursesUsers));
        // 70% Badge
        badgeEntities.add(createBadgeForQuiz(quizUUID, name, silverPassingPercentage, courseUUID, coursesUsers));
        // 90% Badge
        badgeEntities.add(createBadgeForQuiz(quizUUID, name, goldPassingPercentage, courseUUID, coursesUsers));

        return badgeEntities;
    }

    /**
     * Creates a Badge for the new quiz and assigns it to all the users of the course
     *
     * @param quizUUID             the id of the created quiz
     * @param name                 the name of the quiz
     * @param passingPercentage    the required percentage to get this badge
     * @param courseUUID           the id of the course
     * @param coursesUsers         the UUIDs of the members of the course
     */
    public BadgeEntity createBadgeForQuiz(UUID quizUUID, String name, int passingPercentage, UUID courseUUID, Set<UUID> coursesUsers) {

        BadgeEntity badgeEntity = new BadgeEntity();
        badgeEntity.setDescription(descriptionPart1 + passingPercentage + descriptionPart2 + "quiz " + name + descriptionPart3);
        badgeEntity.setPassingPercentage(passingPercentage);
        badgeEntity.setQuizUUID(quizUUID);
        badgeEntity.setCourseUUID(courseUUID);

        badgeEntity = badgeRepository.save(badgeEntity);

        for (UUID userUUID : coursesUsers) {
            assignBadgeToUser(userUUID, badgeEntity.getBadgeUUID());
        }
        return badgeEntity;

    }

}
