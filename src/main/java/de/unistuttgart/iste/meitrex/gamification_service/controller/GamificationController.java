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

import java.util.List;
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

    /**
     * Creates a new course and saves it in the repositories, adds the creator of the course to it.
     * The quest chain is created for the course and assigned to the creator, same for the bloomLevel.
     *
     * @param courseUUID       the id of the created course
     * @param lecturerUUID     the id of the creator of the course
     * @param chapters         the ids of the created chapters of the course, creating levels for the course level.
     */
    @MutationMapping
    public String addCourse(@Argument UUID courseUUID, @Argument UUID lecturerUUID, @Argument List<UUID> chapters) {
        boolean newCourse = courseService.addCourse(courseUUID, lecturerUUID);
        if (newCourse) {
            questService.addCourse(courseUUID, lecturerUUID);
            bloomLevelService.addCourse(courseUUID, chapters, lecturerUUID);
            return "Added course.";
        }
        return "Course already exists.";
    }

    /**
     * Creates a new chapter and therefore a new level inside the course
     *
     * @param courseUUID       the id of the created course
     * @param chapterUUID      the id of the created chapter
     */
    @MutationMapping
    public String addChapter(@Argument UUID courseUUID, @Argument UUID chapterUUID) {
        return bloomLevelService.addChapter(courseUUID, chapterUUID);
    }

    /**
     * Adds a user to the course and assigns the courses badges, the quest chain and the bloomLevel to the user
     *
     * @param userUUID         the id of the user who joined
     * @param courseUUID       the id of the created course
     */
    @MutationMapping
    public String addUserToCourse(@Argument UUID userUUID, @Argument UUID courseUUID) {
        boolean userAddedSuccessFully = courseService.addUserToCourse(userUUID, courseUUID);
        if (userAddedSuccessFully) {
            badgeService.assignCoursesBadgesToUser(courseUUID, userUUID);
            questService.assignQuestChainToUser(userUUID, courseUUID);
            bloomLevelService.addUserToCourse(userUUID, courseUUID);
            return "Added user to course.";
        }
        return "Error at adding user to the course.";
    }

    /**
     * Creates a flashCardSet and all the badges and the quest for it. The required exp for the level increase.
     *
     * @param flashCardSetUUID         the id of the created flashCardSet
     * @param name                     the name of the created flashCardSet
     * @param courseUUID               the id of the course where it was created
     * @param chapterUUID              the id of the chapter where it was created
     * @param skillPoints              the rewarded skill points for the flashCardSet
     * @param skillTypes               the skill types of bloom for the flashCardSet
     */
    @MutationMapping
    public String createFlashCardSet(@Argument UUID flashCardSetUUID,
                                     @Argument String name,
                                     @Argument UUID courseUUID,
                                     @Argument UUID chapterUUID,
                                     @Argument int skillPoints,
                                     @Argument List<SkillType> skillTypes) {
        boolean flashCardSetWasCreated = bloomLevelService.addFlashCardSet(chapterUUID, courseUUID, flashCardSetUUID, skillPoints, skillTypes);
        if (flashCardSetWasCreated) {
            badgeService.createBadgesForFlashCardSet(flashCardSetUUID, name, courseUUID, courseService.getCoursesUsers(courseUUID));
            questService.createQuestForFlashCardSet(flashCardSetUUID, name, courseUUID);
            return "Created flashCardSet successfully.";
        }
        return "Error at creating flashCardSet.";
    }

    /**
     * Creates a quiz and all the badges and the quest for it. The required exp for the level increase.
     *
     * @param quizUUID         the id of the created quiz
     * @param name             the name of the created quiz
     * @param courseUUID       the id of the course where it was created
     * @param chapterUUID      the id of the chapter where it was created
     * @param skillPoints      the rewarded skill points for the quiz
     * @param skillTypes       the skill types of bloom for the quiz
     */
    @MutationMapping
    public String createQuiz(@Argument UUID quizUUID,
                             @Argument String name,
                             @Argument UUID courseUUID,
                             @Argument UUID chapterUUID,
                             @Argument int skillPoints,
                             @Argument List<SkillType> skillTypes) {
        boolean quizWasCreated = bloomLevelService.addQuiz(chapterUUID, courseUUID, quizUUID, skillPoints, skillTypes);
        if (quizWasCreated) {
            badgeService.createBadgesForQuiz(quizUUID, name, courseUUID, courseService.getCoursesUsers(courseUUID));
            questService.createQuestForQuiz(quizUUID, name, courseUUID);
            return "Created quiz successfully.";
        }
        return "Error at creating quiz.";
    }

    /**
     * Deletes all the objects of the course, which are badges, userbadges, quest chains, user quest chains,
     * the course entity itself, the bloomLevels of its students, and the course content (flashCardSets, quizzes).
     *
     * @param courseUUID       the id of the deleted course
     */
    @MutationMapping
    public String deleteBadgesAndQuestsOfCourse(@Argument UUID courseUUID) {
        boolean courseExists = bloomLevelService.deleteCourse(courseUUID);
        if (courseExists) {
            courseService.deleteCourse(courseUUID);
            badgeService.deleteBadgesAndUserBadgesOfCourse(courseUUID);
            questService.deleteQuestChainAndUserQuestChainsOfCourse(courseUUID);
            return "Course deleted.";
        }
        return "Course not found.";
    }

    /**
     * Deletes all the objects of the flashCardSet, which are badges, userbadges, quests and the flashCardSet
     * entity itself. The required exp for the level of the chapter decreased.
     *
     * @param flashCardSetUUID       the id of the deleted flashCardSet
     * @param courseUUID             the id of the course
     * @param chapterUUID            the id of the chapter
     */
    @MutationMapping
    public String deleteBadgesAndQuestOfFlashCardSet(@Argument UUID flashCardSetUUID,
                                                     @Argument UUID courseUUID,
                                                     @Argument UUID chapterUUID) {
        badgeService.deleteBadgesAndUserBadgesOfFCS(flashCardSetUUID);
        questService.deleteQuestOfFCS(courseUUID, flashCardSetUUID);
        bloomLevelService.removeFlashCardSet(courseUUID, chapterUUID, flashCardSetUUID);
        return "FlashCardSet deleted.";
    }

    /**
     * Deletes all the objects of the quiz, which are badges, userbadges, quests and the quiz
     * entity itself. The required exp for the level of the chapter decreased.
     *
     * @param quizUUID       the id of the deleted quiz
     * @param courseUUID     the id of the course
     * @param chapterUUID    the id of the chapter
     */
    @MutationMapping
    public String deleteBadgesAndQuestOfQuiz(@Argument UUID quizUUID,
                                             @Argument UUID courseUUID,
                                             @Argument UUID chapterUUID) {
        badgeService.deleteBadgesAndUserBadgesOfQuiz(quizUUID);
        questService.deleteQuestOfQuiz(courseUUID, quizUUID);
        bloomLevelService.removeQuiz(courseUUID, chapterUUID, quizUUID);
        return "Quiz deleted.";
    }

    /**
     * Changes the name, the skillPoints or the skillTypes of the flashCardSet.
     * Therefore, the data in the repositories are updated, like the description of the
     * flashCardSets badges or its quest.
     *
     * @param flashCardSetUUID       the id of the edited flashCardSet
     * @param courseUUID             the id of the course
     * @param name                   the new name of the flashCardSet
     * @param skillPoints            the new rewarded skillPoints of the flashCardSet
     * @param skillTypes             the new skill types of bloom of the flashCardSet
     */
    @MutationMapping
    public String editFlashCardSet(@Argument UUID flashCardSetUUID,
                                   @Argument UUID courseUUID,
                                   @Argument String name,
                                   @Argument int skillPoints,
                                   @Argument List<SkillType> skillTypes) {
        boolean flashCardSetExists = bloomLevelService.updateContent(courseUUID, flashCardSetUUID, skillPoints, skillTypes);
        if (flashCardSetExists) {
            badgeService.changeFlashCardSetName(flashCardSetUUID, name);
            questService.changeFlashCardSetName(flashCardSetUUID, courseUUID, name);
            return "Changed flashCardSet data!";
        }
        return "Error at editing flashCardSet.";
    }

    /**
     * Changes the name, the skillPoints or the skillTypes of the quiz.
     * Therefore, the data in the repositories are updated, like the description of the
     * quiz badges or its quest.
     *
     * @param quizUUID          the id of the edited quiz
     * @param courseUUID        the id of the course
     * @param name              the new name of the quiz
     * @param skillPoints       the new rewarded skillPoints of the quiz
     * @param skillTypes        the new skill types of bloom of the quiz
     */
    @MutationMapping
    public String editQuiz(@Argument UUID quizUUID,
                           @Argument UUID courseUUID,
                           @Argument String name,
                           @Argument int skillPoints,
                           @Argument List<SkillType> skillTypes) {
        boolean quizExists = bloomLevelService.updateContent(courseUUID, quizUUID, skillPoints, skillTypes);
        if (quizExists) {
            badgeService.changeQuizName(quizUUID, name);
            questService.changeQuizName(quizUUID, courseUUID, name);
            return "Changed quiz data!";
        }
        return "Error at editing quiz.";
    }

    /**
     * Evaluates the currently selected BartleTest with the submitted answers.
     *
     * @param userUUID       the id of the user who took the BartleTest
     */
    @MutationMapping
    public PlayerTypeEntity evaluateTest(@Argument UUID userUUID) {
        return playerTypeService.evaluateTest(userUUID);
    }

    /**
     * The user finishes a quiz. It is checked, whether the requirements for achieving the quizzes badges or quest
     * are fulfilled. The user is rewarded some experience points for finishing the quiz.
     *
     * @param userUUID          the id of the user who finished quiz
     * @param courseUUID        the id of the course where the quiz is located
     * @param quizUUID          the id of the finished quiz
     * @param correctAnswers    the number of correct answers the user had for the quiz
     * @param totalAnswers      the total number of answers for the quiz
     * @param chapterUUID       the id of the chapter where the quiz is located
     */
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

    /**
     * The user finishes a flashCardSet. It is checked, whether the requirements for achieving the
     * flashCardSets badges or quest are fulfilled. The user is rewarded some experience points for finishing
     * the flashCardSet.
     *
     * @param userUUID                  the id of the user who finished flashCardSet
     * @param courseUUID                the id of the course where the flashCardSet is located
     * @param flashCardSetUUID          the id of the finished flashCardSet
     * @param correctAnswers            the number of correct answers the user had for the flashCardSet
     * @param totalAnswers              the total number of answers for the flashCardSet
     * @param chapterUUID               the id of the chapter where the flashCardSet is located
     */
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

    /**
     * Retrieves all the users badges for the course
     *
     * @param userUUID         the id of the user
     * @param courseUUID       the id of the course
     */
    @QueryMapping
    public List<UserBadge> getCoursesUserBadges(@Argument UUID courseUUID, @Argument UUID userUUID) {
        return badgeService.getUserBadgesByCourseUUID(courseUUID, userUUID);
    }

    /**
     * Retrieves the current quest of the user for the course
     *
     * @param userUUID         the id of the user
     * @param courseUUID       the id of the course
     */
    @QueryMapping
    public Quest getCurrentUserQuest(@Argument UUID userUUID, @Argument UUID courseUUID) {
        return questService.getCurrentUserQuest(userUUID, courseUUID);
    }

    /**
     * Retrieves the users bloom level for the course
     *
     * @param userUUID         the id of the user
     * @param courseUUID       the id of the course
     */
    @QueryMapping
    public BloomLevel getUsersBloomLevel(@Argument UUID userUUID, @Argument UUID courseUUID) {
        return bloomLevelService.getUsersBloomLevel(userUUID, courseUUID);
    }

    /**
     * Retrieves the whole quest chain of the user for the course
     *
     * @param userUUID         the id of the user
     * @param courseUUID       the id of the course
     */
    @QueryMapping
    public UserQuestChain getUserQuestChain(@Argument UUID userUUID, @Argument UUID courseUUID) {
        return questService.getUserQuestChain(userUUID, courseUUID);
    }

    /**
     * Removes the user from the course. The user badges are the deleted, as well as the user quest chain
     * and the bloomLevel
     *
     * @param userUUID         the id of the user who left the course
     * @param courseUUID       the id of the course the user left
     */
    @MutationMapping
    public String removeUserFromCourse(@Argument UUID userUUID, @Argument UUID courseUUID) {
        boolean userRemovedSuccessfully = courseService.removeUserFromCourse(userUUID, courseUUID);
        if (userRemovedSuccessfully) {
            badgeService.deleteUserBadgesOfCourse(userUUID, courseUUID);
            questService.deleteUserQuestChain(userUUID, courseUUID);
            bloomLevelService.removeUserFromCourse(userUUID, courseUUID);
            return "Removed user from course.";
        }
        return "Error at removing user from course.";
    }

    /**
     * Submits a answer for a question for the currently selected bartle test
     *
     * @param questionId       the id of the question
     * @param answer           the submitted answer
     */
    @MutationMapping
    public String submitAnswer(@Argument int questionId, @Argument boolean answer) {
        return playerTypeService.submitAnswer(questionId, answer);
    }

    /**
     * Retrieves the questions of an empty bartle test
     */
    @QueryMapping
    public PlayerTypeTestQuestion[] test() {
        return playerTypeService.test();
    }

    /**
     * Retrieves whether the user has already taken the bartle test
     *
     * @param userUUID         the id of the user
     */
    @QueryMapping
    public boolean userHasTakenTest(@Argument UUID userUUID) {
        return playerTypeService.userHasTakenTest(userUUID);
    }

    /**
     * Retrieves the dominant bartles player type of the user
     *
     * @param userUUID         the id of the user
     */
    @QueryMapping
    public PlayerTypeEntity.DominantPlayerType usersDominantPlayerType(@Argument UUID userUUID) {
        return playerTypeService.usersDominantPlayerType(userUUID);
    }

}
