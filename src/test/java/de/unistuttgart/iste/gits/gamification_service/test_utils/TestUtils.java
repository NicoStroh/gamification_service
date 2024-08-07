package de.unistuttgart.iste.gits.gamification_service.test_utils;

import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.*;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.BadgeRepository;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.CourseRepository;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.PlayerTypeRepository;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.QuestChainRepository;
import de.unistuttgart.iste.meitrex.gamification_service.service.BadgeService;
import de.unistuttgart.iste.meitrex.gamification_service.service.QuestService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TestUtils {

    /**
     * Helper method which fills all the repositories to test.
     */
    public static void populateRepositories(PlayerTypeRepository playerTypeRepository,
                                     CourseRepository courseRepository,
                                     BadgeRepository badgeRepository,
                                     QuestChainRepository questChainRepository) {
        List<PlayerTypeEntity> users = populatePlayerTypeRepository(playerTypeRepository);
        List<CourseEntity> courses = populateCourseRepository(courseRepository, users);
        List<BadgeEntity> badges = populateBadgeRepository(badgeRepository, courses.getFirst());
        QuestChainEntity questChain = populateQuestRepository(questChainRepository, courses.getLast());
    }

    /**
     * Helper method which creates some users and saves their playertypes to the repository.
     * @param repo The repository to save the entities to.
     * @return Returns the created playertypes.
     */
    public static List<PlayerTypeEntity> populatePlayerTypeRepository(PlayerTypeRepository repo) {

        List<PlayerTypeEntity> playerTypes = new ArrayList<>();

        int numQuestions = 10;
        int numCombinations = 1 << numQuestions;
        for (int i = 0; i < numCombinations; i++) {
            PlayerTypeTest test = new PlayerTypeTest();

            boolean[] booleans = new boolean[numQuestions];
            // Convert the number to a boolean array
            for (int j = 0; j < numQuestions; j++) {
                booleans[j] = (i & (1 << j)) != 0;
                test.setAnswer(j, booleans[j]);
            }

            UUID userUUID = UUID.randomUUID();
            PlayerTypeEntity playerType = test.evaluateTest(userUUID);
            repo.save(playerType);
            playerTypes.add(playerType);
        }
        return playerTypes;
    }

    /**
     * Helper method which creates some courses with some users in them and saves them to the repository.
     * @param repo The repository to save the entities to.
     * @return Returns the created courses.
     */
    public static List<CourseEntity> populateCourseRepository(CourseRepository repo, List<PlayerTypeEntity> users) {

        List<CourseEntity> courses = new ArrayList<>();

        UUID uuid1 = UUID.randomUUID();
        CourseEntity course1 = new CourseEntity(uuid1, new HashSet<>());
        for (int i = 0; i < 4; i++) {
            if (i < users.size()) {
                course1.addUser(users.get(i).getUserUUID());
            }
        }
        repo.save(course1);
        courses.add(course1);


        UUID uuid2 = UUID.randomUUID();
        CourseEntity course2 = new CourseEntity(uuid2, new HashSet<>());
        for (int i = 2; i < 6; i++) {
            if (i < users.size()) {
                course2.addUser(users.get(i).getUserUUID());
            }
        }
        repo.save(course2);
        courses.add(course2);


        return courses;
    }

    /**
     * Helper method which creates some badges for quizzes and flashcardsets and saves them to the repository.
     * @param badgeRepository The repository to save the entities to.
     * @return Returns the created badges.
     */
    public static List<BadgeEntity> populateBadgeRepository(BadgeRepository badgeRepository, CourseEntity courseEntity) {

        List<BadgeEntity> badges = new ArrayList<>();


        UUID quiz = UUID.randomUUID();

        int passingPercentage = 50;
        String name = "Quiz 1";
        BadgeEntity badgeEntity = new BadgeEntity();
        badgeEntity.setDescription(BadgeService.descriptionPart1 + passingPercentage +
                BadgeService.descriptionPart2 + "quiz " + name + BadgeService.descriptionPart3);
        badgeEntity.setPassingPercentage(passingPercentage);
        badgeEntity.setQuizUUID(quiz);
        badgeEntity.setCourseUUID(courseEntity.getCourseUUID());
        badgeRepository.save(badgeEntity);
        badges.add(badgeEntity);

        passingPercentage = 70;
        badgeEntity = new BadgeEntity();
        badgeEntity.setDescription(BadgeService.descriptionPart1 + passingPercentage +
                BadgeService.descriptionPart2 + "quiz " + name + BadgeService.descriptionPart3);
        badgeEntity.setPassingPercentage(passingPercentage);
        badgeEntity.setQuizUUID(quiz);
        badgeEntity.setCourseUUID(courseEntity.getCourseUUID());
        badgeRepository.save(badgeEntity);
        badges.add(badgeEntity);

        passingPercentage = 90;
        badgeEntity = new BadgeEntity();
        badgeEntity.setDescription(BadgeService.descriptionPart1 + passingPercentage +
                BadgeService.descriptionPart2 + "quiz " + name + BadgeService.descriptionPart3);
        badgeEntity.setPassingPercentage(passingPercentage);
        badgeEntity.setQuizUUID(quiz);
        badgeEntity.setCourseUUID(courseEntity.getCourseUUID());
        badgeRepository.save(badgeEntity);
        badges.add(badgeEntity);


        quiz = UUID.randomUUID();

        passingPercentage = 50;
        name = "Quiz 2";
        badgeEntity = new BadgeEntity();
        badgeEntity.setDescription(BadgeService.descriptionPart1 + passingPercentage +
                BadgeService.descriptionPart2 + "quiz " + name + BadgeService.descriptionPart3);
        badgeEntity.setPassingPercentage(passingPercentage);
        badgeEntity.setQuizUUID(quiz);
        badgeEntity.setCourseUUID(courseEntity.getCourseUUID());
        badgeRepository.save(badgeEntity);
        badges.add(badgeEntity);

        passingPercentage = 70;
        badgeEntity = new BadgeEntity();
        badgeEntity.setDescription(BadgeService.descriptionPart1 + passingPercentage +
                BadgeService.descriptionPart2 + "quiz " + name + BadgeService.descriptionPart3);
        badgeEntity.setPassingPercentage(passingPercentage);
        badgeEntity.setQuizUUID(quiz);
        badgeEntity.setCourseUUID(courseEntity.getCourseUUID());
        badgeRepository.save(badgeEntity);
        badges.add(badgeEntity);

        passingPercentage = 90;
        badgeEntity = new BadgeEntity();
        badgeEntity.setDescription(BadgeService.descriptionPart1 + passingPercentage +
                BadgeService.descriptionPart2 + "quiz " + name + BadgeService.descriptionPart3);
        badgeEntity.setPassingPercentage(passingPercentage);
        badgeEntity.setQuizUUID(quiz);
        badgeEntity.setCourseUUID(courseEntity.getCourseUUID());
        badgeRepository.save(badgeEntity);
        badges.add(badgeEntity);



        UUID fcs = UUID.randomUUID();

        passingPercentage = 50;
        name = "FCS 1";
        badgeEntity = new BadgeEntity();
        badgeEntity.setDescription(BadgeService.descriptionPart1 + passingPercentage +
                BadgeService.descriptionPart2 + "flashcardSet " + name + BadgeService.descriptionPart3);
        badgeEntity.setPassingPercentage(passingPercentage);
        badgeEntity.setQuizUUID(fcs);
        badgeEntity.setCourseUUID(courseEntity.getCourseUUID());
        badgeRepository.save(badgeEntity);
        badges.add(badgeEntity);

        passingPercentage = 70;
        badgeEntity = new BadgeEntity();
        badgeEntity.setDescription(BadgeService.descriptionPart1 + passingPercentage +
                BadgeService.descriptionPart2 + "flashcardSet " + name + BadgeService.descriptionPart3);
        badgeEntity.setPassingPercentage(passingPercentage);
        badgeEntity.setQuizUUID(fcs);
        badgeEntity.setCourseUUID(courseEntity.getCourseUUID());
        badgeRepository.save(badgeEntity);
        badges.add(badgeEntity);

        passingPercentage = 90;
        badgeEntity = new BadgeEntity();
        badgeEntity.setDescription(BadgeService.descriptionPart1 + passingPercentage +
                BadgeService.descriptionPart2 + "flashcardSet " + name + BadgeService.descriptionPart3);
        badgeEntity.setPassingPercentage(passingPercentage);
        badgeEntity.setQuizUUID(fcs);
        badgeEntity.setCourseUUID(courseEntity.getCourseUUID());
        badgeRepository.save(badgeEntity);
        badges.add(badgeEntity);


        fcs = UUID.randomUUID();

        passingPercentage = 50;
        name = "FCS 2";
        badgeEntity = new BadgeEntity();
        badgeEntity.setDescription(BadgeService.descriptionPart1 + passingPercentage +
                BadgeService.descriptionPart2 + "flashcardSet " + name + BadgeService.descriptionPart3);
        badgeEntity.setPassingPercentage(passingPercentage);
        badgeEntity.setQuizUUID(fcs);
        badgeEntity.setCourseUUID(courseEntity.getCourseUUID());
        badgeRepository.save(badgeEntity);
        badges.add(badgeEntity);

        passingPercentage = 70;
        badgeEntity = new BadgeEntity();
        badgeEntity.setDescription(BadgeService.descriptionPart1 + passingPercentage +
                BadgeService.descriptionPart2 + "flashcardSet " + name + BadgeService.descriptionPart3);
        badgeEntity.setPassingPercentage(passingPercentage);
        badgeEntity.setQuizUUID(fcs);
        badgeEntity.setCourseUUID(courseEntity.getCourseUUID());
        badgeRepository.save(badgeEntity);
        badges.add(badgeEntity);

        passingPercentage = 90;
        badgeEntity = new BadgeEntity();
        badgeEntity.setDescription(BadgeService.descriptionPart1 + passingPercentage +
                BadgeService.descriptionPart2 + "flashcardSet " + name + BadgeService.descriptionPart3);
        badgeEntity.setPassingPercentage(passingPercentage);
        badgeEntity.setQuizUUID(fcs);
        badgeEntity.setCourseUUID(courseEntity.getCourseUUID());
        badgeRepository.save(badgeEntity);
        badges.add(badgeEntity);


        return badges;

    }

    /**
     * Helper method which creates some quests for quizzes and flashcardsets and saves them to the repository.
     * @param questChainRepository The repository which contains the quests.
     * @return Returns the created questchain.
     */
    public static QuestChainEntity populateQuestRepository(QuestChainRepository questChainRepository, CourseEntity coursesEntity) {

        QuestChainEntity questChainEntity = new QuestChainEntity();
        questChainEntity.setCourseUUID(coursesEntity.getCourseUUID());
        questChainRepository.save(questChainEntity);


        UUID quiz = UUID.randomUUID();
        String name = "Quiz 1";
        QuestEntity quest = new QuestEntity();
        quest.setQuizUUID(quiz);
        quest.setDescription(QuestService.descriptionPart1 + "quiz" + name + QuestService.descriptionPart2 +
                QuestService.passingPercentage + QuestService.descriptionPart3);
        questChainEntity.addQuest(quest);
        questChainRepository.save(questChainEntity);

        quiz = UUID.randomUUID();
        name = "Quiz 2";
        quest = new QuestEntity();
        quest.setQuizUUID(quiz);
        quest.setDescription(QuestService.descriptionPart1 + "quiz" + name + QuestService.descriptionPart2 +
                QuestService.passingPercentage + QuestService.descriptionPart3);
        questChainEntity.addQuest(quest);
        questChainRepository.save(questChainEntity);


        UUID fcs = UUID.randomUUID();
        name = "FCS 1";
        quest = new QuestEntity();
        quest.setFlashCardSetUUID(fcs);
        quest.setDescription(QuestService.descriptionPart1 + "flashcardSet" + name + QuestService.descriptionPart2 +
                QuestService.passingPercentage + QuestService.descriptionPart3);
        questChainEntity.addQuest(quest);
        questChainRepository.save(questChainEntity);

        fcs = UUID.randomUUID();
        name = "FCS 2";
        quest = new QuestEntity();
        quest.setFlashCardSetUUID(quiz);
        quest.setDescription(QuestService.descriptionPart1 + "flashcardSet" + name + QuestService.descriptionPart2 +
                QuestService.passingPercentage + QuestService.descriptionPart3);
        questChainEntity.addQuest(quest);
        questChainRepository.save(questChainEntity);

        return questChainEntity;

    }

}
