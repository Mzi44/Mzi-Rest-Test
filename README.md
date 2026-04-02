# REST Assured API Tests

Automated API tests for the **Ndosi Automation** development API, written in Java with [REST Assured](https://rest-assured.io/), [TestNG](https://testng.org/), and [Maven](https://maven.apache.org/). The suite exercises a full user lifecycle: registration, admin approval, role promotion, verification via login, and cleanup.

**Base URL:** [https://www.ndosiautomation.co.za/APIDEV](https://www.ndosiautomation.co.za/APIDEV)

Opening that URL in a browser returns a JSON index describing available routes (authentication, admin users, courses, and more).

---

## Table of contents

- [Requirements](#requirements)
- [Tech stack](#tech-stack)
- [Project layout](#project-layout)
- [What the tests do](#what-the-tests-do)
- [Configuration](#configuration)
- [How to run](#how-to-run)
- [Reports and logging](#reports-and-logging)
- [Troubleshooting](#troubleshooting)
- [Extending the suite](#extending-the-suite)

---

## Requirements

| Tool | Version |
|------|---------|
| **JDK** | 17 (matches `maven.compiler.source` / `target` in `pom.xml`) |
| **Maven** | 3.6+ recommended |

Network access to `https://www.ndosiautomation.co.za` is required when you run tests against the live API.

---

## Tech stack

| Library | Role |
|---------|------|
| **REST Assured 5.5.x** | HTTP requests, JSON assertions, Hamcrest matchers |
| **TestNG 7.9.x** | Test execution, ordering (`priority`), dependencies (`dependsOnMethods`) |
| **Jackson** | JSON serialization for request bodies (`RegistrationRequest`, `Auth`, `User`) |
| **Lombok** | Builders and getters/setters on model classes |
| **Maven Surefire** | Runs the TestNG suite defined in `src/test/resources/testng.xml` |

---

## Project layout

```
restassured-api-tests/
├── pom.xml
├── README.md
└── src/test/
    ├── java/com/api/tests/
    │   ├── base/
    │   │   └── BaseTest.java          # base URI, default specs, auth helpers
    │   ├── models/
    │   │   ├── Auth.java              # login payload / optional response fields
    │   │   ├── RegistrationRequest.java
    │   │   └── User.java              # generic user shape (optional reuse)
    │   └── suites/
    │       └── UserLifecycleTest.java # end-to-end scenario
    └── resources/
        └── testng.xml                 # TestNG suite: classes to run
```

---

## What the tests do

`UserLifecycleTest` runs **five** ordered steps against the live API.

| Step | Test method | HTTP | Path | Purpose |
|------|-------------|------|------|---------|
| 1 | `testCreateUser` | `POST` | `/register` | Create a pending user; read `data.id` from the JSON envelope |
| 2 | `testAdminAuthentication` | `POST` | `/login` | Obtain admin **Bearer** token from `data.token` (with fallback to `token`) |
| 3 | `testApproveAndPromoteUser` | `PUT` | `/admin/users/{id}/approve` then `/admin/users/{id}/role` | Approve the user, then set `role` to `admin` |
| 4 | `testVerifyNewUserRole` | `POST` | `/login` | Log in as the new user and assert the resolved role is `admin` |
| 5 | `testCleanupUser` | `DELETE` | `/admin/users/{id}` | Remove the user (runs with `alwaysRun = true` when prior steps created an id) |

The API responds with a consistent pattern such as `success`, `message`, and often a `data` object. Helpers in `BaseTest` normalize **token** and **role** extraction when the server nests fields under `data` or `data.user`.

**Important:** The API documents some admin routes as **super admin** only (for example deleting users or changing roles). If your account is **admin** but not **super admin**, steps 3 or 5 may return `403` or similar. Use credentials that match the permissions your scenario needs.

---

## Configuration

### Base URI

Set in `BaseTest.setup()`:

```text
https://www.ndosiautomation.co.za/APIDEV
```

To point at another environment, change `RestAssured.baseURI` there (or refactor to read from a system property / environment variable).

### Credentials and test data

- **Admin login** is currently coded in `UserLifecycleTest.testAdminAuthentication()` (email and password). Replace with valid admin (or super-admin) credentials for your environment.
- **New user** email is generated with a timestamp to reduce collisions: `testuser_<timestamp>@example.com`.
- **Group ID** is a constant in `UserLifecycleTest` (`groupId`). It must be a **valid, active group** returned by the API (for example via `GET /groups` for public registration metadata).

For real projects, prefer **environment variables** or **Maven profiles** instead of committing secrets.

### Global REST Assured behavior

`BaseTest` applies a default request specification (JSON content type and accept header, verbose request/response logging). Authenticated calls use `Authorization: Bearer <token>` via `getAuthenticatedSpec(token)`.

---

## How to run

From the project root:

```bash
mvn clean test
```

Compile tests only (no HTTP calls):

```bash
mvn test-compile
```

Run a **single** test class without the suite file (optional):

```bash
mvn test -Dtest=UserLifecycleTest
```

The default `maven-surefire-plugin` configuration uses `src/test/resources/testng.xml`, which currently includes `com.api.tests.suites.UserLifecycleTest`.

---

## Reports and logging

- **Console:** REST Assured is configured with `LogDetail.ALL` on the global request spec, so request and response details appear in the Maven test output.
- **Surefire:** Failing tests produce reports under `target/surefire-reports/` (standard Maven layout).

---

## Troubleshooting

| Symptom | Things to check |
|--------|------------------|
| `401` / `INVALID_CREDENTIALS` on login | Admin email/password in `UserLifecycleTest` |
| `403` on `/admin/users/.../role` or `DELETE` | Account may need **super admin**; confirm with API owners |
| `400` on `/register` | `firstName` / `lastName` validation, password rules, `confirmPassword` must match `password`, `groupId` must exist |
| `201` not seen on register | Asserted status is `201` for successful registration; if the API changes, update the expectation |
| Connection errors | Firewall, VPN, or API downtime |

The root index at [https://www.ndosiautomation.co.za/APIDEV](https://www.ndosiautomation.co.za/APIDEV) lists route categories (authentication, `admin_users`, etc.) and is the authoritative map of paths.

---

## Extending the suite

1. Add model classes under `com.api.tests.models` if you need new JSON shapes.
2. Create a class under `com.api.tests.suites` extending `BaseTest`.
3. Register the class in `src/test/resources/testng.xml`, or add a new `<test>` block for grouping.
4. Reuse `getAuthenticatedSpec`, `tokenFromLoginResponse`, and `roleFromLoginResponse` where responses match the same envelope patterns.

For schema-level checks, this project already includes `json-schema-validator` on the classpath; you can add `.body(matchesJsonSchemaInClasspath("..."))` style assertions when you add schema files under `src/test/resources`.

---

## License / usage

This repository is a test harness. Ensure you have permission to run automated tests against the target API and that load and data creation comply with your organization’s policies.
