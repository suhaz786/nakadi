package org.zalando.nakadi.webservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.http.ContentType;
import org.apache.curator.framework.CuratorFramework;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;
import org.zalando.nakadi.config.JsonConfig;
import org.zalando.nakadi.domain.EventType;
import org.zalando.nakadi.service.BlacklistService;
import org.zalando.nakadi.utils.TestUtils;
import org.zalando.nakadi.webservice.utils.NakadiTestUtils;
import org.zalando.nakadi.webservice.utils.ZookeeperTestUtils;

import java.io.IOException;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItems;

public class SettingsControllerAT extends BaseAT {

    private static final String BLACKLIST_URL = "/settings/blacklist";
    private static final String ADMINS_URL="/settings/admins";
    private static final ObjectMapper MAPPER = (new JsonConfig()).jacksonObjectMapper();
    private static final CuratorFramework CURATOR = ZookeeperTestUtils.createCurator(ZOOKEEPER_URL);

    @Test
    public void testBlockFlooder() throws Exception {
        final EventType eventType = NakadiTestUtils.createEventType();
        blacklist(eventType.getName(), BlacklistService.Type.CONSUMER_ET);
        Assert.assertNotNull(CURATOR.checkExists()
                .forPath("/nakadi/blacklist/consumers/event_types/" + eventType.getName()));
    }

    @Test
    public void testUnblockFlooder() throws Exception {
        final EventType eventType = NakadiTestUtils.createEventType();
        blacklist(eventType.getName(), BlacklistService.Type.CONSUMER_ET);

        whitelist(eventType.getName(), BlacklistService.Type.CONSUMER_ET);

        Assert.assertNull(CURATOR.checkExists()
                .forPath("/nakadi/blacklist/consumers/event_types/" + eventType.getName()));
    }

    @Test
    public void testGetFlooders() throws Exception {
        final EventType eventType = NakadiTestUtils.createEventType();
        blacklist(eventType.getName(), BlacklistService.Type.CONSUMER_ET);
        TestUtils.waitFor(
                () -> given()
                        .contentType(ContentType.JSON)
                        .get(BLACKLIST_URL)
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .body("consumers.event_types", hasItems(eventType.getName())),
                1000, 200);
    }

    @Test
    public void testGetAdmins() throws Exception {
        given().contentType(ContentType.JSON).get(ADMINS_URL).then().statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void testPutAdmins() throws Exception {
        given().body("{\"admins\":[{\"data_type\": \"user\", \"value\": \"user1\"}, " +
                "{\"data_type\": \"service\", \"value\": \"service1\"}], " +
                "\"readers\":[{\"data_type\": \"user\", \"value\": \"user1\"}, " +
                "{\"data_type\": \"service\", \"value\": \"service1\"}], " +
                "\"writers\":[{\"data_type\": \"user\", \"value\": \"user1\"}, " +
                "{\"data_type\": \"service\", \"value\": \"service1\"}]}")
                .contentType(ContentType.JSON).put(ADMINS_URL).then().statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void testPutNotAdminsThen422() throws Exception {
        given().body("{\"admins\":[{\"data_type\": \"user\", \"value\": \"user1\"}, " +
                "{\"data_type\": \"service\", \"value\": \"service1\"}], " +
                "\"readers\":[{\"data_type\": \"user\", \"value\": \"user1\"}, " +
                "{\"data_type\": \"service\", \"value\": \"service1\"}], " +
                "\"writers\":[]}")
                .contentType(ContentType.JSON).put(ADMINS_URL).then().statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY);
    }

    public static void blacklist(final String name, final BlacklistService.Type type) throws IOException {
        given()
                .contentType(ContentType.JSON)
                .put(String.format("%s/%s/%s", BLACKLIST_URL, type, name))
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    public static void whitelist(final String name, final BlacklistService.Type type) throws JsonProcessingException {
        given()
                .contentType(ContentType.JSON)
                .delete(String.format("%s/%s/%s", BLACKLIST_URL, type, name))
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

}