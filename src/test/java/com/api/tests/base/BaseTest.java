package com.api.tests.base;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.testng.annotations.BeforeSuite;

// Configuration class for API tests, setting up base URI and common request/response specifications.
public abstract class BaseTest {

    public static RequestSpecification requestSpec;
    public static ResponseSpecification responseSpec;

    @BeforeSuite
    public void setup() {
        RestAssured.baseURI = "https://www.ndosiautomation.co.za/APIDEV";

        // Global Request Specification
        requestSpec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .log(LogDetail.ALL)
                .build();

        // Global Response Specification
        responseSpec = new ResponseSpecBuilder()
                .expectContentType(ContentType.JSON)
                .log(LogDetail.ALL)
                .build();

        // Apply global request specification
        RestAssured.requestSpecification = requestSpec;
    }


    //Token is added as a Bearer token in the Authorization header for authenticated requests.
    public RequestSpecification getAuthenticatedSpec(String token) {
        return new RequestSpecBuilder()
                .addHeader("Authorization", "Bearer " + token)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .log(LogDetail.ALL)
                .build();
    }


    //Token is taken from the login response

    public static String tokenFromLoginResponse(Response response) {
        JsonPath jp = response.jsonPath();
        String token = jp.getString("data.token");
        if (token == null || token.isEmpty()) {
            token = jp.getString("token");
        }
        return token;
    }

  //role is taken from the login response

    public static String roleFromLoginResponse(Response response) {
        JsonPath jp = response.jsonPath();
        String role = jp.getString("data.user.role");
        if (role == null || role.isEmpty()) {
            role = jp.getString("data.role");
        }
        if (role == null || role.isEmpty()) {
            role = jp.getString("role");
        }
        return role;
    }
}
