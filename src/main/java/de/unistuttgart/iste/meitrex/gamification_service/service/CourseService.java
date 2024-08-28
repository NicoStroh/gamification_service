package de.unistuttgart.iste.meitrex.gamification_service.service;

import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.BadgeEntity;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.entity.CourseEntity;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.mapper.BadgeMapper;
import de.unistuttgart.iste.meitrex.gamification_service.persistence.repository.CourseRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CourseService {

    private final CourseRepository courseRepository;

    /**
     * Creates a new course and saves it in the repository, adds the creator of the course to it.
     *
     * @param courseUUID       the id of the created course
     * @param lecturerUUID     the id of the creator of the course
     *
     * @return is the course new
     */
    public boolean addCourse(UUID courseUUID, UUID lecturerUUID) {

        if (courseRepository.existsById(courseUUID)) {
            return false;
        }

        CourseEntity courseEntity = new CourseEntity(courseUUID,
                new HashSet<UUID>(),
                new ArrayList<Integer>(),
                new LinkedList<UUID>(),
                new HashSet<UUID>());
        courseRepository.save(courseEntity);

        addUserToCourse(lecturerUUID, courseUUID);

        return true;
    }

    /**
     * Removes the course from the courseRepository
     *
     * @param courseUUID     the id of the course which shall be deleted
     */
    public void deleteCourse(UUID courseUUID) {
        courseRepository.deleteById(courseUUID);
    }

    /**
     * Adds a user to the course
     *
     * @param userUUID       the id of the user, who joined the course
     * @param courseUUID     the id of the course
     *
     * @return whether the user is added successfully to the course
     */
    public boolean addUserToCourse(UUID userUUID, UUID courseUUID) {

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        if (courseEntity.isPresent()) {
            CourseEntity course = courseEntity.get();

            if (course.getUserUUIDs().contains(userUUID)) {
                return false;
            }

            course.addUser(userUUID);
            courseRepository.save(course);
            return true;
        }
        return false;

    }

    /**
     * Retrieves all the UUIDs of the courses members
     *
     * @param courseUUID     the id of the course
     *
     * @return the UUIDs of the courses members
     */
    public Set<UUID> getCoursesUsers(UUID courseUUID) {

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        if (courseEntity.isPresent()) {
            return courseEntity.get().getUserUUIDs();
        }
        return new HashSet<UUID>();

    }

    /**
     * Removes a user from the course
     *
     * @param userUUID       the id of the user, who left the course
     * @param courseUUID     the id of the course
     *
     * @return whether the user was removed successfully from the course.
     */
    public boolean removeUserFromCourse(UUID userUUID, UUID courseUUID) {

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        if (courseEntity.isEmpty()) {
            return false;
        }

        CourseEntity course = courseEntity.get();
        if (!course.getUserUUIDs().contains(userUUID)) {
            return false;
        }
        course.removeUser(userUUID);
        courseRepository.save(course);
        return true;

    }

}
