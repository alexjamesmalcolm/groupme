package com.alexjamesmalcolm.groupme.service;

import com.alexjamesmalcolm.groupme.request.BotMessage;
import com.alexjamesmalcolm.groupme.response.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static java.lang.Thread.sleep;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GroupMeServiceTest {

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private GroupMeService underTest;

    private long groupId;
    private String token;
    private String baseUrl;

    @Before
    public void setup() throws InterruptedException {
        groupId = 46707218;
        token = "1da573f0282d013738075ad8d3cabcd7";
        baseUrl = "https://api.groupme.com/v3";
        sleep(2000);
    }

    @Test
    public void shouldNotBeNull() {
        assertNotNull(restTemplate);
        assertNotNull(underTest);
    }

    @Test
    public void shouldReturnGroupWithCorrectId() {
        Group group = underTest.getGroup(token, groupId);
        assertThat(group.getId(), is(groupId));
    }

    @Test
    public void shouldHaveOneMember() {
        Group group = underTest.getGroup(token, groupId);
        List<Member> members = group.getMembers();
        assertThat(members.size(), is(1));
    }

    @Test
    public void shouldGetTwentyMessagesByDefault() {
        List<Message> messages = underTest.getMessages(token, groupId);
        assertThat(messages.size(), is(20));
    }

    @Test
    public void shouldGetBotForGroup() {
        List<Bot> bots = underTest.getBots(token, groupId);
        Assert.assertThat(bots, is(not(empty())));
    }

    @Test
    public void shouldGetTenGroupsByDefault() {
        List<Group> groups = underTest.getAllGroups(token);
        assertThat(groups.size(), is(10));
    }

    @Test
    public void checkThatGroupMeIsAvailable() {
        Map json = restTemplate.getForObject(baseUrl + "/users/me?token=" + token, Map.class);
        ObjectMapper objectMapper = new ObjectMapper();
        Me me = objectMapper.convertValue(json.get("response"), Me.class);
        assertNotNull(me);
    }

    @Test
    public void shouldGetGroupFromWrappedEnvelope() {
        String path = baseUrl + "/groups/" + groupId + "?token=" + token;
        Map rawEnvelope = restTemplate.getForObject(path, Map.class);
        ObjectMapper objectMapper = new ObjectMapper();
        Envelope envelope = objectMapper.convertValue(rawEnvelope, Envelope.class);
        Group group = envelope.getResponse(Group.class);
        assertNotNull(group);
    }

    @Test
    public void shouldGetMeWithMyUserId() {
        Long expected = 19742906L;
        Me me = underTest.getMe(token);
        Long actual = me.getUserId();
        assertThat(actual, is(expected));
    }

    @Test
    public void shouldGetMyselfFromMyTestGroupAsAMember() {
        String expected = "Alex Malcolm";
        Group group = underTest.getGroup(token, groupId);
        List<Member> members = group.getMembers();
        Member member = members.get(0);
        assertThat(member.getName(), is(expected));
    }

    @Ignore("Skipping to avoid creating messages in test group")
    @Test
    public void shouldTrySendingABotMessage() {
        List<Bot> bots = underTest.getBots(token, groupId);
        Bot bot = bots.get(0);
        String id = bot.getBotId();
        BotMessage message = new BotMessage("Test", id);
        underTest.sendMessage(message);
    }

    @Ignore("Test doesn't assert anything")
    @Test
    public void shouldGetBots() {
        List<Bot> bots = underTest.getBots(token, groupId);
        bots.get(0);
    }

    @Test
    public void shouldGetMeJsonBack() {
        Object json = restTemplate.getForObject(baseUrl + "/users/me?token=" + token, Object.class);
        System.out.println(json);
    }

    @Test
    public void shouldGetMeAsObjectBack() {
        Me me = underTest.getMe(token);
        System.out.println(me);
    }

    @Test
    public void shouldCreateBotWithCorrectName() {
        String botName = "Bot created from automated testing";
        URI avatarUrl = null;
        URI callbackUrl = null;
        boolean dmNotification = false;
        String botId = underTest.createBot(token, botName, groupId, avatarUrl, callbackUrl, dmNotification);
        Bot bot = underTest.getBot(token, groupId, botId).get();
        assertThat(bot.getName(), is(botName));
        underTest.deleteBot(token, botId);
    }
}