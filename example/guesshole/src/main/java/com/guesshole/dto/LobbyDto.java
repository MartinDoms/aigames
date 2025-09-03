package com.guesshole.dto;

import com.guesshole.entities.Lobby;

public class LobbyDto {
    private String name;
    private String privacy;

    public LobbyDto() {
        // Default constructor for Spring
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrivacy() {
        return privacy;
    }

    public void setPrivacy(String privacy) {
        this.privacy = privacy;
    }

    public Lobby toLobby() {
        return new Lobby(name, privacy);
    }

    @Override
    public String toString() {
        return "LobbyDto{" +
                "name='" + name + '\'' +
                ", privacy='" + privacy + '\'' +
                '}';
    }
}