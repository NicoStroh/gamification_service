package de.unistuttgart.iste.meitrex.gamification_service.service;

import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.*;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.BloomLevelRepository;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.ContentMetaDataRepository;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.CourseRepository;
import de.unistuttgart.iste.meitrex.generated.dto.BloomLevel;
import de.unistuttgart.iste.meitrex.generated.dto.SkillType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BloomLevelService {

    private final CourseRepository courseRepository;

    private final BloomLevelRepository bloomLevelRepository;

    private final ContentMetaDataRepository contentMetaDataRepository;


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

            if (chapters != null) {
                for (UUID chapter : chapters) {
                    course.addChapter(chapter);
                }
            }
            courseRepository.save(course);
            addUserToCourse(lecturerUUID, courseUUID);
        }
    }

    /**
     * Deletes a course and all the bloomLevel of its students and all its content.
     *
     * @param courseUUID          the unique identifier of the deleted
     *
     * @return whether the course exists
     */
    public boolean deleteCourse(UUID courseUUID) {
        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        if (courseEntity.isPresent()) {
            CourseEntity course = courseEntity.get();
            for (UUID user : course.getUserUUIDs()) {
                bloomLevelRepository.deleteByUserUUIDAndCourseUUID(user, courseUUID);
            }
            for (UUID content : course.getContent()) {
                contentMetaDataRepository.deleteById(content);
            }
            return true;
        }
        return false;
    }

    /**
     * Adds a user to the course and saves their bloomLevel in the repository.
     *
     * @param userUUID       the id of the user, who joined the course
     * @param courseUUID     the id of the course
     */
    public void addUserToCourse(UUID userUUID, UUID courseUUID) {
        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        if (courseEntity.isEmpty()) {
            return;
        }
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
     *
     * @return The outcome of trying to add the chapter.
     */
    public String addChapter(UUID courseUUID, UUID chapterUUID) {

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        if (courseEntity.isPresent()) {
            CourseEntity course = courseEntity.get();

            if (course.getChapters().contains(chapterUUID)) {
                return "Chapter already in course";
            }

            course.addChapter(chapterUUID);
            courseRepository.save(course);

            return "Added chapter to course.";
        }
        return "Course not found.";

    }

    /**
     * Validates whether the input is correct for adding content.
     *
     * @param courseUUID     the unique identifier of the course
     * @param chapterUUID    the id of the chapter of the created content
     * @param contentUUID    the id of the created content
     * @param skillPoints    the rewarded skillPoints for finishing the content
     * @param skillTypes     the max skill Type (Bloom's Tax) of the content
     *
     * @return indicates whether the adding is valid
     */
    private boolean validateAdd(UUID courseUUID,
                                UUID contentUUID,
                                UUID chapterUUID,
                                int skillPoints,
                                List<SkillType> skillTypes) {

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        Optional<ContentMetaDataEntity> contentMetaData = contentMetaDataRepository.findById(contentUUID);
        return courseEntity.isPresent()
                && ! courseEntity.get().getContent().contains(contentUUID)
                && courseEntity.get().getChapters().contains(chapterUUID)
                && ! contentMetaData.isPresent()
                && skillPoints >= 0
                && skillPoints <= 100
                && skillTypes != null
                && ! skillTypes.isEmpty();

    }

    /**
     * Adds a quiz to a specific level in the course, which increases the required expPoints for the level.
     *
     * @param chapterUUID        the UUID of the chapter where the quiz will be added
     * @param courseUUID         the unique identifier of the course
     * @param quizUUID           the unique identifier of the quiz
     * @param skillPoints        the skillPoints rewarded for the quiz
     * @param skillTypes         the skillTypes of the quiz
     *
     * @return indicates whether the quiz was created
     */
    public boolean addQuiz(UUID chapterUUID, UUID courseUUID, UUID quizUUID, int skillPoints, List<SkillType> skillTypes) {

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        if (validateAdd(courseUUID, quizUUID, chapterUUID, skillPoints, skillTypes)) {
            CourseEntity course = courseEntity.get();
            if (! course.addContent(quizUUID, chapterUUID, skillPoints)) {
                return false;
            }
            courseRepository.save(course);
            saveContent(courseUUID, quizUUID, chapterUUID, skillPoints, skillTypes);
            return true;
        }

        return false;
    }

    /**
     * Adds a flashCardSet to a specific level in the course, which increases the required expPoints for the level.
     *
     * @param chapterUUID        the UUID of the chapter where the flashCardSet will be added
     * @param courseUUID         the unique identifier of the course
     * @param flashCardSetUUID   the unique identifier of the flashCardSet
     * @param skillPoints        the skillPoints rewarded for the flashCardSet
     * @param skillTypes         the skillTypes of the flashCardSet
     *
     * @return indicates whether the flashCardSet was created
     */
    public boolean addFlashCardSet(UUID chapterUUID, UUID courseUUID, UUID flashCardSetUUID, int skillPoints, List<SkillType> skillTypes) {

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        if (validateAdd(courseUUID, flashCardSetUUID, chapterUUID, skillPoints, skillTypes)) {
            CourseEntity course = courseEntity.get();
            if (! course.addContent(flashCardSetUUID, chapterUUID, skillPoints)) {
                return false;
            }
            courseRepository.save(course);
            saveContent(courseUUID, flashCardSetUUID, chapterUUID, skillPoints, skillTypes);
            return true;
        }

        return false;
    }

    /**
     * Validates whether the input is correct for editing content.
     *
     * @param courseUUID     the unique identifier of the course
     * @param contentUUID    the id of the content
     * @param chapterUUID    the id of the chapter of the edited content
     * @param skillPoints    the rewarded skillPoints for finishing the content
     * @param skillTypes     the max skill Type (Bloom's Tax) of the content
     *
     * @return indicates whether the editing is valid
     */
    private boolean validateEdit(UUID courseUUID,
                                 UUID contentUUID,
                                 UUID chapterUUID,
                                 int skillPoints,
                                 List<SkillType> skillTypes) {

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        Optional<ContentMetaDataEntity> contentMetaData = contentMetaDataRepository.findById(contentUUID);
        return courseEntity.isPresent()
                && courseEntity.get().getContent().contains(contentUUID)
                && courseEntity.get().getChapters().contains(chapterUUID)
                && contentMetaData.isPresent()
                && skillPoints >= 0
                && skillPoints <= 100
                && skillTypes != null
                && ! skillTypes.isEmpty();

    }

    /**
     * Updates the metadata of the content in the repository,
     *
     * @param courseUUID      the id of the course
     * @param contentUUID     the id of the content
     * @param chapterUUID     the id of the chapter
     * @param skillPoints     the skillPoints rewarded for the content
     * @param skillTypes      the skillTypes of the content
     *
     * @return indicates whether the content is updated successfully. if content does not exist, false is returned.
     */
    public boolean updateContent(UUID courseUUID, UUID contentUUID, UUID chapterUUID, int skillPoints, List<SkillType> skillTypes) {

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        if (validateEdit(courseUUID, contentUUID, chapterUUID, skillPoints, skillTypes)) {
            removeContent(courseUUID, chapterUUID, contentUUID);
            return saveContent(courseUUID, contentUUID, chapterUUID, skillPoints, skillTypes);
        }
        return false;

    }

    /**
     * Saves the metadata of the content in the repository,
     *
     * @param courseUUID      the id of the course
     * @param contentUUID     the id of the content
     * @param chapterUUID     the id of the chapter
     * @param skillPoints     the skillPoints rewarded for the content
     * @param skillTypes      the skillTypes of the content
     *
     * @return indicates whether the content is saved successfully
     */
    public boolean saveContent(UUID courseUUID, UUID contentUUID, UUID chapterUUID, int skillPoints, List<SkillType> skillTypes) {

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        SkillType maxSkillType = skillTypes.stream()
                .max(Comparator.comparingInt(Enum::ordinal))
                .get();

        CourseEntity course = courseEntity.get();
        course.addContent(contentUUID);
        courseRepository.save(course);

        ContentMetaDataEntity contentMetaData = new ContentMetaDataEntity(contentUUID, skillPoints, maxSkillType);
        contentMetaDataRepository.save(contentMetaData);
        return true;

    }

    /**
     * Retrieves the level of the chapter in the course.
     *
     * @param courseUUID     the id of the course
     * @param chapterUUID    the id of the chapter
     */
    public int getLevelOfChapter(UUID chapterUUID, UUID courseUUID) {
        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        if (courseEntity.isEmpty()
                || courseEntity.get().getChapters() == null
                || courseEntity.get().getChapters().isEmpty()) {
            return -1;
        }
        return courseEntity.get().getLevelOfChapter(chapterUUID) + 1;
    }

    /**
     * Decreases the required exp for the chapter of the content.
     *
     * @param courseUUID       the id of the course
     * @param chapterUUID      the UUID of the chapter of the removed content
     * @param contentUUID      the UUID of the removed content
     *
     * @return indicates whether the content could be removed successfully.
     */
    public boolean removeContent(UUID courseUUID, UUID chapterUUID, UUID contentUUID) {
        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        Optional<ContentMetaDataEntity> contentMetaDataEntity = contentMetaDataRepository.findById(contentUUID);

        if (contentMetaDataEntity.isPresent()
                && courseEntity.isPresent()
                && courseEntity.get().getContent().contains(contentUUID)
                && courseEntity.get().getChapters().contains(chapterUUID)) {
            CourseEntity course = courseEntity.get();
            course.removeContent(contentUUID, chapterUUID, contentMetaDataEntity.get().getSkillPoints());
            courseRepository.save(course);
            contentMetaDataRepository.deleteById(contentUUID);
            return true;
        }
        return false;
    }

    /**
     * Validates whether the input is correct for finishing content.
     *
     * @param courseUUID     the unique identifier of the course
     * @param userUUID       the unique identifier of the user
     * @param chapterUUID    the id of the chapter of the finished content
     * @param contentUUID    the id of the content
     * @param correctAnswers the number of correct answers given by the user
     * @param totalAnswers   the total number of questions in the content
     *
     * @return indicates whether the finishing is valid
     */
    private boolean validateFinish(UUID courseUUID,
                                  UUID userUUID,
                                  UUID chapterUUID,
                                  UUID contentUUID,
                                  int correctAnswers,
                                  int totalAnswers) {

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        BloomLevelEntity bloomLevel = bloomLevelRepository.findByUserUUIDAndCourseUUID(userUUID, courseUUID);
        Optional<ContentMetaDataEntity> contentMetaData = contentMetaDataRepository.findById(contentUUID);
        return courseEntity.isPresent()
                && courseEntity.get().getUserUUIDs().contains(userUUID)
                && courseEntity.get().getContent().contains(contentUUID)
                && courseEntity.get().getChapters().contains(chapterUUID)
                && bloomLevel != null
                && contentMetaData.isPresent()
                && correctAnswers <= totalAnswers
                && correctAnswers >= 0;

    }

    /**
     * Grants a reward to a user for successfully finishing content.
     *
     * @param courseUUID     the unique identifier of the course
     * @param userUUID       the unique identifier of the user
     * @param chapterUUID    the id of the chapter of the quiz
     * @param contentUUID    the id of the quiz
     * @param correctAnswers the number of correct answers given by the user
     * @param totalAnswers   the total number of questions in the quiz
     *
     * @return indicates whether the finishing is valid
     */
    public boolean grantRewardToUserForFinishingContent(UUID courseUUID,
                                                     UUID userUUID,
                                                     UUID chapterUUID,
                                                     UUID contentUUID,
                                                     int correctAnswers,
                                                     int totalAnswers) {

        BloomLevelEntity bloomLevel = bloomLevelRepository.findByUserUUIDAndCourseUUID(userUUID, courseUUID);
        Optional<ContentMetaDataEntity> contentMetaData = contentMetaDataRepository.findById(contentUUID);

        if (! validateFinish(courseUUID, userUUID, chapterUUID, contentUUID, correctAnswers, totalAnswers)) {
            return false;
        }

        bloomLevel.addExp((int) contentMetaData.get().rewardOfFinishingContent(correctAnswers, totalAnswers));
        bloomLevelRepository.save(bloomLevel);
        return true;
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

        BloomLevelEntity bloomLevelEntity = bloomLevelRepository.findByUserUUIDAndCourseUUID(userUUID, courseUUID);
        if (bloomLevelEntity == null) {
            return new BloomLevel();
        }

        int collectedExp = bloomLevelEntity.getCollectedExp();
        BloomLevel bloomLevel = new BloomLevel();
        bloomLevel.setTotalExp(collectedExp);
        int level = course.calculateLevelForExp(collectedExp);
        bloomLevel.setLevel(level);
        bloomLevel.setExpForCurrentLevel(course.calculateRemainingExpForCurrentLevel(collectedExp));
        bloomLevel.setRequiredExpForCurrentLevel(course.getRequiredExpOfLevel(level));

        return bloomLevel;
    }

}
