# Nodify API Documentation

This document provides a comprehensive overview of the Nodify Headless CMS API, designed to manage content, data, feedback, and nodes.

## Table of Contents

1.  Introduction
2.  Base URL
3.  API Endpoints
    * Content Display Endpoint
    * Content Click Endpoint
    * Data Controller
    * Content Node Endpoint
    * Feedback Endpoint
    * Health Endpoint
    * Node Endpoint
4.  Data Models
5.  Usage Examples
6.  Error Handling
7.  Authentication (if applicable)

## 1. Introduction

The Nodify API allows developers to interact with the Nodify Headless CMS programmatically. It provides endpoints to manage content displays, clicks, data objects, content nodes, feedback, and nodes. This document outlines the available endpoints, request/response formats, and usage examples.

## 2. Base URL

The base URL for the Nodify API is: `http://localhost:9080`

## 3. API Endpoints

### Content Display Endpoint

* **`GET /v0/content-displays/contentCode/{code}`**
    * Retrieve a content display entry by content code.
    * Parameters:
        * `code` (path, string, required): The content code.
    * Response: `ContentDisplay` object.
* **`PATCH /v0/content-displays/contentCode/{code}`**
    * Increment the display count for a content code.
    * Parameters:
        * `code` (path, string, required): The content code.
    * Response: boolean

### Content Click Endpoint

* **`GET /v0/content-clicks/contentCode/{code}`**
    * Retrieve a content click by content code.
    * Parameters:
        * `code` (path, string, required): The content code.
    * Response: `ContentClick` object.
* **`PATCH /v0/content-clicks/contentCode/{code}`**
    * Record a content click.
    * Parameters:
        * `code` (path, string, required): The content code.
    * Response: boolean

### Data Controller

* **`POST /v0/datas/`**
    * Save a new Data object.
    * Request Body: `Data` object (required).
    * Response: `Data` object.
* **`GET /v0/datas/key/{key}`**
    * Find Data by key.
    * Parameters:
        * `key` (path, string, required): The key of the Data object.
    * Response: `Data` object.
* **`GET /v0/datas/contentCode/{code}`**
    * Find Data by content code.
    * Parameters:
        * `code` (path, string, required): The content code.
        * `currentPage` (query, integer, optional, default: 0): Current page index.
        * `limit` (query, integer, optional, default: 50): Limit per page.
    * Response: array of `Data` objects.
* **`DELETE /v0/datas/contentCode/{code}`**
    * Delete Data by content code.
    * Parameters:
        * `code` (path, string, required): The content code.
    * Response: boolean.

### Content Node Endpoint

* **`GET /v0/contents/node/code/{code}`**
    * Retrieve all content nodes by node code.
    * Parameters:
        * `code` (path, string, required): The node code.
        * `status` (query, string, optional, default: "PUBLISHED", enum: ["SNAPSHOT", "PUBLISHED", "ARCHIVE", "DELETED", "NEW"]).
        * `translation` (query, string, optional).
        * `fillValues` (query, boolean, optional).
        * `payloadOnly` (query, boolean, optional).
    * Response: array of `ContentNode` objects.

### Feedback Endpoint

* **`POST /v0/feedbacks/`**
    * Save a new feedback entry.
    * Request Body: `Feedback` object (required).
    * Response: `Feedback` object.
* **`GET /v0/feedbacks/verified/{verified}`**
    * Retrieve feedback by verification status.
    * Parameters:
        * `verified` (path, boolean, required).
        * `currentPage` (query, integer, optional, default: 0): Current page index.
        * `limit` (query, integer, optional, default: 50): Limit per page.
    * Response: array of `Feedback` objects.
* **`GET /v0/feedbacks/userId/{userId}`**
    * Retrieve feedback by user ID.
    * Parameters:
        * `userId` (path, string, required).
        * `currentPage` (query, integer, optional, default: 0): Current page index.
        * `limit` (query, integer, optional, default: 50): Limit per page.
    * Response: array of `Feedback` objects.
* **`GET /v0/feedbacks/evaluation/{evaluation}`**
    * Retrieve feedback by evaluation score.
    * Parameters:
        * `evaluation` (path, integer, required).
        * `currentPage` (query, integer, optional, default: 0): Current page index.
        * `limit` (query, integer, optional, default: 50): Limit per page.
    * Response: array of `Feedback` objects.
* **`GET /v0/feedbacks/contentCode/{code}`**
    * Retrieve feedback by content code.
    * Parameters:
        * `code` (path, string, required).
        * `currentPage` (query, integer, optional, default: 0): Current page index.
        * `limit` (query, integer, optional, default: 50): Limit per page.
    * Response: array of `Feedback` objects.
* **`DELETE /v0/feedbacks/contentCode/{code}`**
    * Delete feedback by content code.
    * Parameters:
        * `code` (path, string, required).
    * Response: boolean.
* **`GET /v0/feedbacks/charts`**
    * Retrieve feedback statistics.
    * Response: array of `FeedbackCharts` objects.

### Node Endpoint

* **`GET /v0/nodes/`**
    * Retrieve all nodes.
    * Parameters:
        * `status` (query, string, optional, default: "PUBLISHED", enum: ["SNAPSHOT", "PUBLISHED", "ARCHIVE", "DELETED", "NEW"]).
    * Response: array of `Node` objects.
* **`GET /v0/nodes/parents`**
    * Retrieve all parent nodes.
    * Parameters:
        * `status` (query, string, optional, default: "PUBLISHED", enum: ["SNAPSHOT", "PUBLISHED", "ARCHIVE", "DELETED", "NEW"]).
    * Response: array of `Node` objects.
* **`GET /v0/nodes/parent/{code}`**
    * Retrieve child nodes by parent code.
    * Parameters:
        * `code` (path, string, required).
        * `status` (query, string, optional, default: "PUBLISHED", enum: ["SNAPSHOT", "PUBLISHED", "ARCHIVE", "DELETED", "NEW"]).
    * Response: array of `Node` objects.
* **`GET /v0/nodes/code/{code}`**
    * Retrieve a node by code.
    * Parameters:
        * `code` (path, string, required).
        * `status` (query, string, optional, default: "PUBLISHED", enum: ["SNAPSHOT", "PUBLISHED", "ARCHIVE", "DELETED", "NEW"]).
    * Response: `Node` object.

### Health Endpoint

* This endpoint is not detailed within the swagger file provided. It is assumed that this endpoint exists to check the health of the application.

## 4. Data Models

Refer to the Swagger/OpenAPI documentation for detailed schema information for `Feedback`, `Data`, `ContentDisplay`, `ContentClick`, `ContentNode`, `Node`, and `FeedbackCharts`.

## 5. Usage Examples

(Add code snippets for various API calls, using tools like `curl` or language-specific HTTP clients)

## 6. Error Handling

(Describe common error codes and how to handle them)

## 7. Authentication

(If applicable, provide details on authentication methods and how to use them)

### API Key Authentication Example

If the API requires API key authentication, you'll need to include the key in the header of each request.

```
Authorization: Bearer VOTRE_CLE_API
```
* **Where to get the API Key:** Describe how developers can obtain their API keys.
* **Security:** Emphasize the importance of keeping API keys secure and not including them in client-side code.

### OAuth 2.0 Authentication Example

If the API uses OAuth 2.0, describe the steps to obtain an access token.

1.  **Obtain an Authorization Token:** Explain how developers can obtain an authorization token.
2.  **Use the Access Token:** Show how to include the access token in the header of each request.
