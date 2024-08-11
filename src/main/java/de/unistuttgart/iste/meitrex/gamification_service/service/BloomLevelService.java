package de.unistuttgart.iste.meitrex.gamification_service.service;

import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.BloomLevelEntity;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.CourseEntity;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.BloomLevelRepository;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.CourseRepository;
import de.unistuttgart.iste.meitrex.generated.dto.BloomLevel;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BloomLevelService {

    private final CourseRepository courseRepository;

    private final BloomLevelRepository bloomLevelRepository;

    /**
     * Adds a new section to the course, which increases the number of levels in the course.
     *
     * @param courseUUID the unique identifier of the course
     */
    public void addSection(UUID courseUUID) {
        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        if (courseEntity.isPresent()) {
            CourseEntity course = courseEntity.get();
            course.addLevel();
            courseRepository.save(courseEntity.get());
        }
    }

    /**
     * Adds a quiz to a specific level in the course, which increases the required expPoints for the level.
     *
     * @param level      the level to which the quiz will be added
     * @param courseUUID the unique identifier of the course
     */
    public void addQuiz(int level, UUID courseUUID) {
        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        if (courseEntity.isPresent()) {
            CourseEntity course = courseEntity.get();
            course.addQuiz(level);
            courseRepository.save(courseEntity.get());
        }
    }

    /**
     * Adds a flashCardSet to a specific level in the course, which increases the required expPoints for the level.
     *
     * @param level      the level to which the flashCardSet will be added
     * @param courseUUID the unique identifier of the course
     */
    public void addFlashCardSet(int level, UUID courseUUID) {
        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        if (courseEntity.isPresent()) {
            CourseEntity course = courseEntity.get();
            course.addFlashCardSet(level);
            courseRepository.save(courseEntity.get());
        }
    }

    /**
     * Grants a reward to a user for successfully completing a quiz.
     *
     * @param courseUUID     the unique identifier of the course
     * @param userUUID       the unique identifier of the user
     * @param level          the level of the quiz
     * @param correctAnswers the number of correct answers given by the user
     * @param totalAnswers   the total number of questions in the quiz
     */
    public void grantRewardToUserForFinishingQuiz(UUID courseUUID,
                                                  UUID userUUID,
                                                  int level,
                                                  int correctAnswers,
                                                  int totalAnswers) {
        BloomLevelEntity bloomLevel = bloomLevelRepository.findByUserUUIDAndCourseUUID(userUUID, courseUUID);
        if (bloomLevel == null) {
            return;
        }
        bloomLevel.addExp(CourseEntity.rewardOfFinishedQuiz(level, correctAnswers, totalAnswers));
        bloomLevelRepository.save(bloomLevel);
    }

    /**
     * Grants a reward to a user for successfully completing a flashCardSet.
     *
     * @param courseUUID     the unique identifier of the course
     * @param userUUID       the unique identifier of the user
     * @param level          the level of the flashCardSet
     * @param correctAnswers the number of correct answers given by the user
     * @param totalAnswers   the total number of questions in the flashCardSet
     */
    public void grantRewardToUserForFinishingFlashCardSet(UUID courseUUID,
                                                          UUID userUUID,
                                                          int level,
                                                          int correctAnswers,
                                                          int totalAnswers) {
        BloomLevelEntity bloomLevel = bloomLevelRepository.findByUserUUIDAndCourseUUID(userUUID, courseUUID);
        if (bloomLevel == null) {
            return;
        }
        bloomLevel.addExp(CourseEntity.rewardOfFinishedFlashCardSet(level, correctAnswers, totalAnswers));
        bloomLevelRepository.save(bloomLevel);
    }

    /**
     * Retrieves the current BloomLevel of a user for a specific course.
     *
     * @param userUUID   the unique identifier of the user
     * @param courseUUID the unique identifier of the course
     *
     * @return a BloomLevel object containing the user's current level and experience points for the course
     */
    public BloomLevel getUsersBloomLevel(UUID userUUID, UUID courseUUID) {

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        if (courseEntity.isEmpty()) {
            return new BloomLevel();
        }
        CourseEntity course = courseEntity.get();

        int collectedExp = bloomLevelRepository.findByUserUUIDAndCourseUUID(userUUID, courseUUID).getCollectedExp();
        BloomLevel bloomLevel = new BloomLevel();
        bloomLevel.setTotalExp(collectedExp);
        int level = course.calculateLevelForExp(collectedExp);
        bloomLevel.setLevel(level);
        bloomLevel.setExpForCurrentLevel(course.calculateRemainingExpForCurrentLevel(collectedExp));
        bloomLevel.setRequiredExpForCurrentLevel(course.getRequiredExpPerLevel().get(level));

        return bloomLevel;
    }

}
