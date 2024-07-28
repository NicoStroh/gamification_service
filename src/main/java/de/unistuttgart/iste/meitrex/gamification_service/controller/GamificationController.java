package de.unistuttgart.iste.meitrex.gamification_service.controller;

import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.PlayerTypeEntity;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.PlayerTypeTest;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.PlayerTypeTestQuestion;
import de.unistuttgart.iste.meitrex.gamification_service.service.BadgeService;
import de.unistuttgart.iste.meitrex.gamification_service.service.CourseService;
import de.unistuttgart.iste.meitrex.gamification_service.service.PlayerTypeService;
import de.unistuttgart.iste.meitrex.gamification_service.service.QuestService;
import de.unistuttgart.iste.meitrex.generated.dto.*;
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
    private final CourseService courseService;
    private final QuestService questService;

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
        if (playerType.isPresent() && playerType.get().getDominantPlayerType() != null) {
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

    @QueryMapping
    public Quest getCurrentUserQuest(@Argument UUID userUUID, @Argument UUID courseUUID) {
        return questService.getCurrentUserQuest(userUUID, courseUUID);
    }

    @QueryMapping
    public UserQuestChain getUserQuestChain(@Argument UUID userUUID, @Argument UUID courseUUID) {
        return questService.getUserQuestChain(userUUID, courseUUID);
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
    public String createOrUpdatePlayerType(@Argument UUID userUUID,
                                               @Argument int achieverPercentage,
                                               @Argument int explorerPercentage,
                                               @Argument int socializerPercentage,
                                               @Argument int killerPercentage) {
        playerTypeService.createOrUpdatePlayerType(userUUID, achieverPercentage, explorerPercentage, socializerPercentage, killerPercentage);
        return "Updated player type successfully!";
    }


    @MutationMapping
    public String addCourse(@Argument UUID courseUUID, @Argument UUID lecturerUUID) {
        courseService.addCourse(courseUUID, lecturerUUID, this.badgeService, this.questService);
        return "Added course.";
    }

    @MutationMapping
    public String addUserToCourse(@Argument UUID userUUID,
                                @Argument UUID courseUUID) {
        courseService.addUserToCourse(userUUID, courseUUID, this.badgeService, this.questService);
        return "Added user to course.";
    }

    @MutationMapping
    public String finishQuiz(@Argument UUID userUUID,
                             @Argument UUID courseUUID,
                             @Argument UUID quizUUID,
                             @Argument int correctAnswers,
                             @Argument int totalAnswers) {
        badgeService.markBadgesAsAchievedIfPassedQuiz(userUUID, quizUUID, correctAnswers, totalAnswers);
        questService.markQuestAsFinishedIfPassedQuiz(userUUID, courseUUID, quizUUID, correctAnswers, totalAnswers);
        return "Finished quiz!";
    }

    @MutationMapping
    public String finishFlashCardSet(@Argument UUID userUUID,
                                     @Argument UUID courseUUID,
                                     @Argument UUID flashCardSetUUID,
                                     @Argument int correctAnswers,
                                     @Argument int totalAnswers) {
        badgeService.markBadgesAsAchievedIfPassedFlashCardSet(userUUID, flashCardSetUUID, correctAnswers, totalAnswers);
        questService.markQuestAsFinishedIfPassedFlashCardSet(userUUID, courseUUID, flashCardSetUUID, correctAnswers, totalAnswers);
        return "Finished FlashCardSet!";
    }

    @MutationMapping
    public String createQuiz(@Argument UUID quizUUID,
                             @Argument String name,
                             @Argument UUID courseUUID) {
        badgeService.createBadgesForQuiz(quizUUID, name, courseUUID, this.courseService);
        questService.createQuestForQuiz(quizUUID, name, courseUUID);
        return "Created quiz successully.";
    }

    @MutationMapping
    public String createFlashCardSet(@Argument UUID flashCardSetUUID,
                                     @Argument String name,
                                     @Argument UUID courseUUID) {
        badgeService.createBadgesForFlashCardSet(flashCardSetUUID, name, courseUUID, this.courseService);
        questService.createQuestForFlashCardSet(flashCardSetUUID, name, courseUUID);
        return "Created Flashcardset successully.";
    }

}
