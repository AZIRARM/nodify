# Nodify API Developer Documentation

## Overview
Welcome to the Nodify API documentation. This API provides endpoints for managing feedback, content nodes, content displays, content clicks, and nodes. The API follows RESTful principles and allows for interaction with various components of the Nodify system.

**Base URL:** `http://localhost:9080`

---

## Endpoints

### Feedback Endpoints
#### Retrieve all feedbacks
`GET /v0/feedbacks`
- **Response:** `200 OK`
- **Returns:** List of feedback objects.

#### Create new feedback
`POST /v0/feedbacks`
- **Request Body:** Feedback object.
- **Response:** `200 OK`
- **Returns:** Created feedback object.

#### Retrieve feedback by ID
`GET /v0/feedbacks/id/{id}`
- **Path Parameter:** `id` (UUID)
- **Response:** `200 OK`
- **Returns:** A feedback object.

#### Delete feedback by ID
`DELETE /v0/feedbacks/id/{id}`
- **Path Parameter:** `id` (UUID)
- **Response:** `200 OK`
- **Returns:** Boolean indicating success.

#### Retrieve feedback by user ID
`GET /v0/feedbacks/userId/{userId}`
- **Path Parameter:** `userId` (String)
- **Response:** `200 OK`
- **Returns:** List of feedbacks.

#### Retrieve feedback by content code
`GET /v0/feedbacks/contentCode/{code}`
- **Path Parameter:** `code` (String)
- **Response:** `200 OK`
- **Returns:** List of feedbacks.

---

### Content Node Endpoints
#### Retrieve data by content code
`GET /v0/contents/code/{code}/data`
- **Path Parameter:** `code` (String)
- **Query Parameter:** `status` (Optional, Default: `PUBLISHED`, Enum: `SNAPSHOT`, `PUBLISHED`, `ARCHIVE`, `DELETED`)
- **Response:** `200 OK`
- **Returns:** List of values.

#### Save data by content code
`PATCH /v0/contents/code/{code}/data`
- **Path Parameter:** `code` (String)
- **Request Body:** Value object.
- **Response:** `200 OK`
- **Returns:** Updated value object.

#### Retrieve all content nodes by node code
`GET /v0/contents/node/code/{code}`
- **Path Parameter:** `code` (String)
- **Response:** `200 OK`
- **Returns:** List of content nodes.

#### Retrieve content node by code
`GET /v0/contents/code/{code}`
- **Path Parameter:** `code` (String)
- **Response:** `200 OK`
- **Returns:** Content node object.

---

### Content Display Endpoints
#### Retrieve content display by content code
`GET /v0/content-displays/contentCode/{code}`
- **Path Parameter:** `code` (String)
- **Response:** `200 OK`
- **Returns:** Content display object.

#### Add display to content code
`PATCH /v0/content-displays/contentCode/{code}`
- **Path Parameter:** `code` (String)
- **Response:** `200 OK`
- **Returns:** Boolean indicating success.

#### Retrieve all content displays
`GET /v0/content-displays`
- **Response:** `200 OK`
- **Returns:** List of content displays.

#### Retrieve content display by ID
`GET /v0/content-displays/id/{id}`
- **Path Parameter:** `id` (UUID)
- **Response:** `200 OK`
- **Returns:** Content display object.

#### Delete content display by ID
`DELETE /v0/content-displays/id/{id}`
- **Path Parameter:** `id` (UUID)
- **Response:** `200 OK`
- **Returns:** Boolean indicating success.

---

### Content Click Endpoints
#### Retrieve content click by content code
`GET /v0/content-clicks/contentCode/{code}`
- **Path Parameter:** `code` (String)
- **Response:** `200 OK`
- **Returns:** Content click object.

#### Save content click by content code
`PATCH /v0/content-clicks/contentCode/{code}`
- **Path Parameter:** `code` (String)
- **Response:** `200 OK`
- **Returns:** Boolean indicating success.

#### Retrieve all content clicks
`GET /v0/content-clicks`
- **Response:** `200 OK`
- **Returns:** List of content clicks.

#### Retrieve content click by ID
`GET /v0/content-clicks/id/{id}`
- **Path Parameter:** `id` (UUID)
- **Response:** `200 OK`
- **Returns:** Content click object.

#### Delete content click by ID
`DELETE /v0/content-clicks/id/{id}`
- **Path Parameter:** `id` (UUID)
- **Response:** `200 OK`
- **Returns:** Boolean indicating success.

---

### Node Endpoints
#### Retrieve all nodes
`GET /v0/nodes`
- **Query Parameter:** `status` (Optional, Default: `PUBLISHED`, Enum: `SNAPSHOT`, `PUBLISHED`, `ARCHIVE`, `DELETED`)
- **Response:** `200 OK`
- **Returns:** List of nodes.

#### Retrieve node by code
`GET /v0/nodes/code/{code}`
- **Path Parameter:** `code` (String)
- **Query Parameter:** `status` (Optional, Default: `PUBLISHED`)
- **Response:** `200 OK`
- **Returns:** Node object.

#### Retrieve parent nodes
`GET /v0/nodes/parents`
- **Query Parameter:** `status` (Optional, Default: `PUBLISHED`)
- **Response:** `200 OK`
- **Returns:** List of parent nodes.

#### Retrieve child nodes by parent code
`GET /v0/nodes/childreens/parent/{code}`
- **Path Parameter:** `code` (String)
- **Query Parameter:** `status` (Optional, Default: `PUBLISHED`)
- **Response:** `200 OK`
- **Returns:** List of child nodes.

---

### Health Check
#### Check API health status
`GET /health`
- **Response:** `200 OK`
- **Returns:** String indicating health status.

---

## Data Models
### Feedback
```json
{
  "id": "uuid",
  "contentCode": "string",
  "evaluation": 0,
  "message": "string",
  "userId": "string",
  "verified": true
}
```

### Value
```json
{
  "id": "uuid",
  "key": "string",
  "value": "string"
}
```

### Node
```json
{
  "id": "uuid",
  "code": "string",
  "status": "PUBLISHED"
}
```

---

## Conclusion
This API provides a robust set of endpoints for managing feedback, content nodes, displays, clicks, and nodes within the Nodify system. For further assistance, contact the development team.

