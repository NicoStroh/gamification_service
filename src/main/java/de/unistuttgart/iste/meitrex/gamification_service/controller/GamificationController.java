package de.unistuttgart.iste.meitrex.gamification_service.controller;

import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.PlayerTypeEntity;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.PlayerTypeTest;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.PlayerTypeTestQuestion;
import de.unistuttgart.iste.meitrex.gamification_service.service.BadgeService;
import de.unistuttgart.iste.meitrex.gamification_service.service.PlayerTypeService;
import de.unistuttgart.iste.meitrex.generated.dto.Badge;
import de.unistuttgart.iste.meitrex.generated.dto.PlayerType;
import de.unistuttgart.iste.meitrex.generated.dto.UserBadge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GamificationController {

    private final PlayerTypeService playerTypeService;
    private final BadgeService badgeService;

    private static final int bronzePassingPercentage = 50;
    private static final int silverPassingPercentage = 70;
    private static final int goldPassingPercentage = 90;

    private PlayerTypeTest test;

    @QueryMapping
    public PlayerTypeTestQuestion[] test() {
        this.test = new PlayerTypeTest();
        return this.test.getQuestions();
    }

    @QueryMapping
    public boolean userHasTakenTest(@Argument final UUID userUUID) {

        Optional<PlayerTypeEntity> entity = playerTypeService.getEntity(userUUID);
        if (entity.isEmpty()) {
            // User is not present in playertype_database
            playerTypeService.createUser(userUUID);
            return false;
        }
        return entity.get().isUserHasTakenTest();

    }

    @QueryMapping
    public PlayerTypeEntity.DominantPlayerType usersDominantPlayerType(@Argument UUID userUUID) {
        Optional<PlayerTypeEntity> playerType = playerTypeService.getPlayerTypeByUserUUID(userUUID);
        if (playerType.isPresent()) {
            return playerType.get().getDominantPlayerType();
        } else {
            return PlayerTypeEntity.DominantPlayerType.None;
        }
    }


    // True if dominant playertype is achiever
    @QueryMapping
    public boolean userCanSeeBadges(@Argument UUID userUUID) {
        Optional<PlayerTypeEntity> playerType = playerTypeService.getPlayerTypeByUserUUID(userUUID);
        return playerType.isPresent() && playerType.get().isAchiever();
    }

    // True if dominant playertype is explorer
    @QueryMapping
    public boolean userCanSeeQuests(@Argument UUID userUUID) {
        Optional<PlayerTypeEntity> playerType = playerTypeService.getPlayerTypeByUserUUID(userUUID);
        return playerType.isPresent() && playerType.get().isExplorer();
    }

    // True if dominant playertype is socializer
    @QueryMapping
    public boolean userCanSeeTeamForum(@Argument UUID userUUID) {
        Optional<PlayerTypeEntity> playerType = playerTypeService.getPlayerTypeByUserUUID(userUUID);
        return playerType.isPresent() && playerType.get().isSocializer();
    }

    // True if dominant playertype is killer
    @QueryMapping
    public boolean userCanSeeScoreboard(@Argument UUID userUUID) {
        Optional<PlayerTypeEntity> playerType = playerTypeService.getPlayerTypeByUserUUID(userUUID);
        return playerType.isPresent() && playerType.get().isKiller();
    }

    @QueryMapping
    public List<UserBadge> getCoursesUserBadges(@Argument UUID courseUUID, @Argument UUID userUUID) {
        return badgeService.getUserBadgesByCourseUUID(courseUUID, userUUID);
    }


    // Not used currently
    @QueryMapping
    public List<UserBadge> userBadges(@Argument UUID userUUID) {
        return badgeService.getUserBadges(userUUID);
    }

    @QueryMapping
    public List<UserBadge> achievedBadges(@Argument UUID userUUID) {
        return badgeService.getAchievedBadges(userUUID);
    }

    @QueryMapping
    public List<Badge> badgesByQuiz(@Argument UUID quizUUID) {
        return badgeService.getBadgesByQuizUUID(quizUUID);
    }

    @QueryMapping
    public List<Badge> badgesByFlashCardSet(@Argument UUID flashCardSetUUID) {
        return badgeService.getBadgesByFlashCardSetUUID(flashCardSetUUID);
    }





    @MutationMapping
    public String submitAnswer(@Argument final int questionId, @Argument final boolean answer) {

        if (this.test != null) {
            this.test.setAnswer(questionId, answer);
            return "Answer submitted successfully!";
        }
        return "No test selected!";

    }

    @MutationMapping
    public PlayerTypeEntity evaluateTest(@Argument final UUID userUUID) {

        if (this.test != null && !this.test.justCreated) {
            PlayerTypeEntity playerTypeEntity = this.test.evaluateTest(userUUID);
            return playerTypeService.saveTestResult(playerTypeEntity);
        }
        return new PlayerTypeEntity(userUUID, false);

    }

    @MutationMapping
    public PlayerType createOrUpdatePlayerType(@Argument UUID userUUID,
                                               @Argument int achieverPercentage,
                                               @Argument int explorerPercentage,
                                               @Argument int socializerPercentage,
                                               @Argument int killerPercentage) {
        return playerTypeService.createOrUpdatePlayerType(userUUID, achieverPercentage, explorerPercentage, socializerPercentage, killerPercentage);
    }


    @MutationMapping
    public String addCourse(@Argument UUID courseUUID, @Argument UUID lecturerUUID) {
        badgeService.addCourse(courseUUID, lecturerUUID);
        return "Added course.";
    }

    @MutationMapping
    public String addUserToCourse(@Argument UUID userUUID,
                                @Argument UUID courseUUID) {
        badgeService.addUserToCourse(userUUID, courseUUID);
        return "Assigned badges of course to user.";
    }

    @MutationMapping
    public List<UserBadge> markBadgesAsAchievedIfPassedQuiz(@Argument UUID userUUID,
                                                            @Argument UUID quizUUID,
                                                            @Argument int correctAnswers,
                                                            @Argument int totalAnswers) {
        return badgeService.markBadgesAsAchievedIfPassedQuiz(userUUID, quizUUID, correctAnswers, totalAnswers);
    }

    @MutationMapping
    public List<UserBadge> markBadgesAsAchievedIfPassedFlashCardSet(@Argument UUID userUUID,
                                                                    @Argument UUID flashCardSetUUID,
                                                                    @Argument int correctAnswers,
                                                                    @Argument int totalAnswers) {
        return badgeService.markBadgesAsAchievedIfPassedFlashCardSet(userUUID, flashCardSetUUID, correctAnswers, totalAnswers);
    }

    @MutationMapping
    public List<Badge> createBadgesForQuiz(@Argument UUID quizUUID,
                                           @Argument String name,
                                           @Argument UUID courseUUID) {
        List<Badge> badges = new LinkedList<Badge>();
        // 50% Badge
        badges.add(badgeService.createBadgeForQuiz(quizUUID, name, bronzePassingPercentage, courseUUID));
        // 70% Badge
        badges.add(badgeService.createBadgeForQuiz(quizUUID, name, silverPassingPercentage, courseUUID));
        // 90% Badge
        badges.add(badgeService.createBadgeForQuiz(quizUUID, name, goldPassingPercentage, courseUUID));
        return badges;
    }

    @MutationMapping
    public List<Badge> createBadgesForFlashCardSet(@Argument UUID flashCardSetUUID,
                                                   @Argument String name,
                                                   @Argument UUID courseUUID) {
        List<Badge> badges = new LinkedList<Badge>();
        // 50% Badge
        badges.add(badgeService.createBadgeForFlashCardSet(flashCardSetUUID, name, bronzePassingPercentage, courseUUID));
        // 70% Badge
        badges.add(badgeService.createBadgeForFlashCardSet(flashCardSetUUID, name, silverPassingPercentage, courseUUID));
        // 90% Badge
        badges.add(badgeService.createBadgeForFlashCardSet(flashCardSetUUID, name, goldPassingPercentage, courseUUID));
        return badges;
    }




    // Not used currently
    @MutationMapping
    public UserBadge assignBadgeToUser(@Argument UUID userUUID, @Argument UUID badgeUUID) {
        return badgeService.assignBadgeToUser(userUUID, badgeUUID);
    }

    @MutationMapping
    public UserBadge markBadgeAsAchieved(@Argument UUID userUUID, @Argument UUID badgeUUID) {
        return badgeService.markBadgeAsAchieved(userUUID, badgeUUID);
    }

}
