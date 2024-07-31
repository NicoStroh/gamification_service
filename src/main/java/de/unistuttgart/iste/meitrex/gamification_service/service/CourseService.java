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
     * Creates a new course and adds the creator of the course to its users.
     *
     * @param courseUUID     the id of the created course
     * @param lecturerUUID   the id of the lecturer, who created the course
     * @param badgeService   the service for badges, to assign the course badges to its users
     * @param questService   the service for quests, to assign the courses quest chain to its users
     */
    public void addCourse(UUID courseUUID, UUID lecturerUUID, BadgeService badgeService, QuestService questService) {
        CourseEntity courseEntity = new CourseEntity(courseUUID, new HashSet<UUID>(), new HashSet<UUID>());
        courseRepository.save(courseEntity);
        questService.addCourse(courseUUID);
        addUserToCourse(lecturerUUID, courseUUID, badgeService, questService);
    }

    /**
     * Adds a user to the course and assign all the courses badges and its quest chain to the user
     *
     * @param userUUID       the id of the user, who joined the course
     * @param courseUUID     the id of the course
     * @param badgeService   the service for badges, to assign the course badges to its users
     * @param questService   the service for quests, to assign the courses quest chain to its users
     */
    public void addUserToCourse(UUID userUUID, UUID courseUUID, BadgeService badgeService, QuestService questService) {

        questService.assignQuestChainToUser(userUUID, courseUUID);

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        if (courseEntity.isPresent()) {
            CourseEntity course = courseEntity.get();

            Set<UUID> badges = courseEntity.get().getBadgeUUIDs();
            for (UUID badge : badges) {
                badgeService.assignBadgeToUser(userUUID, badge);
            }

            course.addUser(userUUID);
            courseRepository.save(course);
        }

    }

    /**
     * Adds a newly created badge to its course and assigns it to all the users of the course
     *
     * @param courseUUID     the id of the course
     * @param badgeUUID      the id of the created badge
     * @param badgeService   the service for badges, to assign the course badges to its users
     */
    public void addBadgeForCourseAndUsers(UUID courseUUID, UUID badgeUUID, BadgeService badgeService) {

        CourseEntity courseEntity = courseRepository.findById(courseUUID).orElseThrow(() -> new RuntimeException("Course not found"));
        courseEntity.addBadge(badgeUUID);

        for (UUID userUUID : courseEntity.getUserUUIDs()) {
            badgeService.assignBadgeToUser(userUUID, badgeUUID);
        }
        courseRepository.save(courseEntity);

    }

}
