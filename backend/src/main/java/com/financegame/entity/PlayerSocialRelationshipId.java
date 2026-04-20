package com.financegame.entity;

import java.io.Serializable;
import java.util.Objects;

public class PlayerSocialRelationshipId implements Serializable {

    private Long playerId;
    private String personId;

    public PlayerSocialRelationshipId() {}

    public PlayerSocialRelationshipId(Long playerId, String personId) {
        this.playerId = playerId;
        this.personId = personId;
    }

    public Long getPlayerId() { return playerId; }
    public String getPersonId() { return personId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerSocialRelationshipId that)) return false;
        return Objects.equals(playerId, that.playerId) && Objects.equals(personId, that.personId);
    }

    @Override
    public int hashCode() { return Objects.hash(playerId, personId); }
}
