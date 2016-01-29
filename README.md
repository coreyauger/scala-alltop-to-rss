# scala-alltop-to-rss
Multple passes to extract and clean the sites listed on the blog agregate site (alltop)[http://alltop.com/]

Here are some of the steps that were taken:
* Captured all the categories and links to sites from alltop
* Hit the site and extract all the link and rss feed items
* Next we travel to each of the links to verify they are in fact RSS feeds.
* The final list contains: topic, alltop url, and then a list of sites for that category.

## Json file format
The resulting file is ~1.2meg and is called cleanrss.json.  This file has the following format

```json
{
  "topic" : "Accounting",
  "url" : "http://accounting.alltop.com/",
  "sites" : [ {
    "site" : "http://retheauditors.com",
    "rss" : [ "http://feeds.feedburner.com/ReTheAuditors", "http://retheauditors.com/feed/", "http://retheauditors.com/?feed=rss" ]
  }, {
    "site" : "http://cpatrendlines.com",
    "rss" : [ "http://cpatrendlines.com/tag/management/feed/", "http://cpatrendlines.com/tag/marketing,sales/feed/", "http://cpatrendlines.com/tag/staff,staffing/feed/", "http://cpatrendlines.com/tag/IT,it,tech,technology,Saas,cloud/feed/" ]
  }, {
    "site" : "http://staciesmoretaxtips.com",
    "rss" : [ "http://feeds.feedburner.com/DontMessWithTaxes", "http://www.taxresolution.com/blog/feed/", "http://staciesmoretaxtips.com/feed/", "http://wanderingtaxpro.blogspot.com/feeds/posts/default" ]
  }, {
    "site" : "http://blog.aicpa.org/",
    "rss" : [ "https://feeds.feedblitz.com/aicpainsights" ]
  }, {
    "site" : "http://www.accountantnextdoor.com",
    "rss" : [ "http://www.accountantnextdoor.com/feed" ]
  }, {
    "site" : "http://www.rflie.com/blog",
    "rss" : [ "http://feeds.feedburner.com/rflie" ]
  } ]
},{
  "topic" : "Acne",
  "url" : "http://acne.alltop.com/",
  "sites" : [ {
    "site" : "http://www.medworm.com/rss/search.php?qu=acne&kid=2&t=Acne&f=c",
    "rss" : [ "http://www.medworm.com/rss/medicalfeeds/conditions/Acne.xml" ]
  }, {
    "site" : "http://www.drbaileyskincare.com/blog",
    "rss" : [ "http://feeds.feedburner.com/otbskincare" ]
  }, {
    "site" : "http://simplepurebeauty.com",
    "rss" : [ "http://feeds.feedburner.com/SimplePureBeauty" ]
  } ]
}, 

... Much more
```

The site contains a link that was used to get to that site and rss contains a list of verified rss feeds that were extracted from that site url.

Enjoy !

