package de.unistuttgart.iste.meitrex.gamification_service.controller;

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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GamificationController {

    private final PlayerTypeService playerTypeService;
    private final BadgeService badgeService;

    @QueryMapping
    public PlayerType getPlayertype(@Argument UUID userUUID) {
        return playerTypeService.getPlayerTypeByUserUUID(userUUID);
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
    public String assignBadgeToUser(@Argument UUID userUUID, @Argument UUID badgeUUID) {
        badgeService.assignBadgeToUser(userUUID, badgeUUID);
        return "Assigned Badge to User.";
    }

    @MutationMapping
    public String markBadgeAsAchieved(@Argument UUID userUUID, @Argument UUID badgeUUID) {
        badgeService.markBadgeAsAchieved(userUUID, badgeUUID);
        return "Marked badge as achieved for user.";
    }

    @MutationMapping
    public Badge createBadgeForQuiz(@Argument UUID quizUUID,
                                    @Argument String name,
                                    @Argument String description,
                                    @Argument int passingPercentage) {
        return badgeService.createBadgeForQuiz(quizUUID, name, description, passingPercentage);
    }

    @MutationMapping
    public Badge createBadgeForFlashCardSet(@Argument UUID flashCardSetUUID,
                                            @Argument String name,
                                            @Argument String description,
                                            @Argument int passingPercentage) {
        return badgeService.createBadgeForFlashCardSet(flashCardSetUUID, name, description, passingPercentage);
    }

}
