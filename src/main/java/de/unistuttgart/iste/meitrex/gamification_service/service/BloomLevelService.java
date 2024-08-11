package de.unistuttgart.iste.meitrex.gamification_service.service;

import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.BloomLevelEntity;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.CourseEntity;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.QuestChainEntity;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.UserQuestChainEntity;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.BloomLevelRepository;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.CourseRepository;
import de.unistuttgart.iste.meitrex.generated.dto.BloomLevel;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
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
     * Adds a new course, with the number of levels in the course.
     *
     * @param courseUUID          the unique identifier of the course
     * @param chapters            the UUIDs of the chapters the course has
     * @param lecturerUUID        the creator of the course
     */
    public void addCourse(UUID courseUUID, List<UUID> chapters, UUID lecturerUUID) {
        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        if (courseEntity.isPresent()) {
            CourseEntity course = courseEntity.get();
            for (UUID chapter : chapters) {
                course.addChapter(chapter);
            }
            courseRepository.save(courseEntity.get());
        }
        addUserToCourse(lecturerUUID, courseUUID);
    }

    /**
     * Adds a user to the course and saves their bloomLevel in the repository.
     *
     * @param userUUID       the id of the user, who joined the course
     * @param courseUUID     the id of the course
     */
    public void addUserToCourse(UUID userUUID, UUID courseUUID) {
        BloomLevelEntity bloomLevelEntity = new BloomLevelEntity();
        bloomLevelEntity.setCourseUUID(courseUUID);
        bloomLevelEntity.setUserUUID(userUUID);
        bloomLevelEntity.setCollectedExp(0);
        bloomLevelRepository.save(bloomLevelEntity);
    }

    /**
     * Removes a user from the course and deletes their bloomLevel for the course from the repository.
     *
     * @param userUUID       the id of the user, who left the course
     * @param courseUUID     the id of the course
     */
    public void removeUserFromCourse(UUID userUUID, UUID courseUUID) {
        bloomLevelRepository.deleteByUserUUIDAndCourseUUID(userUUID, courseUUID);
    }

    /**
     * Adds a new chapter to the course.
     *
     * @param courseUUID     the unique identifier of the course
     * @param chapterUUID    the unique identifier of the course
     */
    public void addChapter(UUID courseUUID, UUID chapterUUID) {
        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        if (courseEntity.isPresent()) {
            CourseEntity course = courseEntity.get();
            course.addChapter(chapterUUID);
            courseRepository.save(courseEntity.get());
        }
    }

    /**
     * Adds a quiz to a specific level in the course, which increases the required expPoints for the level.
     *
     * @param chapterUUID      the UUID of the chapter where the quiz will be added
     * @param courseUUID       the unique identifier of the course
     */
    public void addQuiz(UUID chapterUUID, UUID courseUUID) {
        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        if (courseEntity.isPresent()) {
            CourseEntity course = courseEntity.get();
            course.addQuiz(chapterUUID);
            courseRepository.save(courseEntity.get());
        }
    }

    /**
     * Adds a flashCardSet to a specific level in the course, which increases the required expPoints for the level.
     *
     * @param chapterUUID      the UUID of the chapter where the flashCardSet will be added
     * @param courseUUID       the unique identifier of the course
     */
    public void addFlashCardSet(UUID chapterUUID, UUID courseUUID) {
        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        if (courseEntity.isPresent()) {
            CourseEntity course = courseEntity.get();
            course.addFlashCardSet(chapterUUID);
            courseRepository.save(courseEntity.get());
        }
    }

    /**
     * Retrieves the level of the chapter in the course.
     *
     * @param courseUUID     the id of the course
     * @param chapterUUID    the id of the chapter
     */
    public int getLevelOfChapter(UUID chapterUUID, UUID courseUUID) {
        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        if (courseEntity.isEmpty()) {
            return 0;
        }
        return courseEntity.get().getChapters().indexOf(chapterUUID);
    }

    /**
     * Decreases the required exp for the chapter of the quiz.
     *
     * @param courseUUID     the id of the course
     * @param chapterUUID    the UUID of the chapter of the deleted quiz
     */
    public void removeQuiz(UUID courseUUID, UUID chapterUUID) {
        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        if (courseEntity.isPresent()) {
            CourseEntity course = courseEntity.get();
            course.removeQuiz(chapterUUID);
            courseRepository.save(courseEntity.get());
        }
    }

    /**
     * Decreases the required exp for the chapter of the flashCardSet.
     *
     * @param courseUUID     the id of the course
     * @param chapterUUID    the UUID of the chapter of the deleted flashCardSet
     */
    public void removeFlashCardSet(UUID courseUUID, UUID chapterUUID) {
        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        if (courseEntity.isPresent()) {
            CourseEntity course = courseEntity.get();
            course.removeFlashCardSet(chapterUUID);
            courseRepository.save(courseEntity.get());
        }
    }

    /**
     * Grants a reward to a user for successfully completing a quiz.
     *
     * @param courseUUID     the unique identifier of the course
     * @param userUUID       the unique identifier of the user
     * @param chapterUUID    the id of the chapter of the quiz
     * @param correctAnswers the number of correct answers given by the user
     * @param totalAnswers   the total number of questions in the quiz
     */
    public void grantRewardToUserForFinishingQuiz(UUID courseUUID,
                                                  UUID userUUID,
                                                  UUID chapterUUID,
                                                  int correctAnswers,
                                                  int totalAnswers) {
        BloomLevelEntity bloomLevel = bloomLevelRepository.findByUserUUIDAndCourseUUID(userUUID, courseUUID);
        if (bloomLevel == null) {
            return;
        }

        int level = getLevelOfChapter(chapterUUID, courseUUID);
        bloomLevel.addExp(CourseEntity.rewardOfFinishedQuiz(level, correctAnswers, totalAnswers));
        bloomLevelRepository.save(bloomLevel);
    }

    /**
     * Grants a reward to a user for successfully completing a flashCardSet.
     *
     * @param courseUUID     the unique identifier of the course
     * @param userUUID       the unique identifier of the user
     * @param chapterUUID    the id of the chapter of the flashCardSet
     * @param correctAnswers the number of correct answers given by the user
     * @param totalAnswers   the total number of questions in the flashCardSet
     */
    public void grantRewardToUserForFinishingFlashCardSet(UUID courseUUID,
                                                          UUID userUUID,
                                                          UUID chapterUUID,
                                                          int correctAnswers,
                                                          int totalAnswers) {
        BloomLevelEntity bloomLevel = bloomLevelRepository.findByUserUUIDAndCourseUUID(userUUID, courseUUID);
        if (bloomLevel == null) {
            return;
        }

        int level = getLevelOfChapter(chapterUUID, courseUUID);
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
