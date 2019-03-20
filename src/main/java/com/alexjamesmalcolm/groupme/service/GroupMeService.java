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

    public Group getGroup(String token, Long groupId) {
        String path = baseUrl + "/groups/" + groupId + "?token=" + token;
        Envelope envelope = makeRequest(path);
        return envelope.getResponse(Group.class);
    }

    public Group getGroup(String token, Message message) {
        long groupId = message.getGroupId();
        return getGroup(token, groupId);
    }

    public List<Message> getMessages(String token, Long groupId) {
        String path = baseUrl + "/groups/" + groupId + "/messages?token=" + token;
        Map json = restTemplate.getForObject(path, Map.class);
        Map response = (Map) json.get("response");
        Message[] messages = objectMapper.convertValue(response.get("messages"), Message[].class);
        return Arrays.asList(messages);
    }

    public List<Bot> getBots(String token, Long groupId) {
        List<Bot> bots = getBots(token);
        return bots.stream().filter(bot -> bot.getGroupId().equals(groupId)).collect(Collectors.toList());
    }

    public List<Bot> getBots(String token) {
        String path = baseUrl + "/bots?token=" + token;
        Map json = restTemplate.getForObject(path, Map.class);
        Bot[] bots = objectMapper.convertValue(json.get("response"), Bot[].class);
        return Arrays.asList(bots);
    }

    public List<Group> getAllGroups(String token) {
        String path = baseUrl + "/groups?token=" + token;
        Map json = restTemplate.getForObject(path, Map.class);
        Group[] groups = objectMapper.convertValue(json.get("response"), Group[].class);
        return Arrays.asList(groups);
    }

    public List<Member> getAllMembers(String token) {
        List<Group> groups = getAllGroups(token);
        return groups.stream().map(Group::getMembers).flatMap(Collection::stream).collect(Collectors.toList());
    }

    public Member getMember(String token, long userId) {
        List<Member> members = getAllMembers(token);
        return members.stream().filter(member -> member.getUserId() == userId).findFirst().get();
    }

    public Member getMember(String token, Message message) {
        long groupId = message.getGroupId();
        Group group = getGroup(token, groupId);
        long userId = message.getUserId();
        return group.getMembers().stream().filter(member -> member.getUserId() == userId).findFirst().get();
    }

    public Me getMe(String token) {
        String path = baseUrl + "/users/me?token=" + token;
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

    public Optional<Bot> getBot(String token, Long groupId, URI callback) {
        List<Bot> bots = getBots(token, groupId);
        return bots.stream().filter(bot -> bot.getCallbackUrl().equals(callback)).findFirst();
    }

    public Optional<Bot> getBot(String token, Long groupId, String botId) {
        return getBots(token, groupId).stream().filter(bot -> bot.getBotId().equals(botId)).findFirst();
    }

    public String createBot(String token, String botName, Long groupId, URI avatarUrl, URI callbackUrl, boolean dmNotification) {
        Group group = getGroup(token, groupId);
        UriComponentsBuilder builder = UriComponentsBuilder.fromUri(baseUrl);
        builder.path("/bots");
        builder.queryParam("token", token);
        String url = builder.toUriString();
        Map<String, Object> params = new HashMap<>();
        params.put("name", botName);
        params.put("group_id", group.getGroupId());
        if (avatarUrl != null) {
            params.put("avatar_url", avatarUrl);
        }
        if (callbackUrl != null) {
            params.put("callback_url", callbackUrl);
        }
        params.put("dm_notification", dmNotification);
        Map<String, Map> bot = new HashMap<>();
        bot.put("bot", params);
        HttpEntity<Map> request = new HttpEntity<>(bot);
        Map response = restTemplate.postForObject(url, request, Map.class);
        return (String) ((Map) ((Map) response.get("response")).get("bot")).get("bot_id");
    }

    public void deleteBot(String token, String botId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUri(baseUrl);
        builder.path("/bots/destroy");
        builder.queryParam("bot_id", botId);
        builder.queryParam("token", token);
        String url = builder.toUriString();
        restTemplate.execute(url, HttpMethod.POST, null, null);
    }
}
