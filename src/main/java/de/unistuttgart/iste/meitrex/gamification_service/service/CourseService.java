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

    private final BadgeMapper badgeMapper;

    public void addCourse(UUID courseUUID, UUID lecturerUUID, BadgeService badgeService, QuestService questService) {
        CourseEntity courseEntity = new CourseEntity(courseUUID, new HashSet<UUID>(), new HashSet<UUID>());
        courseRepository.save(courseEntity);
        questService.addCourse(courseUUID);
        addUserToCourse(lecturerUUID, courseUUID, badgeService, questService);
    }

    public void addUserToCourse(UUID userUUID, UUID courseUUID, BadgeService badgeService, QuestService questService) {

        List<BadgeEntity> badges = badgeService.getBadgesByCourseUUID(courseUUID);
        for (BadgeEntity badge : badges) {
            badgeService.assignBadgeToUser(userUUID, badge);
        }

        questService.assignQuestChainToUser(userUUID, courseUUID);

        Optional<CourseEntity> courseEntity = courseRepository.findById(courseUUID);
        if (courseEntity.isPresent()) {
            CourseEntity course = courseEntity.get();
            course.addUser(userUUID);
            courseRepository.save(course);
        }

    }

    public void addBadgeForCourseAndUsers(UUID courseUUID, BadgeEntity badgeEntity, BadgeService badgeService) {

        CourseEntity courseEntity = courseRepository.findById(courseUUID).orElseThrow(() -> new RuntimeException("Course not found"));
        courseEntity.addBadge(badgeEntity.getBadgeUUID());

        for (UUID userUUID : courseEntity.getUserUUIDs()) {
            badgeService.assignBadgeToUser(userUUID, badgeEntity);
        }
        courseRepository.save(courseEntity);

    }

}
