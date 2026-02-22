```markdown
# Help & Documentation - Nodify CMS

Welcome to the Nodify Help Center! This page provides comprehensive documentation tailored for different user profiles. Whether you're a **Developer** integrating Nodify into your applications or a **Digital Marketing Specialist** managing content, you'll find the resources you need here.

---

## 👨‍💻 Developer Documentation

### ⚠️ IMPORTANT SAFETY WARNING

> **The Nodify API provides full access to your content. With great power comes great responsibility!**

Since the API allows direct manipulation of all content, nodes, and configurations, **you must follow these safety rules**:

#### 🔴 CRITICAL: Always Work on SNAPSHOT, Never Directly on PUBLISHED

```java
// ❌ DANGEROUS - NEVER DO THIS
// This modifies published content directly - can cause downtime!
Mono<Node> dangerous = client.findNodeByCodeAndStatus("PROJECT-001", "PUBLISHED");

// ✅ SAFE - ALWAYS DO THIS
// Work on SNAPSHOT version, then publish when ready
Mono<Node> safe = client.findNodeByCodeAndStatus("PROJECT-001", "SNAPSHOT");
// Make your changes...
Mono<Node> saved = client.saveNode(modifiedNode);
// Test, validate, then publish
Mono<Node> published = client.publishNode("PROJECT-001");
```

#### 💾 Backup Before Major Operations

Before making significant changes to existing nodes or content:

```java
// 1. Export the node as backup
Mono<byte[]> backup = client.exportAllNodes("PROJECT-001", "production");
// Save this backup file locally

// 2. Work on SNAPSHOT
Mono<Node> workingCopy = client.findNodeByCodeAndStatus("PROJECT-001", "SNAPSHOT");

// 3. Make your changes
// ...

// 4. Test thoroughly
// ...

// 5. Only then publish
Mono<Node> published = client.publishNode("PROJECT-001");
```

#### 📋 Best Practices Checklist

- ✅ **ALWAYS** work on SNAPSHOT versions
- ✅ **ALWAYS** backup before modifying existing content
- ✅ **TEST** in staging environment first
- ✅ **COMMUNICATE** with your team (locks help!)
- ✅ **PUBLISH** only when fully validated
- ❌ **NEVER** modify PUBLISHED content directly

---

### Getting Started with Nodify Java Client

Nodify provides a powerful reactive Java client for interacting with the Headless CMS API.

#### Installation

Since the library is not yet available in a public Maven repository, you need to build and install it locally first.

**Step 1: Clone the repository**

```bash
git clone https://github.com/AZIRARM/nodify-clients.git
cd nodify-clients/java
```

**Step 2: Build and install locally**

```bash
mvn clean install
```

**Step 3: Add dependency to your project**

```xml
<dependency>
    <groupId>com.itexpert</groupId>
    <artifactId>nodify-client-reactive</artifactId>
    <version>1.0.0</version>
</dependency>
```

If you're using Gradle:

```gradle
implementation 'com.itexpert:nodify-client-reactive:1.0.0'
```

#### Quick Start

```java
import com.itexpert.content.client.ReactiveNodifyClient;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.Node;
import reactor.core.publisher.Mono;

public class NodifyExample {
    
    public static void main(String[] args) {
        // Create the client
        ReactiveNodifyClient client = ReactiveNodifyClient.create(
            ReactiveNodifyClient.builder()
                .withBaseUrl("https://your-nodify-instance.com")
                .withTimeout(30000)
                .build()
        );
        
        // Authenticate
        client.login("admin", "your-password")
            .flatMap(auth -> {
                System.out.println("✅ Authenticated! Token: " + auth.getToken());
                
                // Create a node (always SNAPSHOT by default)
                Node node = new Node();
                node.setName("My First Project");
                node.setCode("PROJECT-" + System.currentTimeMillis());
                node.setSlug("my-first-project");
                node.setDefaultLanguage("EN");
                node.setType("PROJECT");
                node.setStatus(StatusEnum.SNAPSHOT); // Always SNAPSHOT!
                
                return client.saveNode(node);
            })
            .subscribe(
                savedNode -> System.out.println("✅ Node created: " + savedNode.getCode()),
                error -> System.err.println("❌ Error: " + error.getMessage())
            );
        
        // Keep the application running
        try { Thread.sleep(5000); } catch (InterruptedException e) { }
    }
}
```

### Core Concepts

#### Understanding the Node Hierarchy

```
Root Node (Project)
├── Child Node (Section)
│   ├── Sub-child Node
│   └── Content (HTML, JSON, etc.)
├── Another Child Node
│   └── Content (File, Image, etc.)
└── Content (Direct under root)
```

#### Working with Snapshots (REVISIT THIS SECTION CAREFULLY)

**⚠️ CRITICAL: This is the most important concept to understand!**

```java
// ❌ INCORRECT - NEVER DO THIS
Mono<Node> published = client.findNodeByCodeAndStatus("PROJECT-001", "PUBLISHED");
published.map(node -> {
    node.setName("New Name");
    return client.saveNode(node); // This would modify published content!
});

// ✅ CORRECT - ALWAYS DO THIS
// 1. Get the SNAPSHOT version
Mono<Node> snapshot = client.findNodeByCodeAndStatus("PROJECT-001", StatusEnum.SNAPSHOT.name());

// 2. Make your changes
snapshot.flatMap(node -> {
    node.setName("Updated Name");
    return client.saveNode(node); // Saves as new SNAPSHOT version
})
.flatMap(saved -> {
    // 3. Test your changes
    return testChanges(saved);
})
.flatMap(validated -> {
    // 4. Only now publish
    return client.publishNode("PROJECT-001");
})
.subscribe();
```

#### The Safe Workflow

```
1. GET SNAPSHOT → 2. MODIFY → 3. SAVE (new SNAPSHOT) → 4. TEST → 5. PUBLISH
```

```java
public Mono<Node> safelyUpdateNode(String nodeCode, Consumer<Node> updater) {
    // Always work on SNAPSHOT
    return client.findNodeByCodeAndStatus(nodeCode, StatusEnum.SNAPSHOT.name())
        .doOnNext(node -> System.out.println("✅ Working on SNAPSHOT version: " + node.getVersion()))
        .map(node -> {
            updater.accept(node);
            return node;
        })
        .flatMap(client::saveNode)
        .doOnNext(saved -> System.out.println("✅ Changes saved as new SNAPSHOT: " + saved.getVersion()))
        .flatMap(saved -> {
            // Test your changes here
            return testNode(saved).thenReturn(saved);
        })
        .flatMap(validated -> client.publishNode(nodeCode))
        .doOnNext(published -> System.out.println("✅ Published to PRODUCTION"));
}
```

#### Inheritance System

Children automatically inherit translations, values, and rules from their parents.

```java
// Parent node has translations
Node parent = new Node();
Translation welcome = new Translation();
welcome.setKey("WELCOME");
welcome.setLanguage("EN");
welcome.setValue("Welcome");
parent.setTranslations(List.of(welcome));

// Child node automatically gets these translations
Node child = new Node();
child.setParentCode(parent.getCode());

// You can override specific translations
Translation override = new Translation();
override.setKey("WELCOME");
override.setLanguage("EN");
override.setValue("Custom Welcome");
child.setTranslations(List.of(override));
```

### Complete Working Example

Here's a complete, tested example that creates a parent node, a child node, and HTML content with translations and values:

```java
package com.itexpert.content.client.example;

import com.itexpert.content.client.ReactiveNodifyClient;
import com.itexpert.content.lib.enums.ContentTypeEnum;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.ContentNode;
import com.itexpert.content.lib.models.Node;
import com.itexpert.content.lib.models.Translation;
import com.itexpert.content.lib.models.Value;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class NodifyReactiveExample {

    public static void main(String[] args) {
        // Create reactive client
        ReactiveNodifyClient client = ReactiveNodifyClient.create(
                ReactiveNodifyClient.builder()
                        .withBaseUrl("https://your-nodify-instance.com")
                        .withTimeout(30000)
                        .build()
        );

        // Execute the scenario
        client.login("admin", "your-password")
                .flatMap(auth -> {
                    System.out.println("✅ Authenticated successfully");
                    return createCompleteScenario(client);
                })
                .subscribe(
                        result -> System.out.println("✅ Scenario completed successfully!"),
                        error -> System.err.println("❌ Error: " + error.getMessage())
                );

        // Wait for reactive operations to complete
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static Mono<Void> createCompleteScenario(ReactiveNodifyClient client) {
        // Step 1: Create parent node with EN as default language
        return createParentNode(client)
                .flatMap(parentNode -> {
                    System.out.println("✅ Parent node created: " + parentNode.getName() +
                            " (Code: " + parentNode.getCode() + ")");

                    // Step 2: Create a child node under the parent
                    return createChildNode(client, parentNode.getCode())
                            .flatMap(childNode -> {
                                System.out.println("✅ Child node created: " + childNode.getName() +
                                        " (Code: " + childNode.getCode() + ")");

                                // Step 3: Create HTML content with $translate and $val
                                return createHtmlContent(client, childNode.getCode())
                                        .flatMap(contentNode -> createTranslations(client, contentNode))
                                        .flatMap(contentNode -> createUserNameValue(client, contentNode))
                                        .flatMap(content -> {
                                            System.out.println("✅ HTML content created");

                                            // Step 4: Publish the content
                                            return publishContent(client, content.getCode())
                                                    .flatMap(published -> {
                                                        System.out.println("✅ Content published");

                                                        // Step 5: Publish the parent node
                                                        return publishNode(client, parentNode.getCode())
                                                                .map(publishedNode -> {
                                                                    System.out.println("✅ Parent node published");
                                                                    displayFinalInfo(parentNode, childNode, content);
                                                                    return publishedNode;
                                                                });
                                                    });
                                        });
                            });
                })
                .then();
    }

    private static Mono<Node> createParentNode(ReactiveNodifyClient client) {
        Node parentNode = new Node();
        parentNode.setName("My English Website");
        parentNode.setCode("SITE-EN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        parentNode.setSlug("my-english-website");
        parentNode.setEnvironmentCode("production");
        parentNode.setDefaultLanguage("EN");
        parentNode.setDescription("My personal website with English content");
        parentNode.setType("SITE");
        parentNode.setStatus(StatusEnum.SNAPSHOT); // Always SNAPSHOT!

        return client.saveNode(parentNode);
    }

    private static Mono<Node> createChildNode(ReactiveNodifyClient client, String parentCode) {
        Node childNode = new Node();
        childNode.setParentCode(parentCode);
        childNode.setName("Welcome Page");
        childNode.setCode("PAGE-WELCOME-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        childNode.setSlug("welcome");
        childNode.setEnvironmentCode("production");
        childNode.setDefaultLanguage("EN");
        childNode.setDescription("Welcome page with dynamic content");
        childNode.setType("PAGE");
        childNode.setStatus(StatusEnum.SNAPSHOT); // Always SNAPSHOT!

        return client.saveNode(childNode);
    }

    private static Mono<ContentNode> createTranslations(ReactiveNodifyClient client, ContentNode contentNode) {
        List<Translation> translations = Arrays.asList(
                createTranslation("HELLO_WORLD", "EN", "Hello World"),
                createTranslation("HELLO_WORLD", "FR", "Bonjour le monde"),
                createTranslation("HELLO_WORLD", "ES", "¡Hola Mundo"),
                createTranslation("HELLO_WORLD", "DE", "Hallo Welt"),
                createTranslation("EXPLORE_MORE", "EN", "Explore more"),
                createTranslation("EXPLORE_MORE", "FR", "Explorer plus"),
                createTranslation("EXPLORE_MORE", "ES", "Explorar más")
        );

        contentNode.setTranslations(translations);
        return client.saveContentNode(contentNode);
    }

    private static Translation createTranslation(String key, String language, String value) {
        Translation translation = new Translation();
        translation.setKey(key);
        translation.setLanguage(language);
        translation.setValue(value);
        return translation;
    }

    private static Mono<ContentNode> createUserNameValue(ReactiveNodifyClient client, ContentNode contentNode) {
        Value userNameValue = new Value();
        userNameValue.setKey("USER_NAME");
        userNameValue.setValue("John Doe");

        contentNode.setValues(Arrays.asList(userNameValue));
        return client.saveContentNode(contentNode);
    }

    private static Mono<ContentNode> createHtmlContent(ReactiveNodifyClient client, String nodeCode) {
        ContentNode htmlContent = new ContentNode();
        htmlContent.setParentCode(nodeCode);
        htmlContent.setCode("HTML-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        htmlContent.setSlug("welcome-message");
        htmlContent.setEnvironmentCode("production");
        htmlContent.setLanguage("EN");
        htmlContent.setType(ContentTypeEnum.HTML);
        htmlContent.setTitle("Welcome Message with Dynamic Content");
        htmlContent.setDescription("Dynamic welcome page using translations and values");
        htmlContent.setStatus(StatusEnum.SNAPSHOT); // Always SNAPSHOT!

        String html = """
                <!DOCTYPE html>
                <html>
                <body>
                    <h1>$translate(HELLO_WORLD)</h1>
                    <p>Welcome <strong>$value(USER_NAME)</strong>!</p>
                    <a href="https://github.com/AZIRARM/nodify">$translate(EXPLORE_MORE)</a>
                </body>
                </html>
                """;

        htmlContent.setContent(html);
        return client.saveContentNode(htmlContent);
    }

    private static Mono<ContentNode> publishContent(ReactiveNodifyClient client, String contentCode) {
        return client.publishContentNode(contentCode, true);
    }

    private static Mono<Node> publishNode(ReactiveNodifyClient client, String nodeCode) {
        return client.publishNode(nodeCode);
    }

    private static void displayFinalInfo(Node parentNode, Node childNode, ContentNode content) {
        System.out.println("\n✅ Parent Node: " + parentNode.getCode());
        System.out.println("✅ Child Node: " + childNode.getCode());
        System.out.println("✅ Content: " + content.getCode());
        System.out.println("✅ Translations: " + content.getTranslations().size());
        System.out.println("✅ Values: " + content.getValues().size());
    }
}
```

### API Reference

#### Authentication

```java
// Login with credentials
Mono<AuthResponse> auth = client.login("admin", "password123");

// Set token directly (if you already have one)
client.setAuthToken("your-jwt-token");

// Get current token
String token = client.getAuthToken();

// Logout
client.logout();
```

#### Node Operations

```java
// Find all root nodes
Flux<Node> nodes = client.findAllNodes();

// Find node by code (ALWAYS specify status!)
Mono<Node> node = client.findNodeByCodeAndStatus("PROJECT-001", StatusEnum.SNAPSHOT.name());

// Find nodes by parent code
Flux<Node> children = client.findNodesByParentCode("PROJECT-001");

// Find all descendants (entire tree)
Flux<Node> allDescendants = client.findAllDescendants("PROJECT-001");

// Create a node (always SNAPSHOT)
Node newNode = new Node();
newNode.setName("New Section");
newNode.setCode("SECTION-" + System.currentTimeMillis());
newNode.setSlug("new-section");
newNode.setParentCode("PROJECT-001");
newNode.setDefaultLanguage("EN");
newNode.setType("SECTION");
newNode.setStatus(StatusEnum.SNAPSHOT); // ALWAYS SNAPSHOT!

Mono<Node> saved = client.saveNode(newNode);

// Publish a node (promote SNAPSHOT to PUBLISHED)
Mono<Node> published = client.publishNode("SECTION-001");

// Delete a node (soft delete)
Mono<Boolean> deleted = client.deleteNode("SECTION-001");

// Delete permanently
Mono<Boolean> permanentlyDeleted = client.deleteNodeDefinitively("SECTION-001");
```

#### Content Operations

```java
// Find all content
Flux<ContentNode> contents = client.findAllContentNodes();

// Find content by code (ALWAYS specify status!)
Mono<ContentNode> content = client.findContentNodeByCodeAndStatus("HTML-001", StatusEnum.SNAPSHOT.name());

// Find content by node code
Flux<ContentNode> contents = client.findContentNodesByNodeCode("PAGE-001");

// Create HTML content (always SNAPSHOT)
ContentNode html = new ContentNode();
html.setParentCode("PAGE-001");
html.setCode("HTML-" + System.currentTimeMillis());
html.setSlug("welcome-page");
html.setType(ContentTypeEnum.HTML);
html.setLanguage("EN");
html.setTitle("Welcome Page");
html.setContent("<h1>Hello World</h1>");
html.setStatus(StatusEnum.SNAPSHOT); // ALWAYS SNAPSHOT!

Mono<ContentNode> saved = client.saveContentNode(html);

// Publish content
Mono<ContentNode> published = client.publishContentNode("HTML-001", true);
```

#### Translations Management

```java
// Add translations to content
ContentNode content = new ContentNode();
content.setCode("CONTENT-001");

List<Translation> translations = Arrays.asList(
    new Translation("WELCOME", "EN", "Welcome"),
    new Translation("WELCOME", "FR", "Bienvenue"),
    new Translation("WELCOME", "ES", "Bienvenido")
);

content.setTranslations(translations);
client.saveContentNode(content);
```

#### Values Management

```java
// Add values to content
ContentNode content = new ContentNode();
content.setCode("CONTENT-001");

List<Value> values = Arrays.asList(
    new Value("USER_NAME", "John Doe"),
    new Value("DISCOUNT", "20")
);

content.setValues(values);
client.saveContentNode(content);
```

#### Lock System

```java
// Acquire lock before editing (prevents conflicts)
Mono<Boolean> acquired = client.acquireLock("PROJECT-001");

if (acquired.block()) {
    try {
        // Safe to edit - you have exclusive access
        Mono<Node> snapshot = client.findNodeByCodeAndStatus("PROJECT-001", StatusEnum.SNAPSHOT.name());
        // Make your changes...
    } finally {
        // Always release when done
        client.releaseLock("PROJECT-001");
    }
}

// Check who has the lock
Mono<LockInfo> lockInfo = client.getLockOwner("PROJECT-001");
System.out.println("Locked by: " + lockInfo.block().getOwner());
```

### Building from Source

```bash
# Clone the repository
git clone https://github.com/AZIRARM/nodify-clients.git
cd nodify-clients/java

# Build and install locally
mvn clean install

# Run tests
mvn test

# Generate Javadoc
mvn javadoc:javadoc
```

The JAR will be installed in your local Maven repository (`~/.m2/repository/com/itexpert/nodify-client-reactive/1.0.0/`).

---

## 📊 Digital Marketing & E-merchandiser Documentation

[Documentation marketing complète ici...]
```
