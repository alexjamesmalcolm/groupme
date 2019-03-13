package com.alexjamesmalcolm.groupme.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.List;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Long.parseLong;

public class Member {

    private Long userId;
    private String nickname;
    private URI imageUrl;
    private Long id;
    private Boolean muted;
    private Boolean autokicked;
    private List<String> roles;
    private String name;

    @JsonCreator
    private Member(@JsonProperty("user_id") String userId,
                   @JsonProperty("nickname") String nickname,
                   @JsonProperty("image_url") String imageUrl,
                   @JsonProperty("id") String id,
                   @JsonProperty("muted") String muted,
                   @JsonProperty("autokicked") String autokicked,
                   @JsonProperty("roles") List<String> roles,
                   @JsonProperty("name") String name) {
        this.userId = parseLong(userId);
        this.nickname = nickname;
        this.imageUrl = parseToUri(imageUrl);
        this.id = parseLong(id);
        this.muted = parseBoolean(muted);
        this.autokicked = parseBoolean(autokicked);
        this.roles = roles;
        this.name = name;
    }

    private URI parseToUri(String url) {
        return url != null && !url.isEmpty() ? URI.create(url) : null;
    }


    public String getName() {
        return name;
    }

    public Long getUserId() {
        return userId;
    }

    public String getNickname() {
        return nickname;
    }

    public URI getImageUrl() {
        return imageUrl;
    }

    public Long getId() {
        return id;
    }

    public Boolean getMuted() {
        return muted;
    }

    public Boolean getAutokicked() {
        return autokicked;
    }

    public List<String> getRoles() {
        return roles;
    }
}
