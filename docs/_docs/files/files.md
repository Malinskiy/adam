---
layout: default
title: Files
nav_order: 3
has_children: true
has_toc: false
permalink: /docs/files
---

# File requests

This request type transfers files or information about files

If you want to push/pull without worrying about the device features and protocols - see [recommended requests][1].

When working with sync v1/v2 protocols, a proper fallback has to be implemented if device doesn't support some of the features. To simplify
 working with this - see [compatibility requests][2].

If you want to directly interact with sync v1 see [sync v1 docs][3], for sync v2 - [sync v2 docs][4]

For compatibility reasons, you might want to use plain `ls`. This is wrapped in [ListFilesRequest][5]

[1]: {% link _docs/files/recommended.md %}
[2]: {% link _docs/files/compat.md %}
[3]: {% link _docs/files/sync-v1.md %}
[4]: {% link _docs/files/sync-v2.md %}
[5]: {% link _docs/files/ls.md %}