package com.alexjamesmalcolm.groupme.service;

import com.alexjamesmalcolm.groupme.request.BotMessage;
import com.alexjamesmalcolm.groupme.response.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GroupMeService {

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private ObjectMapper objectMapper;

    private URI baseUrl;

    public GroupMeService() {
        this.baseUrl = URI.create("https://api.groupme.com/v3");
    }

    public Group getGroup(String accessToken, Long groupId) {
        String path = baseUrl + "/groups/" + groupId + "?token=" + accessToken;
        Envelope envelope = makeRequest(path);
        return envelope.getResponse(Group.class);
    }

    public Group getGroup(String accessToken, Message message) {
        long groupId = message.getGroupId();
        return getGroup(accessToken, groupId);
    }

    public List<Message> getMessages(String accessToken, Long groupId) {
        String path = baseUrl + "/groups/" + groupId + "/messages?token=" + accessToken;
        Map json = restTemplate.getForObject(path, Map.class);
        Map response = (Map) json.get("response");
        Message[] messages = objectMapper.convertValue(response.get("messages"), Message[].class);
        return Arrays.asList(messages);
    }

    public List<Bot> getBots(String accessToken, Long groupId) {
        List<Bot> bots = getBots(accessToken);
        return bots.stream().filter(bot -> bot.getGroupId().equals(groupId)).collect(Collectors.toList());
    }

    public List<Bot> getBots(String accessToken) {
        String path = baseUrl + "/bots?token=" + accessToken;
        Map json = restTemplate.getForObject(path, Map.class);
        Bot[] bots = objectMapper.convertValue(json.get("response"), Bot[].class);
        return Arrays.asList(bots);
    }

    public List<Group> getAllGroups(String accessToken) {
        String path = baseUrl + "/groups?token=" + accessToken;
        Map json = restTemplate.getForObject(path, Map.class);
        Group[] groups = objectMapper.convertValue(json.get("response"), Group[].class);
        return Arrays.asList(groups);
    }

    public List<Member> getAllMembers(String accessToken) {
        List<Group> groups = getAllGroups(accessToken);
        return groups.stream().map(Group::getMembers).flatMap(Collection::stream).collect(Collectors.toList());
    }

    public Member getMember(String accessToken, long userId) {
        List<Member> members = getAllMembers(accessToken);
        return members.stream().filter(member -> member.getUserId() == userId).findFirst().get();
    }

    public Member getMember(String accessToken, Message message) {
        long groupId = message.getGroupId();
        Group group = getGroup(accessToken, groupId);
        long userId = message.getUserId();
        return group.getMembers().stream().filter(member -> member.getUserId() == userId).findFirst().get();
    }

    public Me getMe(String accessToken) {
        String path = baseUrl + "/users/me?token=" + accessToken;
        Envelope envelope = makeRequest(path);
        return envelope.getResponse(Me.class);
    }

    public void sendMessage(BotMessage message) {
        HttpEntity<BotMessage> entity = new HttpEntity<>(message);
        String botMessageUrl = baseUrl + "/bots/post";
        restTemplate.postForLocation(botMessageUrl, entity);
    }

    public void sendMessage(String botId, String text) {
        BotMessage botMessage = new BotMessage(text, botId);
        sendMessage(botMessage);
    }

    private Envelope makeRequest(String path) {
        return restTemplate.getForObject(path, Envelope.class);
    }

    public Optional<Bot> getBot(String accessToken, Long groupId, URI callback) {
        List<Bot> bots = getBots(accessToken, groupId);
        return bots.stream().filter(bot -> bot.getCallbackUrl().equals(callback)).findFirst();
    }

    public Optional<Bot> getBot(String accessToken, Long groupId, String botId) {
        return getBots(accessToken, groupId).stream().filter(bot -> bot.getBotId().equals(botId)).findFirst();
    }

    public void createBot(String token, String botName, Long groupId, URI avatarUrl, URI callbackUrl, boolean dmNotification) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUri(baseUrl);
        builder.path("/bots");
        builder.queryParam("name", botName);
        builder.queryParam("group_id", groupId);
        builder.queryParam("avatar_url", avatarUrl);
        builder.queryParam("callback_url", callbackUrl);
        builder.queryParam("dm_notification", dmNotification);
        builder.queryParam("token", token);
        String url = builder.toUriString();
        restTemplate.execute(url, HttpMethod.POST, null, null);
    }
}
