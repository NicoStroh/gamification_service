package de.unistuttgart.iste.meitrex.gamification_service.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.LinkedList;
import java.util.UUID;

@Entity(name = "QuestChain")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestChainEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID questChainUUID;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private LinkedList<QuestEntity> quests;

    private UUID courseUUID;

    public void addQuest(QuestEntity quest) {
        if (this.quests == null) {
            this.quests = new LinkedList<QuestEntity>();
        }
        this.quests.add(quest);
    }

    public int length() {
        if (this.quests == null) {
            return 0;
        }
        return this.quests.size();
    }

}
