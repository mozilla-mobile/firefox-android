/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

"use strict";

/* globals module, require */

const AVAILABLE_SHIMS = [
  {
    hiddenInAboutCompat: true,
    id: "LiveTestShim",
    platform: "all",
    name: "Live test shim",
    bug: "livetest",
    file: "live-test-shim.js",
    matches: ["*://webcompat-addon-testbed.herokuapp.com/shims_test.js"],
    needsShimHelpers: ["getOptions", "optIn"],
  },
  {
    hiddenInAboutCompat: true,
    id: "MochitestShim",
    platform: "all",
    branch: ["all:ignoredOtherPlatform"],
    name: "Test shim for Mochitests",
    bug: "mochitest",
    file: "mochitest-shim-1.js",
    matches: [
      "*://example.com/browser/browser/extensions/webcompat/tests/browser/shims_test.js",
    ],
    needsShimHelpers: ["getOptions", "optIn"],
    options: {
      simpleOption: true,
      complexOption: { a: 1, b: "test" },
      branchValue: { value: true, branches: [] },
      platformValue: { value: true, platform: "neverUsed" },
    },
    unblocksOnOptIn: ["*://trackertest.org/*"],
  },
  {
    hiddenInAboutCompat: true,
    disabled: true,
    id: "MochitestShim2",
    platform: "all",
    name: "Test shim for Mochitests (disabled by default)",
    bug: "mochitest",
    file: "mochitest-shim-2.js",
    matches: [
      "*://example.com/browser/browser/extensions/webcompat/tests/browser/shims_test_2.js",
    ],
    needsShimHelpers: ["getOptions", "optIn"],
    options: {
      simpleOption: true,
      complexOption: { a: 1, b: "test" },
      branchValue: { value: true, branches: [] },
      platformValue: { value: true, platform: "neverUsed" },
    },
    unblocksOnOptIn: ["*://trackertest.org/*"],
  },
  {
    hiddenInAboutCompat: true,
    id: "MochitestShim3",
    platform: "all",
    name: "Test shim for Mochitests (host)",
    bug: "mochitest",
    file: "mochitest-shim-3.js",
    notHosts: ["example.com"],
    matches: [
      "*://example.com/browser/browser/extensions/webcompat/tests/browser/shims_test_3.js",
    ],
  },
  {
    hiddenInAboutCompat: true,
    id: "MochitestShim4",
    platform: "all",
    name: "Test shim for Mochitests (notHost)",
    bug: "mochitest",
    file: "mochitest-shim-3.js",
    hosts: ["example.net"],
    matches: [
      "*://example.com/browser/browser/extensions/webcompat/tests/browser/shims_test_3.js",
    ],
  },
  {
    hiddenInAboutCompat: true,
    id: "MochitestShim5",
    platform: "all",
    name: "Test shim for Mochitests (branch)",
    bug: "mochitest",
    file: "mochitest-shim-3.js",
    branches: ["never matches"],
    matches: [
      "*://example.com/browser/browser/extensions/webcompat/tests/browser/shims_test_3.js",
    ],
  },
  {
    hiddenInAboutCompat: true,
    id: "MochitestShim6",
    platform: "never matches",
    name: "Test shim for Mochitests (platform)",
    bug: "mochitest",
    file: "mochitest-shim-3.js",
    matches: [
      "*://example.com/browser/browser/extensions/webcompat/tests/browser/shims_test_3.js",
    ],
  },
  {
    id: "AddThis",
    platform: "all",
    name: "AddThis",
    bug: "1713694",
    file: "addthis-angular.js",
    matches: [
      "*://s7.addthis.com/icons/official-addthis-angularjs/current/dist/official-addthis-angularjs.min.js*",
    ],
    onlyIfBlockedByETP: true,
  },
  {
    id: "Adform",
    platform: "all",
    name: "Adform",
    bug: "1713695",
    file: "adform.js",
    matches: [
      "*://track.adform.net/serving/scripts/trackpoint/",
      "*://track.adform.net/serving/scripts/trackpoint/async/",
      {
        patterns: ["*://track.adform.net/Serving/TrackPoint/*"],
        target: "tracking-pixel.png",
        types: ["image", "imageset", "xmlhttprequest"],
      },
    ],
    onlyIfBlockedByETP: true,
  },
  {
    id: "AdNexusAST",
    platform: "all",
    name: "AdNexus AST",
    bug: "1734130",
    file: "adnexus-ast.js",
    matches: ["*://*.adnxs.com/*/ast.js*"],
    onlyIfBlockedByETP: true,
  },
  {
    id: "AdNexusPrebid",
    platform: "all",
    name: "AdNexus Prebid",
    bug: "1713696",
    file: "adnexus-prebid.js",
    matches: ["*://*.adnxs.com/*/pb.js*", "*://*.adnxs.com/*/prebid*"],
    onlyIfBlockedByETP: true,
  },
  {
    id: "AdobeEverestJS",
    platform: "all",
    name: "Adobe EverestJS",
    bug: "1728114",
    file: "everest.js",
    matches: ["*://www.everestjs.net/static/st.v3.js*"],
    onlyIfBlockedByETP: true,
  },
  {
    // keep this above AdSafeProtectedTrackingPixels
    id: "AdSafeProtectedGoogleIMAAdapter",
    platform: "all",
    name: "Ad Safe Protected Google IMA Adapter",
    bug: "1508639",
    file: "adsafeprotected-ima.js",
    matches: ["*://static.adsafeprotected.com/vans-adapter-google-ima.js"],
    onlyIfBlockedByETP: true,
  },
  {
    id: "AdsByGoogle",
    platform: "all",
    name: "Ads by Google",
    bug: "1713726",
    file: "google-ads.js",
    matches: [
      "*://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js",
      {
        patterns: [
          "*://pagead2.googlesyndication.com/pagead/*.js*fcd=true",
          "*://pagead2.googlesyndication.com/pagead/js/*.js*fcd=true",
        ],
        target: "empty-script.js",
        types: ["xmlhttprequest"],
      },
    ],
    onlyIfBlockedByETP: true,
  },
  {
    id: "AdvertisingCom",
    platform: "all",
    name: "advertising.com",
    bug: "1701685",
    matches: [
      {
        patterns: ["*://pixel.advertising.com/firefox-etp"],
        target: "tracking-pixel.png",
        types: ["image", "imageset", "xmlhttprequest"],
        onlyIfBlockedByETP: true,
      },
      {
        patterns: ["*://cdn.cmp.advertising.com/firefox-etp"],
        target: "empty-script.js",
        types: ["xmlhttprequest"],
        onlyIfBlockedByETP: true,
      },
      {
        patterns: ["*://*.advertising.com/*.js*"],
        target: "https://cdn.cmp.advertising.com/firefox-etp",
        types: ["image", "imageset", "xmlhttprequest"],
        onlyIfBlockedByETP: true,
      },
      {
        patterns: ["*://*.advertising.com/*"],
        target: "https://pixel.advertising.com/firefox-etp",
        types: ["image", "imageset", "xmlhttprequest"],
        onlyIfBlockedByETP: true,
      },
    ],
  },
  {
    id: "Branch",
    platform: "all",
    name: "Branch Web SDK",
    bug: "1716220",
    file: "branch.js",
    matches: ["*://cdn.branch.io/branch-latest.min.js*"],
    onlyIfBlockedByETP: true,
  },
  {
    id: "DoubleVerify",
    platform: "all",
    name: "DoubleVerify",
    bug: "1771557",
    file: "doubleverify.js",
    matches: ["*://pub.doubleverify.com/signals/pub.js*"],
    onlyIfBlockedByETP: true,
  },
  {
    id: "AmazonTAM",
    platform: "all",
    name: "Amazon Transparent Ad Marketplace",
    bug: "1713698",
    file: "apstag.js",
    matches: ["*://c.amazon-adsystem.com/aax2/apstag.js"],
    onlyIfBlockedByETP: true,
  },
  {
    id: "BmAuth",
    platform: "all",
    name: "BmAuth by 9c9media",
    bug: "1486337",
    file: "bmauth.js",
    matches: ["*://auth.9c9media.ca/auth/main.js"],
    onlyIfBlockedByETP: true,
  },
  {
    id: "Chartbeat",
    platform: "all",
    name: "Chartbeat",
    bug: "1713699",
    file: "chartbeat.js",
    matches: [
      "*://static.chartbeat.com/js/chartbeat.js",
      "*://static.chartbeat.com/js/chartbeat_video.js",
    ],
    onlyIfBlockedByETP: true,
  },
  {
    id: "Criteo",
    platform: "all",
    name: "Criteo",
    bug: "1713720",
    file: "criteo.js",
    matches: ["*://static.criteo.net/js/ld/publishertag.js"],
    onlyIfBlockedByETP: true,
  },
  {
    // keep this above AdSafeProtectedTrackingPixels
    id: "Doubleclick",
    platform: "all",
    name: "Doubleclick",
    bug: "1713693",
    matches: [
      {
        patterns: [
          "*://securepubads.g.doubleclick.net/gampad/*ad-blk*",
          "*://pubads.g.doubleclick.net/gampad/*ad-blk*",
        ],
        target: "empty-shim.txt",
        types: ["image", "imageset", "xmlhttprequest"],
      },
      {
        patterns: [
          "*://securepubads.g.doubleclick.net/gampad/*xml_vmap1*",
          "*://pubads.g.doubleclick.net/gampad/*xml_vmap1*",
        ],
        target: "vmad.xml",
        types: ["image", "imageset", "xmlhttprequest"],
      },
      {
        patterns: [
          "*://vast.adsafeprotected.com/vast*",
          "*://securepubads.g.doubleclick.net/gampad/*xml_vmap2*",
          "*://pubads.g.doubleclick.net/gampad/*xml_vmap2*",
        ],
        target: "vast2.xml",
        types: ["image", "imageset", "xmlhttprequest"],
      },
      {
        patterns: [
          "*://securepubads.g.doubleclick.net/gampad/*ad*",
          "*://pubads.g.doubleclick.net/gampad/*ad*",
        ],
        target: "vast3.xml",
        types: ["image", "imageset", "xmlhttprequest"],
      },
    ],
    onlyIfBlockedByETP: true,
  },
  {
    id: "PBMWebAPIFixes",
    platform: "all",
    name: "Private Browsing Web APIs",
    bug: "1773110",
    runFirst: "private-browsing-web-api-fixes.js",
    matches: [
      "*://*.imgur.com/js/vendor.*.bundle.js",
      "*://*.imgur.io/js/vendor.*.bundle.js",
      "*://www.rva311.com/static/js/main.*.chunk.js",
      "*://web-assets.toggl.com/app/assets/scripts/*.js", // bug 1783919
    ],
    onlyIfPrivateBrowsing: true,
  },
  {
    id: "Eluminate",
    platform: "all",
    name: "Eluminate",
    bug: "1503211",
    file: "eluminate.js",
    matches: ["*://libs.coremetrics.com/eluminate.js"],
    onlyIfBlockedByETP: true,
  },
  {
    id: "FacebookSDK",
    platform: "all",
    branches: ["nightly:android"],
    name: "Facebook SDK",
    bug: "1226498",
    file: "facebook-sdk.js",
    logos: ["facebook.svg", "play.svg"],
    matches: [
      "*://connect.facebook.net/*/sdk.js*",
      "*://connect.facebook.net/*/all.js*",
      {
        patterns: ["*://www.facebook.com/platform/impression.php*"],
        target: "tracking-pixel.png",
        types: ["image", "imageset", "xmlhttprequest"],
      },
    ],
    needsShimHelpers: ["optIn", "getOptions"],
    onlyIfBlockedByETP: true,
    unblocksOnOptIn: [
      "*://connect.facebook.net/*/sdk.js*",
      "*://connect.facebook.net/*/all.js*",
      "*://*.xx.fbcdn.net/*", // covers:
      // "*://scontent-.*-\d.xx.fbcdn.net/*",
      // "*://static.xx.fbcdn.net/rsrc.php/*",
      "*://graph.facebook.com/v2*access_token*",
      "*://graph.facebook.com/v*/me*",
      "*://graph.facebook.com/*/picture*",
      "*://www.facebook.com/*/plugins/login_button.php*",
      "*://www.facebook.com/x/oauth/status*",
      {
        patterns: [
          "*://www.facebook.com/*/plugins/video.php*",
          "*://www.facebook.com/rsrc.php/*",
        ],
        branches: ["nightly"],
      },
    ],
  },
  {
    id: "Fastclick",
    platform: "all",
    name: "Fastclick",
    bug: "1738220",
    file: "fastclick.js",
    matches: [
      "*://secure.cdn.fastclick.net/js/cnvr-launcher/*/launcher-stub.min.js*",
    ],
    onlyIfBlockedByETP: true,
  },
  {
    id: "GoogleAnalyticsAndTagManager",
    platform: "all",
    name: "Google Analytics and Tag Manager",
    bug: "1713687",
    file: "google-analytics-and-tag-manager.js",
    matches: [
      "*://www.google-analytics.com/analytics.js*",
      "*://www.google-analytics.com/gtm/js*",
      "*://www.googletagmanager.com/gtm.js*",
    ],
    onlyIfBlockedByETP: true,
  },
  {
    id: "GoogleAnalyticsECommercePlugin",
    platform: "all",
    name: "Google Analytics E-Commerce Plugin",
    bug: "1620533",
    file: "google-analytics-ecommerce-plugin.js",
    matches: ["*://www.google-analytics.com/plugins/ua/ec.js"],
    onlyIfBlockedByETP: true,
  },
  {
    id: "GoogleAnalyticsLegacy",
    platform: "all",
    name: "Google Analytics (legacy version)",
    bug: "1487072",
    file: "google-analytics-legacy.js",
    matches: ["*://ssl.google-analytics.com/ga.js"],
    onlyIfBlockedByETP: true,
  },
  {
    id: "GoogleIMA",
    platform: "all",
    name: "Google Interactive Media Ads",
    bug: "1713690",
    file: "google-ima.js",
    matches: [
      "*://s0.2mdn.net/instream/html5/ima3.js",
      "*://imasdk.googleapis.com/js/sdkloader/ima3.js",
    ],
    onlyIfBlockedByETP: true,
  },
  {
    id: "GooglePageAd",
    platform: "all",
    name: "Google Page Ad",
    bug: "1713692",
    file: "google-page-ad.js",
    matches: ["*://www.googleadservices.com/pagead/conversion_async.js"],
    onlyIfBlockedByETP: true,
  },
  {
    id: "GooglePublisherTags",
    platform: "all",
    name: "Google Publisher Tags",
    bug: "1713685",
    file: "google-publisher-tags.js",
    matches: [
      "*://www.googletagservices.com/tag/js/gpt.js*",
      "*://pagead2.googlesyndication.com/tag/js/gpt.js*",
      "*://pagead2.googlesyndication.com/gpt/pubads_impl_*.js*",
      "*://securepubads.g.doubleclick.net/tag/js/gpt.js*",
      "*://securepubads.g.doubleclick.net/gpt/pubads_impl_*.js*",
    ],
    onlyIfBlockedByETP: true,
  },
  {
    id: "Google SafeFrame",
    platform: "all",
    name: "Google SafeFrame",
    bug: "1713691",
    matches: [
      {
        patterns: [
          "*://tpc.googlesyndication.com/safeframe/*/html/container.html",
          "*://*.safeframe.googlesyndication.com/safeframe/*/html/container.html",
        ],
        target: "google-safeframe.html",
        types: ["sub_frame"],
      },
    ],
    onlyIfBlockedByETP: true,
  },
  {
    id: "GoogleTrends",
    platform: "all",
    name: "Google Trends",
    bug: "1624914",
    custom: "google-trends-dfpi-fix",
    onlyIfDFPIActive: true,
    matches: [
      {
        patterns: ["*://trends.google.com/trends/embed*"],
        types: ["sub_frame"],
      },
    ],
  },
  {
    id: "IAM",
    platform: "all",
    name: "INFOnline IAM",
    bug: "1761774",
    file: "iam.js",
    matches: ["*://script.ioam.de/iam.js"],
    onlyIfBlockedByETP: true,
  },
  {
    // keep this above AdSafeProtectedTrackingPixels
    id: "IASPET",
    platform: "all",
    name: "Integral Ad Science PET",
    bug: "1713701",
    file: "iaspet.js",
    matches: [
      "*://cdn.adsafeprotected.com/iasPET.1.js",
      "*://static.adsafeprotected.com/iasPET.1.js",
    ],
    onlyIfBlockedByETP: true,
  },
  {
    id: "MNet",
    platform: "all",
    name: "Media.net Ads",
    bug: "1713703",
    file: "empty-script.js",
    matches: ["*://adservex.media.net/videoAds.js*"],
    onlyIfBlockedByETP: true,
  },
  {
    id: "Moat",
    platform: "all",
    name: "Moat",
    bug: "1713704",
    file: "moat.js",
    matches: [
      "*://*.moatads.com/*/moatad.js*",
      "*://*.moatads.com/*/moatapi.js*",
      "*://*.moatads.com/*/moatheader.js*",
      "*://*.moatads.com/*/yi.js*",
    ],
    onlyIfBlockedByETP: true,
  },
  {
    id: "Nielsen",
    platform: "all",
    name: "Nielsen",
    bug: "1760754",
    file: "nielsen.js",
    matches: ["*://*.imrworldwide.com/v60.js"],
    onlyIfBlockedByETP: true,
  },
  {
    id: "Optimizely",
    platform: "all",
    name: "Optimizely",
    bug: "1714431",
    file: "optimizely.js",
    matches: [
      "*://cdn.optimizely.com/js/*.js",
      "*://cdn.optimizely.com/public/*.js",
    ],
    onlyIfBlockedByETP: true,
  },
  {
    id: "Rambler",
    platform: "all",
    name: "Rambler Authenticator",
    bug: "1606428",
    file: "rambler-authenticator.js",
    matches: ["*://id.rambler.ru/rambler-id-helper/auth_events.js"],
    needsShimHelpers: ["optIn"],
    onlyIfBlockedByETP: true,
  },
  {
    id: "RichRelevance",
    platform: "all",
    name: "Rich Relevance",
    bug: "1713725",
    file: "rich-relevance.js",
    matches: ["*://media.richrelevance.com/rrserver/js/1.2/p13n.js"],
    onlyIfBlockedByETP: true,
  },
  {
    id: "Firebase",
    platform: "all",
    name: "Firebase",
    bug: "1771783",
    onlyIfPrivateBrowsing: true,
    runFirst: "firebase.js",
    matches: [
      // bugs 1750699, 1767407
      "*://www.gstatic.com/firebasejs/*/firebase-messaging.js*",
    ],
    contentScripts: [
      {
        cookieStoreId: "firefox-private",
        js: "firebase.js",
        runAt: "document_start",
        matches: [
          "*://www.homedepot.ca/*", // bug 1778993
          "*://orangerie.eu/*", // bug 1758442
          "*://web.whatsapp.com/*", // bug 1767407
          "*://www.tripadvisor.com/*", // bug 1779536
          "*://www.office.com/*", // bug 1783921
        ],
      },
    ],
  },
  {
    id: "StickyAdsTV",
    platform: "all",
    name: "StickyAdsTV",
    bug: "1717806",
    matches: [
      {
        patterns: ["https://ads.stickyadstv.com/firefox-etp"],
        target: "tracking-pixel.png",
        types: ["image", "imageset", "xmlhttprequest"],
        onlyIfBlockedByETP: true,
      },
      {
        patterns: [
          "*://ads.stickyadstv.com/auto-user-sync*",
          "*://ads.stickyadstv.com/user-matching*",
        ],
        target: "https://ads.stickyadstv.com/firefox-etp",
        types: ["image", "imageset", "xmlhttprequest"],
        onlyIfBlockedByETP: true,
      },
    ],
  },
  {
    id: "Vidible",
    branch: ["nightly"],
    platform: "all",
    name: "Vidible",
    bug: "1713710",
    file: "vidible.js",
    logos: ["play.svg"],
    matches: [
      "*://*.vidible.tv/*/vidible-min.js*",
      "*://vdb-cdn-files.s3.amazonaws.com/*/vidible-min.js*",
    ],
    needsShimHelpers: ["optIn"],
    onlyIfBlockedByETP: true,
    unblocksOnOptIn: [
      "*://delivery.vidible.tv/jsonp/pid=*/vid=*/*.js*",
      "*://delivery.vidible.tv/placement/*",
      "*://img.vidible.tv/prod/*",
      "*://cdn-ssl.vidible.tv/prod/player/js/*.js",
      "*://hlsrv.vidible.tv/prod/*.m3u8*",
      "*://videos.vidible.tv/prod/*.key*",
      "*://videos.vidible.tv/prod/*.mp4*",
      "*://videos.vidible.tv/prod/*.webm*",
      "*://videos.vidible.tv/prod/*.ts*",
    ],
  },
  {
    id: "Kinja",
    platform: "all",
    name: "Kinja",
    bug: "1656171",
    contentScripts: [
      {
        js: "kinja.js",
        matches: [
          "*://www.avclub.com/*",
          "*://deadspin.com/*",
          "*://gizmodo.com/*",
          "*://jalopnik.com/*",
          "*://jezebel.com/*",
          "*://kotaku.com/*",
          "*://lifehacker.com/*",
          "*://www.theonion.com/*",
          "*://www.theroot.com/*",
          "*://thetakeout.com/*",
          "*://theinventory.com/*",
        ],
        runAt: "document_start",
        allFrames: true,
      },
    ],
    onlyIfDFPIActive: true,
  },
  {
    id: "MicrosoftLogin",
    platform: "desktop",
    name: "Microsoft Login",
    bug: "1638383",
    requestStorageAccessForRedirect: [
      ["*://web.powerva.microsoft.com/*", "*://login.microsoftonline.com/*"],
      ["*://teams.microsoft.com/*", "*://login.microsoftonline.com/*"],
      ["*://*.teams.microsoft.us/*", "*://login.microsoftonline.us/*"],
    ],
    contentScripts: [
      {
        js: "microsoftLogin.js",
        matches: [
          "*://web.powerva.microsoft.com/*",
          "*://teams.microsoft.com/*",
          "*://*.teams.microsoft.us/*",
        ],
        runAt: "document_start",
      },
    ],
    onlyIfDFPIActive: true,
  },
  {
    id: "MicrosoftVirtualAssistant",
    platform: "all",
    name: "Microsoft Virtual Assistant",
    bug: "1801277",
    contentScripts: [
      {
        js: "microsoftVirtualAssistant.js",
        matches: ["*://publisher.liveperson.net/*"],
        runAt: "document_start",
        allFrames: true,
      },
    ],
  },
  {
    id: "History",
    platform: "all",
    name: "History.com",
    bug: "1624853",
    contentScripts: [
      {
        js: "history.js",
        matches: ["*://play.history.com/*"],
        runAt: "document_start",
      },
    ],
    onlyIfDFPIActive: true,
  },
  {
    id: "Crave.ca",
    platform: "all",
    name: "Crave.ca",
    bug: "1746439",
    contentScripts: [
      {
        js: "crave-ca.js",
        matches: ["*://account.bellmedia.ca/login*"],
        runAt: "document_start",
      },
    ],
    onlyIfDFPIActive: true,
  },
  {
    id: "Instagram.com",
    platform: "android",
    name: "Instagram.com",
    bug: "1804445",
    contentScripts: [
      {
        js: "instagram.js",
        matches: ["*://www.instagram.com/*"],
        runAt: "document_start",
      },
    ],
    onlyIfDFPIActive: true,
  },
  {
    id: "MaxMindGeoIP",
    platform: "all",
    name: "MaxMind GeoIP",
    bug: "1754389",
    file: "maxmind-geoip.js",
    matches: ["*://js.maxmind.com/js/apis/geoip2/*/geoip2.js"],
    onlyIfBlockedByETP: true,
  },
  {
    id: "WebTrends",
    platform: "all",
    name: "WebTrends",
    bug: "1766414",
    file: "webtrends.js",
    matches: [
      "*://s.webtrends.com/js/advancedLinkTracking.js",
      "*://s.webtrends.com/js/webtrends.js",
      "*://s.webtrends.com/js/webtrends.min.js",
    ],
    onlyIfBlockedByETP: true,
  },
  {
    id: "Blogger",
    platform: "all",
    name: "Blogger",
    bug: "1776869",
    contentScripts: [
      {
        js: "blogger.js",
        matches: ["*://www.blogger.com/comment/frame/*"],
        runAt: "document_start",
        allFrames: true,
      },
      {
        js: "bloggerAccount.js",
        matches: ["*://www.blogger.com/blog/*"],
        runAt: "document_end",
      },
    ],
    onlyIfDFPIActive: true,
  },
  {
    // keep this below any other shims checking adsafeprotected URLs
    id: "AdSafeProtectedTrackingPixels",
    platform: "all",
    name: "Ad Safe Protected tracking pixels",
    bug: "1717806",
    matches: [
      {
        patterns: ["https://static.adsafeprotected.com/firefox-etp-pixel"],
        target: "tracking-pixel.png",
        types: ["image", "imageset", "xmlhttprequest"],
      },
      {
        patterns: ["https://static.adsafeprotected.com/firefox-etp-js"],
        target: "empty-script.js",
        types: ["xmlhttprequest"],
      },
      {
        patterns: [
          "*://*.adsafeprotected.com/*.gif*",
          "*://*.adsafeprotected.com/*.png*",
        ],
        target: "https://static.adsafeprotected.com/firefox-etp-pixel",
        types: ["image", "imageset", "xmlhttprequest"],
        onlyIfBlockedByETP: true,
      },
      {
        patterns: [
          "*://*.adsafeprotected.com/*.js*",
          "*://*.adsafeprotected.com/*/adj*",
          "*://*.adsafeprotected.com/*/imp/*",
          "*://*.adsafeprotected.com/*/Serving/*",
          "*://*.adsafeprotected.com/*/unit/*",
          "*://*.adsafeprotected.com/jload",
          "*://*.adsafeprotected.com/jload?*",
          "*://*.adsafeprotected.com/jsvid",
          "*://*.adsafeprotected.com/jsvid?*",
          "*://*.adsafeprotected.com/mon*",
          "*://*.adsafeprotected.com/tpl",
          "*://*.adsafeprotected.com/tpl?*",
          "*://*.adsafeprotected.com/services/pub*",
        ],
        target: "https://static.adsafeprotected.com/firefox-etp-js",
        types: ["image", "imageset", "xmlhttprequest"],
        onlyIfBlockedByETP: true,
      },
      {
        // note, fallback case seems to be an image
        patterns: ["*://*.adsafeprotected.com/*"],
        target: "https://static.adsafeprotected.com/firefox-etp-pixel",
        types: ["image", "imageset", "xmlhttprequest"],
        onlyIfBlockedByETP: true,
      },
    ],
  },
  {
    id: "SpotifyEmbed",
    platform: "all",
    name: "SpotifyEmbed",
    bug: "1792395",
    contentScripts: [
      {
        js: "spotify-embed.js",
        matches: ["*://open.spotify.com/embed/*"],
        runAt: "document_start",
        allFrames: true,
      },
    ],
    onlyIfDFPIActive: true,
  },
];

module.exports = AVAILABLE_SHIMS;
