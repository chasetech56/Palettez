# Google Search Console Sitemap Fix

## Issue
In Google Search Console, the sitemap submission shows:
- **Sitemap**: `/sitemap.xml`
- **Status**: `Couldn't fetch`

Search Console expects paths relative to the domain root without a leading slash, because it automatically prefixes `https://chasetech56.github.io/Palettez/`. Submitting `/sitemap.xml` attempts to fetch `https://chasetech56.github.io/Palettez//sitemap.xml` or fails internal route validation.

## Resolution Steps
1. Navigate to [Google Search Console](https://search.google.com/search-console).
2. Select property `https://chasetech56.github.io/Palettez/`.
3. In the left navigation, click **Sitemaps** under Indexing.
4. Under **Submitted sitemaps**, click the row with `/sitemap.xml`.
5. Click the three vertical dots (`⋮`) in the top-right corner and select **Remove sitemap**.
6. In the **Add a new sitemap** input field:
   - Notice the prefix already shown: `https://chasetech56.github.io/Palettez/`
   - Enter strictly: `sitemap.xml` (do NOT type a leading `/`)
7. Click **SUBMIT**.
8. The status will update to `Success` with discovered pages listed.
