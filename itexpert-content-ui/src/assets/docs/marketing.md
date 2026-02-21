```markdown
## 📊 Digital Marketing & E-merchandiser Documentation

### Getting Started with Nodify CMS

Welcome to Nodify! This guide will help you understand the core concepts and start managing your content effectively.

#### First Login

1. Connect to your Nodify Studio using the URL provided by your administrator
2. Enter your credentials
3. You'll see the dashboard with your projects

#### Understanding the Dashboard

```
Your Dashboard
├── Black Friday 2024 (Campaign)
├── Main Website (Live Site)
├── Mobile App Content
└── Summer Sale 2024 (Promotion)
```

Each item is a **Project Node** - think of them as separate websites, campaigns, or digital properties.

### Core Concepts

#### The Node Hierarchy

Nodify organizes content in a tree structure:

```
Project: Summer Sale Campaign
├── Category: Swimwear
│   ├── Product: Swim Trunks
│   └── Product: Bikini
├── Category: Accessories
│   └── Product: Sunglasses
└── Content: Landing Page
```

**Key Terms:**

| Term | Description | Example |
|------|-------------|---------|
| **Node** | A container that can have children | A category folder |
| **Root Node** | Top-level node (your project) | "Summer Sale Campaign" |
| **Content** | The actual deliverable | A product page, image, file |
| **Child Node** | A node under another node | "Swimwear" under "Summer Sale" |

#### Inheritance - The Magic of Nodify

**Everything flows DOWN the tree.** What you set at a higher level is automatically available to all children.

```
Project Level: "WELCOME" = "Welcome to our store!"
├── Category Level: No override needed → "Welcome to our store!"
├── Product Level: No override needed → "Welcome to our store!"
└── Override at Product: "Welcome to this specific product!"
```

This means:
- Set global translations once at the project level
- Override only when needed at lower levels
- Save time and ensure consistency

**For Rules:** Parent node rules also flow down to all children. If a parent node has active rules, they apply to all children. Children can add their own rules, which combine with parent rules.

### Working with Content

#### Content Types

Nodify supports a wide variety of content types for different use cases:

| Type | Description | Best For |
|------|-------------|----------|
| **FILE** | Binary files, documents | PDFs, Word docs, spreadsheets |
| **PICTURE** | Image files | Product photos, banners, logos |
| **SCRIPT** | JavaScript code | Tracking pixels, custom functionality |
| **STYLE** | CSS code | Styling, themes, custom designs |
| **HTML** | Web pages, formatted text | Landing pages, articles, blog posts |
| **JSON** | Structured data | Product catalogs, API responses |
| **URLS** | List of URLs with metadata | API endpoints, image repositories, links |
| **DATA** | Key-value pairs | Translations, configuration, settings |
| **XML** | XML data | RSS feeds, sitemaps, data exchange |

#### URLS Content Type

The **URLS** content type is special - it allows you to manage collections of URLs with rich metadata:

```json
[
  {
    "url": "https://api.example.com/products",
    "description": "Product API endpoint",
    "type": "API"
  },
  {
    "url": "https://images.example.com/catalog/",
    "description": "Product image repository",
    "type": "IMAGE_REPO"
  },
  {
    "url": "https://docs.example.com/guide",
    "description": "User documentation",
    "type": "DOCS"
  },
  {
    "url": "https://github.com/example/repo",
    "description": "Source code repository",
    "type": "REPO"
  }
]
```

**URL Types:**

| Type | Purpose |
|------|---------|
| **API** | API endpoints for integrations |
| **IMAGE_REPO** | Image repositories or CDN paths |
| **DOCS** | Documentation links |
| **REPO** | Code repositories |
| **PAGE** | Web page URLs |
| **MEDIA** | Media files (videos, audio) |

#### Creating Your First Content

**Step 1: Navigate to your project**
- Click on a project node from the dashboard

**Step 2: Create new content**
- Click the **+ New Content** button
- Choose content type

**Step 3: Fill basic information**

```
Title: "Summer Sale Landing Page"
Code: (auto-generated, or enter "SUMMER-LP-2024")
Slug: "summer-sale-2024" (this becomes the URL)
Language: "EN"
```

**Step 4: Add your content**
- For **HTML**: Use the WYSIWYG editor
- For **JSON**: Enter structured data
- For **URLS**: Add your list of URLs with descriptions and types
- For **FILE/PICTURE**: Upload your file
- For **SCRIPT/STYLE**: Enter your code

**Step 5: Save and preview**
- Click **Save** (this creates a SNAPSHOT - your working copy)
- Click **Preview** to see how it looks

**Step 6: Publish when ready**
- When you're happy with the result
- Click **Publish** to make it live

### Working with Translations

#### Understanding Translation Inheritance

Translations flow down the tree. Set them once at the project level, and they're available everywhere.

#### Adding Translations

**Step 1: Navigate to your node or content**
- Go to the item you want to add translations to

**Step 2: Open the Translations tab**
- Click on **Translations** in the properties panel

**Step 3: View inherited translations**
- Grayed out entries come from parent nodes
- These are read-only here (edit them at the parent level)

**Step 4: Add a new translation**

1. Click **+ Add Translation**
2. Enter the **Key** (e.g., "WELCOME_MESSAGE")
3. Select the **Language** (e.g., "FR" for French)
4. Enter the **Value** (e.g., "Bienvenue dans notre boutique!")
5. Click **Save**

**Step 5: Override an inherited translation**
1. Find the inherited translation you want to override
2. Click **Override**
3. Enter your new value
4. Click **Save**

#### Using Translations in Content

In your HTML content, use the special directive:

```html
<h1>$translate(WELCOME_MESSAGE)</h1>
<p>$translate(SALE_DESCRIPTION)</p>
```

The system will automatically display:
- The user's language version (based on browser or selection)
- The inherited value if not overridden
- The parent value if not set at current level

### Working with Values (Key/Value Pairs)

Values are perfect for dynamic content that changes frequently - prices, discounts, user names, etc.

#### Creating Values

**Step 1: Navigate to your node or content**
**Step 2: Click the **Values** tab**
**Step 3: Click **+ Add Value****

**Step 4: Configure the value**

```
Key: "SALE_PERCENTAGE"
Value: "20"
Type: "NUMBER" (options: TEXT, NUMBER, BOOLEAN, DATE)
```

**Step 5: Click **Save****

#### Value Inheritance

Like translations, values are inherited:

```
Project Level: "DISCOUNT" = "20"
├── Category Level: No override → "20"
├── Product Level: Override → "50" (Flash sale!)
└── Another Product: No override → "20"
```

#### Using Values in Content

In your HTML content:

```html
<h2>$val(SALE_PERCENTAGE)% OFF everything!</h2>
<p>Free shipping on orders over $val(FREE_SHIPPING_THRESHOLD)€</p>
<p>Welcome back, $val(USER_NAME)!</p>
```

### Working with Rules

Rules let you control **when** content is accessible. When a rule is active, the backend automatically checks if conditions are met. If not, the content returns a 404 (not found) response.

#### What Rules Do

- **DATE rules**: Content is only accessible during specific time periods
- **BOOLEAN rules**: Content is only accessible when a condition is true

The rules work **transparently** - if a user tries to access content that doesn't satisfy the rules, they simply get a "not found" page.

#### Rule Types

Nodify supports two types of rules:

| Type | Purpose | Example |
|------|---------|---------|
| **DATE** | Content available only during specific dates | Christmas content available Dec 1-25 only |
| **BOOLEAN** | Content available only when condition is true | VIP content only visible to VIP users |

#### Rule Inheritance

**Rules flow down from parent to children**, just like translations and values.

```
Project Level: DATE rule active (June-August)
├── Category Level: BOOLEAN rule active
│   └── Product Level: Inherits BOTH rules automatically!
└── Another Category: Inherits only the DATE rule
```

#### How Multiple Rules Work

When multiple rules apply to content (from parents + its own), **ALL rules must be satisfied** for the content to be accessible.

**Example 1: Date rule only**
```
Product has: DATE rule (June 1 - August 31)
Result: Product returns 404 outside this date range
```

**Example 2: Boolean rule only**
```
Product has: BOOLEAN rule (in_stock = true)
Result: Product returns 404 when out of stock
```

**Example 3: Multiple rules (AND logic)**
```
Product inherits from parent:
- DATE rule (June 1 - August 31)

Product adds its own:
- BOOLEAN rule (in_stock = true)

Result: Product returns 404 unless:
✓ Date is between June 1 and August 31
✓ AND product is in stock
```

#### Creating a Rule

**Step 1: Navigate to your node or content**
**Step 2: Click the **Rules** tab**
**Step 3: Click **+ Add Rule****

**Step 4: Configure the rule**

**For a DATE rule (time-based access):**
```
Type: "DATE"
Name: "Christmas Campaign"
Start Date: "2024-12-01"
End Date: "2024-12-25"
```
Content will return 404 outside Dec 1-25.

**For a BOOLEAN rule (condition-based access):**
```
Type: "BOOLEAN"
Name: "VIP Only"
Value: "true"
```
Content will return 404 when condition is false (non-VIP users).

**Step 5: Activate the rule**
- Toggle **Active** to ON
- **Important:** Rules only take effect when activated!

**Step 6: Click **Save****

#### Managing Rules

| Action | How to |
|--------|--------|
| **Activate a rule** | Toggle the Active switch to ON |
| **Deactivate a rule** | Toggle the Active switch to OFF |
| **Edit a rule** | Click the rule, modify, save |
| **Delete a rule** | Click the trash icon next to the rule |

#### Rule Activation Best Practices

- ✅ Always check that your rule is **Active** before expecting it to work
- ✅ Test rules in staging environment first
- ✅ Use clear, descriptive rule names
- ✅ Remember that multiple rules = AND logic (all must be true)
- ❌ Don't leave test rules active in production

### Working with Versions (The Snapshot System)

**Important: You NEVER work directly on published content.** You always work on a SNAPSHOT version.

#### Your Workflow

```
1. EDIT (Snapshot) → 2. PREVIEW → 3. TEST → 4. PUBLISH
```

#### Version History

Every time you save, a new version is created:

```
Product Page "Swim Trunks"
├── v1.0.0 (PUBLISHED) - What customers see
├── v1.1.0 (SNAPSHOT) - Your current working copy
├── v1.0.2 (SNAPSHOT) - Previous draft
└── v1.0.1 (ARCHIVE) - Old version (kept for history)
```

#### How to Use Versions

**View version history:**
1. Open any content
2. Click **Version History** in the top bar
3. See all versions with timestamps and authors

**Revert to an older version:**
1. Find the version you want
2. Click **Revert to this version**
3. A new snapshot is created from that version
4. Continue editing or publish

**Archive old versions:**
1. Select versions you no longer need
2. Click **Archive**
3. They're kept but hidden from main list

### Import/Export

#### Export

You can export at ANY level - entire project, a section, or even a single content.

**How to export:**
1. Navigate to what you want to export
2. Click the **Export** button
3. Choose format:
  - **JSON** (for data exchange)
  - **ZIP** (includes all assets)
4. Click **Download**

#### Import

**How to import:**
1. Go to destination (project or node)
2. Click **Import**
3. Upload your export file
4. Choose options:
  - **Overwrite existing** (replace matching items)
  - **Create new versions** (keep both)
  - **Keep original IDs** (for exact restoration)
5. Click **Import**

### Deployment

Deploy your content to different environments:

```
Available Environments:
├── Development (your working area)
├── Staging (testing environment)
└── Production (live website)
```

#### How to Deploy

**Full deployment:**
1. Make sure your snapshot is ready
2. Click **Deploy** button
3. Choose target environment:
  - **Staging**: Test before going live
  - **Production**: Make it live
4. Confirm deployment

**Partial deployment:**
1. Select specific node or content
2. Right-click → **Deploy this only**
3. Choose environment
4. Confirm

### Collaboration Features

#### Lock System

When you edit something, you automatically get a lock:

```
🔒 "Summer Sale Landing Page" is being edited by Sarah
⏳ Others see this warning when they try to edit
```

**What happens:**
- Others see a warning when they try to edit the same item
- They can see who's editing it
- They can request notification when you're done

**Best practices:**
- ✅ Lock before making significant changes
- ✅ Release lock when done or on break
- ✅ Check with team before force-releasing

#### Admin Override

Admins can force-release locks:

1. Go to **Admin** → **Active Locks**
2. See all currently locked items
3. Click **Release Lock** next to any item
4. Confirm override

### E-merchandising Features

#### Product Content Management

**Creating a product:**
1. Navigate to your product category node
2. Click **+ New Content**
3. Choose **JSON** as content type
4. Enter product information:

```json
{
  "name": "Men's Swim Trunks",
  "sku": "SWIM-001",
  "price": 49.99,
  "currency": "EUR",
  "stock": 150,
  "brand": "BeachLife",
  "colors": ["Blue", "Red", "Black"],
  "sizes": ["S", "M", "L", "XL"],
  "description": "Comfortable swim trunks for summer"
}
```

**Adding product images:**
1. Create a **PICTURE** content for each image
2. Upload the image file
3. Link it to your product using values

#### Managing URL Collections

For catalogs or external references, use the **URLS** content type:

```json
[
  {
    "url": "https://api.products.com/v1/items",
    "description": "Main product API",
    "type": "API"
  },
  {
    "url": "https://images.cdn.com/products/",
    "description": "Product image repository",
    "type": "IMAGE_REPO"
  },
  {
    "url": "https://docs.internal.com/api-guide",
    "description": "API documentation",
    "type": "DOCS"
  }
]
```

#### Cross-selling & Upselling

**Setting up related products:**

1. Edit a product
2. Go to **Relations** tab
3. Click **+ Add Relation**
4. Choose type:
  - **Cross-sell**: "Customers also bought"
  - **Upsell**: "Better version available"
  - **Accessories**: "Complete the look"
5. Select target product
6. Click **Save**

### Analytics & Reporting

#### Built-in Reports

| Report | What It Shows | Use For |
|--------|---------------|---------|
| **Content Views** | Page views per content | Popular content |
| **Click-through Rate** | CTA effectiveness | Conversion optimization |
| **User Engagement** | Time on page, scroll depth | Content quality |
| **Conversion Rate** | Goal completions | Campaign success |
| **Top Products** | Most viewed products | Merchandising |

#### Accessing Reports

1. Go to **Analytics** in the main menu
2. Select the report you want
3. Choose date range
4. Click **Generate**

#### Exporting Reports

1. Generate your report
2. Click **Export**
3. Choose format: CSV, PDF, or Excel
4. Download file

### A/B Testing

#### Setting Up a Test

**Step 1: Create two versions**
- Version A: Original content
- Version B: Variation (new headline, different image)

**Step 2: Go to Marketing → A/B Tests**
**Step 3: Click + New Test**

**Step 4: Configure test**

```
Name: "Homepage Headline Test"
Content to test: "Homepage" (select from list)
Traffic split: 50/50
Success metric: "Click-through Rate"
Duration: "2 weeks"
Minimum sample: "1000 visitors"
```

**Step 5: Launch test**

#### Analyzing Results

After the test completes, you'll see:

| Version | Visitors | Conversions | Rate | Winner |
|---------|----------|-------------|------|--------|
| A | 1,234 | 123 | 9.97% | |
| B | 1,256 | 156 | 12.42% | ✓ |

Click **Apply Winner** to automatically publish the winning version.
```
