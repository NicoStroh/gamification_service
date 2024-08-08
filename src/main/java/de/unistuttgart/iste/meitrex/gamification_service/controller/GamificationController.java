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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GamificationController {

    private final PlayerTypeService playerTypeService;
    private final CourseService courseService;
    private final BadgeService badgeService;
    private final QuestService questService;

    private PlayerTypeTest test;

    @MutationMapping
    public String addCourse(@Argument UUID courseUUID, @Argument UUID lecturerUUID) {
        courseService.addCourse(courseUUID);
        courseService.addUserToCourse(lecturerUUID, courseUUID);

        questService.addCourse(courseUUID);
        questService.assignQuestChainToUser(lecturerUUID, courseUUID);
        return "Added course.";
    }

    @MutationMapping
    public String addUserToCourse(@Argument UUID userUUID, @Argument UUID courseUUID) {
        courseService.addUserToCourse(userUUID, courseUUID);
        badgeService.assignCoursesBadgesToUser(courseUUID, userUUID);
        questService.assignQuestChainToUser(userUUID, courseUUID);
        return "Added user to course.";
    }

    @MutationMapping
    public String createFlashCardSet(@Argument UUID flashCardSetUUID,
                                     @Argument String name,
                                     @Argument UUID courseUUID) {
        badgeService.createBadgesForFlashCardSet(flashCardSetUUID, name, courseUUID, courseService.getCoursesUsers(courseUUID));
        questService.createQuestForFlashCardSet(flashCardSetUUID, name, courseUUID);
        return "Created Flashcardset successfully.";
    }

    @MutationMapping
    public String createQuiz(@Argument UUID quizUUID,
                             @Argument String name,
                             @Argument UUID courseUUID) {
        badgeService.createBadgesForQuiz(quizUUID, name, courseUUID, courseService.getCoursesUsers(courseUUID));
        questService.createQuestForQuiz(quizUUID, name, courseUUID);
        return "Created quiz successfully.";
    }

    @MutationMapping
    public String deleteBadgesAndQuestsOfCourse(@Argument UUID courseUUID) {
        courseService.deleteCourse(courseUUID);
        badgeService.deleteBadgesAndUserBadgesOfCourse(courseUUID);
        questService.deleteQuestChainAndUserQuestChainsOfCourse(courseUUID);
        return "Course deleted.";
    }

    @MutationMapping
    public String deleteBadgesAndQuestOfFlashCardSet(@Argument UUID flashcardSetUUID, @Argument UUID courseUUID) {
        badgeService.deleteBadgesAndUserBadgesOfFCS(flashcardSetUUID);
        questService.deleteQuestOfFCS(courseUUID, flashcardSetUUID);
        return "FlashcardSet deleted.";
    }

    @MutationMapping
    public String deleteBadgesAndQuestOfQuiz(@Argument UUID quizUUID, @Argument UUID courseUUID) {
        badgeService.deleteBadgesAndUserBadgesOfQuiz(quizUUID);
        questService.deleteQuestOfQuiz(courseUUID, quizUUID);
        return "Quiz deleted.";
    }

    @MutationMapping
    public String editFlashcardSetName(@Argument UUID flashcardSetUUID,
                                       @Argument UUID courseUUID,
                                       @Argument String name) {
        badgeService.changeFlashCardSetName(flashcardSetUUID, name);
        questService.changeFlashcardSetName(flashcardSetUUID, courseUUID, name);
        return "Changed flashcardset name!";
    }

    @MutationMapping
    public String editQuizName(@Argument UUID quizUUID,
                               @Argument UUID courseUUID,
                               @Argument String name) {
        badgeService.changeQuizName(quizUUID, name);
        questService.changeQuizName(quizUUID, courseUUID, name);
        return "Changed quiz name!";
    }

    @MutationMapping
    public PlayerTypeEntity evaluateTest(@Argument UUID userUUID) {

        if (this.test != null && !this.test.justCreated) {
            PlayerTypeEntity playerTypeEntity = this.test.evaluateTest(userUUID);
            return playerTypeService.saveTestResult(playerTypeEntity);
        }
        return new PlayerTypeEntity(userUUID, false);

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
    public String removeUserFromCourse(@Argument UUID userUUID, @Argument UUID courseUUID) {
        courseService.removeUserFromCourse(userUUID, courseUUID);
        badgeService.deleteUserBadgesOfCourse(userUUID, courseUUID);
        questService.deleteUserQuestChain(userUUID, courseUUID);
        return "Removed user from course.";
    }

    @MutationMapping
    public String submitAnswer(@Argument int questionId, @Argument boolean answer) {

        if (this.test != null && 0 <= questionId && questionId < this.test.length()) {
            this.test.setAnswer(questionId, answer);
            return "Answer submitted successfully!";
        } else if (this.test == null) {
            return "No test selected!";
        } else if (0 >= questionId || questionId >= this.test.length()) {
            return "Id out of bounds!";
        }
        return "Unknown error occurred";

    }

    @QueryMapping
    public PlayerTypeTestQuestion[] test() {
        this.test = new PlayerTypeTest();
        return this.test.getQuestions();
    }

    @QueryMapping
    public boolean userHasTakenTest(@Argument UUID userUUID) {

        Optional<PlayerTypeEntity> entity = playerTypeService.getPlayerTypeByUserUUID(userUUID);
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

}
