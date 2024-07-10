package de.unistuttgart.iste.meitrex.gamification_service.controller;

import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.PlayerTypeEntity;
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

    private static final int lowPassingPercentage = 50;
    private static final int middlePassingPercentage = 70;
    private static final int highPassingPercentage = 90;


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
    public boolean userCanSeeSocializerElement(@Argument UUID userUUID) {
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
    public PlayerType createOrUpdatePlayerType(@Argument UUID userUUID,
                                               @Argument int achieverPercentage,
                                               @Argument int explorerPercentage,
                                               @Argument int socializerPercentage,
                                               @Argument int killerPercentage) {
        return playerTypeService.createOrUpdatePlayerType(userUUID, achieverPercentage, explorerPercentage, socializerPercentage, killerPercentage);
    }

    @MutationMapping
    public List<UserBadge> markBadgesAsAchievedIfPassedQuiz(@Argument UUID userUUID, @Argument UUID quizUUID, @Argument int correctAnswers, @Argument int totalAnswers) {
        return badgeService.markBadgesAsAchievedIfPassedQuiz(userUUID, quizUUID, correctAnswers, totalAnswers);
    }

    @MutationMapping
    public List<UserBadge> markBadgesAsAchievedIfPassedFlashCardSet(@Argument UUID userUUID, @Argument UUID flashCardSetUUID, @Argument int correctAnswers, @Argument int totalAnswers) {
        return badgeService.markBadgesAsAchievedIfPassedFlashCardSet(userUUID, flashCardSetUUID, correctAnswers, totalAnswers);
    }

    @MutationMapping
    public UserBadge assignBadgeToUser(@Argument UUID userUUID, @Argument UUID badgeUUID) {
        return badgeService.assignBadgeToUser(userUUID, badgeUUID);
    }

    @MutationMapping
    public UserBadge markBadgeAsAchieved(@Argument UUID userUUID, @Argument UUID badgeUUID) {
        return badgeService.markBadgeAsAchieved(userUUID, badgeUUID);
    }

    @MutationMapping
    public List<Badge> createBadgesForQuiz(@Argument UUID quizUUID,
                                    @Argument String name,
                                    @Argument String description) {
        List<Badge> badges = new LinkedList<Badge>();
        // 50% Badge
        badges.add(badgeService.createBadgeForQuiz(quizUUID, name, description, lowPassingPercentage));
        // 70% Badge
        badges.add(badgeService.createBadgeForQuiz(quizUUID, name, description, middlePassingPercentage));
        // 90% Badge
        badges.add(badgeService.createBadgeForQuiz(quizUUID, name, description, highPassingPercentage));
        return badges;
    }

    @MutationMapping
    public List<Badge> createBadgesForFlashCardSet(@Argument UUID flashCardSetUUID,
                                           @Argument String name,
                                           @Argument String description) {
        List<Badge> badges = new LinkedList<Badge>();
        // 50% Badge
        badges.add(badgeService.createBadgeForFlashCardSet(flashCardSetUUID, name, description, lowPassingPercentage));
        // 70% Badge
        badges.add(badgeService.createBadgeForFlashCardSet(flashCardSetUUID, name, description, middlePassingPercentage));
        // 90% Badge
        badges.add(badgeService.createBadgeForFlashCardSet(flashCardSetUUID, name, description, highPassingPercentage));
        return badges;
    }

}
