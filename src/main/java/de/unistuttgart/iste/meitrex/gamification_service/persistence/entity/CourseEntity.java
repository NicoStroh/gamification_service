package de.unistuttgart.iste.meitrex.gamification_service.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Entity(name = "Course")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID courseUUID;

    @ElementCollection
    private Set<UUID> userUUIDs;

    @ElementCollection
    private Set<UUID> badgeUUIDs;

    public void addUser(UUID userUUID) {
        this.userUUIDs.add(userUUID);
    }

    public void addBadge(UUID badgeUUID) {
        this.badgeUUIDs.add(badgeUUID);
    }

}
