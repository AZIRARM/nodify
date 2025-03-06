## User Documentation: Nodify

Nodify is a powerful and flexible headless content management system (CMS) designed to let you create and manage your content efficiently.

This document will guide you through the main features of Nodify.

## Initial Login and License Update
* **Login:**
    * **User:** admin
    * **Password:** Admin13579++

## Main Interface and Configuration
* **Dashboard:** The main interface provides an overview of your content and various configuration options.
* **User management:** Create and manage users, define their roles, and assign them specific access to certain nodes.
* **Language management:** Configure the languages supported by your site and manage translations.

## Content Structure: Nodes
* **Nodes:** Nodes are the basic building blocks of your content structure. They can contain different types of content (text, HTML, JSON, images, files, etc.).
* **Hierarchy:** You can create a hierarchy of nodes to organize your content logically.
* **Visibility:** A node being edited or one of its contents will only be visible when it is deployed.
* **Metadata:** Associate metadata (key/value pairs, tags) with your nodes to facilitate searching and filtering.
* **Node creation:** To create a new node (project/environment...), go to "My projects" in the menu and click on the "+" button. Sub-nodes are added in the same way. For node creation, only a name and a default language are required; other fields are optional.
* **Recommendations:** It is advisable to create a node for a specific content type or category. For example, for a "blog" node, it is recommended to create a sub-node for HTML or JSON content and a separate sub-node for your post images. A blog template is provided in this repository: [AZIRARM/nodify-templates](https://github.com/AZIRARM/nodify-templates). Import it and examine how the nodes are organized.
* **Custom data:** On each node and each content, you have the option to add custom data (key/value pairs) accessible in the content via the keyword: `$value(CODE_VALUE)`.
* **Translations:** On each node and each content, you have the option to add word translations (key/value pairs and language code) accessible in the content via the keyword: `$trans(CODE_MESSAGE)`.
* **Management rules:** On each node and each content, you have the option to add management rules by clicking on "Rules". Two options are available: boolean or date, and the action can be to disable or enable a node or content. If a node is disabled and the rule is enabled, the node and its sub-nodes as well as all their contents will be inaccessible. If a content is disabled, only that content will be disabled.
* **Data inheritance:** For values and translations, a content can access the translations and values of its parent nodes...
* **Deployment:** You have the option to deploy a node or content, which changes it to the "PUBLISHED" state. Click on the "Deploy" button. As soon as a change is made to a content or node, it changes to the "SNAPSHOT" state.
    * **SNAPSHOT vs. PUBLISHED:** What is in "SNAPSHOT" remains visible only in "SNAPSHOT" and will only be visible in "PUBLISHED" when you have deployed it.
    * **Versions:** The deployed "SNAPSHOT" version will be saved, and a small green button on the left of the node or content will appear to indicate that it is deployed. Orange indicates that it is being modified, and red that it has just been created.
    * **Version history:** You always have the option to revert to a previous version by clicking on this small button. All versions will appear, and you can revert to an earlier version (be careful not to overuse this manipulation).
* **Deletion:** You also have the option to delete a node or content via the "Delete" button on the line of the content or node concerned in "Actions". This is not a permanent deletion. The permanent deletion occurs when you click on the trash can at the top. There, you have two choices: permanently delete or cancel the deletion of the content or node. Deleting a node results in the deletion of its sub-nodes and their contents.
* **Content Creation:** To create content on a node or sub-node, you must first create at least one node. Then, go to the line of the node concerned and click on "Contents". A window displaying the contents of the node will open, and you can create contents of different types there.

## Content Creation and Management
* **Content types:** Create different content types (articles, pages, products, etc.) by defining the fields and properties specific to each type.
* **Content editing:** Modify the content of your nodes directly in the interface, using a WYSIWYG editor or by entering code.
* **Translation:** Translations are managed at both the node and content levels. You can translate metadata fields and the content itself.

## Advanced Features
* **Multilingualism:** Nodify is fully multilingual. You can create translated versions of your content for different markets.
* **Workflows:** Implement workflows to approve content changes before publication.
* **Integrations:** Integrate Nodify with other tools and services (CRM, e-commerce, etc.).

## Security
* **Access control:** Configure permissions precisely for each user to protect your content.
* **Backups:** Regularly back up your content to prevent data loss.

## Support
For any questions or problems, please contact our support at the following address: \[Replace with the support email address]

## Conclusion
This document has provided you with an overview of Nodify's features. For more in-depth usage, we invite you to explore the various options offered by the platform.
