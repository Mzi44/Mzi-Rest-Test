package com.api.tests.suites;

import com.api.tests.base.BaseTest;
import com.api.tests.models.Auth;
import com.api.tests.models.RegistrationRequest;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;


//End-to-end flow:
public class NdosiUserFlow extends BaseTest {

    public static String userId;
    public static String adminToken;

    public static final String NEW_USER_EMAIL = "testuser_" + System.currentTimeMillis() + "@example.com";
    public static final String NEW_USER_PASSWORD = "UserPassword@123";
    public static final String GROUP_ID = "1deae17a-c67a-4bb0-bdeb-df0fc9e2e526";

    @Test(priority = 1, description = "Step 1: Register a new user (pending approval)")
    public void testCreateUser() {
        RegistrationRequest registration = RegistrationRequest.builder()
                .firstName("Test")
                .lastName("User")
                .email(NEW_USER_EMAIL)
                .password(NEW_USER_PASSWORD)
                .confirmPassword(NEW_USER_PASSWORD)
                .groupId(GROUP_ID)
                .build();

        Response response = given()
                .body(registration)
                .when()
                .post("/register")
                .then()
                .statusCode(201)
                .body("success", equalTo(true))
                .extract().response();

        userId = response.jsonPath().getString("data.id");

        Assert.assertNotNull(userId, "User ID should not be null after registration.");
        System.out.println("Registered User ID: " + userId);
    }

    @Test(priority = 2, description = "Step 2: Authenticate as Admin")
    public void testAdminAuthentication() {
        Auth adminLogin = Auth.builder()
                .email("spare@admin.com")
                .password("@12345678")
                .build();

        Response response = given()
                .body(adminLogin)
                .when()
                .post("/login")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .extract().response();

        adminToken = tokenFromLoginResponse(response);

        Assert.assertNotNull(adminToken, "Admin token should not be null.");
        System.out.println("Admin token extracted.");
    }

    @Test(priority = 3, dependsOnMethods = {"testCreateUser", "testAdminAuthentication"},
            description = "Step 3: Approve user, then set role to admin")
    public void testApproveAndPromoteUser() {
        given()
                .spec(getAuthenticatedSpec(adminToken))
                .when()
                .put("/admin/users/" + userId + "/approve")
                .then()
                .statusCode(200)
                .body("success", equalTo(true));

        Map<String, String> rolePayload = new HashMap<>();
        rolePayload.put("role", "admin");

        given()
                .spec(getAuthenticatedSpec(adminToken))
                .body(rolePayload)
                .when()
                .put("/admin/users/" + userId + "/role")
                .then()
                .statusCode(200)
                .body("success", equalTo(true));

        System.out.println("User " + userId + " approved and role set to admin.");
    }

    @Test(priority = 4, dependsOnMethods = "testApproveAndPromoteUser",
            description = "Step 4: Verify new user has admin role after login")
    public void testVerifyNewUserRole() {
        Auth userLogin = Auth.builder()
                .email(NEW_USER_EMAIL)
                .password(NEW_USER_PASSWORD)
                .build();

        Response response = given()
                .body(userLogin)
                .when()
                .post("/login")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .extract().response();

        String role = roleFromLoginResponse(response);
        Assert.assertNotNull(role, "Role should be present in login response.");
        Assert.assertEquals(role.toLowerCase(), "admin",
                "New user should have admin role after promotion.");

        System.out.println("Verification successful: new user logged in with admin role.");
    }

    @Test(priority = 5, dependsOnMethods = "testCreateUser",
            description = "Step 5: Cleanup — delete the created user (super admin)", alwaysRun = true)
    public void testCleanupUser() {
        if (userId != null && adminToken != null) {
            given()
                    .spec(getAuthenticatedSpec(adminToken))
                    .when()
                    .delete("/admin/users/" + userId)
                    .then()
                    .statusCode(anyOf(is(200), is(204)));

            System.out.println("Cleanup successful: user " + userId + " deleted.");
        }
    }
}
