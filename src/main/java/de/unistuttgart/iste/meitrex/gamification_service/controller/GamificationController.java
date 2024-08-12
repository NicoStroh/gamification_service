package de.unistuttgart.iste.meitrex.gamification_service.controller;

import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.PlayerTypeEntity;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.PlayerTypeTestQuestion;
import de.unistuttgart.iste.meitrex.gamification_service.service.*;
import de.unistuttgart.iste.meitrex.generated.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GamificationController {

    private final PlayerTypeService playerTypeService;
    private final CourseService courseService;
    private final BadgeService badgeService;
    private final QuestService questService;
    private final BloomLevelService bloomLevelService;


    @MutationMapping
    public String addCourse(@Argument UUID courseUUID, @Argument UUID lecturerUUID, @Argument List<UUID> chapters) {
        courseService.addCourse(courseUUID, lecturerUUID);
        questService.addCourse(courseUUID, lecturerUUID);
        bloomLevelService.addCourse(courseUUID, chapters, lecturerUUID);
        return "Added course.";
    }

    @MutationMapping
    public String addChapter(@Argument UUID courseUUID, @Argument UUID chapterUUID) {
        bloomLevelService.addChapter(courseUUID, chapterUUID);
        return "Added section to course.";
    }

    @MutationMapping
    public String addUserToCourse(@Argument UUID userUUID, @Argument UUID courseUUID) {
        courseService.addUserToCourse(userUUID, courseUUID);
        badgeService.assignCoursesBadgesToUser(courseUUID, userUUID);
        questService.assignQuestChainToUser(userUUID, courseUUID);
        bloomLevelService.addUserToCourse(userUUID, courseUUID);
        return "Added user to course.";
    }

    @MutationMapping
    public String createFlashCardSet(@Argument UUID flashCardSetUUID,
                                     @Argument String name,
                                     @Argument UUID courseUUID,
                                     @Argument UUID chapterUUID,
                                     @Argument int skillPoints,
                                     @Argument SkillType skillType) {
        badgeService.createBadgesForFlashCardSet(flashCardSetUUID, name, courseUUID, courseService.getCoursesUsers(courseUUID));
        questService.createQuestForFlashCardSet(flashCardSetUUID, name, courseUUID);
        bloomLevelService.addFlashCardSet(chapterUUID, courseUUID, flashCardSetUUID, skillPoints, skillType);
        return "Created flashCardSet successfully.";
    }

    @MutationMapping
    public String createQuiz(@Argument UUID quizUUID,
                             @Argument String name,
                             @Argument UUID courseUUID,
                             @Argument UUID chapterUUID,
                             @Argument int skillPoints,
                             @Argument SkillType skillType) {
        badgeService.createBadgesForQuiz(quizUUID, name, courseUUID, courseService.getCoursesUsers(courseUUID));
        questService.createQuestForQuiz(quizUUID, name, courseUUID);
        bloomLevelService.addQuiz(chapterUUID, courseUUID, quizUUID, skillPoints, skillType);
        return "Created quiz successfully.";
    }

    @MutationMapping
    public String deleteBadgesAndQuestsOfCourse(@Argument UUID courseUUID) {
        HashSet<UUID> courseMembers = courseService.deleteCourse(courseUUID);
        badgeService.deleteBadgesAndUserBadgesOfCourse(courseUUID);
        questService.deleteQuestChainAndUserQuestChainsOfCourse(courseUUID);
        bloomLevelService.deleteCourse(courseUUID, courseMembers);
        return "Course deleted.";
    }

    @MutationMapping
    public String deleteBadgesAndQuestOfFlashCardSet(@Argument UUID flashCardSetUUID,
                                                     @Argument UUID courseUUID,
                                                     @Argument UUID chapterUUID) {
        badgeService.deleteBadgesAndUserBadgesOfFCS(flashCardSetUUID);
        questService.deleteQuestOfFCS(courseUUID, flashCardSetUUID);
        bloomLevelService.removeFlashCardSet(courseUUID, chapterUUID, flashCardSetUUID);
        return "FlashCardSet deleted.";
    }

    @MutationMapping
    public String deleteBadgesAndQuestOfQuiz(@Argument UUID quizUUID,
                                             @Argument UUID courseUUID,
                                             @Argument UUID chapterUUID) {
        badgeService.deleteBadgesAndUserBadgesOfQuiz(quizUUID);
        questService.deleteQuestOfQuiz(courseUUID, quizUUID);
        bloomLevelService.removeQuiz(courseUUID, chapterUUID, quizUUID);
        return "Quiz deleted.";
    }

    @MutationMapping
    public String editFlashCardSet(@Argument UUID flashCardSetUUID,
                                   @Argument UUID courseUUID,
                                   @Argument String name,
                                   @Argument int skillPoints,
                                   @Argument SkillType skillType) {
        badgeService.changeFlashCardSetName(flashCardSetUUID, name);
        questService.changeFlashCardSetName(flashCardSetUUID, courseUUID, name);
        bloomLevelService.saveContent(flashCardSetUUID, skillPoints, skillType);
        return "Changed flashCardSet name!";
    }

    @MutationMapping
    public String editQuiz(@Argument UUID quizUUID,
                           @Argument UUID courseUUID,
                           @Argument String name,
                           @Argument int skillPoints,
                           @Argument SkillType skillType) {
        badgeService.changeQuizName(quizUUID, name);
        questService.changeQuizName(quizUUID, courseUUID, name);
        bloomLevelService.saveContent(quizUUID, skillPoints, skillType);
        return "Changed quiz name!";
    }

    @MutationMapping
    public PlayerTypeEntity evaluateTest(@Argument UUID userUUID) {
        return playerTypeService.evaluateTest(userUUID);
    }

    @MutationMapping
    public String finishQuiz(@Argument UUID userUUID,
                             @Argument UUID courseUUID,
                             @Argument UUID quizUUID,
                             @Argument int correctAnswers,
                             @Argument int totalAnswers,
                             @Argument UUID chapterUUID) {
        badgeService.markBadgesAsAchievedIfPassedQuiz(userUUID, quizUUID, correctAnswers, totalAnswers);
        questService.markQuestAsFinishedIfPassedQuiz(userUUID, courseUUID, quizUUID, correctAnswers, totalAnswers);
        bloomLevelService.grantRewardToUserForFinishingQuiz(courseUUID, userUUID, chapterUUID,
                quizUUID, correctAnswers, totalAnswers);
        return "Finished quiz!";
    }

    @MutationMapping
    public String finishFlashCardSet(@Argument UUID userUUID,
                                     @Argument UUID courseUUID,
                                     @Argument UUID flashCardSetUUID,
                                     @Argument int correctAnswers,
                                     @Argument int totalAnswers,
                                     @Argument UUID chapterUUID) {
        badgeService.markBadgesAsAchievedIfPassedFlashCardSet(userUUID, flashCardSetUUID, correctAnswers, totalAnswers);
        questService.markQuestAsFinishedIfPassedFlashCardSet(userUUID, courseUUID, flashCardSetUUID, correctAnswers, totalAnswers);
        bloomLevelService.grantRewardToUserForFinishingFlashCardSet(courseUUID, userUUID, chapterUUID,
                flashCardSetUUID, correctAnswers, totalAnswers);
        return "Finished flashCardSet!";
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
    public BloomLevel getUsersBloomLevel(@Argument UUID userUUID, @Argument UUID courseUUID) {
        return bloomLevelService.getUsersBloomLevel(userUUID, courseUUID);
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
        bloomLevelService.removeUserFromCourse(userUUID, courseUUID);
        return "Removed user from course.";
    }

    @MutationMapping
    public String submitAnswer(@Argument int questionId, @Argument boolean answer) {
        return playerTypeService.submitAnswer(questionId, answer);
    }

    @QueryMapping
    public PlayerTypeTestQuestion[] test() {
        return playerTypeService.test();
    }

    @QueryMapping
    public boolean userHasTakenTest(@Argument UUID userUUID) {
        return playerTypeService.userHasTakenTest(userUUID);
    }

    @QueryMapping
    public PlayerTypeEntity.DominantPlayerType usersDominantPlayerType(@Argument UUID userUUID) {
        return playerTypeService.usersDominantPlayerType(userUUID);
    }

}
