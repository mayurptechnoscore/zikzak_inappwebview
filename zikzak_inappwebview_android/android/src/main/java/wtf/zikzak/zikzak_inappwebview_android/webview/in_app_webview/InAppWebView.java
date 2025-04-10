package wtf.zikzak.zikzak_inappwebview_android.webview.in_app_webview;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static wtf.zikzak.zikzak_inappwebview_android.types.PreferredContentModeOptionType.fromValue;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebHistoryItem;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import wtf.zikzak.zikzak_inappwebview_android.InAppWebViewFlutterPlugin;
import wtf.zikzak.zikzak_inappwebview_android.R;
import wtf.zikzak.zikzak_inappwebview_android.Util;
import wtf.zikzak.zikzak_inappwebview_android.content_blocker.ContentBlocker;
import wtf.zikzak.zikzak_inappwebview_android.content_blocker.ContentBlockerAction;
import wtf.zikzak.zikzak_inappwebview_android.content_blocker.ContentBlockerHandler;
import wtf.zikzak.zikzak_inappwebview_android.content_blocker.ContentBlockerTrigger;
import wtf.zikzak.zikzak_inappwebview_android.find_interaction.FindInteractionController;
import wtf.zikzak.zikzak_inappwebview_android.in_app_browser.InAppBrowserDelegate;
import wtf.zikzak.zikzak_inappwebview_android.plugin_scripts_js.ConsoleLogJS;
import wtf.zikzak.zikzak_inappwebview_android.plugin_scripts_js.InterceptAjaxRequestJS;
import wtf.zikzak.zikzak_inappwebview_android.plugin_scripts_js.InterceptFetchRequestJS;
import wtf.zikzak.zikzak_inappwebview_android.plugin_scripts_js.JavaScriptBridgeJS;
import wtf.zikzak.zikzak_inappwebview_android.plugin_scripts_js.OnLoadResourceJS;
import wtf.zikzak.zikzak_inappwebview_android.plugin_scripts_js.OnWindowBlurEventJS;
import wtf.zikzak.zikzak_inappwebview_android.plugin_scripts_js.OnWindowFocusEventJS;
import wtf.zikzak.zikzak_inappwebview_android.plugin_scripts_js.PluginScriptsUtil;
import wtf.zikzak.zikzak_inappwebview_android.plugin_scripts_js.PrintJS;
import wtf.zikzak.zikzak_inappwebview_android.plugin_scripts_js.PromisePolyfillJS;
import wtf.zikzak.zikzak_inappwebview_android.print_job.PrintJobController;
import wtf.zikzak.zikzak_inappwebview_android.print_job.PrintJobSettings;
import wtf.zikzak.zikzak_inappwebview_android.pull_to_refresh.PullToRefreshLayout;
import wtf.zikzak.zikzak_inappwebview_android.types.ContentWorld;
import wtf.zikzak.zikzak_inappwebview_android.types.DownloadStartRequest;
import wtf.zikzak.zikzak_inappwebview_android.types.PluginScript;
import wtf.zikzak.zikzak_inappwebview_android.types.PreferredContentModeOptionType;
import wtf.zikzak.zikzak_inappwebview_android.types.URLRequest;
import wtf.zikzak.zikzak_inappwebview_android.types.UserContentController;
import wtf.zikzak.zikzak_inappwebview_android.types.UserScript;
import wtf.zikzak.zikzak_inappwebview_android.types.WebViewAssetLoaderExt;
import wtf.zikzak.zikzak_inappwebview_android.webview.ContextMenuSettings;
import wtf.zikzak.zikzak_inappwebview_android.webview.InAppWebViewInterface;
import wtf.zikzak.zikzak_inappwebview_android.webview.JavaScriptBridgeInterface;
import wtf.zikzak.zikzak_inappwebview_android.webview.WebViewChannelDelegate;
import wtf.zikzak.zikzak_inappwebview_android.webview.web_message.WebMessageChannel;
import wtf.zikzak.zikzak_inappwebview_android.webview.web_message.WebMessageListener;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import io.flutter.plugin.common.MethodChannel;

final public class InAppWebView extends InputAwareWebView implements InAppWebViewInterface {
  protected static final String LOG_TAG = "InAppWebView";
  public static final String METHOD_CHANNEL_NAME_PREFIX = "wtf.zikzak/zikzak_inappwebview_";

  @Nullable
  public InAppWebViewFlutterPlugin plugin;
  @Nullable
  public InAppBrowserDelegate inAppBrowserDelegate;
  public Object id;
  @Nullable
  public Integer windowId;
  @Nullable
  public InAppWebViewClient inAppWebViewClient;
  @Nullable
  public InAppWebViewClientCompat inAppWebViewClientCompat;
  @Nullable
  public InAppWebViewChromeClient inAppWebViewChromeClient;
  @Nullable
  public InAppWebViewRenderProcessClient inAppWebViewRenderProcessClient;
  @Nullable
  public WebViewChannelDelegate channelDelegate;
  @Nullable
  public JavaScriptBridgeInterface javaScriptBridgeInterface;
  public InAppWebViewSettings customSettings = new InAppWebViewSettings();
  public boolean isLoading = false;
  private boolean inFullscreen = false;
  public float zoomScale = 1.0f;
  public ContentBlockerHandler contentBlockerHandler = new ContentBlockerHandler();
  public Pattern regexToCancelSubFramesLoadingCompiled;
  @Nullable
  public GestureDetector gestureDetector = null;
  @Nullable
  public LinearLayout floatingContextMenu = null;
  @Nullable
  public Map<String, Object> contextMenu = null;
  public Handler mainLooperHandler = new Handler(getWebViewLooper());
  static Handler mHandler = new Handler();

  public Runnable checkScrollStoppedTask;
  public int initialPositionScrollStoppedTask;
  public int newCheckScrollStoppedTask = 100; // ms

  public Runnable checkContextMenuShouldBeClosedTask;
  public int newCheckContextMenuShouldBeClosedTaskTask = 100; // ms

  public UserContentController userContentController = new UserContentController(this);

  public Map<String, ValueCallback<String>> callAsyncJavaScriptCallbacks = new HashMap<>();
  public Map<String, ValueCallback<String>> evaluateJavaScriptContentWorldCallbacks = new HashMap<>();

  public Map<String, WebMessageChannel> webMessageChannels = new HashMap<>();
  public List<WebMessageListener> webMessageListeners = new ArrayList<>();

  private List<UserScript> initialUserOnlyScripts = new ArrayList<>();

  @Nullable
  public FindInteractionController findInteractionController;

  @Nullable
  public WebViewAssetLoaderExt webViewAssetLoaderExt;

  @Nullable
  private PluginScript interceptOnlyAsyncAjaxRequestsPluginScript;

  public InAppWebView(Context context) {
    super(context);
  }

  public InAppWebView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public InAppWebView(Context context, AttributeSet attrs, int defaultStyle) {
    super(context, attrs, defaultStyle);
  }

  public InAppWebView(Context context, @NonNull InAppWebViewFlutterPlugin plugin,
                      @NonNull Object id, @Nullable Integer windowId, InAppWebViewSettings customSettings,
                      @Nullable Map<String, Object> contextMenu, View containerView,
                      List<UserScript> userScripts) {
    super(context, containerView, customSettings.useHybridComposition);
    this.plugin = plugin;
    this.id = id;
    final MethodChannel channel = new MethodChannel(plugin.messenger, METHOD_CHANNEL_NAME_PREFIX + id);
    this.channelDelegate = new WebViewChannelDelegate(this, channel);
    this.windowId = windowId;
    this.customSettings = customSettings;
    this.contextMenu = contextMenu;
    this.initialUserOnlyScripts = userScripts;
    if (plugin != null && plugin.activity != null) {
      plugin.activity.registerForContextMenu(this);
    }
  }

  public WebViewClient createWebViewClient(InAppBrowserDelegate inAppBrowserDelegate) {
    // bug https://bugs.chromium.org/p/chromium/issues/detail?id=925887
    PackageInfo packageInfo = WebViewCompat.getCurrentWebViewPackage(getContext());
    if (packageInfo == null) {
      Log.d(LOG_TAG, "Using InAppWebViewClient implementation");
      return new InAppWebViewClient(inAppBrowserDelegate);
    }

    boolean isChromiumWebView = "com.android.webview".equals(packageInfo.packageName) ||
                                "com.google.android.webview".equals(packageInfo.packageName) ||
                                "com.android.chrome".equals(packageInfo.packageName);
    boolean isChromiumWebViewBugFixed = false;
    if (isChromiumWebView) {
      String versionName = packageInfo.versionName != null ? packageInfo.versionName : "";
      try {
        int majorVersion = versionName.contains(".") ?
                Integer.parseInt(versionName.split("\\.")[0]) : 0;
        isChromiumWebViewBugFixed = majorVersion >= 73;
      } catch (NumberFormatException ignored) {}
    }

    if (isChromiumWebViewBugFixed || !isChromiumWebView) {
      Log.d(LOG_TAG, "Using InAppWebViewClientCompat implementation");
      return new InAppWebViewClientCompat(inAppBrowserDelegate);
    } else {
      Log.d(LOG_TAG, "Using InAppWebViewClient implementation");
      return new InAppWebViewClient(inAppBrowserDelegate);
    }
  }

  @SuppressLint("RestrictedApi")
  public void prepare() {
      WebSettings settings = getSettings();
      
      // Core settings
      settings.setJavaScriptEnabled(customSettings.javaScriptEnabled);
      settings.setDomStorageEnabled(customSettings.domStorageEnabled);
      
      // Safe Browsing
      if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
          WebSettingsCompat.setSafeBrowsingEnabled(settings, customSettings.safeBrowsingEnabled);
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          settings.setSafeBrowsingEnabled(customSettings.safeBrowsingEnabled);
      }
      
      // Modern WebView features
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
              WebSettingsCompat.setForceDark(settings, customSettings.forceDark);
          }
          if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
              WebSettingsCompat.setForceDarkStrategy(settings, customSettings.forceDarkStrategy);
          }
          if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
              WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, customSettings.algorithmicDarkeningAllowed);
          }
      }
      
      // Enterprise and security features
      if (WebViewFeature.isFeatureSupported(WebViewFeature.ENTERPRISE_AUTHENTICATION_APP_LINK_POLICY)) {
          WebSettingsCompat.setEnterpriseAuthenticationAppLinkPolicyEnabled(settings, customSettings.enterpriseAuthenticationAppLinkPolicyEnabled);
      }
      
      // Content settings
      settings.setLoadWithOverviewMode(customSettings.loadWithOverviewMode);
      settings.setUseWideViewPort(customSettings.useWideViewPort);
      settings.setSupportZoom(customSettings.supportZoom);
      settings.setDisplayZoomControls(customSettings.displayZoomControls);
      
      // Clear cache if needed
      if (customSettings.clearCache) {
          clearAllCache();
      }
      
      if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE))
        WebSettingsCompat.setSafeBrowsingEnabled(settings, customSettings.safeBrowsingEnabled);
      else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        settings.setSafeBrowsingEnabled(customSettings.safeBrowsingEnabled);

      settings.setMediaPlaybackRequiresUserGesture(customSettings.mediaPlaybackRequiresUserGesture);

      settings.setDatabaseEnabled(customSettings.databaseEnabled);
      settings.setDomStorageEnabled(customSettings.domStorageEnabled);

      if (customSettings.userAgent != null && !customSettings.userAgent.isEmpty())
        settings.setUserAgentString(customSettings.userAgent);
      else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
        settings.setUserAgentString(WebSettings.getDefaultUserAgent(getContext()));

      if (customSettings.applicationNameForUserAgent != null && !customSettings.applicationNameForUserAgent.isEmpty()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
          String userAgent = (customSettings.userAgent != null && !customSettings.userAgent.isEmpty()) ? customSettings.userAgent : WebSettings.getDefaultUserAgent(getContext());
          String userAgentWithApplicationName = userAgent + " " + customSettings.applicationNameForUserAgent;
          settings.setUserAgentString(userAgentWithApplicationName);
        }
      }

      if (customSettings.clearCache)
        clearAllCache();
      else if (customSettings.clearSessionCache)
        CookieManager.getInstance().removeSessionCookie();

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, customSettings.thirdPartyCookiesEnabled);

      settings.setLoadWithOverviewMode(customSettings.loadWithOverviewMode);
      settings.setUseWideViewPort(customSettings.useWideViewPort);
      settings.setSupportZoom(customSettings.supportZoom);
      settings.setTextZoom(customSettings.textZoom);

      setVerticalScrollBarEnabled(!customSettings.disableVerticalScroll && customSettings.verticalScrollBarEnabled);
      setHorizontalScrollBarEnabled(!customSettings.disableHorizontalScroll && customSettings.horizontalScrollBarEnabled);

      if (customSettings.transparentBackground)
        setBackgroundColor(Color.TRANSPARENT);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && customSettings.mixedContentMode != null)
        settings.setMixedContentMode(customSettings.mixedContentMode);

      settings.setAllowContentAccess(customSettings.allowContentAccess);
      settings.setAllowFileAccess(customSettings.allowFileAccess);
      settings.setAllowFileAccessFromFileURLs(customSettings.allowFileAccessFromFileURLs);
      settings.setAllowUniversalAccessFromFileURLs(customSettings.allowUniversalAccessFromFileURLs);
      setCacheEnabled(customSettings.cacheEnabled);
      if (customSettings.appCachePath != null && !customSettings.appCachePath.isEmpty() && customSettings.cacheEnabled) {
        // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
        // settings.setAppCachePath(customSettings.appCachePath);
        Util.invokeMethodIfExists(settings, "setAppCachePath", customSettings.appCachePath);
      }
      settings.setBlockNetworkImage(customSettings.blockNetworkImage);
      settings.setBlockNetworkLoads(customSettings.blockNetworkLoads);
      if (customSettings.cacheMode != null)
        settings.setCacheMode(customSettings.cacheMode);
      settings.setCursiveFontFamily(customSettings.cursiveFontFamily);
      settings.setDefaultFixedFontSize(customSettings.defaultFixedFontSize);
      settings.setDefaultFontSize(customSettings.defaultFontSize);
      settings.setDefaultTextEncodingName(customSettings.defaultTextEncodingName);
      if (customSettings.disabledActionModeMenuItems != null) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DISABLED_ACTION_MODE_MENU_ITEMS))
          WebSettingsCompat.setDisabledActionModeMenuItems(settings, customSettings.disabledActionModeMenuItems);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
          settings.setDisabledActionModeMenuItems(customSettings.disabledActionModeMenuItems);
      }
      settings.setFantasyFontFamily(customSettings.fantasyFontFamily);
      settings.setFixedFontFamily(customSettings.fixedFontFamily);
      if (customSettings.forceDark != null) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK))
          WebSettingsCompat.setForceDark(settings, customSettings.forceDark);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
          settings.setForceDark(customSettings.forceDark);
      }
      if (customSettings.forceDarkStrategy != null && WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
        WebSettingsCompat.setForceDarkStrategy(settings, customSettings.forceDarkStrategy);
      }
      settings.setGeolocationEnabled(customSettings.geolocationEnabled);
      if (customSettings.layoutAlgorithm != null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && customSettings.layoutAlgorithm.equals(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING)) {
          settings.setLayoutAlgorithm(customSettings.layoutAlgorithm);
        } else {
          settings.setLayoutAlgorithm(customSettings.layoutAlgorithm);
        }
      }
      settings.setLoadsImagesAutomatically(customSettings.loadsImagesAutomatically);
      settings.setMinimumFontSize(customSettings.minimumFontSize);
      settings.setMinimumLogicalFontSize(customSettings.minimumLogicalFontSize);
      setInitialScale(customSettings.initialScale);
      settings.setNeedInitialFocus(customSettings.needInitialFocus);
      if (WebViewFeature.isFeatureSupported(WebViewFeature.OFF_SCREEN_PRERASTER))
        WebSettingsCompat.setOffscreenPreRaster(settings, customSettings.offscreenPreRaster);
      else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        settings.setOffscreenPreRaster(customSettings.offscreenPreRaster);
      settings.setSansSerifFontFamily(customSettings.sansSerifFontFamily);
      settings.setSerifFontFamily(customSettings.serifFontFamily);
      settings.setStandardFontFamily(customSettings.standardFontFamily);
      if (customSettings.preferredContentMode != null &&
              customSettings.preferredContentMode == PreferredContentModeOptionType.DESKTOP.toValue()) {
        setDesktopMode(true);
      }
      settings.setSaveFormData(customSettings.saveFormData);
      if (customSettings.incognito)
        setIncognito(true);
      if (customSettings.useHybridComposition) {
        if (customSettings.hardwareAcceleration)
          setLayerType(View.LAYER_TYPE_HARDWARE, null);
        else
          setLayerType(View.LAYER_TYPE_NONE, null);
      }
      if (customSettings.regexToCancelSubFramesLoading != null) {
        regexToCancelSubFramesLoadingCompiled = Pattern.compile(customSettings.regexToCancelSubFramesLoading);
      }
      setScrollBarStyle(customSettings.scrollBarStyle);
      if (customSettings.scrollBarDefaultDelayBeforeFade != null) {
        setScrollBarDefaultDelayBeforeFade(customSettings.scrollBarDefaultDelayBeforeFade);
      } else {
        customSettings.scrollBarDefaultDelayBeforeFade = getScrollBarDefaultDelayBeforeFade();
      }
      setScrollbarFadingEnabled(customSettings.scrollbarFadingEnabled);
      if (customSettings.scrollBarFadeDuration != null) {
        setScrollBarFadeDuration(customSettings.scrollBarFadeDuration);
      } else {
        customSettings.scrollBarFadeDuration = getScrollBarFadeDuration();
      }
      setVerticalScrollbarPosition(customSettings.verticalScrollbarPosition);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        if (customSettings.verticalScrollbarThumbColor != null)
          setVerticalScrollbarThumbDrawable(new ColorDrawable(Color.parseColor(customSettings.verticalScrollbarThumbColor)));
        if (customSettings.verticalScrollbarTrackColor != null)
          setVerticalScrollbarTrackDrawable(new ColorDrawable(Color.parseColor(customSettings.verticalScrollbarTrackColor)));
        if (customSettings.horizontalScrollbarThumbColor != null)
          setHorizontalScrollbarThumbDrawable(new ColorDrawable(Color.parseColor(customSettings.horizontalScrollbarThumbColor)));
        if (customSettings.horizontalScrollbarTrackColor != null)
          setHorizontalScrollbarTrackDrawable(new ColorDrawable(Color.parseColor(customSettings.horizontalScrollbarTrackColor)));
      }

      setOverScrollMode(customSettings.overScrollMode);
      if (customSettings.networkAvailable != null) {
        setNetworkAvailable(customSettings.networkAvailable);
      }
      if (customSettings.rendererPriorityPolicy != null && (customSettings.rendererPriorityPolicy.get("rendererRequestedPriority") != null || customSettings.rendererPriorityPolicy.get("waivedWhenNotVisible") != null) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        setRendererPriorityPolicy(
                (int) customSettings.rendererPriorityPolicy.get("rendererRequestedPriority"),
                (boolean) customSettings.rendererPriorityPolicy.get("waivedWhenNotVisible"));
      }

      if (WebViewFeature.isFeatureSupported(WebViewFeature.SUPPRESS_ERROR_PAGE)) {
        WebSettingsCompat.setWillSuppressErrorPage(settings, customSettings.disableDefaultErrorPage);
      }
      if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, customSettings.algorithmicDarkeningAllowed);
      }
      if (WebViewFeature.isFeatureSupported(WebViewFeature.ENTERPRISE_AUTHENTICATION_APP_LINK_POLICY)) {
        WebSettingsCompat.setEnterpriseAuthenticationAppLinkPolicyEnabled(settings, customSettings.enterpriseAuthenticationAppLinkPolicyEnabled);
      }
      if (customSettings.requestedWithHeaderOriginAllowList != null &&
              WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
        WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, customSettings.requestedWithHeaderOriginAllowList);
      }

      contentBlockerHandler.getRuleList().clear();
      for (Map<String, Map<String, Object>> contentBlocker : customSettings.contentBlockers) {
        // compile ContentBlockerTrigger urlFilter
        ContentBlockerTrigger trigger = ContentBlockerTrigger.fromMap(contentBlocker.get("trigger"));
        ContentBlockerAction action = ContentBlockerAction.fromMap(contentBlocker.get("action"));
        contentBlockerHandler.getRuleList().add(new ContentBlocker(trigger, action));
      }

      setFindListener(new FindListener() {
        @Override
        public void onFindResultReceived(int activeMatchOrdinal, int numberOfMatches, boolean isDoneCounting) {
          if (findInteractionController != null && findInteractionController.channelDelegate != null)
            findInteractionController.channelDelegate.onFindResultReceived(activeMatchOrdinal, numberOfMatches, isDoneCounting);
          if (channelDelegate != null)
            channelDelegate.onFindResultReceived(activeMatchOrdinal, numberOfMatches, isDoneCounting);
        }
      });

      gestureDetector = new GestureDetector(this.getContext(), new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent ev) {
          if (floatingContextMenu != null) {
            hideContextMenu();
          }
          return super.onSingleTapUp(ev);
        }
      });

      checkScrollStoppedTask = new Runnable() {
        @Override
        public void run() {
          int newPosition = getScrollY();
          if (initialPositionScrollStoppedTask - newPosition == 0) {
            // has stopped
            onScrollStopped();
          } else {
            initialPositionScrollStoppedTask = getScrollY();
            mainLooperHandler.postDelayed(checkScrollStoppedTask, newCheckScrollStoppedTask);
          }
        }
      };

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && !customSettings.useHybridComposition) {
        checkContextMenuShouldBeClosedTask = new Runnable() {
          @Override
          public void run() {
            if (floatingContextMenu != null) {
              evaluateJavascript(PluginScriptsUtil.CHECK_CONTEXT_MENU_SHOULD_BE_HIDDEN_JS_SOURCE, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                  if (value == null || value.equals("true")) {
                    if (floatingContextMenu != null) {
                      hideContextMenu();
                    }
                  } else {
                    mainLooperHandler.postDelayed(checkContextMenuShouldBeClosedTask, newCheckContextMenuShouldBeClosedTaskTask);
                  }
                }
              });
            }
          }
        };
      }

      setOnTouchListener(new OnTouchListener() {
        float m_downX;
        float m_downY;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
          gestureDetector.onTouchEvent(event);

          if (event.getAction() == MotionEvent.ACTION_UP) {
            checkScrollStoppedTask.run();
          }

          if (customSettings.disableHorizontalScroll && customSettings.disableVerticalScroll) {
            return (event.getAction() == MotionEvent.ACTION_MOVE);
          } else if (customSettings.disableHorizontalScroll || customSettings.disableVerticalScroll) {
            switch (event.getAction()) {
              case MotionEvent.ACTION_DOWN: {
                // save the x
                m_downX = event.getX();
                // save the y
                m_downY = event.getY();
                break;
              }
              case MotionEvent.ACTION_MOVE:
              case MotionEvent.ACTION_CANCEL:
              case MotionEvent.ACTION_UP: {
                if (customSettings.disableHorizontalScroll) {
                  // set x so that it doesn't move
                  event.setLocation(m_downX, event.getY());
                } else {
                  // set y so that it doesn't move
                  event.setLocation(event.getX(), m_downY);
                }
                break;
              }
            }
          }
          return false;
        }
      });

      setOnLongClickListener(new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
          wtf.zikzak.zikzak_inappwebview_android.types.HitTestResult hitTestResult =
                  wtf.zikzak.zikzak_inappwebview_android.types.HitTestResult.fromWebViewHitTestResult(getHitTestResult());
          if (channelDelegate != null) channelDelegate.onLongPressHitTestResult(hitTestResult);
          return false;
        }
      });
    }

    public void prepareAndAddUserScripts() {
      userContentController.addPluginScript(PromisePolyfillJS.PROMISE_POLYFILL_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(JavaScriptBridgeJS.JAVASCRIPT_BRIDGE_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(ConsoleLogJS.CONSOLE_LOG_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(PrintJS.PRINT_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(OnWindowBlurEventJS.ON_WINDOW_BLUR_EVENT_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(OnWindowFocusEventJS.ON_WINDOW_FOCUS_EVENT_JS_PLUGIN_SCRIPT);
      interceptOnlyAsyncAjaxRequestsPluginScript = InterceptAjaxRequestJS.createInterceptOnlyAsyncAjaxRequestsPluginScript(customSettings.interceptOnlyAsyncAjaxRequests);
      if (customSettings.useShouldInterceptAjaxRequest) {
        userContentController.addPluginScript(interceptOnlyAsyncAjaxRequestsPluginScript);
        userContentController.addPluginScript(InterceptAjaxRequestJS.INTERCEPT_AJAX_REQUEST_JS_PLUGIN_SCRIPT);
      }
      if (customSettings.useShouldInterceptFetchRequest) {
        userContentController.addPluginScript(InterceptFetchRequestJS.INTERCEPT_FETCH_REQUEST_JS_PLUGIN_SCRIPT);
      }
      if (customSettings.useOnLoadResource) {
        userContentController.addPluginScript(OnLoadResourceJS.ON_LOAD_RESOURCE_JS_PLUGIN_SCRIPT);
      }
      if (!customSettings.useHybridComposition) {
        userContentController.addPluginScript(PluginScriptsUtil.CHECK_GLOBAL_KEY_DOWN_EVENT_TO_HIDE_CONTEXT_MENU_JS_PLUGIN_SCRIPT);
      }
      this.userContentController.addUserOnlyScripts(this.initialUserOnlyScripts);
    }

    public void setIncognito(boolean enabled) {
      WebSettings settings = getSettings();
      if (enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          CookieManager.getInstance().removeAllCookies(null);
        } else {
          CookieManager.getInstance().removeAllCookie();
        }

        // Disable caching
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
        // settings.setAppCacheEnabled(false);
        Util.invokeMethodIfExists(settings, "setAppCacheEnabled", false);

        clearHistory();
        clearCache(true);

        // No form data or autofill enabled
        clearFormData();
        settings.setSavePassword(false);
        settings.setSaveFormData(false);
      } else {
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
        // settings.setAppCacheEnabled(true);
        Util.invokeMethodIfExists(settings, "setAppCacheEnabled", true);

        settings.setSavePassword(true);
        settings.setSaveFormData(true);
      }
    }

    public void setCacheEnabled(boolean enabled) {
      WebSettings settings = getSettings();
      if (enabled) {
        Context ctx = getContext();
        if (ctx != null) {
          // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
          // settings.setAppCachePath(ctx.getCacheDir().getAbsolutePath());
          Util.invokeMethodIfExists(settings, "setAppCachePath", ctx.getCacheDir().getAbsolutePath());

          settings.setCacheMode(WebSettings.LOAD_DEFAULT);

          // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
          // settings.setAppCacheEnabled(true);
          Util.invokeMethodIfExists(settings, "setAppCacheEnabled", true);
        }
      } else {
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
        // settings.setAppCacheEnabled(false);
        Util.invokeMethodIfExists(settings, "setAppCacheEnabled", false);
      }
    }

    public void loadUrl(URLRequest urlRequest) {
      String url = urlRequest.getUrl();
      String method = urlRequest.getMethod();
      if (method != null && method.equals("POST")) {
        byte[] postData = urlRequest.getBody();
        postUrl(url, postData);
        return;
      }
      Map<String, String> headers = urlRequest.getHeaders();
      if (headers != null) {
        loadUrl(url, headers);
        return;
      }
      loadUrl(url);
    }

    public void loadFile(String assetFilePath) throws IOException {
      if (plugin == null) {
        return;
      }

      loadUrl(Util.getUrlAsset(plugin, assetFilePath));
    }

    public boolean isLoading() {
      return isLoading;
    }

    /**
     * @deprecated
     */
    @Deprecated
    private void clearCookies() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        CookieManager.getInstance().removeAllCookies(new ValueCallback<Boolean>() {
          @Override
          public void onReceiveValue(Boolean aBoolean) {

          }
        });
      } else {
        CookieManager.getInstance().removeAllCookie();
      }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void clearAllCache() {
      clearCache(true);
      clearCookies();
      clearFormData();
      WebStorage.getInstance().deleteAllData();
    }

    public void takeScreenshot(final @Nullable Map<String, Object> screenshotConfiguration, final MethodChannel.Result result) {
      final float pixelDensity = Util.getPixelDensity(getContext());

      mainLooperHandler.post(new Runnable() {
        @Override
        public void run() {
          try {
            Bitmap screenshotBitmap = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(screenshotBitmap);
            c.translate(-getScrollX(), -getScrollY());
            draw(c);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.PNG;
            int quality = 100;

            if (screenshotConfiguration != null) {
              Map<String, Double> rect = (Map<String, Double>) screenshotConfiguration.get("rect");
              if (rect != null) {
                int rectX = (int) Math.floor(rect.get("x") * pixelDensity + 0.5);
                int rectY = (int) Math.floor(rect.get("y") * pixelDensity + 0.5);
                int rectWidth = Math.min(screenshotBitmap.getWidth(), (int) Math.floor(rect.get("width") * pixelDensity + 0.5));
                int rectHeight = Math.min(screenshotBitmap.getHeight(), (int) Math.floor(rect.get("height") * pixelDensity + 0.5));
                screenshotBitmap = Bitmap.createBitmap(
                        screenshotBitmap,
                        rectX,
                        rectY,
                        rectWidth,
                        rectHeight);
              }

              Double snapshotWidth = (Double) screenshotConfiguration.get("snapshotWidth");
              if (snapshotWidth != null) {
                int dstWidth = (int) Math.floor(snapshotWidth * pixelDensity + 0.5);
                float ratioBitmap = (float) screenshotBitmap.getWidth() / (float) screenshotBitmap.getHeight();
                int dstHeight = (int) ((float) dstWidth / ratioBitmap);
                screenshotBitmap = Bitmap.createScaledBitmap(screenshotBitmap, dstWidth, dstHeight, true);
              }

              try {
                compressFormat = Bitmap.CompressFormat.valueOf((String) screenshotConfiguration.get("compressFormat"));
              } catch (IllegalArgumentException e) {
                Log.e(LOG_TAG, "", e);
              }

              quality = (Integer) screenshotConfiguration.get("quality");
            }

            screenshotBitmap.compress(
                    compressFormat,
                    quality,
                    byteArrayOutputStream);

            try {
              byteArrayOutputStream.close();
            } catch (IOException e) {
              Log.e(LOG_TAG, "", e);
            }
            screenshotBitmap.recycle();
            result.success(byteArrayOutputStream.toByteArray());

          } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "", e);
            result.success(null);
          }
        }
      });
    }

    @SuppressLint("RestrictedApi")
    public void setSettings(InAppWebViewSettings newCustomSettings, HashMap<String, Object> newSettingsMap) {

      WebSettings settings = getSettings();

      if (newSettingsMap.get("javaScriptEnabled") != null && customSettings.javaScriptEnabled != newCustomSettings.javaScriptEnabled)
        settings.setJavaScriptEnabled(newCustomSettings.javaScriptEnabled);

      if (newSettingsMap.get("useShouldInterceptAjaxRequest") != null && customSettings.useShouldInterceptAjaxRequest != newCustomSettings.useShouldInterceptAjaxRequest) {
        enablePluginScriptAtRuntime(
                InterceptAjaxRequestJS.FLAG_VARIABLE_FOR_SHOULD_INTERCEPT_AJAX_REQUEST_JS_SOURCE,
                newCustomSettings.useShouldInterceptAjaxRequest,
                InterceptAjaxRequestJS.INTERCEPT_AJAX_REQUEST_JS_PLUGIN_SCRIPT
        );
      }

      if (newSettingsMap.get("interceptOnlyAsyncAjaxRequests") != null && customSettings.interceptOnlyAsyncAjaxRequests != newCustomSettings.interceptOnlyAsyncAjaxRequests) {
        enablePluginScriptAtRuntime(
                InterceptAjaxRequestJS.FLAG_VARIABLE_FOR_INTERCEPT_ONLY_ASYNC_AJAX_REQUESTS_JS_SOURCE,
                newCustomSettings.interceptOnlyAsyncAjaxRequests,
                interceptOnlyAsyncAjaxRequestsPluginScript
        );
      }

      if (newSettingsMap.get("useShouldInterceptFetchRequest") != null && customSettings.useShouldInterceptFetchRequest != newCustomSettings.useShouldInterceptFetchRequest) {
        enablePluginScriptAtRuntime(
                InterceptFetchRequestJS.FLAG_VARIABLE_FOR_SHOULD_INTERCEPT_FETCH_REQUEST_JS_SOURCE,
                newCustomSettings.useShouldInterceptFetchRequest,
                InterceptFetchRequestJS.INTERCEPT_FETCH_REQUEST_JS_PLUGIN_SCRIPT
        );
      }

      if (newSettingsMap.get("useOnLoadResource") != null && customSettings.useOnLoadResource != newCustomSettings.useOnLoadResource) {
        enablePluginScriptAtRuntime(
                OnLoadResourceJS.FLAG_VARIABLE_FOR_ON_LOAD_RESOURCE_JS_SOURCE,
                newCustomSettings.useOnLoadResource,
                OnLoadResourceJS.ON_LOAD_RESOURCE_JS_PLUGIN_SCRIPT
        );
      }

      if (newSettingsMap.get("javaScriptCanOpenWindowsAutomatically") != null && customSettings.javaScriptCanOpenWindowsAutomatically != newCustomSettings.javaScriptCanOpenWindowsAutomatically)
        settings.setJavaScriptCanOpenWindowsAutomatically(newCustomSettings.javaScriptCanOpenWindowsAutomatically);

      if (newSettingsMap.get("builtInZoomControls") != null && customSettings.builtInZoomControls != newCustomSettings.builtInZoomControls)
        settings.setBuiltInZoomControls(newCustomSettings.builtInZoomControls);

      if (newSettingsMap.get("displayZoomControls") != null && customSettings.displayZoomControls != newCustomSettings.displayZoomControls)
        settings.setDisplayZoomControls(newCustomSettings.displayZoomControls);

      if (newSettingsMap.get("safeBrowsingEnabled") != null && customSettings.safeBrowsingEnabled != newCustomSettings.safeBrowsingEnabled) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE))
          WebSettingsCompat.setSafeBrowsingEnabled(settings, newCustomSettings.safeBrowsingEnabled);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
          settings.setSafeBrowsingEnabled(newCustomSettings.safeBrowsingEnabled);
      }

      if (newSettingsMap.get("mediaPlaybackRequiresUserGesture") != null && customSettings.mediaPlaybackRequiresUserGesture != newCustomSettings.mediaPlaybackRequiresUserGesture)
        settings.setMediaPlaybackRequiresUserGesture(newCustomSettings.mediaPlaybackRequiresUserGesture);

      if (newSettingsMap.get("databaseEnabled") != null && customSettings.databaseEnabled != newCustomSettings.databaseEnabled)
        settings.setDatabaseEnabled(newCustomSettings.databaseEnabled);

      if (newSettingsMap.get("domStorageEnabled") != null && customSettings.domStorageEnabled != newCustomSettings.domStorageEnabled)
        settings.setDomStorageEnabled(newCustomSettings.domStorageEnabled);

      if (newSettingsMap.get("userAgent") != null && !customSettings.userAgent.equals(newCustomSettings.userAgent) && !newCustomSettings.userAgent.isEmpty())
        settings.setUserAgentString(newCustomSettings.userAgent);

      if (newSettingsMap.get("applicationNameForUserAgent") != null && !customSettings.applicationNameForUserAgent.equals(newCustomSettings.applicationNameForUserAgent) && !newCustomSettings.applicationNameForUserAgent.isEmpty()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
          String userAgent = (newCustomSettings.userAgent != null && !newCustomSettings.userAgent.isEmpty()) ? newCustomSettings.userAgent : WebSettings.getDefaultUserAgent(getContext());
          String userAgentWithApplicationName = userAgent + " " + customSettings.applicationNameForUserAgent;
          settings.setUserAgentString(userAgentWithApplicationName);
        }
      }

      if (newSettingsMap.get("clearCache") != null && newCustomSettings.clearCache)
        clearAllCache();
      else if (newSettingsMap.get("clearSessionCache") != null && newCustomSettings.clearSessionCache)
        CookieManager.getInstance().removeSessionCookie();

      if (newSettingsMap.get("thirdPartyCookiesEnabled") != null && customSettings.thirdPartyCookiesEnabled != newCustomSettings.thirdPartyCookiesEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, newCustomSettings.thirdPartyCookiesEnabled);

      if (newSettingsMap.get("useWideViewPort") != null && customSettings.useWideViewPort != newCustomSettings.useWideViewPort)
        settings.setUseWideViewPort(newCustomSettings.useWideViewPort);

      if (newSettingsMap.get("supportZoom") != null && customSettings.supportZoom != newCustomSettings.supportZoom)
        settings.setSupportZoom(newCustomSettings.supportZoom);

      if (newSettingsMap.get("textZoom") != null && !customSettings.textZoom.equals(newCustomSettings.textZoom))
        settings.setTextZoom(newCustomSettings.textZoom);

      if (newSettingsMap.get("verticalScrollBarEnabled") != null && customSettings.verticalScrollBarEnabled != newCustomSettings.verticalScrollBarEnabled)
        setVerticalScrollBarEnabled(newCustomSettings.verticalScrollBarEnabled);

      if (newSettingsMap.get("horizontalScrollBarEnabled") != null && customSettings.horizontalScrollBarEnabled != newCustomSettings.horizontalScrollBarEnabled)
        setHorizontalScrollBarEnabled(newCustomSettings.horizontalScrollBarEnabled);

      if (newSettingsMap.get("transparentBackground") != null && customSettings.transparentBackground != newCustomSettings.transparentBackground) {
        if (newCustomSettings.transparentBackground) {
          setBackgroundColor(Color.TRANSPARENT);
        } else {
          setBackgroundColor(Color.parseColor("#FFFFFF"));
        }
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        if (newSettingsMap.get("mixedContentMode") != null && (customSettings.mixedContentMode == null || !customSettings.mixedContentMode.equals(newCustomSettings.mixedContentMode)))
          settings.setMixedContentMode(newCustomSettings.mixedContentMode);

      if (newSettingsMap.get("supportMultipleWindows") != null && customSettings.supportMultipleWindows != newCustomSettings.supportMultipleWindows)
        settings.setSupportMultipleWindows(newCustomSettings.supportMultipleWindows);

      if (newSettingsMap.get("useOnDownloadStart") != null && customSettings.useOnDownloadStart != newCustomSettings.useOnDownloadStart) {
        if (newCustomSettings.useOnDownloadStart) {
          setDownloadListener(new DownloadStartListener());
        } else {
          setDownloadListener(null);
        }
      }

      if (newSettingsMap.get("allowContentAccess") != null && customSettings.allowContentAccess != newCustomSettings.allowContentAccess)
        settings.setAllowContentAccess(newCustomSettings.allowContentAccess);

      if (newSettingsMap.get("allowFileAccess") != null && customSettings.allowFileAccess != newCustomSettings.allowFileAccess)
        settings.setAllowFileAccess(newCustomSettings.allowFileAccess);

      if (newSettingsMap.get("allowFileAccessFromFileURLs") != null && customSettings.allowFileAccessFromFileURLs != newCustomSettings.allowFileAccessFromFileURLs)
        settings.setAllowFileAccessFromFileURLs(newCustomSettings.allowFileAccessFromFileURLs);

      if (newSettingsMap.get("allowUniversalAccessFromFileURLs") != null && customSettings.allowUniversalAccessFromFileURLs != newCustomSettings.allowUniversalAccessFromFileURLs)
        settings.setAllowUniversalAccessFromFileURLs(newCustomSettings.allowUniversalAccessFromFileURLs);

      if (newSettingsMap.get("cacheEnabled") != null && customSettings.cacheEnabled != newCustomSettings.cacheEnabled)
        setCacheEnabled(newCustomSettings.cacheEnabled);

      if (newSettingsMap.get("appCachePath") != null && (customSettings.appCachePath == null || !customSettings.appCachePath.equals(newCustomSettings.appCachePath))) {
        // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
        // settings.setAppCachePath(newCustomSettings.appCachePath);
        Util.invokeMethodIfExists(settings, "setAppCachePath", newCustomSettings.appCachePath);
      }

      if (newSettingsMap.get("blockNetworkImage") != null && customSettings.blockNetworkImage != newCustomSettings.blockNetworkImage)
        settings.setBlockNetworkImage(newCustomSettings.blockNetworkImage);

      if (newSettingsMap.get("blockNetworkLoads") != null && customSettings.blockNetworkLoads != newCustomSettings.blockNetworkLoads)
        settings.setBlockNetworkLoads(newCustomSettings.blockNetworkLoads);

      if (newSettingsMap.get("cacheMode") != null && !customSettings.cacheMode.equals(newCustomSettings.cacheMode))
        settings.setCacheMode(newCustomSettings.cacheMode);

      if (newSettingsMap.get("cursiveFontFamily") != null && !customSettings.cursiveFontFamily.equals(newCustomSettings.cursiveFontFamily))
        settings.setCursiveFontFamily(newCustomSettings.cursiveFontFamily);

      if (newSettingsMap.get("defaultFixedFontSize") != null && !customSettings.defaultFixedFontSize.equals(newCustomSettings.defaultFixedFontSize))
        settings.setDefaultFixedFontSize(newCustomSettings.defaultFixedFontSize);

      if (newSettingsMap.get("defaultFontSize") != null && !customSettings.defaultFontSize.equals(newCustomSettings.defaultFontSize))
        settings.setDefaultFontSize(newCustomSettings.defaultFontSize);

      if (newSettingsMap.get("defaultTextEncodingName") != null && !customSettings.defaultTextEncodingName.equals(newCustomSettings.defaultTextEncodingName))
        settings.setDefaultTextEncodingName(newCustomSettings.defaultTextEncodingName);

      if (newSettingsMap.get("disabledActionModeMenuItems") != null &&
              (customSettings.disabledActionModeMenuItems == null ||
              !customSettings.disabledActionModeMenuItems.equals(newCustomSettings.disabledActionModeMenuItems))) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DISABLED_ACTION_MODE_MENU_ITEMS))
          WebSettingsCompat.setDisabledActionModeMenuItems(settings, newCustomSettings.disabledActionModeMenuItems);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
          settings.setDisabledActionModeMenuItems(newCustomSettings.disabledActionModeMenuItems);
      }

      if (newSettingsMap.get("fantasyFontFamily") != null && !customSettings.fantasyFontFamily.equals(newCustomSettings.fantasyFontFamily))
        settings.setFantasyFontFamily(newCustomSettings.fantasyFontFamily);

      if (newSettingsMap.get("fixedFontFamily") != null && !customSettings.fixedFontFamily.equals(newCustomSettings.fixedFontFamily))
        settings.setFixedFontFamily(newCustomSettings.fixedFontFamily);

      if (newSettingsMap.get("forceDark") != null && !customSettings.forceDark.equals(newCustomSettings.forceDark)) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK))
          WebSettingsCompat.setForceDark(settings, newCustomSettings.forceDark);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
          settings.setForceDark(newCustomSettings.forceDark);
      }

      if (newSettingsMap.get("forceDarkStrategy") != null &&
              !customSettings.forceDarkStrategy.equals(newCustomSettings.forceDarkStrategy) &&
              WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
        WebSettingsCompat.setForceDarkStrategy(settings, newCustomSettings.forceDarkStrategy);
      }

      settings.setGeolocationEnabled(customSettings.geolocationEnabled);
      if (customSettings.layoutAlgorithm != null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && customSettings.layoutAlgorithm.equals(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING)) {
          settings.setLayoutAlgorithm(customSettings.layoutAlgorithm);
        } else {
          settings.setLayoutAlgorithm(customSettings.layoutAlgorithm);
        }
      }
      settings.setLoadsImagesAutomatically(customSettings.loadsImagesAutomatically);
      settings.setMinimumFontSize(customSettings.minimumFontSize);
      settings.setMinimumLogicalFontSize(customSettings.minimumLogicalFontSize);
      setInitialScale(customSettings.initialScale);
      settings.setNeedInitialFocus(customSettings.needInitialFocus);
      if (WebViewFeature.isFeatureSupported(WebViewFeature.OFF_SCREEN_PRERASTER))
        WebSettingsCompat.setOffscreenPreRaster(settings, customSettings.offscreenPreRaster);
      else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        settings.setOffscreenPreRaster(customSettings.offscreenPreRaster);
      settings.setSansSerifFontFamily(customSettings.sansSerifFontFamily);
      settings.setSerifFontFamily(customSettings.serifFontFamily);
      settings.setStandardFontFamily(customSettings.standardFontFamily);
      if (customSettings.preferredContentMode != null &&
              customSettings.preferredContentMode == PreferredContentModeOptionType.DESKTOP.toValue()) {
        setDesktopMode(true);
      }
      settings.setSaveFormData(customSettings.saveFormData);
      if (customSettings.incognito)
        setIncognito(true);
      if (customSettings.useHybridComposition) {
        if (customSettings.hardwareAcceleration)
          setLayerType(View.LAYER_TYPE_HARDWARE, null);
        else
          setLayerType(View.LAYER_TYPE_NONE, null);
      }
      if (customSettings.regexToCancelSubFramesLoading != null) {
        regexToCancelSubFramesLoadingCompiled = Pattern.compile(customSettings.regexToCancelSubFramesLoading);
      }
      setScrollBarStyle(customSettings.scrollBarStyle);
      if (customSettings.scrollBarDefaultDelayBeforeFade != null) {
        setScrollBarDefaultDelayBeforeFade(customSettings.scrollBarDefaultDelayBeforeFade);
      } else {
        customSettings.scrollBarDefaultDelayBeforeFade = getScrollBarDefaultDelayBeforeFade();
      }
      setScrollbarFadingEnabled(customSettings.scrollbarFadingEnabled);
      if (customSettings.scrollBarFadeDuration != null) {
        setScrollBarFadeDuration(customSettings.scrollBarFadeDuration);
      } else {
        customSettings.scrollBarFadeDuration = getScrollBarFadeDuration();
      }
      setVerticalScrollbarPosition(customSettings.verticalScrollbarPosition);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        if (customSettings.verticalScrollbarThumbColor != null)
          setVerticalScrollbarThumbDrawable(new ColorDrawable(Color.parseColor(customSettings.verticalScrollbarThumbColor)));
        if (customSettings.verticalScrollbarTrackColor != null)
          setVerticalScrollbarTrackDrawable(new ColorDrawable(Color.parseColor(customSettings.verticalScrollbarTrackColor)));
        if (customSettings.horizontalScrollbarThumbColor != null)
          setHorizontalScrollbarThumbDrawable(new ColorDrawable(Color.parseColor(customSettings.horizontalScrollbarThumbColor)));
        if (customSettings.horizontalScrollbarTrackColor != null)
          setHorizontalScrollbarTrackDrawable(new ColorDrawable(Color.parseColor(customSettings.horizontalScrollbarTrackColor)));
      }

      setOverScrollMode(customSettings.overScrollMode);
      if (customSettings.networkAvailable != null) {
        setNetworkAvailable(customSettings.networkAvailable);
      }
      if (customSettings.rendererPriorityPolicy != null && (customSettings.rendererPriorityPolicy.get("rendererRequestedPriority") != null || customSettings.rendererPriorityPolicy.get("waivedWhenNotVisible") != null) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        setRendererPriorityPolicy(
                (int) customSettings.rendererPriorityPolicy.get("rendererRequestedPriority"),
                (boolean) customSettings.rendererPriorityPolicy.get("waivedWhenNotVisible"));
      }

      if (WebViewFeature.isFeatureSupported(WebViewFeature.SUPPRESS_ERROR_PAGE)) {
        WebSettingsCompat.setWillSuppressErrorPage(settings, customSettings.disableDefaultErrorPage);
      }
      if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, customSettings.algorithmicDarkeningAllowed);
      }
      if (WebViewFeature.isFeatureSupported(WebViewFeature.ENTERPRISE_AUTHENTICATION_APP_LINK_POLICY)) {
        WebSettingsCompat.setEnterpriseAuthenticationAppLinkPolicyEnabled(settings, customSettings.enterpriseAuthenticationAppLinkPolicyEnabled);
      }
      if (customSettings.requestedWithHeaderOriginAllowList != null &&
              WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
        WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, customSettings.requestedWithHeaderOriginAllowList);
      }

      contentBlockerHandler.getRuleList().clear();
      for (Map<String, Map<String, Object>> contentBlocker : customSettings.contentBlockers) {
        // compile ContentBlockerTrigger urlFilter
        ContentBlockerTrigger trigger = ContentBlockerTrigger.fromMap(contentBlocker.get("trigger"));
        ContentBlockerAction action = ContentBlockerAction.fromMap(contentBlocker.get("action"));
        contentBlockerHandler.getRuleList().add(new ContentBlocker(trigger, action));
      }

      setFindListener(new FindListener() {
        @Override
        public void onFindResultReceived(int activeMatchOrdinal, int numberOfMatches, boolean isDoneCounting) {
          if (findInteractionController != null && findInteractionController.channelDelegate != null)
            findInteractionController.channelDelegate.onFindResultReceived(activeMatchOrdinal, numberOfMatches, isDoneCounting);
          if (channelDelegate != null)
            channelDelegate.onFindResultReceived(activeMatchOrdinal, numberOfMatches, isDoneCounting);
        }
      });

      gestureDetector = new GestureDetector(this.getContext(), new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent ev) {
          if (floatingContextMenu != null) {
            hideContextMenu();
          }
          return super.onSingleTapUp(ev);
        }
      });

      checkScrollStoppedTask = new Runnable() {
        @Override
        public void run() {
          int newPosition = getScrollY();
          if (initialPositionScrollStoppedTask - newPosition == 0) {
            // has stopped
            onScrollStopped();
          } else {
            initialPositionScrollStoppedTask = getScrollY();
            mainLooperHandler.postDelayed(checkScrollStoppedTask, newCheckScrollStoppedTask);
          }
        }
      };

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && !customSettings.useHybridComposition) {
        checkContextMenuShouldBeClosedTask = new Runnable() {
          @Override
          public void run() {
            if (floatingContextMenu != null) {
              evaluateJavascript(PluginScriptsUtil.CHECK_CONTEXT_MENU_SHOULD_BE_HIDDEN_JS_SOURCE, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                  if (value == null || value.equals("true")) {
                    if (floatingContextMenu != null) {
                      hideContextMenu();
                    }
                  } else {
                    mainLooperHandler.postDelayed(checkContextMenuShouldBeClosedTask, newCheckContextMenuShouldBeClosedTaskTask);
                  }
                }
              });
            }
          }
        };
      }

      setOnTouchListener(new OnTouchListener() {
        float m_downX;
        float m_downY;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
          gestureDetector.onTouchEvent(event);

          if (event.getAction() == MotionEvent.ACTION_UP) {
            checkScrollStoppedTask.run();
          }

          if (customSettings.disableHorizontalScroll && customSettings.disableVerticalScroll) {
            return (event.getAction() == MotionEvent.ACTION_MOVE);
          } else if (customSettings.disableHorizontalScroll || customSettings.disableVerticalScroll) {
            switch (event.getAction()) {
              case MotionEvent.ACTION_DOWN: {
                // save the x
                m_downX = event.getX();
                // save the y
                m_downY = event.getY();
                break;
              }
              case MotionEvent.ACTION_MOVE:
              case MotionEvent.ACTION_CANCEL:
              case MotionEvent.ACTION_UP: {
                if (customSettings.disableHorizontalScroll) {
                  // set x so that it doesn't move
                  event.setLocation(m_downX, event.getY());
                } else {
                  // set y so that it doesn't move
                  event.setLocation(event.getX(), m_downY);
                }
                break;
              }
            }
          }
          return false;
        }
      });

      setOnLongClickListener(new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
          wtf.zikzak.zikzak_inappwebview_android.types.HitTestResult hitTestResult =
                  wtf.zikzak.zikzak_inappwebview_android.types.HitTestResult.fromWebViewHitTestResult(getHitTestResult());
          if (channelDelegate != null) channelDelegate.onLongPressHitTestResult(hitTestResult);
          return false;
        }
      });
    }

    public void prepareAndAddUserScripts() {
      userContentController.addPluginScript(PromisePolyfillJS.PROMISE_POLYFILL_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(JavaScriptBridgeJS.JAVASCRIPT_BRIDGE_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(ConsoleLogJS.CONSOLE_LOG_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(PrintJS.PRINT_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(OnWindowBlurEventJS.ON_WINDOW_BLUR_EVENT_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(OnWindowFocusEventJS.ON_WINDOW_FOCUS_EVENT_JS_PLUGIN_SCRIPT);
      interceptOnlyAsyncAjaxRequestsPluginScript = InterceptAjaxRequestJS.createInterceptOnlyAsyncAjaxRequestsPluginScript(customSettings.interceptOnlyAsyncAjaxRequests);
      if (customSettings.useShouldInterceptAjaxRequest) {
        userContentController.addPluginScript(interceptOnlyAsyncAjaxRequestsPluginScript);
        userContentController.addPluginScript(InterceptAjaxRequestJS.INTERCEPT_AJAX_REQUEST_JS_PLUGIN_SCRIPT);
      }
      if (customSettings.useShouldInterceptFetchRequest) {
        userContentController.addPluginScript(InterceptFetchRequestJS.INTERCEPT_FETCH_REQUEST_JS_PLUGIN_SCRIPT);
      }
      if (customSettings.useOnLoadResource) {
        userContentController.addPluginScript(OnLoadResourceJS.ON_LOAD_RESOURCE_JS_PLUGIN_SCRIPT);
      }
      if (!customSettings.useHybridComposition) {
        userContentController.addPluginScript(PluginScriptsUtil.CHECK_GLOBAL_KEY_DOWN_EVENT_TO_HIDE_CONTEXT_MENU_JS_PLUGIN_SCRIPT);
      }
      this.userContentController.addUserOnlyScripts(this.initialUserOnlyScripts);
    }

    public void setIncognito(boolean enabled) {
      WebSettings settings = getSettings();
      if (enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          CookieManager.getInstance().removeAllCookies(null);
        } else {
          CookieManager.getInstance().removeAllCookie();
        }

        // Disable caching
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
        // settings.setAppCacheEnabled(false);
        Util.invokeMethodIfExists(settings, "setAppCacheEnabled", false);

        clearHistory();
        clearCache(true);

        // No form data or autofill enabled
        clearFormData();
        settings.setSavePassword(false);
        settings.setSaveFormData(false);
      } else {
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
        // settings.setAppCacheEnabled(true);
        Util.invokeMethodIfExists(settings, "setAppCacheEnabled", true);

        settings.setSavePassword(true);
        settings.setSaveFormData(true);
      }
    }

    public void setCacheEnabled(boolean enabled) {
      WebSettings settings = getSettings();
      if (enabled) {
        Context ctx = getContext();
        if (ctx != null) {
          // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
          // settings.setAppCachePath(ctx.getCacheDir().getAbsolutePath());
          Util.invokeMethodIfExists(settings, "setAppCachePath", ctx.getCacheDir().getAbsolutePath());

          settings.setCacheMode(WebSettings.LOAD_DEFAULT);

          // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
          // settings.setAppCacheEnabled(true);
          Util.invokeMethodIfExists(settings, "setAppCacheEnabled", true);
        }
      } else {
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
        // settings.setAppCacheEnabled(false);
        Util.invokeMethodIfExists(settings, "setAppCacheEnabled", false);
      }
    }

    public void loadUrl(URLRequest urlRequest) {
      String url = urlRequest.getUrl();
      String method = urlRequest.getMethod();
      if (method != null && method.equals("POST")) {
        byte[] postData = urlRequest.getBody();
        postUrl(url, postData);
        return;
      }
      Map<String, String> headers = urlRequest.getHeaders();
      if (headers != null) {
        loadUrl(url, headers);
        return;
      }
      loadUrl(url);
    }

    public void loadFile(String assetFilePath) throws IOException {
      if (plugin == null) {
        return;
      }

      loadUrl(Util.getUrlAsset(plugin, assetFilePath));
    }

    public boolean isLoading() {
      return isLoading;
    }

    /**
     * @deprecated
     */
    @Deprecated
    private void clearCookies() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        CookieManager.getInstance().removeAllCookies(new ValueCallback<Boolean>() {
          @Override
          public void onReceiveValue(Boolean aBoolean) {

          }
        });
      } else {
        CookieManager.getInstance().removeAllCookie();
      }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void clearAllCache() {
      clearCache(true);
      clearCookies();
      clearFormData();
      WebStorage.getInstance().deleteAllData();
    }

    public void takeScreenshot(final @Nullable Map<String, Object> screenshotConfiguration, final MethodChannel.Result result) {
      final float pixelDensity = Util.getPixelDensity(getContext());

      mainLooperHandler.post(new Runnable() {
        @Override
        public void run() {
          try {
            Bitmap screenshotBitmap = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(screenshotBitmap);
            c.translate(-getScrollX(), -getScrollY());
            draw(c);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.PNG;
            int quality = 100;

            if (screenshotConfiguration != null) {
              Map<String, Double> rect = (Map<String, Double>) screenshotConfiguration.get("rect");
              if (rect != null) {
                int rectX = (int) Math.floor(rect.get("x") * pixelDensity + 0.5);
                int rectY = (int) Math.floor(rect.get("y") * pixelDensity + 0.5);
                int rectWidth = Math.min(screenshotBitmap.getWidth(), (int) Math.floor(rect.get("width") * pixelDensity + 0.5));
                int rectHeight = Math.min(screenshotBitmap.getHeight(), (int) Math.floor(rect.get("height") * pixelDensity + 0.5));
                screenshotBitmap = Bitmap.createBitmap(
                        screenshotBitmap,
                        rectX,
                        rectY,
                        rectWidth,
                        rectHeight);
              }

              Double snapshotWidth = (Double) screenshotConfiguration.get("snapshotWidth");
              if (snapshotWidth != null) {
                int dstWidth = (int) Math.floor(snapshotWidth * pixelDensity + 0.5);
                float ratioBitmap = (float) screenshotBitmap.getWidth() / (float) screenshotBitmap.getHeight();
                int dstHeight = (int) ((float) dstWidth / ratioBitmap);
                screenshotBitmap = Bitmap.createScaledBitmap(screenshotBitmap, dstWidth, dstHeight, true);
              }

              try {
                compressFormat = Bitmap.CompressFormat.valueOf((String) screenshotConfiguration.get("compressFormat"));
              } catch (IllegalArgumentException e) {
                Log.e(LOG_TAG, "", e);
              }

              quality = (Integer) screenshotConfiguration.get("quality");
            }

            screenshotBitmap.compress(
                    compressFormat,
                    quality,
                    byteArrayOutputStream);

            try {
              byteArrayOutputStream.close();
            } catch (IOException e) {
              Log.e(LOG_TAG, "", e);
            }
            screenshotBitmap.recycle();
            result.success(byteArrayOutputStream.toByteArray());

          } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "", e);
            result.success(null);
          }
        }
      });
    }

    @SuppressLint("RestrictedApi")
    public void setSettings(InAppWebViewSettings newCustomSettings, HashMap<String, Object> newSettingsMap) {

      WebSettings settings = getSettings();

      if (newSettingsMap.get("javaScriptEnabled") != null && customSettings.javaScriptEnabled != newCustomSettings.javaScriptEnabled)
        settings.setJavaScriptEnabled(newCustomSettings.javaScriptEnabled);

      if (newSettingsMap.get("useShouldInterceptAjaxRequest") != null && customSettings.useShouldInterceptAjaxRequest != newCustomSettings.useShouldInterceptAjaxRequest) {
        enablePluginScriptAtRuntime(
                InterceptAjaxRequestJS.FLAG_VARIABLE_FOR_SHOULD_INTERCEPT_AJAX_REQUEST_JS_SOURCE,
                newCustomSettings.useShouldInterceptAjaxRequest,
                InterceptAjaxRequestJS.INTERCEPT_AJAX_REQUEST_JS_PLUGIN_SCRIPT
        );
      }

      if (newSettingsMap.get("interceptOnlyAsyncAjaxRequests") != null && customSettings.interceptOnlyAsyncAjaxRequests != newCustomSettings.interceptOnlyAsyncAjaxRequests) {
        enablePluginScriptAtRuntime(
                InterceptAjaxRequestJS.FLAG_VARIABLE_FOR_INTERCEPT_ONLY_ASYNC_AJAX_REQUESTS_JS_SOURCE,
                newCustomSettings.interceptOnlyAsyncAjaxRequests,
                interceptOnlyAsyncAjaxRequestsPluginScript
        );
      }

      if (newSettingsMap.get("useShouldInterceptFetchRequest") != null && customSettings.useShouldInterceptFetchRequest != newCustomSettings.useShouldInterceptFetchRequest) {
        enablePluginScriptAtRuntime(
                InterceptFetchRequestJS.FLAG_VARIABLE_FOR_SHOULD_INTERCEPT_FETCH_REQUEST_JS_SOURCE,
                newCustomSettings.useShouldInterceptFetchRequest,
                InterceptFetchRequestJS.INTERCEPT_FETCH_REQUEST_JS_PLUGIN_SCRIPT
        );
      }

      if (newSettingsMap.get("useOnLoadResource") != null && customSettings.useOnLoadResource != newCustomSettings.useOnLoadResource) {
        enablePluginScriptAtRuntime(
                OnLoadResourceJS.FLAG_VARIABLE_FOR_ON_LOAD_RESOURCE_JS_SOURCE,
                newCustomSettings.useOnLoadResource,
                OnLoadResourceJS.ON_LOAD_RESOURCE_JS_PLUGIN_SCRIPT
        );
      }

      if (newSettingsMap.get("javaScriptCanOpenWindowsAutomatically") != null && customSettings.javaScriptCanOpenWindowsAutomatically != newCustomSettings.javaScriptCanOpenWindowsAutomatically)
        settings.setJavaScriptCanOpenWindowsAutomatically(newCustomSettings.javaScriptCanOpenWindowsAutomatically);

      if (newSettingsMap.get("builtInZoomControls") != null && customSettings.builtInZoomControls != newCustomSettings.builtInZoomControls)
        settings.setBuiltInZoomControls(newCustomSettings.builtInZoomControls);

      if (newSettingsMap.get("displayZoomControls") != null && customSettings.displayZoomControls != newCustomSettings.displayZoomControls)
        settings.setDisplayZoomControls(newCustomSettings.displayZoomControls);

      if (newSettingsMap.get("safeBrowsingEnabled") != null && customSettings.safeBrowsingEnabled != newCustomSettings.safeBrowsingEnabled) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE))
          WebSettingsCompat.setSafeBrowsingEnabled(settings, newCustomSettings.safeBrowsingEnabled);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
          settings.setSafeBrowsingEnabled(newCustomSettings.safeBrowsingEnabled);
      }

      if (newSettingsMap.get("mediaPlaybackRequiresUserGesture") != null && customSettings.mediaPlaybackRequiresUserGesture != newCustomSettings.mediaPlaybackRequiresUserGesture)
        settings.setMediaPlaybackRequiresUserGesture(newCustomSettings.mediaPlaybackRequiresUserGesture);

      if (newSettingsMap.get("databaseEnabled") != null && customSettings.databaseEnabled != newCustomSettings.databaseEnabled)
        settings.setDatabaseEnabled(newCustomSettings.databaseEnabled);

      if (newSettingsMap.get("domStorageEnabled") != null && customSettings.domStorageEnabled != newCustomSettings.domStorageEnabled)
        settings.setDomStorageEnabled(newCustomSettings.domStorageEnabled);

      if (newSettingsMap.get("userAgent") != null && !customSettings.userAgent.equals(newCustomSettings.userAgent) && !newCustomSettings.userAgent.isEmpty())
        settings.setUserAgentString(newCustomSettings.userAgent);

      if (newSettingsMap.get("applicationNameForUserAgent") != null && !customSettings.applicationNameForUserAgent.equals(newCustomSettings.applicationNameForUserAgent) && !newCustomSettings.applicationNameForUserAgent.isEmpty()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
          String userAgent = (newCustomSettings.userAgent != null && !newCustomSettings.userAgent.isEmpty()) ? newCustomSettings.userAgent : WebSettings.getDefaultUserAgent(getContext());
          String userAgentWithApplicationName = userAgent + " " + customSettings.applicationNameForUserAgent;
          settings.setUserAgentString(userAgentWithApplicationName);
        }
      }

      if (newSettingsMap.get("clearCache") != null && newCustomSettings.clearCache)
        clearAllCache();
      else if (newSettingsMap.get("clearSessionCache") != null && newCustomSettings.clearSessionCache)
        CookieManager.getInstance().removeSessionCookie();

      if (newSettingsMap.get("thirdPartyCookiesEnabled") != null && customSettings.thirdPartyCookiesEnabled != newCustomSettings.thirdPartyCookiesEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, newCustomSettings.thirdPartyCookiesEnabled);

      if (newSettingsMap.get("useWideViewPort") != null && customSettings.useWideViewPort != newCustomSettings.useWideViewPort)
        settings.setUseWideViewPort(newCustomSettings.useWideViewPort);

      if (newSettingsMap.get("supportZoom") != null && customSettings.supportZoom != newCustomSettings.supportZoom)
        settings.setSupportZoom(newCustomSettings.supportZoom);

      if (newSettingsMap.get("textZoom") != null && !customSettings.textZoom.equals(newCustomSettings.textZoom))
        settings.setTextZoom(newCustomSettings.textZoom);

      if (newSettingsMap.get("verticalScrollBarEnabled") != null && customSettings.verticalScrollBarEnabled != newCustomSettings.verticalScrollBarEnabled)
        setVerticalScrollBarEnabled(newCustomSettings.verticalScrollBarEnabled);

      if (newSettingsMap.get("horizontalScrollBarEnabled") != null && customSettings.horizontalScrollBarEnabled != newCustomSettings.horizontalScrollBarEnabled)
        setHorizontalScrollBarEnabled(newCustomSettings.horizontalScrollBarEnabled);

      if (newSettingsMap.get("transparentBackground") != null && customSettings.transparentBackground != newCustomSettings.transparentBackground) {
        if (newCustomSettings.transparentBackground) {
          setBackgroundColor(Color.TRANSPARENT);
        } else {
          setBackgroundColor(Color.parseColor("#FFFFFF"));
        }
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        if (newSettingsMap.get("mixedContentMode") != null && (customSettings.mixedContentMode == null || !customSettings.mixedContentMode.equals(newCustomSettings.mixedContentMode)))
          settings.setMixedContentMode(newCustomSettings.mixedContentMode);

      if (newSettingsMap.get("supportMultipleWindows") != null && customSettings.supportMultipleWindows != newCustomSettings.supportMultipleWindows)
        settings.setSupportMultipleWindows(newCustomSettings.supportMultipleWindows);

      if (newSettingsMap.get("useOnDownloadStart") != null && customSettings.useOnDownloadStart != newCustomSettings.useOnDownloadStart) {
        if (newCustomSettings.useOnDownloadStart) {
          setDownloadListener(new DownloadStartListener());
        } else {
          setDownloadListener(null);
        }
      }

      if (newSettingsMap.get("allowContentAccess") != null && customSettings.allowContentAccess != newCustomSettings.allowContentAccess)
        settings.setAllowContentAccess(newCustomSettings.allowContentAccess);

      if (newSettingsMap.get("allowFileAccess") != null && customSettings.allowFileAccess != newCustomSettings.allowFileAccess)
        settings.setAllowFileAccess(newCustomSettings.allowFileAccess);

      if (newSettingsMap.get("allowFileAccessFromFileURLs") != null && customSettings.allowFileAccessFromFileURLs != newCustomSettings.allowFileAccessFromFileURLs)
        settings.setAllowFileAccessFromFileURLs(newCustomSettings.allowFileAccessFromFileURLs);

      if (newSettingsMap.get("allowUniversalAccessFromFileURLs") != null && customSettings.allowUniversalAccessFromFileURLs != newCustomSettings.allowUniversalAccessFromFileURLs)
        settings.setAllowUniversalAccessFromFileURLs(newCustomSettings.allowUniversalAccessFromFileURLs);

      if (newSettingsMap.get("cacheEnabled") != null && customSettings.cacheEnabled != newCustomSettings.cacheEnabled)
        setCacheEnabled(newCustomSettings.cacheEnabled);

      if (newSettingsMap.get("appCachePath") != null && (customSettings.appCachePath == null || !customSettings.appCachePath.equals(newCustomSettings.appCachePath))) {
        // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
        // settings.setAppCachePath(newCustomSettings.appCachePath);
        Util.invokeMethodIfExists(settings, "setAppCachePath", newCustomSettings.appCachePath);
      }

      if (newSettingsMap.get("blockNetworkImage") != null && customSettings.blockNetworkImage != newCustomSettings.blockNetworkImage)
        settings.setBlockNetworkImage(newCustomSettings.blockNetworkImage);

      if (newSettingsMap.get("blockNetworkLoads") != null && customSettings.blockNetworkLoads != newCustomSettings.blockNetworkLoads)
        settings.setBlockNetworkLoads(newCustomSettings.blockNetworkLoads);

      if (newSettingsMap.get("cacheMode") != null && !customSettings.cacheMode.equals(newCustomSettings.cacheMode))
        settings.setCacheMode(newCustomSettings.cacheMode);

      if (newSettingsMap.get("cursiveFontFamily") != null && !customSettings.cursiveFontFamily.equals(newCustomSettings.cursiveFontFamily))
        settings.setCursiveFontFamily(newCustomSettings.cursiveFontFamily);

      if (newSettingsMap.get("defaultFixedFontSize") != null && !customSettings.defaultFixedFontSize.equals(newCustomSettings.defaultFixedFontSize))
        settings.setDefaultFixedFontSize(newCustomSettings.defaultFixedFontSize);

      if (newSettingsMap.get("defaultFontSize") != null && !customSettings.defaultFontSize.equals(newCustomSettings.defaultFontSize))
        settings.setDefaultFontSize(newCustomSettings.defaultFontSize);

      if (newSettingsMap.get("defaultTextEncodingName") != null && !customSettings.defaultTextEncodingName.equals(newCustomSettings.defaultTextEncodingName))
        settings.setDefaultTextEncodingName(newCustomSettings.defaultTextEncodingName);

      if (newSettingsMap.get("disabledActionModeMenuItems") != null &&
              (customSettings.disabledActionModeMenuItems == null ||
              !customSettings.disabledActionModeMenuItems.equals(newCustomSettings.disabledActionModeMenuItems))) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DISABLED_ACTION_MODE_MENU_ITEMS))
          WebSettingsCompat.setDisabledActionModeMenuItems(settings, newCustomSettings.disabledActionModeMenuItems);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
          settings.setDisabledActionModeMenuItems(newCustomSettings.disabledActionModeMenuItems);
      }

      if (newSettingsMap.get("fantasyFontFamily") != null && !customSettings.fantasyFontFamily.equals(newCustomSettings.fantasyFontFamily))
        settings.setFantasyFontFamily(newCustomSettings.fantasyFontFamily);

      if (newSettingsMap.get("fixedFontFamily") != null && !customSettings.fixedFontFamily.equals(newCustomSettings.fixedFontFamily))
        settings.setFixedFontFamily(newCustomSettings.fixedFontFamily);

      if (newSettingsMap.get("forceDark") != null && !customSettings.forceDark.equals(newCustomSettings.forceDark)) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK))
          WebSettingsCompat.setForceDark(settings, newCustomSettings.forceDark);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
          settings.setForceDark(newCustomSettings.forceDark);
      }

      if (newSettingsMap.get("forceDarkStrategy") != null &&
              !customSettings.forceDarkStrategy.equals(newCustomSettings.forceDarkStrategy) &&
              WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
        WebSettingsCompat.setForceDarkStrategy(settings, newCustomSettings.forceDarkStrategy);
      }

      settings.setGeolocationEnabled(customSettings.geolocationEnabled);
      if (customSettings.layoutAlgorithm != null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && customSettings.layoutAlgorithm.equals(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING)) {
          settings.setLayoutAlgorithm(customSettings.layoutAlgorithm);
        } else {
          settings.setLayoutAlgorithm(customSettings.layoutAlgorithm);
        }
      }
      settings.setLoadsImagesAutomatically(customSettings.loadsImagesAutomatically);
      settings.setMinimumFontSize(customSettings.minimumFontSize);
      settings.setMinimumLogicalFontSize(customSettings.minimumLogicalFontSize);
      setInitialScale(customSettings.initialScale);
      settings.setNeedInitialFocus(customSettings.needInitialFocus);
      if (WebViewFeature.isFeatureSupported(WebViewFeature.OFF_SCREEN_PRERASTER))
        WebSettingsCompat.setOffscreenPreRaster(settings, customSettings.offscreenPreRaster);
      else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        settings.setOffscreenPreRaster(customSettings.offscreenPreRaster);
      settings.setSansSerifFontFamily(customSettings.sansSerifFontFamily);
      settings.setSerifFontFamily(customSettings.serifFontFamily);
      settings.setStandardFontFamily(customSettings.standardFontFamily);
      if (customSettings.preferredContentMode != null &&
              customSettings.preferredContentMode == PreferredContentModeOptionType.DESKTOP.toValue()) {
        setDesktopMode(true);
      }
      settings.setSaveFormData(customSettings.saveFormData);
      if (customSettings.incognito)
        setIncognito(true);
      if (customSettings.useHybridComposition) {
        if (customSettings.hardwareAcceleration)
          setLayerType(View.LAYER_TYPE_HARDWARE, null);
        else
          setLayerType(View.LAYER_TYPE_NONE, null);
      }
      if (customSettings.regexToCancelSubFramesLoading != null) {
        regexToCancelSubFramesLoadingCompiled = Pattern.compile(customSettings.regexToCancelSubFramesLoading);
      }
      setScrollBarStyle(customSettings.scrollBarStyle);
      if (customSettings.scrollBarDefaultDelayBeforeFade != null) {
        setScrollBarDefaultDelayBeforeFade(customSettings.scrollBarDefaultDelayBeforeFade);
      } else {
        customSettings.scrollBarDefaultDelayBeforeFade = getScrollBarDefaultDelayBeforeFade();
      }
      setScrollbarFadingEnabled(customSettings.scrollbarFadingEnabled);
      if (customSettings.scrollBarFadeDuration != null) {
        setScrollBarFadeDuration(customSettings.scrollBarFadeDuration);
      } else {
        customSettings.scrollBarFadeDuration = getScrollBarFadeDuration();
      }
      setVerticalScrollbarPosition(customSettings.verticalScrollbarPosition);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        if (customSettings.verticalScrollbarThumbColor != null)
          setVerticalScrollbarThumbDrawable(new ColorDrawable(Color.parseColor(customSettings.verticalScrollbarThumbColor)));
        if (customSettings.verticalScrollbarTrackColor != null)
          setVerticalScrollbarTrackDrawable(new ColorDrawable(Color.parseColor(customSettings.verticalScrollbarTrackColor)));
        if (customSettings.horizontalScrollbarThumbColor != null)
          setHorizontalScrollbarThumbDrawable(new ColorDrawable(Color.parseColor(customSettings.horizontalScrollbarThumbColor)));
        if (customSettings.horizontalScrollbarTrackColor != null)
          setHorizontalScrollbarTrackDrawable(new ColorDrawable(Color.parseColor(customSettings.horizontalScrollbarTrackColor)));
      }

      setOverScrollMode(customSettings.overScrollMode);
      if (customSettings.networkAvailable != null) {
        setNetworkAvailable(customSettings.networkAvailable);
      }
      if (customSettings.rendererPriorityPolicy != null && (customSettings.rendererPriorityPolicy.get("rendererRequestedPriority") != null || customSettings.rendererPriorityPolicy.get("waivedWhenNotVisible") != null) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        setRendererPriorityPolicy(
                (int) customSettings.rendererPriorityPolicy.get("rendererRequestedPriority"),
                (boolean) customSettings.rendererPriorityPolicy.get("waivedWhenNotVisible"));
      }

      if (WebViewFeature.isFeatureSupported(WebViewFeature.SUPPRESS_ERROR_PAGE)) {
        WebSettingsCompat.setWillSuppressErrorPage(settings, customSettings.disableDefaultErrorPage);
      }
      if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, customSettings.algorithmicDarkeningAllowed);
      }
      if (WebViewFeature.isFeatureSupported(WebViewFeature.ENTERPRISE_AUTHENTICATION_APP_LINK_POLICY)) {
        WebSettingsCompat.setEnterpriseAuthenticationAppLinkPolicyEnabled(settings, customSettings.enterpriseAuthenticationAppLinkPolicyEnabled);
      }
      if (customSettings.requestedWithHeaderOriginAllowList != null &&
              WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
        WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, customSettings.requestedWithHeaderOriginAllowList);
      }

      contentBlockerHandler.getRuleList().clear();
      for (Map<String, Map<String, Object>> contentBlocker : customSettings.contentBlockers) {
        // compile ContentBlockerTrigger urlFilter
        ContentBlockerTrigger trigger = ContentBlockerTrigger.fromMap(contentBlocker.get("trigger"));
        ContentBlockerAction action = ContentBlockerAction.fromMap(contentBlocker.get("action"));
        contentBlockerHandler.getRuleList().add(new ContentBlocker(trigger, action));
      }

      setFindListener(new FindListener() {
        @Override
        public void onFindResultReceived(int activeMatchOrdinal, int numberOfMatches, boolean isDoneCounting) {
          if (findInteractionController != null && findInteractionController.channelDelegate != null)
            findInteractionController.channelDelegate.onFindResultReceived(activeMatchOrdinal, numberOfMatches, isDoneCounting);
          if (channelDelegate != null)
            channelDelegate.onFindResultReceived(activeMatchOrdinal, numberOfMatches, isDoneCounting);
        }
      });

      gestureDetector = new GestureDetector(this.getContext(), new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent ev) {
          if (floatingContextMenu != null) {
            hideContextMenu();
          }
          return super.onSingleTapUp(ev);
        }
      });

      checkScrollStoppedTask = new Runnable() {
        @Override
        public void run() {
          int newPosition = getScrollY();
          if (initialPositionScrollStoppedTask - newPosition == 0) {
            // has stopped
            onScrollStopped();
          } else {
            initialPositionScrollStoppedTask = getScrollY();
            mainLooperHandler.postDelayed(checkScrollStoppedTask, newCheckScrollStoppedTask);
          }
        }
      };

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && !customSettings.useHybridComposition) {
        checkContextMenuShouldBeClosedTask = new Runnable() {
          @Override
          public void run() {
            if (floatingContextMenu != null) {
              evaluateJavascript(PluginScriptsUtil.CHECK_CONTEXT_MENU_SHOULD_BE_HIDDEN_JS_SOURCE, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                  if (value == null || value.equals("true")) {
                    if (floatingContextMenu != null) {
                      hideContextMenu();
                    }
                  } else {
                    mainLooperHandler.postDelayed(checkContextMenuShouldBeClosedTask, newCheckContextMenuShouldBeClosedTaskTask);
                  }
                }
              });
            }
          }
        };
      }

      setOnTouchListener(new OnTouchListener() {
        float m_downX;
        float m_downY;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
          gestureDetector.onTouchEvent(event);

          if (event.getAction() == MotionEvent.ACTION_UP) {
            checkScrollStoppedTask.run();
          }

          if (customSettings.disableHorizontalScroll && customSettings.disableVerticalScroll) {
            return (event.getAction() == MotionEvent.ACTION_MOVE);
          } else if (customSettings.disableHorizontalScroll || customSettings.disableVerticalScroll) {
            switch (event.getAction()) {
              case MotionEvent.ACTION_DOWN: {
                // save the x
                m_downX = event.getX();
                // save the y
                m_downY = event.getY();
                break;
              }
              case MotionEvent.ACTION_MOVE:
              case MotionEvent.ACTION_CANCEL:
              case MotionEvent.ACTION_UP: {
                if (customSettings.disableHorizontalScroll) {
                  // set x so that it doesn't move
                  event.setLocation(m_downX, event.getY());
                } else {
                  // set y so that it doesn't move
                  event.setLocation(event.getX(), m_downY);
                }
                break;
              }
            }
          }
          return false;
        }
      });

      setOnLongClickListener(new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
          wtf.zikzak.zikzak_inappwebview_android.types.HitTestResult hitTestResult =
                  wtf.zikzak.zikzak_inappwebview_android.types.HitTestResult.fromWebViewHitTestResult(getHitTestResult());
          if (channelDelegate != null) channelDelegate.onLongPressHitTestResult(hitTestResult);
          return false;
        }
      });
    }

    public void prepareAndAddUserScripts() {
      userContentController.addPluginScript(PromisePolyfillJS.PROMISE_POLYFILL_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(JavaScriptBridgeJS.JAVASCRIPT_BRIDGE_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(ConsoleLogJS.CONSOLE_LOG_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(PrintJS.PRINT_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(OnWindowBlurEventJS.ON_WINDOW_BLUR_EVENT_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(OnWindowFocusEventJS.ON_WINDOW_FOCUS_EVENT_JS_PLUGIN_SCRIPT);
      interceptOnlyAsyncAjaxRequestsPluginScript = InterceptAjaxRequestJS.createInterceptOnlyAsyncAjaxRequestsPluginScript(customSettings.interceptOnlyAsyncAjaxRequests);
      if (customSettings.useShouldInterceptAjaxRequest) {
        userContentController.addPluginScript(interceptOnlyAsyncAjaxRequestsPluginScript);
        userContentController.addPluginScript(InterceptAjaxRequestJS.INTERCEPT_AJAX_REQUEST_JS_PLUGIN_SCRIPT);
      }
      if (customSettings.useShouldInterceptFetchRequest) {
        userContentController.addPluginScript(InterceptFetchRequestJS.INTERCEPT_FETCH_REQUEST_JS_PLUGIN_SCRIPT);
      }
      if (customSettings.useOnLoadResource) {
        userContentController.addPluginScript(OnLoadResourceJS.ON_LOAD_RESOURCE_JS_PLUGIN_SCRIPT);
      }
      if (!customSettings.useHybridComposition) {
        userContentController.addPluginScript(PluginScriptsUtil.CHECK_GLOBAL_KEY_DOWN_EVENT_TO_HIDE_CONTEXT_MENU_JS_PLUGIN_SCRIPT);
      }
      this.userContentController.addUserOnlyScripts(this.initialUserOnlyScripts);
    }

    public void setIncognito(boolean enabled) {
      WebSettings settings = getSettings();
      if (enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          CookieManager.getInstance().removeAllCookies(null);
        } else {
          CookieManager.getInstance().removeAllCookie();
        }

        // Disable caching
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
        // settings.setAppCacheEnabled(false);
        Util.invokeMethodIfExists(settings, "setAppCacheEnabled", false);

        clearHistory();
        clearCache(true);

        // No form data or autofill enabled
        clearFormData();
        settings.setSavePassword(false);
        settings.setSaveFormData(false);
      } else {
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
        // settings.setAppCacheEnabled(true);
        Util.invokeMethodIfExists(settings, "setAppCacheEnabled", true);

        settings.setSavePassword(true);
        settings.setSaveFormData(true);
      }
    }

    public void setCacheEnabled(boolean enabled) {
      WebSettings settings = getSettings();
      if (enabled) {
        Context ctx = getContext();
        if (ctx != null) {
          // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
          // settings.setAppCachePath(ctx.getCacheDir().getAbsolutePath());
          Util.invokeMethodIfExists(settings, "setAppCachePath", ctx.getCacheDir().getAbsolutePath());

          settings.setCacheMode(WebSettings.LOAD_DEFAULT);

          // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
          // settings.setAppCacheEnabled(true);
          Util.invokeMethodIfExists(settings, "setAppCacheEnabled", true);
        }
      } else {
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
        // settings.setAppCacheEnabled(false);
        Util.invokeMethodIfExists(settings, "setAppCacheEnabled", false);
      }
    }

    public void loadUrl(URLRequest urlRequest) {
      String url = urlRequest.getUrl();
      String method = urlRequest.getMethod();
      if (method != null && method.equals("POST")) {
        byte[] postData = urlRequest.getBody();
        postUrl(url, postData);
        return;
      }
      Map<String, String> headers = urlRequest.getHeaders();
      if (headers != null) {
        loadUrl(url, headers);
        return;
      }
      loadUrl(url);
    }

    public void loadFile(String assetFilePath) throws IOException {
      if (plugin == null) {
        return;
      }

      loadUrl(Util.getUrlAsset(plugin, assetFilePath));
    }

    public boolean isLoading() {
      return isLoading;
    }

    /**
     * @deprecated
     */
    @Deprecated
    private void clearCookies() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        CookieManager.getInstance().removeAllCookies(new ValueCallback<Boolean>() {
          @Override
          public void onReceiveValue(Boolean aBoolean) {

          }
        });
      } else {
        CookieManager.getInstance().removeAllCookie();
      }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void clearAllCache() {
      clearCache(true);
      clearCookies();
      clearFormData();
      WebStorage.getInstance().deleteAllData();
    }

    public void takeScreenshot(final @Nullable Map<String, Object> screenshotConfiguration, final MethodChannel.Result result) {
      final float pixelDensity = Util.getPixelDensity(getContext());

      mainLooperHandler.post(new Runnable() {
        @Override
        public void run() {
          try {
            Bitmap screenshotBitmap = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(screenshotBitmap);
            c.translate(-getScrollX(), -getScrollY());
            draw(c);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.PNG;
            int quality = 100;

            if (screenshotConfiguration != null) {
              Map<String, Double> rect = (Map<String, Double>) screenshotConfiguration.get("rect");
              if (rect != null) {
                int rectX = (int) Math.floor(rect.get("x") * pixelDensity + 0.5);
                int rectY = (int) Math.floor(rect.get("y") * pixelDensity + 0.5);
                int rectWidth = Math.min(screenshotBitmap.getWidth(), (int) Math.floor(rect.get("width") * pixelDensity + 0.5));
                int rectHeight = Math.min(screenshotBitmap.getHeight(), (int) Math.floor(rect.get("height") * pixelDensity + 0.5));
                screenshotBitmap = Bitmap.createBitmap(
                        screenshotBitmap,
                        rectX,
                        rectY,
                        rectWidth,
                        rectHeight);
              }

              Double snapshotWidth = (Double) screenshotConfiguration.get("snapshotWidth");
              if (snapshotWidth != null) {
                int dstWidth = (int) Math.floor(snapshotWidth * pixelDensity + 0.5);
                float ratioBitmap = (float) screenshotBitmap.getWidth() / (float) screenshotBitmap.getHeight();
                int dstHeight = (int) ((float) dstWidth / ratioBitmap);
                screenshotBitmap = Bitmap.createScaledBitmap(screenshotBitmap, dstWidth, dstHeight, true);
              }

              try {
                compressFormat = Bitmap.CompressFormat.valueOf((String) screenshotConfiguration.get("compressFormat"));
              } catch (IllegalArgumentException e) {
                Log.e(LOG_TAG, "", e);
              }

              quality = (Integer) screenshotConfiguration.get("quality");
            }

            screenshotBitmap.compress(
                    compressFormat,
                    quality,
                    byteArrayOutputStream);

            try {
              byteArrayOutputStream.close();
            } catch (IOException e) {
              Log.e(LOG_TAG, "", e);
            }
            screenshotBitmap.recycle();
            result.success(byteArrayOutputStream.toByteArray());

          } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "", e);
            result.success(null);
          }
        }
      });
    }

    @SuppressLint("RestrictedApi")
    public void setSettings(InAppWebViewSettings newCustomSettings, HashMap<String, Object> newSettingsMap) {

      WebSettings settings = getSettings();

      if (newSettingsMap.get("javaScriptEnabled") != null && customSettings.javaScriptEnabled != newCustomSettings.javaScriptEnabled)
        settings.setJavaScriptEnabled(newCustomSettings.javaScriptEnabled);

      if (newSettingsMap.get("useShouldInterceptAjaxRequest") != null && customSettings.useShouldInterceptAjaxRequest != newCustomSettings.useShouldInterceptAjaxRequest) {
        enablePluginScriptAtRuntime(
                InterceptAjaxRequestJS.FLAG_VARIABLE_FOR_SHOULD_INTERCEPT_AJAX_REQUEST_JS_SOURCE,
                newCustomSettings.useShouldInterceptAjaxRequest,
                InterceptAjaxRequestJS.INTERCEPT_AJAX_REQUEST_JS_PLUGIN_SCRIPT
        );
      }

      if (newSettingsMap.get("interceptOnlyAsyncAjaxRequests") != null && customSettings.interceptOnlyAsyncAjaxRequests != newCustomSettings.interceptOnlyAsyncAjaxRequests) {
        enablePluginScriptAtRuntime(
                InterceptAjaxRequestJS.FLAG_VARIABLE_FOR_INTERCEPT_ONLY_ASYNC_AJAX_REQUESTS_JS_SOURCE,
                newCustomSettings.interceptOnlyAsyncAjaxRequests,
                interceptOnlyAsyncAjaxRequestsPluginScript
        );
      }

      if (newSettingsMap.get("useShouldInterceptFetchRequest") != null && customSettings.useShouldInterceptFetchRequest != newCustomSettings.useShouldInterceptFetchRequest) {
        enablePluginScriptAtRuntime(
                InterceptFetchRequestJS.FLAG_VARIABLE_FOR_SHOULD_INTERCEPT_FETCH_REQUEST_JS_SOURCE,
                newCustomSettings.useShouldInterceptFetchRequest,
                InterceptFetchRequestJS.INTERCEPT_FETCH_REQUEST_JS_PLUGIN_SCRIPT
        );
      }

      if (newSettingsMap.get("useOnLoadResource") != null && customSettings.useOnLoadResource != newCustomSettings.useOnLoadResource) {
        enablePluginScriptAtRuntime(
                OnLoadResourceJS.FLAG_VARIABLE_FOR_ON_LOAD_RESOURCE_JS_SOURCE,
                newCustomSettings.useOnLoadResource,
                OnLoadResourceJS.ON_LOAD_RESOURCE_JS_PLUGIN_SCRIPT
        );
      }

      if (newSettingsMap.get("javaScriptCanOpenWindowsAutomatically") != null && customSettings.javaScriptCanOpenWindowsAutomatically != newCustomSettings.javaScriptCanOpenWindowsAutomatically)
        settings.setJavaScriptCanOpenWindowsAutomatically(newCustomSettings.javaScriptCanOpenWindowsAutomatically);

      if (newSettingsMap.get("builtInZoomControls") != null && customSettings.builtInZoomControls != newCustomSettings.builtInZoomControls)
        settings.setBuiltInZoomControls(newCustomSettings.builtInZoomControls);

      if (newSettingsMap.get("displayZoomControls") != null && customSettings.displayZoomControls != newCustomSettings.displayZoomControls)
        settings.setDisplayZoomControls(newCustomSettings.displayZoomControls);

      if (newSettingsMap.get("safeBrowsingEnabled") != null && customSettings.safeBrowsingEnabled != newCustomSettings.safeBrowsingEnabled) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE))
          WebSettingsCompat.setSafeBrowsingEnabled(settings, newCustomSettings.safeBrowsingEnabled);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
          settings.setSafeBrowsingEnabled(newCustomSettings.safeBrowsingEnabled);
      }

      if (newSettingsMap.get("mediaPlaybackRequiresUserGesture") != null && customSettings.mediaPlaybackRequiresUserGesture != newCustomSettings.mediaPlaybackRequiresUserGesture)
        settings.setMediaPlaybackRequiresUserGesture(newCustomSettings.mediaPlaybackRequiresUserGesture);

      if (newSettingsMap.get("databaseEnabled") != null && customSettings.databaseEnabled != newCustomSettings.databaseEnabled)
        settings.setDatabaseEnabled(newCustomSettings.databaseEnabled);

      if (newSettingsMap.get("domStorageEnabled") != null && customSettings.domStorageEnabled != newCustomSettings.domStorageEnabled)
        settings.setDomStorageEnabled(newCustomSettings.domStorageEnabled);

      if (newSettingsMap.get("userAgent") != null && !customSettings.userAgent.equals(newCustomSettings.userAgent) && !newCustomSettings.userAgent.isEmpty())
        settings.setUserAgentString(newCustomSettings.userAgent);

      if (newSettingsMap.get("applicationNameForUserAgent") != null && !customSettings.applicationNameForUserAgent.equals(newCustomSettings.applicationNameForUserAgent) && !newCustomSettings.applicationNameForUserAgent.isEmpty()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
          String userAgent = (newCustomSettings.userAgent != null && !newCustomSettings.userAgent.isEmpty()) ? newCustomSettings.userAgent : WebSettings.getDefaultUserAgent(getContext());
          String userAgentWithApplicationName = userAgent + " " + customSettings.applicationNameForUserAgent;
          settings.setUserAgentString(userAgentWithApplicationName);
        }
      }

      if (newSettingsMap.get("clearCache") != null && newCustomSettings.clearCache)
        clearAllCache();
      else if (newSettingsMap.get("clearSessionCache") != null && newCustomSettings.clearSessionCache)
        CookieManager.getInstance().removeSessionCookie();

      if (newSettingsMap.get("thirdPartyCookiesEnabled") != null && customSettings.thirdPartyCookiesEnabled != newCustomSettings.thirdPartyCookiesEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, newCustomSettings.thirdPartyCookiesEnabled);

      if (newSettingsMap.get("useWideViewPort") != null && customSettings.useWideViewPort != newCustomSettings.useWideViewPort)
        settings.setUseWideViewPort(newCustomSettings.useWideViewPort);

      if (newSettingsMap.get("supportZoom") != null && customSettings.supportZoom != newCustomSettings.supportZoom)
        settings.setSupportZoom(newCustomSettings.supportZoom);

      if (newSettingsMap.get("textZoom") != null && !customSettings.textZoom.equals(newCustomSettings.textZoom))
        settings.setTextZoom(newCustomSettings.textZoom);

      if (newSettingsMap.get("verticalScrollBarEnabled") != null && customSettings.verticalScrollBarEnabled != newCustomSettings.verticalScrollBarEnabled)
        setVerticalScrollBarEnabled(newCustomSettings.verticalScrollBarEnabled);

      if (newSettingsMap.get("horizontalScrollBarEnabled") != null && customSettings.horizontalScrollBarEnabled != newCustomSettings.horizontalScrollBarEnabled)
        setHorizontalScrollBarEnabled(newCustomSettings.horizontalScrollBarEnabled);

      if (newSettingsMap.get("transparentBackground") != null && customSettings.transparentBackground != newCustomSettings.transparentBackground) {
        if (newCustomSettings.transparentBackground) {
          setBackgroundColor(Color.TRANSPARENT);
        } else {
          setBackgroundColor(Color.parseColor("#FFFFFF"));
        }
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        if (newSettingsMap.get("mixedContentMode") != null && (customSettings.mixedContentMode == null || !customSettings.mixedContentMode.equals(newCustomSettings.mixedContentMode)))
          settings.setMixedContentMode(newCustomSettings.mixedContentMode);

      if (newSettingsMap.get("supportMultipleWindows") != null && customSettings.supportMultipleWindows != newCustomSettings.supportMultipleWindows)
        settings.setSupportMultipleWindows(newCustomSettings.supportMultipleWindows);

      if (newSettingsMap.get("useOnDownloadStart") != null && customSettings.useOnDownloadStart != newCustomSettings.useOnDownloadStart) {
        if (newCustomSettings.useOnDownloadStart) {
          setDownloadListener(new DownloadStartListener());
        } else {
          setDownloadListener(null);
        }
      }

      if (newSettingsMap.get("allowContentAccess") != null && customSettings.allowContentAccess != newCustomSettings.allowContentAccess)
        settings.setAllowContentAccess(newCustomSettings.allowContentAccess);

      if (newSettingsMap.get("allowFileAccess") != null && customSettings.allowFileAccess != newCustomSettings.allowFileAccess)
        settings.setAllowFileAccess(newCustomSettings.allowFileAccess);

      if (newSettingsMap.get("allowFileAccessFromFileURLs") != null && customSettings.allowFileAccessFromFileURLs != newCustomSettings.allowFileAccessFromFileURLs)
        settings.setAllowFileAccessFromFileURLs(newCustomSettings.allowFileAccessFromFileURLs);

      if (newSettingsMap.get("allowUniversalAccessFromFileURLs") != null && customSettings.allowUniversalAccessFromFileURLs != newCustomSettings.allowUniversalAccessFromFileURLs)
        settings.setAllowUniversalAccessFromFileURLs(newCustomSettings.allowUniversalAccessFromFileURLs);

      if (newSettingsMap.get("cacheEnabled") != null && customSettings.cacheEnabled != newCustomSettings.cacheEnabled)
        setCacheEnabled(newCustomSettings.cacheEnabled);

      if (newSettingsMap.get("appCachePath") != null && (customSettings.appCachePath == null || !customSettings.appCachePath.equals(newCustomSettings.appCachePath))) {
        // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
        // settings.setAppCachePath(newCustomSettings.appCachePath);
        Util.invokeMethodIfExists(settings, "setAppCachePath", newCustomSettings.appCachePath);
      }

      if (newSettingsMap.get("blockNetworkImage") != null && customSettings.blockNetworkImage != newCustomSettings.blockNetworkImage)
        settings.setBlockNetworkImage(newCustomSettings.blockNetworkImage);

      if (newSettingsMap.get("blockNetworkLoads") != null && customSettings.blockNetworkLoads != newCustomSettings.blockNetworkLoads)
        settings.setBlockNetworkLoads(newCustomSettings.blockNetworkLoads);

      if (newSettingsMap.get("cacheMode") != null && !customSettings.cacheMode.equals(newCustomSettings.cacheMode))
        settings.setCacheMode(newCustomSettings.cacheMode);

      if (newSettingsMap.get("cursiveFontFamily") != null && !customSettings.cursiveFontFamily.equals(newCustomSettings.cursiveFontFamily))
        settings.setCursiveFontFamily(newCustomSettings.cursiveFontFamily);

      if (newSettingsMap.get("defaultFixedFontSize") != null && !customSettings.defaultFixedFontSize.equals(newCustomSettings.defaultFixedFontSize))
        settings.setDefaultFixedFontSize(newCustomSettings.defaultFixedFontSize);

      if (newSettingsMap.get("defaultFontSize") != null && !customSettings.defaultFontSize.equals(newCustomSettings.defaultFontSize))
        settings.setDefaultFontSize(newCustomSettings.defaultFontSize);

      if (newSettingsMap.get("defaultTextEncodingName") != null && !customSettings.defaultTextEncodingName.equals(newCustomSettings.defaultTextEncodingName))
        settings.setDefaultTextEncodingName(newCustomSettings.defaultTextEncodingName);

      if (newSettingsMap.get("disabledActionModeMenuItems") != null &&
              (customSettings.disabledActionModeMenuItems == null ||
              !customSettings.disabledActionModeMenuItems.equals(newCustomSettings.disabledActionModeMenuItems))) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DISABLED_ACTION_MODE_MENU_ITEMS))
          WebSettingsCompat.setDisabledActionModeMenuItems(settings, newCustomSettings.disabledActionModeMenuItems);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
          settings.setDisabledActionModeMenuItems(newCustomSettings.disabledActionModeMenuItems);
      }

      if (newSettingsMap.get("fantasyFontFamily") != null && !customSettings.fantasyFontFamily.equals(newCustomSettings.fantasyFontFamily))
        settings.setFantasyFontFamily(newCustomSettings.fantasyFontFamily);

      if (newSettingsMap.get("fixedFontFamily") != null && !customSettings.fixedFontFamily.equals(newCustomSettings.fixedFontFamily))
        settings.setFixedFontFamily(newCustomSettings.fixedFontFamily);

      if (newSettingsMap.get("forceDark") != null && !customSettings.forceDark.equals(newCustomSettings.forceDark)) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK))
          WebSettingsCompat.setForceDark(settings, newCustomSettings.forceDark);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
          settings.setForceDark(newCustomSettings.forceDark);
      }

      if (newSettingsMap.get("forceDarkStrategy") != null &&
              !customSettings.forceDarkStrategy.equals(newCustomSettings.forceDarkStrategy) &&
              WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
        WebSettingsCompat.setForceDarkStrategy(settings, newCustomSettings.forceDarkStrategy);
      }

      settings.setGeolocationEnabled(customSettings.geolocationEnabled);
      if (customSettings.layoutAlgorithm != null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && customSettings.layoutAlgorithm.equals(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING)) {
          settings.setLayoutAlgorithm(customSettings.layoutAlgorithm);
        } else {
          settings.setLayoutAlgorithm(customSettings.layoutAlgorithm);
        }
      }
      settings.setLoadsImagesAutomatically(customSettings.loadsImagesAutomatically);
      settings.setMinimumFontSize(customSettings.minimumFontSize);
      settings.setMinimumLogicalFontSize(customSettings.minimumLogicalFontSize);
      setInitialScale(customSettings.initialScale);
      settings.setNeedInitialFocus(customSettings.needInitialFocus);
      if (WebViewFeature.isFeatureSupported(WebViewFeature.OFF_SCREEN_PRERASTER))
        WebSettingsCompat.setOffscreenPreRaster(settings, customSettings.offscreenPreRaster);
      else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        settings.setOffscreenPreRaster(customSettings.offscreenPreRaster);
      settings.setSansSerifFontFamily(customSettings.sansSerifFontFamily);
      settings.setSerifFontFamily(customSettings.serifFontFamily);
      settings.setStandardFontFamily(customSettings.standardFontFamily);
      if (customSettings.preferredContentMode != null &&
              customSettings.preferredContentMode == PreferredContentModeOptionType.DESKTOP.toValue()) {
        setDesktopMode(true);
      }
      settings.setSaveFormData(customSettings.saveFormData);
      if (customSettings.incognito)
        setIncognito(true);
      if (customSettings.useHybridComposition) {
        if (customSettings.hardwareAcceleration)
          setLayerType(View.LAYER_TYPE_HARDWARE, null);
        else
          setLayerType(View.LAYER_TYPE_NONE, null);
      }
      if (customSettings.regexToCancelSubFramesLoading != null) {
        regexToCancelSubFramesLoadingCompiled = Pattern.compile(customSettings.regexToCancelSubFramesLoading);
      }
      setScrollBarStyle(customSettings.scrollBarStyle);
      if (customSettings.scrollBarDefaultDelayBeforeFade != null) {
        setScrollBarDefaultDelayBeforeFade(customSettings.scrollBarDefaultDelayBeforeFade);
      } else {
        customSettings.scrollBarDefaultDelayBeforeFade = getScrollBarDefaultDelayBeforeFade();
      }
      setScrollbarFadingEnabled(customSettings.scrollbarFadingEnabled);
      if (customSettings.scrollBarFadeDuration != null) {
        setScrollBarFadeDuration(customSettings.scrollBarFadeDuration);
      } else {
        customSettings.scrollBarFadeDuration = getScrollBarFadeDuration();
      }
      setVerticalScrollbarPosition(customSettings.verticalScrollbarPosition);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        if (customSettings.verticalScrollbarThumbColor != null)
          setVerticalScrollbarThumbDrawable(new ColorDrawable(Color.parseColor(customSettings.verticalScrollbarThumbColor)));
        if (customSettings.verticalScrollbarTrackColor != null)
          setVerticalScrollbarTrackDrawable(new ColorDrawable(Color.parseColor(customSettings.verticalScrollbarTrackColor)));
        if (customSettings.horizontalScrollbarThumbColor != null)
          setHorizontalScrollbarThumbDrawable(new ColorDrawable(Color.parseColor(customSettings.horizontalScrollbarThumbColor)));
        if (customSettings.horizontalScrollbarTrackColor != null)
          setHorizontalScrollbarTrackDrawable(new ColorDrawable(Color.parseColor(customSettings.horizontalScrollbarTrackColor)));
      }

      setOverScrollMode(customSettings.overScrollMode);
      if (customSettings.networkAvailable != null) {
        setNetworkAvailable(customSettings.networkAvailable);
      }
      if (customSettings.rendererPriorityPolicy != null && (customSettings.rendererPriorityPolicy.get("rendererRequestedPriority") != null || customSettings.rendererPriorityPolicy.get("waivedWhenNotVisible") != null) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        setRendererPriorityPolicy(
                (int) customSettings.rendererPriorityPolicy.get("rendererRequestedPriority"),
                (boolean) customSettings.rendererPriorityPolicy.get("waivedWhenNotVisible"));
      }

      if (WebViewFeature.isFeatureSupported(WebViewFeature.SUPPRESS_ERROR_PAGE)) {
        WebSettingsCompat.setWillSuppressErrorPage(settings, customSettings.disableDefaultErrorPage);
      }
      if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, customSettings.algorithmicDarkeningAllowed);
      }
      if (WebViewFeature.isFeatureSupported(WebViewFeature.ENTERPRISE_AUTHENTICATION_APP_LINK_POLICY)) {
        WebSettingsCompat.setEnterpriseAuthenticationAppLinkPolicyEnabled(settings, customSettings.enterpriseAuthenticationAppLinkPolicyEnabled);
      }
      if (customSettings.requestedWithHeaderOriginAllowList != null &&
              WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
        WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, customSettings.requestedWithHeaderOriginAllowList);
      }

      contentBlockerHandler.getRuleList().clear();
      for (Map<String, Map<String, Object>> contentBlocker : customSettings.contentBlockers) {
        // compile ContentBlockerTrigger urlFilter
        ContentBlockerTrigger trigger = ContentBlockerTrigger.fromMap(contentBlocker.get("trigger"));
        ContentBlockerAction action = ContentBlockerAction.fromMap(contentBlocker.get("action"));
        contentBlockerHandler.getRuleList().add(new ContentBlocker(trigger, action));
      }

      setFindListener(new FindListener() {
        @Override
        public void onFindResultReceived(int activeMatchOrdinal, int numberOfMatches, boolean isDoneCounting) {
          if (findInteractionController != null && findInteractionController.channelDelegate != null)
            findInteractionController.channelDelegate.onFindResultReceived(activeMatchOrdinal, numberOfMatches, isDoneCounting);
          if (channelDelegate != null)
            channelDelegate.onFindResultReceived(activeMatchOrdinal, numberOfMatches, isDoneCounting);
        }
      });

      gestureDetector = new GestureDetector(this.getContext(), new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent ev) {
          if (floatingContextMenu != null) {
            hideContextMenu();
          }
          return super.onSingleTapUp(ev);
        }
      });

      checkScrollStoppedTask = new Runnable() {
        @Override
        public void run() {
          int newPosition = getScrollY();
          if (initialPositionScrollStoppedTask - newPosition == 0) {
            // has stopped
            onScrollStopped();
          } else {
            initialPositionScrollStoppedTask = getScrollY();
            mainLooperHandler.postDelayed(checkScrollStoppedTask, newCheckScrollStoppedTask);
          }
        }
      };

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && !customSettings.useHybridComposition) {
        checkContextMenuShouldBeClosedTask = new Runnable() {
          @Override
          public void run() {
            if (floatingContextMenu != null) {
              evaluateJavascript(PluginScriptsUtil.CHECK_CONTEXT_MENU_SHOULD_BE_HIDDEN_JS_SOURCE, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                  if (value == null || value.equals("true")) {
                    if (floatingContextMenu != null) {
                      hideContextMenu();
                    }
                  } else {
                    mainLooperHandler.postDelayed(checkContextMenuShouldBeClosedTask, newCheckContextMenuShouldBeClosedTaskTask);
                  }
                }
              });
            }
          }
        };
      }

      setOnTouchListener(new OnTouchListener() {
        float m_downX;
        float m_downY;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
          gestureDetector.onTouchEvent(event);

          if (event.getAction() == MotionEvent.ACTION_UP) {
            checkScrollStoppedTask.run();
          }

          if (customSettings.disableHorizontalScroll && customSettings.disableVerticalScroll) {
            return (event.getAction() == MotionEvent.ACTION_MOVE);
          } else if (customSettings.disableHorizontalScroll || customSettings.disableVerticalScroll) {
            switch (event.getAction()) {
              case MotionEvent.ACTION_DOWN: {
                // save the x
                m_downX = event.getX();
                // save the y
                m_downY = event.getY();
                break;
              }
              case MotionEvent.ACTION_MOVE:
              case MotionEvent.ACTION_CANCEL:
              case MotionEvent.ACTION_UP: {
                if (customSettings.disableHorizontalScroll) {
                  // set x so that it doesn't move
                  event.setLocation(m_downX, event.getY());
                } else {
                  // set y so that it doesn't move
                  event.setLocation(event.getX(), m_downY);
                }
                break;
              }
            }
          }
          return false;
        }
      });

      setOnLongClickListener(new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
          wtf.zikzak.zikzak_inappwebview_android.types.HitTestResult hitTestResult =
                  wtf.zikzak.zikzak_inappwebview_android.types.HitTestResult.fromWebViewHitTestResult(getHitTestResult());
          if (channelDelegate != null) channelDelegate.onLongPressHitTestResult(hitTestResult);
          return false;
        }
      });
    }

    public void prepareAndAddUserScripts() {
      userContentController.addPluginScript(PromisePolyfillJS.PROMISE_POLYFILL_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(JavaScriptBridgeJS.JAVASCRIPT_BRIDGE_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(ConsoleLogJS.CONSOLE_LOG_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(PrintJS.PRINT_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(OnWindowBlurEventJS.ON_WINDOW_BLUR_EVENT_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(OnWindowFocusEventJS.ON_WINDOW_FOCUS_EVENT_JS_PLUGIN_SCRIPT);
      interceptOnlyAsyncAjaxRequestsPluginScript = InterceptAjaxRequestJS.createInterceptOnlyAsyncAjaxRequestsPluginScript(customSettings.interceptOnlyAsyncAjaxRequests);
      if (customSettings.useShouldInterceptAjaxRequest) {
        userContentController.addPluginScript(interceptOnlyAsyncAjaxRequestsPluginScript);
        userContentController.addPluginScript(InterceptAjaxRequestJS.INTERCEPT_AJAX_REQUEST_JS_PLUGIN_SCRIPT);
      }
      if (customSettings.useShouldInterceptFetchRequest) {
        userContentController.addPluginScript(InterceptFetchRequestJS.INTERCEPT_FETCH_REQUEST_JS_PLUGIN_SCRIPT);
      }
      if (customSettings.useOnLoadResource) {
        userContentController.addPluginScript(OnLoadResourceJS.ON_LOAD_RESOURCE_JS_PLUGIN_SCRIPT);
      }
      if (!customSettings.useHybridComposition) {
        userContentController.addPluginScript(PluginScriptsUtil.CHECK_GLOBAL_KEY_DOWN_EVENT_TO_HIDE_CONTEXT_MENU_JS_PLUGIN_SCRIPT);
      }
      this.userContentController.addUserOnlyScripts(this.initialUserOnlyScripts);
    }

    public void setIncognito(boolean enabled) {
      WebSettings settings = getSettings();
      if (enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          CookieManager.getInstance().removeAllCookies(null);
        } else {
          CookieManager.getInstance().removeAllCookie();
        }

        // Disable caching
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
        // settings.setAppCacheEnabled(false);
        Util.invokeMethodIfExists(settings, "setAppCacheEnabled", false);

        clearHistory();
        clearCache(true);

        // No form data or autofill enabled
        clearFormData();
        settings.setSavePassword(false);
        settings.setSaveFormData(false);
      } else {
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
        // settings.setAppCacheEnabled(true);
        Util.invokeMethodIfExists(settings, "setAppCacheEnabled", true);

        settings.setSavePassword(true);
        settings.setSaveFormData(true);
      }
    }

    public void setCacheEnabled(boolean enabled) {
      WebSettings settings = getSettings();
      if (enabled) {
        Context ctx = getContext();
        if (ctx != null) {
          // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
          // settings.setAppCachePath(ctx.getCacheDir().getAbsolutePath());
          Util.invokeMethodIfExists(settings, "setAppCachePath", ctx.getCacheDir().getAbsolutePath());

          settings.setCacheMode(WebSettings.LOAD_DEFAULT);

          // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
          // settings.setAppCacheEnabled(true);
          Util.invokeMethodIfExists(settings, "setAppCacheEnabled", true);
        }
      } else {
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
        // settings.setAppCacheEnabled(false);
        Util.invokeMethodIfExists(settings, "setAppCacheEnabled", false);
      }
    }

    public void loadUrl(URLRequest urlRequest) {
      String url = urlRequest.getUrl();
      String method = urlRequest.getMethod();
      if (method != null && method.equals("POST")) {
        byte[] postData = urlRequest.getBody();
        postUrl(url, postData);
        return;
      }
      Map<String, String> headers = urlRequest.getHeaders();
      if (headers != null) {
        loadUrl(url, headers);
        return;
      }
      loadUrl(url);
    }

    public void loadFile(String assetFilePath) throws IOException {
      if (plugin == null) {
        return;
      }

      loadUrl(Util.getUrlAsset(plugin, assetFilePath));
    }

    public boolean isLoading() {
      return isLoading;
    }

    /**
     * @deprecated
     */
    @Deprecated
    private void clearCookies() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        CookieManager.getInstance().removeAllCookies(new ValueCallback<Boolean>() {
          @Override
          public void onReceiveValue(Boolean aBoolean) {

          }
        });
      } else {
        CookieManager.getInstance().removeAllCookie();
      }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void clearAllCache() {
      clearCache(true);
      clearCookies();
      clearFormData();
      WebStorage.getInstance().deleteAllData();
    }

    public void takeScreenshot(final @Nullable Map<String, Object> screenshotConfiguration, final MethodChannel.Result result) {
      final float pixelDensity = Util.getPixelDensity(getContext());

      mainLooperHandler.post(new Runnable() {
        @Override
        public void run() {
          try {
            Bitmap screenshotBitmap = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(screenshotBitmap);
            c.translate(-getScrollX(), -getScrollY());
            draw(c);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.PNG;
            int quality = 100;

            if (screenshotConfiguration != null) {
              Map<String, Double> rect = (Map<String, Double>) screenshotConfiguration.get("rect");
              if (rect != null) {
                int rectX = (int) Math.floor(rect.get("x") * pixelDensity + 0.5);
                int rectY = (int) Math.floor(rect.get("y") * pixelDensity + 0.5);
                int rectWidth = Math.min(screenshotBitmap.getWidth(), (int) Math.floor(rect.get("width") * pixelDensity + 0.5));
                int rectHeight = Math.min(screenshotBitmap.getHeight(), (int) Math.floor(rect.get("height") * pixelDensity + 0.5));
                screenshotBitmap = Bitmap.createBitmap(
                        screenshotBitmap,
                        rectX,
                        rectY,
                        rectWidth,
                        rectHeight);
              }

              Double snapshotWidth = (Double) screenshotConfiguration.get("snapshotWidth");
              if (snapshotWidth != null) {
                int dstWidth = (int) Math.floor(snapshotWidth * pixelDensity + 0.5);
                float ratioBitmap = (float) screenshotBitmap.getWidth() / (float) screenshotBitmap.getHeight();
                int dstHeight = (int) ((float) dstWidth / ratioBitmap);
                screenshotBitmap = Bitmap.createScaledBitmap(screenshotBitmap, dstWidth, dstHeight, true);
              }

              try {
                compressFormat = Bitmap.CompressFormat.valueOf((String) screenshotConfiguration.get("compressFormat"));
              } catch (IllegalArgumentException e) {
                Log.e(LOG_TAG, "", e);
              }

              quality = (Integer) screenshotConfiguration.get("quality");
            }

            screenshotBitmap.compress(
                    compressFormat,
                    quality,
                    byteArrayOutputStream);

            try {
              byteArrayOutputStream.close();
            } catch (IOException e) {
              Log.e(LOG_TAG, "", e);
            }
            screenshotBitmap.recycle();
            result.success(byteArrayOutputStream.toByteArray());

          } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "", e);
            result.success(null);
          }
        }
      });
    }

    @SuppressLint("RestrictedApi")
    public void setSettings(InAppWebViewSettings newCustomSettings, HashMap<String, Object> newSettingsMap) {

      WebSettings settings = getSettings();

      if (newSettingsMap.get("javaScriptEnabled") != null && customSettings.javaScriptEnabled != newCustomSettings.javaScriptEnabled)
        settings.setJavaScriptEnabled(newCustomSettings.javaScriptEnabled);

      if (newSettingsMap.get("useShouldInterceptAjaxRequest") != null && customSettings.useShouldInterceptAjaxRequest != newCustomSettings.useShouldInterceptAjaxRequest) {
        enablePluginScriptAtRuntime(
                InterceptAjaxRequestJS.FLAG_VARIABLE_FOR_SHOULD_INTERCEPT_AJAX_REQUEST_JS_SOURCE,
                newCustomSettings.useShouldInterceptAjaxRequest,
                InterceptAjaxRequestJS.INTERCEPT_AJAX_REQUEST_JS_PLUGIN_SCRIPT
        );
      }

      if (newSettingsMap.get("interceptOnlyAsyncAjaxRequests") != null && customSettings.interceptOnlyAsyncAjaxRequests != newCustomSettings.interceptOnlyAsyncAjaxRequests) {
        enablePluginScriptAtRuntime(
                InterceptAjaxRequestJS.FLAG_VARIABLE_FOR_INTERCEPT_ONLY_ASYNC_AJAX_REQUESTS_JS_SOURCE,
                newCustomSettings.interceptOnlyAsyncAjaxRequests,
                interceptOnlyAsyncAjaxRequestsPluginScript
        );
      }

      if (newSettingsMap.get("useShouldInterceptFetchRequest") != null && customSettings.useShouldInterceptFetchRequest != newCustomSettings.useShouldInterceptFetchRequest) {
        enablePluginScriptAtRuntime(
                InterceptFetchRequestJS.FLAG_VARIABLE_FOR_SHOULD_INTERCEPT_FETCH_REQUEST_JS_SOURCE,
                newCustomSettings.useShouldInterceptFetchRequest,
                InterceptFetchRequestJS.INTERCEPT_FETCH_REQUEST_JS_PLUGIN_SCRIPT
        );
      }

      if (newSettingsMap.get("useOnLoadResource") != null && customSettings.useOnLoadResource != newCustomSettings.useOnLoadResource) {
        enablePluginScriptAtRuntime(
                OnLoadResourceJS.FLAG_VARIABLE_FOR_ON_LOAD_RESOURCE_JS_SOURCE,
                newCustomSettings.useOnLoadResource,
                OnLoadResourceJS.ON_LOAD_RESOURCE_JS_PLUGIN_SCRIPT
        );
      }

      if (newSettingsMap.get("javaScriptCanOpenWindowsAutomatically") != null && customSettings.javaScriptCanOpenWindowsAutomatically != newCustomSettings.javaScriptCanOpenWindowsAutomatically)
        settings.setJavaScriptCanOpenWindowsAutomatically(newCustomSettings.javaScriptCanOpenWindowsAutomatically);

      if (newSettingsMap.get("builtInZoomControls") != null && customSettings.builtInZoomControls != newCustomSettings.builtInZoomControls)
        settings.setBuiltInZoomControls(newCustomSettings.builtInZoomControls);

      if (newSettingsMap.get("displayZoomControls") != null && customSettings.displayZoomControls != newCustomSettings.displayZoomControls)
        settings.setDisplayZoomControls(newCustomSettings.displayZoomControls);

      if (newSettingsMap.get("safeBrowsingEnabled") != null && customSettings.safeBrowsingEnabled != newCustomSettings.safeBrowsingEnabled) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE))
          WebSettingsCompat.setSafeBrowsingEnabled(settings, newCustomSettings.safeBrowsingEnabled);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
          settings.setSafeBrowsingEnabled(newCustomSettings.safeBrowsingEnabled);
      }

      if (newSettingsMap.get("mediaPlaybackRequiresUserGesture") != null && customSettings.mediaPlaybackRequiresUserGesture != newCustomSettings.mediaPlaybackRequiresUserGesture)
        settings.setMediaPlaybackRequiresUserGesture(newCustomSettings.mediaPlaybackRequiresUserGesture);

      if (newSettingsMap.get("databaseEnabled") != null && customSettings.databaseEnabled != newCustomSettings.databaseEnabled)
        settings.setDatabaseEnabled(newCustomSettings.databaseEnabled);

      if (newSettingsMap.get("domStorageEnabled") != null && customSettings.domStorageEnabled != newCustomSettings.domStorageEnabled)
        settings.setDomStorageEnabled(newCustomSettings.domStorageEnabled);

      if (newSettingsMap.get("userAgent") != null && !customSettings.userAgent.equals(newCustomSettings.userAgent) && !newCustomSettings.userAgent.isEmpty())
        settings.setUserAgentString(newCustomSettings.userAgent);

      if (newSettingsMap.get("applicationNameForUserAgent") != null && !customSettings.applicationNameForUserAgent.equals(newCustomSettings.applicationNameForUserAgent) && !newCustomSettings.applicationNameForUserAgent.isEmpty()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
          String userAgent = (newCustomSettings.userAgent != null && !newCustomSettings.userAgent.isEmpty()) ? newCustomSettings.userAgent : WebSettings.getDefaultUserAgent(getContext());
          String userAgentWithApplicationName = userAgent + " " + customSettings.applicationNameForUserAgent;
          settings.setUserAgentString(userAgentWithApplicationName);
        }
      }

      if (newSettingsMap.get("clearCache") != null && newCustomSettings.clearCache)
        clearAllCache();
      else if (newSettingsMap.get("clearSessionCache") != null && newCustomSettings.clearSessionCache)
        CookieManager.getInstance().removeSessionCookie();

      if (newSettingsMap.get("thirdPartyCookiesEnabled") != null && customSettings.thirdPartyCookiesEnabled != newCustomSettings.thirdPartyCookiesEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, newCustomSettings.thirdPartyCookiesEnabled);

      if (newSettingsMap.get("useWideViewPort") != null && customSettings.useWideViewPort != newCustomSettings.useWideViewPort)
        settings.setUseWideViewPort(newCustomSettings.useWideViewPort);

      if (newSettingsMap.get("supportZoom") != null && customSettings.supportZoom != newCustomSettings.supportZoom)
        settings.setSupportZoom(newCustomSettings.supportZoom);

      if (newSettingsMap.get("textZoom") != null && !customSettings.textZoom.equals(newCustomSettings.textZoom))
        settings.setTextZoom(newCustomSettings.textZoom);

      if (newSettingsMap.get("verticalScrollBarEnabled") != null && customSettings.verticalScrollBarEnabled != newCustomSettings.verticalScrollBarEnabled)
        setVerticalScrollBarEnabled(newCustomSettings.verticalScrollBarEnabled);

      if (newSettingsMap.get("horizontalScrollBarEnabled") != null && customSettings.horizontalScrollBarEnabled != newCustomSettings.horizontalScrollBarEnabled)
        setHorizontalScrollBarEnabled(newCustomSettings.horizontalScrollBarEnabled);

      if (newSettingsMap.get("transparentBackground") != null && customSettings.transparentBackground != newCustomSettings.transparentBackground) {
        if (newCustomSettings.transparentBackground) {
          setBackgroundColor(Color.TRANSPARENT);
        } else {
          setBackgroundColor(Color.parseColor("#FFFFFF"));
        }
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        if (newSettingsMap.get("mixedContentMode") != null && (customSettings.mixedContentMode == null || !customSettings.mixedContentMode.equals(newCustomSettings.mixedContentMode)))
          settings.setMixedContentMode(newCustomSettings.mixedContentMode);

      if (newSettingsMap.get("supportMultipleWindows") != null && customSettings.supportMultipleWindows != newCustomSettings.supportMultipleWindows)
        settings.setSupportMultipleWindows(newCustomSettings.supportMultipleWindows);

      if (newSettingsMap.get("useOnDownloadStart") != null && customSettings.useOnDownloadStart != newCustomSettings.useOnDownloadStart) {
        if (newCustomSettings.useOnDownloadStart) {
          setDownloadListener(new DownloadStartListener());
        } else {
          setDownloadListener(null);
        }
      }

      if (newSettingsMap.get("allowContentAccess") != null && customSettings.allowContentAccess != newCustomSettings.allowContentAccess)
        settings.setAllowContentAccess(newCustomSettings.allowContentAccess);

      if (newSettingsMap.get("allowFileAccess") != null && customSettings.allowFileAccess != newCustomSettings.allowFileAccess)
        settings.setAllowFileAccess(newCustomSettings.allowFileAccess);

      if (newSettingsMap.get("allowFileAccessFromFileURLs") != null && customSettings.allowFileAccessFromFileURLs != newCustomSettings.allowFileAccessFromFileURLs)
        settings.setAllowFileAccessFromFileURLs(newCustomSettings.allowFileAccessFromFileURLs);

      if (newSettingsMap.get("allowUniversalAccessFromFileURLs") != null && customSettings.allowUniversalAccessFromFileURLs != newCustomSettings.allowUniversalAccessFromFileURLs)
        settings.setAllowUniversalAccessFromFileURLs(newCustomSettings.allowUniversalAccessFromFileURLs);

      if (newSettingsMap.get("cacheEnabled") != null && customSettings.cacheEnabled != newCustomSettings.cacheEnabled)
        setCacheEnabled(newCustomSettings.cacheEnabled);

      if (newSettingsMap.get("appCachePath") != null && (customSettings.appCachePath == null || !customSettings.appCachePath.equals(newCustomSettings.appCachePath))) {
        // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
        // settings.setAppCachePath(newCustomSettings.appCachePath);
        Util.invokeMethodIfExists(settings, "setAppCachePath", newCustomSettings.appCachePath);
      }

      if (newSettingsMap.get("blockNetworkImage") != null && customSettings.blockNetworkImage != newCustomSettings.blockNetworkImage)
        settings.setBlockNetworkImage(newCustomSettings.blockNetworkImage);

      if (newSettingsMap.get("blockNetworkLoads") != null && customSettings.blockNetworkLoads != newCustomSettings.blockNetworkLoads)
        settings.setBlockNetworkLoads(newCustomSettings.blockNetworkLoads);

      if (newSettingsMap.get("cacheMode") != null && !customSettings.cacheMode.equals(newCustomSettings.cacheMode))
        settings.setCacheMode(newCustomSettings.cacheMode);

      if (newSettingsMap.get("cursiveFontFamily") != null && !customSettings.cursiveFontFamily.equals(newCustomSettings.cursiveFontFamily))
        settings.setCursiveFontFamily(newCustomSettings.cursiveFontFamily);

      if (newSettingsMap.get("defaultFixedFontSize") != null && !customSettings.defaultFixedFontSize.equals(newCustomSettings.defaultFixedFontSize))
        settings.setDefaultFixedFontSize(newCustomSettings.defaultFixedFontSize);

      if (newSettingsMap.get("defaultFontSize") != null && !customSettings.defaultFontSize.equals(newCustomSettings.defaultFontSize))
        settings.setDefaultFontSize(newCustomSettings.defaultFontSize);

      if (newSettingsMap.get("defaultTextEncodingName") != null && !customSettings.defaultTextEncodingName.equals(newCustomSettings.defaultTextEncodingName))
        settings.setDefaultTextEncodingName(newCustomSettings.defaultTextEncodingName);

      if (newSettingsMap.get("disabledActionModeMenuItems") != null &&
              (customSettings.disabledActionModeMenuItems == null ||
              !customSettings.disabledActionModeMenuItems.equals(newCustomSettings.disabledActionModeMenuItems))) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DISABLED_ACTION_MODE_MENU_ITEMS))
          WebSettingsCompat.setDisabledActionModeMenuItems(settings, newCustomSettings.disabledActionModeMenuItems);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
          settings.setDisabledActionModeMenuItems(newCustomSettings.disabledActionModeMenuItems);
      }

      if (newSettingsMap.get("fantasyFontFamily") != null && !customSettings.fantasyFontFamily.equals(newCustomSettings.fantasyFontFamily))
        settings.setFantasyFontFamily(newCustomSettings.fantasyFontFamily);

      if (newSettingsMap.get("fixedFontFamily") != null && !customSettings.fixedFontFamily.equals(newCustomSettings.fixedFontFamily))
        settings.setFixedFontFamily(newCustomSettings.fixedFontFamily);

      if (newSettingsMap.get("forceDark") != null && !customSettings.forceDark.equals(newCustomSettings.forceDark)) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK))
          WebSettingsCompat.setForceDark(settings, newCustomSettings.forceDark);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
          settings.setForceDark(newCustomSettings.forceDark);
      }

      if (newSettingsMap.get("forceDarkStrategy") != null &&
              !customSettings.forceDarkStrategy.equals(newCustomSettings.forceDarkStrategy) &&
              WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
        WebSettingsCompat.setForceDarkStrategy(settings, newCustomSettings.forceDarkStrategy);
      }

      settings.setGeolocationEnabled(customSettings.geolocationEnabled);
      if (customSettings.layoutAlgorithm != null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && customSettings.layoutAlgorithm.equals(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING)) {
          settings.setLayoutAlgorithm(customSettings.layoutAlgorithm);
        } else {
          settings.setLayoutAlgorithm(customSettings.layoutAlgorithm);
        }
      }
      settings.setLoadsImagesAutomatically(customSettings.loadsImagesAutomatically);
      settings.setMinimumFontSize(customSettings.minimumFontSize);
      settings.setMinimumLogicalFontSize(customSettings.minimumLogicalFontSize);
      setInitialScale(customSettings.initialScale);
      settings.setNeedInitialFocus(customSettings.needInitialFocus);
      if (WebViewFeature.isFeatureSupported(WebViewFeature.OFF_SCREEN_PRERASTER))
        WebSettingsCompat.setOffscreenPreRaster(settings, customSettings.offscreenPreRaster);
      else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        settings.setOffscreenPreRaster(customSettings.offscreenPreRaster);
      settings.setSansSerifFontFamily(customSettings.sansSerifFontFamily);
      settings.setSerifFontFamily(customSettings.serifFontFamily);
      settings.setStandardFontFamily(customSettings.standardFontFamily);
      if (customSettings.preferredContentMode != null &&
              customSettings.preferredContentMode == PreferredContentModeOptionType.DESKTOP.toValue()) {
        setDesktopMode(true);
      }
      settings.setSaveFormData(customSettings.saveFormData);
      if (customSettings.incognito)
        setIncognito(true);
      if (customSettings.useHybridComposition) {
        if (customSettings.hardwareAcceleration)
          setLayerType(View.LAYER_TYPE_HARDWARE, null);
        else
          setLayerType(View.LAYER_TYPE_NONE, null);
      }
      if (customSettings.regexToCancelSubFramesLoading != null) {
        regexToCancelSubFramesLoadingCompiled = Pattern.compile(customSettings.regexToCancelSubFramesLoading);
      }
      setScrollBarStyle(customSettings.scrollBarStyle);
      if (customSettings.scrollBarDefaultDelayBeforeFade != null) {
        setScrollBarDefaultDelayBeforeFade(customSettings.scrollBarDefaultDelayBeforeFade);
      } else {
        customSettings.scrollBarDefaultDelayBeforeFade = getScrollBarDefaultDelayBeforeFade();
      }
      setScrollbarFadingEnabled(customSettings.scrollbarFadingEnabled);
      if (customSettings.scrollBarFadeDuration != null) {
        setScrollBarFadeDuration(customSettings.scrollBarFadeDuration);
      } else {
        customSettings.scrollBarFadeDuration = getScrollBarFadeDuration();
      }
      setVerticalScrollbarPosition(customSettings.verticalScrollbarPosition);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        if (customSettings.verticalScrollbarThumbColor != null)
          setVerticalScrollbarThumbDrawable(new ColorDrawable(Color.parseColor(customSettings.verticalScrollbarThumbColor)));
        if (customSettings.verticalScrollbarTrackColor != null)
          setVerticalScrollbarTrackDrawable(new ColorDrawable(Color.parseColor(customSettings.verticalScrollbarTrackColor)));
        if (customSettings.horizontalScrollbarThumbColor != null)
          setHorizontalScrollbarThumbDrawable(new ColorDrawable(Color.parseColor(customSettings.horizontalScrollbarThumbColor)));
        if (customSettings.horizontalScrollbarTrackColor != null)
          setHorizontalScrollbarTrackDrawable(new ColorDrawable(Color.parseColor(customSettings.horizontalScrollbarTrackColor)));
      }

      setOverScrollMode(customSettings.overScrollMode);
      if (customSettings.networkAvailable != null) {
        setNetworkAvailable(customSettings.networkAvailable);
      }
      if (customSettings.rendererPriorityPolicy != null && (customSettings.rendererPriorityPolicy.get("rendererRequestedPriority") != null || customSettings.rendererPriorityPolicy.get("waivedWhenNotVisible") != null) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        setRendererPriorityPolicy(
                (int) customSettings.rendererPriorityPolicy.get("rendererRequestedPriority"),
                (boolean) customSettings.rendererPriorityPolicy.get("waivedWhenNotVisible"));
      }

      if (WebViewFeature.isFeatureSupported(WebViewFeature.SUPPRESS_ERROR_PAGE)) {
        WebSettingsCompat.setWillSuppressErrorPage(settings, customSettings.disableDefaultErrorPage);
      }
      if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, customSettings.algorithmicDarkeningAllowed);
      }
      if (WebViewFeature.isFeatureSupported(WebViewFeature.ENTERPRISE_AUTHENTICATION_APP_LINK_POLICY)) {
        WebSettingsCompat.setEnterpriseAuthenticationAppLinkPolicyEnabled(settings, customSettings.enterpriseAuthenticationAppLinkPolicyEnabled);
      }
      if (customSettings.requestedWithHeaderOriginAllowList != null &&
              WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
        WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, customSettings.requestedWithHeaderOriginAllowList);
      }

      contentBlockerHandler.getRuleList().clear();
      for (Map<String, Map<String, Object>> contentBlocker : customSettings.contentBlockers) {
        // compile ContentBlockerTrigger urlFilter
        ContentBlockerTrigger trigger = ContentBlockerTrigger.fromMap(contentBlocker.get("trigger"));
        ContentBlockerAction action = ContentBlockerAction.fromMap(contentBlocker.get("action"));
        contentBlockerHandler.getRuleList().add(new ContentBlocker(trigger, action));
      }

      setFindListener(new FindListener() {
        @Override
        public void onFindResultReceived(int activeMatchOrdinal, int numberOfMatches, boolean isDoneCounting) {
          if (findInteractionController != null && findInteractionController.channelDelegate != null)
            findInteractionController.channelDelegate.onFindResultReceived(activeMatchOrdinal, numberOfMatches, isDoneCounting);
          if (channelDelegate != null)
            channelDelegate.onFindResultReceived(activeMatchOrdinal, numberOfMatches, isDoneCounting);
        }
      });

      gestureDetector = new GestureDetector(this.getContext(), new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent ev) {
          if (floatingContextMenu != null) {
            hideContextMenu();
          }
          return super.onSingleTapUp(ev);
        }
      });

      checkScrollStoppedTask = new Runnable() {
        @Override
        public void run() {
          int newPosition = getScrollY();
          if (initialPositionScrollStoppedTask - newPosition == 0) {
            // has stopped
            onScrollStopped();
          } else {
            initialPositionScrollStoppedTask = getScrollY();
            mainLooperHandler.postDelayed(checkScrollStoppedTask, newCheckScrollStoppedTask);
          }
        }
      };

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && !customSettings.useHybridComposition) {
        checkContextMenuShouldBeClosedTask = new Runnable() {
          @Override
          public void run() {
            if (floatingContextMenu != null) {
              evaluateJavascript(PluginScriptsUtil.CHECK_CONTEXT_MENU_SHOULD_BE_HIDDEN_JS_SOURCE, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                  if (value == null || value.equals("true")) {
                    if (floatingContextMenu != null) {
                      hideContextMenu();
                    }
                  } else {
                    mainLooperHandler.postDelayed(checkContextMenuShouldBeClosedTask, newCheckContextMenuShouldBeClosedTaskTask);
                  }
                }
              });
            }
          }
        };
      }

      setOnTouchListener(new OnTouchListener() {
        float m_downX;
        float m_downY;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
          gestureDetector.onTouchEvent(event);

          if (event.getAction() == MotionEvent.ACTION_UP) {
            checkScrollStoppedTask.run();
          }

          if (customSettings.disableHorizontalScroll && customSettings.disableVerticalScroll) {
            return (event.getAction() == MotionEvent.ACTION_MOVE);
          } else if (customSettings.disableHorizontalScroll || customSettings.disableVerticalScroll) {
            switch (event.getAction()) {
              case MotionEvent.ACTION_DOWN: {
                // save the x
                m_downX = event.getX();
                // save the y
                m_downY = event.getY();
                break;
              }
              case MotionEvent.ACTION_MOVE:
              case MotionEvent.ACTION_CANCEL:
              case MotionEvent.ACTION_UP: {
                if (customSettings.disableHorizontalScroll) {
                  // set x so that it doesn't move
                  event.setLocation(m_downX, event.getY());
                } else {
                  // set y so that it doesn't move
                  event.setLocation(event.getX(), m_downY);
                }
                break;
              }
            }
          }
          return false;
        }
      });

      setOnLongClickListener(new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
          wtf.zikzak.zikzak_inappwebview_android.types.HitTestResult hitTestResult =
                  wtf.zikzak.zikzak_inappwebview_android.types.HitTestResult.fromWebViewHitTestResult(getHitTestResult());
          if (channelDelegate != null) channelDelegate.onLongPressHitTestResult(hitTestResult);
          return false;
        }
      });
    }

    public void prepareAndAddUserScripts() {
      userContentController.addPluginScript(PromisePolyfillJS.PROMISE_POLYFILL_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(JavaScriptBridgeJS.JAVASCRIPT_BRIDGE_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(ConsoleLogJS.CONSOLE_LOG_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(PrintJS.PRINT_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(OnWindowBlurEventJS.ON_WINDOW_BLUR_EVENT_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(OnWindowFocusEventJS.ON_WINDOW_FOCUS_EVENT_JS_PLUGIN_SCRIPT);
      interceptOnlyAsyncAjaxRequestsPluginScript = InterceptAjaxRequestJS.createInterceptOnlyAsyncAjaxRequestsPluginScript(customSettings.interceptOnlyAsyncAjaxRequests);
      if (customSettings.useShouldInterceptAjaxRequest) {
        userContentController.addPluginScript(interceptOnlyAsyncAjaxRequestsPluginScript);
        userContentController.addPluginScript(InterceptAjaxRequestJS.INTERCEPT_AJAX_REQUEST_JS_PLUGIN_SCRIPT);
      }
      if (customSettings.useShouldInterceptFetchRequest) {
        userContentController.addPluginScript(InterceptFetchRequestJS.INTERCEPT_FETCH_REQUEST_JS_PLUGIN_SCRIPT);
      }
      if (customSettings.useOnLoadResource) {
        userContentController.addPluginScript(OnLoadResourceJS.ON_LOAD_RESOURCE_JS_PLUGIN_SCRIPT);
      }
      if (!customSettings.useHybridComposition) {
        userContentController.addPluginScript(PluginScriptsUtil.CHECK_GLOBAL_KEY_DOWN_EVENT_TO_HIDE_CONTEXT_MENU_JS_PLUGIN_SCRIPT);
      }
      this.userContentController.addUserOnlyScripts(this.initialUserOnlyScripts);
    }

    public void setIncognito(boolean enabled) {
      WebSettings settings = getSettings();
      if (enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          CookieManager.getInstance().removeAllCookies(null);
        } else {
          CookieManager.getInstance().removeAllCookie();
        }

        // Disable caching
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
        // settings.setAppCacheEnabled(false);
        Util.invokeMethodIfExists(settings, "setAppCacheEnabled", false);

        clearHistory();
        clearCache(true);

        // No form data or autofill enabled
        clearFormData();
        settings.setSavePassword(false);
        settings.setSaveFormData(false);
      } else {
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
        // settings.setAppCacheEnabled(true);
        Util.invokeMethodIfExists(settings, "setAppCacheEnabled", true);

        settings.setSavePassword(true);
        settings.setSaveFormData(true);
      }
    }

    public void setCacheEnabled(boolean enabled) {
      WebSettings settings = getSettings();
      if (enabled) {
        Context ctx = getContext();
        if (ctx != null) {
          // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
          // settings.setAppCachePath(ctx.getCacheDir().getAbsolutePath());
          Util.invokeMethodIfExists(settings, "setAppCachePath", ctx.getCacheDir().getAbsolutePath());

          settings.setCacheMode(WebSettings.LOAD_DEFAULT);

          // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
          // settings.setAppCacheEnabled(true);
          Util.invokeMethodIfExists(settings, "setAppCacheEnabled", true);
        }
      } else {
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
        // settings.setAppCacheEnabled(false);
        Util.invokeMethodIfExists(settings, "setAppCacheEnabled", false);
      }
    }

    public void loadUrl(URLRequest urlRequest) {
      String url = urlRequest.getUrl();
      String method = urlRequest.getMethod();
      if (method != null && method.equals("POST")) {
        byte[] postData = urlRequest.getBody();
        postUrl(url, postData);
        return;
      }
      Map<String, String> headers = urlRequest.getHeaders();
      if (headers != null) {
        loadUrl(url, headers);
        return;
      }
      loadUrl(url);
    }

    public void loadFile(String assetFilePath) throws IOException {
      if (plugin == null) {
        return;
      }

      loadUrl(Util.getUrlAsset(plugin, assetFilePath));
    }

    public boolean isLoading() {
      return isLoading;
    }

    /**
     * @deprecated
     */
    @Deprecated
    private void clearCookies() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        CookieManager.getInstance().removeAllCookies(new ValueCallback<Boolean>() {
          @Override
          public void onReceiveValue(Boolean aBoolean) {

          }
        });
      } else {
        CookieManager.getInstance().removeAllCookie();
      }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void clearAllCache() {
      clearCache(true);
      clearCookies();
      clearFormData();
      WebStorage.getInstance().deleteAllData();
    }

    public void takeScreenshot(final @Nullable Map<String, Object> screenshotConfiguration, final MethodChannel.Result result) {
      final float pixelDensity = Util.getPixelDensity(getContext());

      mainLooperHandler.post(new Runnable() {
        @Override
        public void run() {
          try {
            Bitmap screenshotBitmap = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(screenshotBitmap);
            c.translate(-getScrollX(), -getScrollY());
            draw(c);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.PNG;
            int quality = 100;

            if (screenshotConfiguration != null) {
              Map<String, Double> rect = (Map<String, Double>) screenshotConfiguration.get("rect");
              if (rect != null) {
                int rectX = (int) Math.floor(rect.get("x") * pixelDensity + 0.5);
                int rectY = (int) Math.floor(rect.get("y") * pixelDensity + 0.5);
                int rectWidth = Math.min(screenshotBitmap.getWidth(), (int) Math.floor(rect.get("width") * pixelDensity + 0.5));
                int rectHeight = Math.min(screenshotBitmap.getHeight(), (int) Math.floor(rect.get("height") * pixelDensity + 0.5));
                screenshotBitmap = Bitmap.createBitmap(
                        screenshotBitmap,
                        rectX,
                        rectY,
                        rectWidth,
                        rectHeight);
              }

              Double snapshotWidth = (Double) screenshotConfiguration.get("snapshotWidth");
              if (snapshotWidth != null) {
                int dstWidth = (int) Math.floor(snapshotWidth * pixelDensity + 0.5);
                float ratioBitmap = (float) screenshotBitmap.getWidth() / (float) screenshotBitmap.getHeight();
                int dstHeight = (int) ((float) dstWidth / ratioBitmap);
                screenshotBitmap = Bitmap.createScaledBitmap(screenshotBitmap, dstWidth, dstHeight, true);
              }

              try {
                compressFormat = Bitmap.CompressFormat.valueOf((String) screenshotConfiguration.get("compressFormat"));
              } catch (IllegalArgumentException e) {
                Log.e(LOG_TAG, "", e);
              }

              quality = (Integer) screenshotConfiguration.get("quality");
            }

            screenshotBitmap.compress(
                    compressFormat,
                    quality,
                    byteArrayOutputStream);

            try {
              byteArrayOutputStream.close();
            } catch (IOException e) {
              Log.e(LOG_TAG, "", e);
            }
            screenshotBitmap.recycle();
            result.success(byteArrayOutputStream.toByteArray());

          } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "", e);
            result.success(null);
          }
        }
      });
    }

    @SuppressLint("RestrictedApi")
    public void setSettings(InAppWebViewSettings newCustomSettings, HashMap<String, Object> newSettingsMap) {

      WebSettings settings = getSettings();

      if (newSettingsMap.get("javaScriptEnabled") != null && customSettings.javaScriptEnabled != newCustomSettings.javaScriptEnabled)
        settings.setJavaScriptEnabled(newCustomSettings.javaScriptEnabled);

      if (newSettingsMap.get("useShouldInterceptAjaxRequest") != null && customSettings.useShouldInterceptAjaxRequest != newCustomSettings.useShouldInterceptAjaxRequest) {
        enablePluginScriptAtRuntime(
                InterceptAjaxRequestJS.FLAG_VARIABLE_FOR_SHOULD_INTERCEPT_AJAX_REQUEST_JS_SOURCE,
                newCustomSettings.useShouldInterceptAjaxRequest,
                InterceptAjaxRequestJS.INTERCEPT_AJAX_REQUEST_JS_PLUGIN_SCRIPT
        );
      }

      if (newSettingsMap.get("interceptOnlyAsyncAjaxRequests") != null && customSettings.interceptOnlyAsyncAjaxRequests != newCustomSettings.interceptOnlyAsyncAjaxRequests) {
        enablePluginScriptAtRuntime(
                InterceptAjaxRequestJS.FLAG_VARIABLE_FOR_INTERCEPT_ONLY_ASYNC_AJAX_REQUESTS_JS_SOURCE,
                newCustomSettings.interceptOnlyAsyncAjaxRequests,
                interceptOnlyAsyncAjaxRequestsPluginScript
        );
      }

      if (newSettingsMap.get("useShouldInterceptFetchRequest") != null && customSettings.useShouldInterceptFetchRequest != newCustomSettings.useShouldInterceptFetchRequest) {
        enablePluginScriptAtRuntime(
                InterceptFetchRequestJS.FLAG_VARIABLE_FOR_SHOULD_INTERCEPT_FETCH_REQUEST_JS_SOURCE,
                newCustomSettings.useShouldInterceptFetchRequest,
                InterceptFetchRequestJS.INTERCEPT_FETCH_REQUEST_JS_PLUGIN_SCRIPT
        );
      }

      if (newSettingsMap.get("useOnLoadResource") != null && customSettings.useOnLoadResource != newCustomSettings.useOnLoadResource) {
        enablePluginScriptAtRuntime(
                OnLoadResourceJS.FLAG_VARIABLE_FOR_ON_LOAD_RESOURCE_JS_SOURCE,
                newCustomSettings.useOnLoadResource,
                OnLoadResourceJS.ON_LOAD_RESOURCE_JS_PLUGIN_SCRIPT
        );
      }

      if (newSettingsMap.get("javaScriptCanOpenWindowsAutomatically") != null && customSettings.javaScriptCanOpenWindowsAutomatically != newCustomSettings.javaScriptCanOpenWindowsAutomatically)
        settings.setJavaScriptCanOpenWindowsAutomatically(newCustomSettings.javaScriptCanOpenWindowsAutomatically);

      if (newSettingsMap.get("builtInZoomControls") != null && customSettings.builtInZoomControls != newCustomSettings.builtInZoomControls)
        settings.setBuiltInZoomControls(newCustomSettings.builtInZoomControls);

      if (newSettingsMap.get("displayZoomControls") != null && customSettings.displayZoomControls != newCustomSettings.displayZoomControls)
        settings.setDisplayZoomControls(newCustomSettings.displayZoomControls);

      if (newSettingsMap.get("safeBrowsingEnabled") != null && customSettings.safeBrowsingEnabled != newCustomSettings.safeBrowsingEnabled) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE))
          WebSettingsCompat.setSafeBrowsingEnabled(settings, newCustomSettings.safeBrowsingEnabled);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
          settings.setSafeBrowsingEnabled(newCustomSettings.safeBrowsingEnabled);
      }

      if (newSettingsMap.get("mediaPlaybackRequiresUserGesture") != null && customSettings.mediaPlaybackRequiresUserGesture != newCustomSettings.mediaPlaybackRequiresUserGesture)
        settings.setMediaPlaybackRequiresUserGesture(newCustomSettings.mediaPlaybackRequiresUserGesture);

      if (newSettingsMap.get("databaseEnabled") != null && customSettings.databaseEnabled != newCustomSettings.databaseEnabled)
        settings.setDatabaseEnabled(newCustomSettings.databaseEnabled);

      if (newSettingsMap.get("domStorageEnabled") != null && customSettings.domStorageEnabled != newCustomSettings.domStorageEnabled)
        settings.setDomStorageEnabled(newCustomSettings.domStorageEnabled);

      if (newSettingsMap.get("userAgent") != null && !customSettings.userAgent.equals(newCustomSettings.userAgent) && !newCustomSettings.userAgent.isEmpty())
        settings.setUserAgentString(newCustomSettings.userAgent);

      if (newSettingsMap.get("applicationNameForUserAgent") != null && !customSettings.applicationNameForUserAgent.equals(newCustomSettings.applicationNameForUserAgent) && !newCustomSettings.applicationNameForUserAgent.isEmpty()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
          String userAgent = (newCustomSettings.userAgent != null && !newCustomSettings.userAgent.isEmpty()) ? newCustomSettings.userAgent : WebSettings.getDefaultUserAgent(getContext());
          String userAgentWithApplicationName = userAgent + " " + customSettings.applicationNameForUserAgent;
          settings.setUserAgentString(userAgentWithApplicationName);
        }
      }

      if (newSettingsMap.get("clearCache") != null && newCustomSettings.clearCache)
        clearAllCache();
      else if (newSettingsMap.get("clearSessionCache") != null && newCustomSettings.clearSessionCache)
        CookieManager.getInstance().removeSessionCookie();

      if (newSettingsMap.get("thirdPartyCookiesEnabled") != null && customSettings.thirdPartyCookiesEnabled != newCustomSettings.thirdPartyCookiesEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, newCustomSettings.thirdPartyCookiesEnabled);

      if (newSettingsMap.get("useWideViewPort") != null && customSettings.useWideViewPort != newCustomSettings.useWideViewPort)
        settings.setUseWideViewPort(newCustomSettings.useWideViewPort);

      if (newSettingsMap.get("supportZoom") != null && customSettings.supportZoom != newCustomSettings.supportZoom)
        settings.setSupportZoom(newCustomSettings.supportZoom);

      if (newSettingsMap.get("textZoom") != null && !customSettings.textZoom.equals(newCustomSettings.textZoom))
        settings.setTextZoom(newCustomSettings.textZoom);

      if (newSettingsMap.get("verticalScrollBarEnabled") != null && customSettings.verticalScrollBarEnabled != newCustomSettings.verticalScrollBarEnabled)
        setVerticalScrollBarEnabled(newCustomSettings.verticalScrollBarEnabled);

      if (newSettingsMap.get("horizontalScrollBarEnabled") != null && customSettings.horizontalScrollBarEnabled != newCustomSettings.horizontalScrollBarEnabled)
        setHorizontalScrollBarEnabled(newCustomSettings.horizontalScrollBarEnabled);

      if (newSettingsMap.get("transparentBackground") != null && customSettings.transparentBackground != newCustomSettings.transparentBackground) {
        if (newCustomSettings.transparentBackground) {
          setBackgroundColor(Color.TRANSPARENT);
        } else {
          setBackgroundColor(Color.parseColor("#FFFFFF"));
        }
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        if (newSettingsMap.get("mixedContentMode") != null && (customSettings.mixedContentMode == null || !customSettings.mixedContentMode.equals(newCustomSettings.mixedContentMode)))
          settings.setMixedContentMode(newCustomSettings.mixedContentMode);

      if (newSettingsMap.get("supportMultipleWindows") != null && customSettings.supportMultipleWindows != newCustomSettings.supportMultipleWindows)
        settings.setSupportMultipleWindows(newCustomSettings.supportMultipleWindows);

      if (newSettingsMap.get("useOnDownloadStart") != null && customSettings.useOnDownloadStart != newCustomSettings.useOnDownloadStart) {
        if (newCustomSettings.useOnDownloadStart) {
          setDownloadListener(new DownloadStartListener());
        } else {
          setDownloadListener(null);
        }
      }

      if (newSettingsMap.get("allowContentAccess") != null && customSettings.allowContentAccess != newCustomSettings.allowContentAccess)
        settings.setAllowContentAccess(newCustomSettings.allowContentAccess);

      if (newSettingsMap.get("allowFileAccess") != null && customSettings.allowFileAccess != newCustomSettings.allowFileAccess)
        settings.setAllowFileAccess(newCustomSettings.allowFileAccess);

      if (newSettingsMap.get("allowFileAccessFromFileURLs") != null && customSettings.allowFileAccessFromFileURLs != newCustomSettings.allowFileAccessFromFileURLs)
        settings.setAllowFileAccessFromFileURLs(newCustomSettings.allowFileAccessFromFileURLs);

      if (newSettingsMap.get("allowUniversalAccessFromFileURLs") != null && customSettings.allowUniversalAccessFromFileURLs != newCustomSettings.allowUniversalAccessFromFileURLs)
        settings.setAllowUniversalAccessFromFileURLs(newCustomSettings.allowUniversalAccessFromFileURLs);

      if (newSettingsMap.get("cacheEnabled") != null && customSettings.cacheEnabled != newCustomSettings.cacheEnabled)
        setCacheEnabled(newCustomSettings.cacheEnabled);

      if (newSettingsMap.get("appCachePath") != null && (customSettings.appCachePath == null || !customSettings.appCachePath.equals(newCustomSettings.appCachePath))) {
        // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
        // settings.setAppCachePath(newCustomSettings.appCachePath);
        Util.invokeMethodIfExists(settings, "setAppCachePath", newCustomSettings.appCachePath);
      }

      if (newSettingsMap.get("blockNetworkImage") != null && customSettings.blockNetworkImage != newCustomSettings.blockNetworkImage)
        settings.setBlockNetworkImage(newCustomSettings.blockNetworkImage);

      if (newSettingsMap.get("blockNetworkLoads") != null && customSettings.blockNetworkLoads != newCustomSettings.blockNetworkLoads)
        settings.setBlockNetworkLoads(newCustomSettings.blockNetworkLoads);

      if (newSettingsMap.get("cacheMode") != null && !customSettings.cacheMode.equals(newCustomSettings.cacheMode))
        settings.setCacheMode(newCustomSettings.cacheMode);

      if (newSettingsMap.get("cursiveFontFamily") != null && !customSettings.cursiveFontFamily.equals(newCustomSettings.cursiveFontFamily))
        settings.setCursiveFontFamily(newCustomSettings.cursiveFontFamily);

      if (newSettingsMap.get("defaultFixedFontSize") != null && !customSettings.defaultFixedFontSize.equals(newCustomSettings.defaultFixedFontSize))
        settings.setDefaultFixedFontSize(newCustomSettings.defaultFixedFontSize);

      if (newSettingsMap.get("defaultFontSize") != null && !customSettings.defaultFontSize.equals(newCustomSettings.defaultFontSize))
        settings.setDefaultFontSize(newCustomSettings.defaultFontSize);

      if (newSettingsMap.get("defaultTextEncodingName") != null && !customSettings.defaultTextEncodingName.equals(newCustomSettings.defaultTextEncodingName))
        settings.setDefaultTextEncodingName(newCustomSettings.defaultTextEncodingName);

      if (newSettingsMap.get("disabledActionModeMenuItems") != null &&
              (customSettings.disabledActionModeMenuItems == null ||
              !customSettings.disabledActionModeMenuItems.equals(newCustomSettings.disabledActionModeMenuItems))) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DISABLED_ACTION_MODE_MENU_ITEMS))
          WebSettingsCompat.setDisabledActionModeMenuItems(settings, newCustomSettings.disabledActionModeMenuItems);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
          settings.setDisabledActionModeMenuItems(newCustomSettings.disabledActionModeMenuItems);
      }

      if (newSettingsMap.get("fantasyFontFamily") != null && !customSettings.fantasyFontFamily.equals(newCustomSettings.fantasyFontFamily))
        settings.setFantasyFontFamily(newCustomSettings.fantasyFontFamily);

      if (newSettingsMap.get("fixedFontFamily") != null && !customSettings.fixedFontFamily.equals(newCustomSettings.fixedFontFamily))
        settings.setFixedFontFamily(newCustomSettings.fixedFontFamily);

      if (newSettingsMap.get("forceDark") != null && !customSettings.forceDark.equals(newCustomSettings.forceDark)) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK))
          WebSettingsCompat.setForceDark(settings, newCustomSettings.forceDark);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
          settings.setForceDark(newCustomSettings.forceDark);
      }

      if (newSettingsMap.get("forceDarkStrategy") != null &&
              !customSettings.forceDarkStrategy.equals(newCustomSettings.forceDarkStrategy) &&
              WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
        WebSettingsCompat.setForceDarkStrategy(settings, newCustomSettings.forceDarkStrategy);
      }

      settings.setGeolocationEnabled(customSettings.geolocationEnabled);
      if (customSettings.layoutAlgorithm != null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && customSettings.layoutAlgorithm.equals(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING)) {
          settings.setLayoutAlgorithm(customSettings.layoutAlgorithm);
        } else {
          settings.setLayoutAlgorithm(customSettings.layoutAlgorithm);
        }
      }
      settings.setLoadsImagesAutomatically(customSettings.loadsImagesAutomatically);
      settings.setMinimumFontSize(customSettings.minimumFontSize);
      settings.setMinimumLogicalFontSize(customSettings.minimumLogicalFontSize);
      setInitialScale(customSettings.initialScale);
      settings.setNeedInitialFocus(customSettings.needInitialFocus);
      if (WebViewFeature.isFeatureSupported(WebViewFeature.OFF_SCREEN_PRERASTER))
        WebSettingsCompat.setOffscreenPreRaster(settings, customSettings.offscreenPreRaster);
      else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        settings.setOffscreenPreRaster(customSettings.offscreenPreRaster);
      settings.setSansSerifFontFamily(customSettings.sansSerifFontFamily);
      settings.setSerifFontFamily(customSettings.serifFontFamily);
      settings.setStandardFontFamily(customSettings.standardFontFamily);
      if (customSettings.preferredContentMode != null &&
              customSettings.preferredContentMode == PreferredContentModeOptionType.DESKTOP.toValue()) {
        setDesktopMode(true);
      }
      settings.setSaveFormData(customSettings.saveFormData);
      if (customSettings.incognito)
        setIncognito(true);
      if (customSettings.useHybridComposition) {
        if (customSettings.hardwareAcceleration)
          setLayerType(View.LAYER_TYPE_HARDWARE, null);
        else
          setLayerType(View.LAYER_TYPE_NONE, null);
      }
      if (customSettings.regexToCancelSubFramesLoading != null) {
        regexToCancelSubFramesLoadingCompiled = Pattern.compile(customSettings.regexToCancelSubFramesLoading);
      }
      setScrollBarStyle(customSettings.scrollBarStyle);
      if (customSettings.scrollBarDefaultDelayBeforeFade != null) {
        setScrollBarDefaultDelayBeforeFade(customSettings.scrollBarDefaultDelayBeforeFade);
      } else {
        customSettings.scrollBarDefaultDelayBeforeFade = getScrollBarDefaultDelayBeforeFade();
      }
      setScrollbarFadingEnabled(customSettings.scrollbarFadingEnabled);
      if (customSettings.scrollBarFadeDuration != null) {
        setScrollBarFadeDuration(customSettings.scrollBarFadeDuration);
      } else {
        customSettings.scrollBarFadeDuration = getScrollBarFadeDuration();
      }
      setVerticalScrollbarPosition(customSettings.verticalScrollbarPosition);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        if (customSettings.verticalScrollbarThumbColor != null)
          setVerticalScrollbarThumbDrawable(new ColorDrawable(Color.parseColor(customSettings.verticalScrollbarThumbColor)));
        if (customSettings.verticalScrollbarTrackColor != null)
          setVerticalScrollbarTrackDrawable(new ColorDrawable(Color.parseColor(customSettings.verticalScrollbarTrackColor)));
        if (customSettings.horizontalScrollbarThumbColor != null)
          setHorizontalScrollbarThumbDrawable(new ColorDrawable(Color.parseColor(customSettings.horizontalScrollbarThumbColor)));
        if (customSettings.horizontalScrollbarTrackColor != null)
          setHorizontalScrollbarTrackDrawable(new ColorDrawable(Color.parseColor(customSettings.horizontalScrollbarTrackColor)));
      }

      setOverScrollMode(customSettings.overScrollMode);
      if (customSettings.networkAvailable != null) {
        setNetworkAvailable(customSettings.networkAvailable);
      }
      if (customSettings.rendererPriorityPolicy != null && (customSettings.rendererPriorityPolicy.get("rendererRequestedPriority") != null || customSettings.rendererPriorityPolicy.get("waivedWhenNotVisible") != null) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        setRendererPriorityPolicy(
                (int) customSettings.rendererPriorityPolicy.get("rendererRequestedPriority"),
                (boolean) customSettings.rendererPriorityPolicy.get("waivedWhenNotVisible"));
      }

      if (WebViewFeature.isFeatureSupported(WebViewFeature.SUPPRESS_ERROR_PAGE)) {
        WebSettingsCompat.setWillSuppressErrorPage(settings, customSettings.disableDefaultErrorPage);
      }
      if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, customSettings.algorithmicDarkeningAllowed);
      }
      if (WebViewFeature.isFeatureSupported(WebViewFeature.ENTERPRISE_AUTHENTICATION_APP_LINK_POLICY)) {
        WebSettingsCompat.setEnterpriseAuthenticationAppLinkPolicyEnabled(settings, customSettings.enterpriseAuthenticationAppLinkPolicyEnabled);
      }
      if (customSettings.requestedWithHeaderOriginAllowList != null &&
              WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
        WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, customSettings.requestedWithHeaderOriginAllowList);
      }

      contentBlockerHandler.getRuleList().clear();
      for (Map<String, Map<String, Object>> contentBlocker : customSettings.contentBlockers) {
        // compile ContentBlockerTrigger urlFilter
        ContentBlockerTrigger trigger = ContentBlockerTrigger.fromMap(contentBlocker.get("trigger"));
        ContentBlockerAction action = ContentBlockerAction.fromMap(contentBlocker.get("action"));
        contentBlockerHandler.getRuleList().add(new ContentBlocker(trigger, action));
      }

      setFindListener(new FindListener() {
        @Override
        public void onFindResultReceived(int activeMatchOrdinal, int numberOfMatches, boolean isDoneCounting) {
          if (findInteractionController != null && findInteractionController.channelDelegate != null)
            findInteractionController.channelDelegate.onFindResultReceived(activeMatchOrdinal, numberOfMatches, isDoneCounting);
          if (channelDelegate != null)
            channelDelegate.onFindResultReceived(activeMatchOrdinal, numberOfMatches, isDoneCounting);
        }
      });

      gestureDetector = new GestureDetector(this.getContext(), new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent ev) {
          if (floatingContextMenu != null) {
            hideContextMenu();
          }
          return super.onSingleTapUp(ev);
        }
      });

      checkScrollStoppedTask = new Runnable() {
        @Override
        public void run() {
          int newPosition = getScrollY();
          if (initialPositionScrollStoppedTask - newPosition == 0) {
            // has stopped
            onScrollStopped();
          } else {
            initialPositionScrollStoppedTask = getScrollY();
            mainLooperHandler.postDelayed(checkScrollStoppedTask, newCheckScrollStoppedTask);
          }
        }
      };

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && !customSettings.useHybridComposition) {
        checkContextMenuShouldBeClosedTask = new Runnable() {
          @Override
          public void run() {
            if (floatingContextMenu != null) {
              evaluateJavascript(PluginScriptsUtil.CHECK_CONTEXT_MENU_SHOULD_BE_HIDDEN_JS_SOURCE, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                  if (value == null || value.equals("true")) {
                    if (floatingContextMenu != null) {
                      hideContextMenu();
                    }
                  } else {
                    mainLooperHandler.postDelayed(checkContextMenuShouldBeClosedTask, newCheckContextMenuShouldBeClosedTaskTask);
                  }
                }
              });
            }
          }
        };
      }

      setOnTouchListener(new OnTouchListener() {
        float m_downX;
        float m_downY;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
          gestureDetector.onTouchEvent(event);

          if (event.getAction() == MotionEvent.ACTION_UP) {
            checkScrollStoppedTask.run();
          }

          if (customSettings.disableHorizontalScroll && customSettings.disableVerticalScroll) {
            return (event.getAction() == MotionEvent.ACTION_MOVE);
          } else if (customSettings.disableHorizontalScroll || customSettings.disableVerticalScroll) {
            switch (event.getAction()) {
              case MotionEvent.ACTION_DOWN: {
                // save the x
                m_downX = event.getX();
                // save the y
                m_downY = event.getY();
                break;
              }
              case MotionEvent.ACTION_MOVE:
              case MotionEvent.ACTION_CANCEL:
              case MotionEvent.ACTION_UP: {
                if (customSettings.disableHorizontalScroll) {
                  // set x so that it doesn't move
                  event.setLocation(m_downX, event.getY());
                } else {
                  // set y so that it doesn't move
                  event.setLocation(event.getX(), m_downY);
                }
                break;
              }
            }
          }
          return false;
        }
      });

      setOnLongClickListener(new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
          wtf.zikzak.zikzak_inappwebview_android.types.HitTestResult hitTestResult =
                  wtf.zikzak.zikzak_inappwebview_android.types.HitTestResult.fromWebViewHitTestResult(getHitTestResult());
          if (channelDelegate != null) channelDelegate.onLongPressHitTestResult(hitTestResult);
          return false;
        }
      });
    }

    public void prepareAndAddUserScripts() {
      userContentController.addPluginScript(PromisePolyfillJS.PROMISE_POLYFILL_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(JavaScriptBridgeJS.JAVASCRIPT_BRIDGE_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(ConsoleLogJS.CONSOLE_LOG_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(PrintJS.PRINT_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(OnWindowBlurEventJS.ON_WINDOW_BLUR_EVENT_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(OnWindowFocusEventJS.ON_WINDOW_FOCUS_EVENT_JS_PLUGIN_SCRIPT);
      interceptOnlyAsyncAjaxRequestsPluginScript = InterceptAjaxRequestJS.createInterceptOnlyAsyncAjaxRequestsPluginScript(customSettings.interceptOnlyAsyncAjaxRequests);
      if (customSettings.useShouldInterceptAjaxRequest) {
        userContentController.addPluginScript(interceptOnlyAsyncAjaxRequestsPluginScript);
        userContentController.addPluginScript(InterceptAjaxRequestJS.INTERCEPT_AJAX_REQUEST_JS_PLUGIN_SCRIPT);
      }
      if (customSettings.useShouldInterceptFetchRequest) {
        userContentController.addPluginScript(InterceptFetchRequestJS.INTERCEPT_FETCH_REQUEST_JS_PLUGIN_SCRIPT);
      }
      if (customSettings.useOnLoadResource) {
        userContentController.addPluginScript(OnLoadResourceJS.ON_LOAD_RESOURCE_JS_PLUGIN_SCRIPT);
      }
      if (!customSettings.useHybridComposition) {
        userContentController.addPluginScript(PluginScriptsUtil.CHECK_GLOBAL_KEY_DOWN_EVENT_TO_HIDE_CONTEXT_MENU_JS_PLUGIN_SCRIPT);
      }
      this.userContentController.addUserOnlyScripts(this.initialUserOnlyScripts);
    }

    public void setIncognito(boolean enabled) {
      WebSettings settings = getSettings();
      if (enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          CookieManager.getInstance().removeAllCookies(null);
        } else {
          CookieManager.getInstance().removeAllCookie();
        }

        // Disable caching
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
        // settings.setAppCacheEnabled(false);
        Util.invokeMethodIfExists(settings, "setAppCacheEnabled", false);

        clearHistory();
        clearCache(true);

        // No form data or autofill enabled
        clearFormData();
        settings.setSavePassword(false);
        settings.setSaveFormData(false);
      } else {
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
        // settings.setAppCacheEnabled(true);
        Util.invokeMethodIfExists(settings, "setAppCacheEnabled", true);

        settings.setSavePassword(true);
        settings.setSaveFormData(true);
      }
    }

    public void setCacheEnabled(boolean enabled) {
      WebSettings settings = getSettings();
      if (enabled) {
        Context ctx = getContext();
        if (ctx != null) {
          // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
          // settings.setAppCachePath(ctx.getCacheDir().getAbsolutePath());
          Util.invokeMethodIfExists(settings, "setAppCachePath", ctx.getCacheDir().getAbsolutePath());

          settings.setCacheMode(WebSettings.LOAD_DEFAULT);

          // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
          // settings.setAppCacheEnabled(true);
          Util.invokeMethodIfExists(settings, "setAppCacheEnabled", true);
        }
      } else {
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
        // settings.setAppCacheEnabled(false);
        Util.invokeMethodIfExists(settings, "setAppCacheEnabled", false);
      }
    }

    public void loadUrl(URLRequest urlRequest) {
      String url = urlRequest.getUrl();
      String method = urlRequest.getMethod();
      if (method != null && method.equals("POST")) {
        byte[] postData = urlRequest.getBody();
        postUrl(url, postData);
        return;
      }
      Map<String, String> headers = urlRequest.getHeaders();
      if (headers != null) {
        loadUrl(url, headers);
        return;
      }
      loadUrl(url);
    }

    public void loadFile(String assetFilePath) throws IOException {
      if (plugin == null) {
        return;
      }

      loadUrl(Util.getUrlAsset(plugin, assetFilePath));
    }

    public boolean isLoading() {
      return isLoading;
    }

    /**
     * @deprecated
     */
    @Deprecated
    private void clearCookies() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        CookieManager.getInstance().removeAllCookies(new ValueCallback<Boolean>() {
          @Override
          public void onReceiveValue(Boolean aBoolean) {

          }
        });
      } else {
        CookieManager.getInstance().removeAllCookie();
      }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void clearAllCache() {
      clearCache(true);
      clearCookies();
      clearFormData();
      WebStorage.getInstance().deleteAllData();
    }

    public void takeScreenshot(final @Nullable Map<String, Object> screenshotConfiguration, final MethodChannel.Result result) {
      final float pixelDensity = Util.getPixelDensity(getContext());

      mainLooperHandler.post(new Runnable() {
        @Override
        public void run() {
          try {
            Bitmap screenshotBitmap = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(screenshotBitmap);
            c.translate(-getScrollX(), -getScrollY());
            draw(c);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.PNG;
            int quality = 100;

            if (screenshotConfiguration != null) {
              Map<String, Double> rect = (Map<String, Double>) screenshotConfiguration.get("rect");
              if (rect != null) {
                int rectX = (int) Math.floor(rect.get("x") * pixelDensity + 0.5);
                int rectY = (int) Math.floor(rect.get("y") * pixelDensity + 0.5);
                int rectWidth = Math.min(screenshotBitmap.getWidth(), (int) Math.floor(rect.get("width") * pixelDensity + 0.5));
                int rectHeight = Math.min(screenshotBitmap.getHeight(), (int) Math.floor(rect.get("height") * pixelDensity + 0.5));
                screenshotBitmap = Bitmap.createBitmap(
                        screenshotBitmap,
                        rectX,
                        rectY,
                        rectWidth,
                        rectHeight);
              }

              Double snapshotWidth = (Double) screenshotConfiguration.get("snapshotWidth");
              if (snapshotWidth != null) {
                int dstWidth = (int) Math.floor(snapshotWidth * pixelDensity + 0.5);
                float ratioBitmap = (float) screenshotBitmap.getWidth() / (float) screenshotBitmap.getHeight();
                int dstHeight = (int) ((float) dstWidth / ratioBitmap);
                screenshotBitmap = Bitmap.createScaledBitmap(screenshotBitmap, dstWidth, dstHeight, true);
              }

              try {
                compressFormat = Bitmap.CompressFormat.valueOf((String) screenshotConfiguration.get("compressFormat"));
              } catch (IllegalArgumentException e) {
                Log.e(LOG_TAG, "", e);
              }

              quality = (Integer) screenshotConfiguration.get("quality");
            }

            screenshotBitmap.compress(
                    compressFormat,
                    quality,
                    byteArrayOutputStream);

            try {
              byteArrayOutputStream.close();
            } catch (IOException e) {
              Log.e(LOG_TAG, "", e);
            }
            screenshotBitmap.recycle();
            result.success(byteArrayOutputStream.toByteArray());

          } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "", e);
            result.success(null);
          }
        }
      });
    }

    @SuppressLint("RestrictedApi")
    public void setSettings(InAppWebViewSettings newCustomSettings, HashMap<String, Object> newSettingsMap) {

      WebSettings settings = getSettings();

      if (newSettingsMap.get("javaScriptEnabled") != null && customSettings.javaScriptEnabled != newCustomSettings.javaScriptEnabled)
        settings.setJavaScriptEnabled(newCustomSettings.javaScriptEnabled);

      if (newSettingsMap.get("useShouldInterceptAjaxRequest") != null && customSettings.useShouldInterceptAjaxRequest != newCustomSettings.useShouldInterceptAjaxRequest) {
        enablePluginScriptAtRuntime(
                InterceptAjaxRequestJS.FLAG_VARIABLE_FOR_SHOULD_INTERCEPT_AJAX_REQUEST_JS_SOURCE,
                newCustomSettings.useShouldInterceptAjaxRequest,
                InterceptAjaxRequestJS.INTERCEPT_AJAX_REQUEST_JS_PLUGIN_SCRIPT
        );
      }

      if (newSettingsMap.get("interceptOnlyAsyncAjaxRequests") != null && customSettings.interceptOnlyAsyncAjaxRequests != newCustomSettings.interceptOnlyAsyncAjaxRequests) {
        enablePluginScriptAtRuntime(
                InterceptAjaxRequestJS.FLAG_VARIABLE_FOR_INTERCEPT_ONLY_ASYNC_AJAX_REQUESTS_JS_SOURCE,
                newCustomSettings.interceptOnlyAsyncAjaxRequests,
                interceptOnlyAsyncAjaxRequestsPluginScript
        );
      }

      if (newSettingsMap.get("useShouldInterceptFetchRequest") != null && customSettings.useShouldInterceptFetchRequest != newCustomSettings.useShouldInterceptFetchRequest) {
        enablePluginScriptAtRuntime(
                InterceptFetchRequestJS.FLAG_VARIABLE_FOR_SHOULD_INTERCEPT_FETCH_REQUEST_JS_SOURCE,
                newCustomSettings.useShouldInterceptFetchRequest,
                InterceptFetchRequestJS.INTERCEPT_FETCH_REQUEST_JS_PLUGIN_SCRIPT
        );
      }

      if (newSettingsMap.get("useOnLoadResource") != null && customSettings.useOnLoadResource != newCustomSettings.useOnLoadResource) {
        enablePluginScriptAtRuntime(
                OnLoadResourceJS.FLAG_VARIABLE_FOR_ON_LOAD_RESOURCE_JS_SOURCE,
                newCustomSettings.useOnLoadResource,
                OnLoadResourceJS.ON_LOAD_RESOURCE_JS_PLUGIN_SCRIPT
        );
      }

      if (newSettingsMap.get("javaScriptCanOpenWindowsAutomatically") != null && customSettings.javaScriptCanOpenWindowsAutomatically != newCustomSettings.javaScriptCanOpenWindowsAutomatically)
        settings.setJavaScriptCanOpenWindowsAutomatically(newCustomSettings.javaScriptCanOpenWindowsAutomatically);

      if (newSettingsMap.get("builtInZoomControls") != null && customSettings.builtInZoomControls != newCustomSettings.builtInZoomControls)
        settings.setBuiltInZoomControls(newCustomSettings.builtInZoomControls);

      if (newSettingsMap.get("displayZoomControls") != null && customSettings.displayZoomControls != newCustomSettings.displayZoomControls)
        settings.setDisplayZoomControls(newCustomSettings.displayZoomControls);

      if (newSettingsMap.get("safeBrowsingEnabled") != null && customSettings.safeBrowsingEnabled != newCustomSettings.safeBrowsingEnabled) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE))
          WebSettingsCompat.setSafeBrowsingEnabled(settings, newCustomSettings.safeBrowsingEnabled);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
          settings.setSafeBrowsingEnabled(newCustomSettings.safeBrowsingEnabled);
      }

      if (newSettingsMap.get("mediaPlaybackRequiresUserGesture") != null && customSettings.mediaPlaybackRequiresUserGesture != newCustomSettings.mediaPlaybackRequiresUserGesture)
        settings.setMediaPlaybackRequiresUserGesture(newCustomSettings.mediaPlaybackRequiresUserGesture);

      if (newSettingsMap.get("databaseEnabled") != null && customSettings.databaseEnabled != newCustomSettings.databaseEnabled)
        settings.setDatabaseEnabled(newCustomSettings.databaseEnabled);

      if (newSettingsMap.get("domStorageEnabled") != null && customSettings.domStorageEnabled != newCustomSettings.domStorageEnabled)
        settings.setDomStorageEnabled(newCustomSettings.domStorageEnabled);

      if (newSettingsMap.get("userAgent") != null && !customSettings.userAgent.equals(newCustomSettings.userAgent) && !newCustomSettings.userAgent.isEmpty())
        settings.setUserAgentString(newCustomSettings.userAgent);

      if (newSettingsMap.get("applicationNameForUserAgent") != null && !customSettings.applicationNameForUserAgent.equals(newCustomSettings.applicationNameForUserAgent) && !newCustomSettings.applicationNameForUserAgent.isEmpty()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
          String userAgent = (newCustomSettings.userAgent != null && !newCustomSettings.userAgent.isEmpty()) ? newCustomSettings.userAgent : WebSettings.getDefaultUserAgent(getContext());
          String userAgentWithApplicationName = userAgent + " " + customSettings.applicationNameForUserAgent;
          settings.setUserAgentString(userAgentWithApplicationName);
        }
      }

      if (newSettingsMap.get("clearCache") != null && newCustomSettings.clearCache)
        clearAllCache();
      else if (newSettingsMap.get("clearSessionCache") != null && newCustomSettings.clearSessionCache)
        CookieManager.getInstance().removeSessionCookie();

      if (newSettingsMap.get("thirdPartyCookiesEnabled") != null && customSettings.thirdPartyCookiesEnabled != newCustomSettings.thirdPartyCookiesEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, newCustomSettings.thirdPartyCookiesEnabled);

      if (newSettingsMap.get("useWideViewPort") != null && customSettings.useWideViewPort != newCustomSettings.useWideViewPort)
        settings.setUseWideViewPort(newCustomSettings.useWideViewPort);

      if (newSettingsMap.get("supportZoom") != null && customSettings.supportZoom != newCustomSettings.supportZoom)
        settings.setSupportZoom(newCustomSettings.supportZoom);

      if (newSettingsMap.get("textZoom") != null && !customSettings.textZoom.equals(newCustomSettings.textZoom))
        settings.setTextZoom(newCustomSettings.textZoom);

      if (newSettingsMap.get("verticalScrollBarEnabled") != null && customSettings.verticalScrollBarEnabled != newCustomSettings.verticalScrollBarEnabled)
        setVerticalScrollBarEnabled(newCustomSettings.verticalScrollBarEnabled);

      if (newSettingsMap.get("horizontalScrollBarEnabled") != null && customSettings.horizontalScrollBarEnabled != newCustomSettings.horizontalScrollBarEnabled)
        setHorizontalScrollBarEnabled(newCustomSettings.horizontalScrollBarEnabled);

      if (newSettingsMap.get("transparentBackground") != null && customSettings.transparentBackground != newCustomSettings.transparentBackground) {
        if (newCustomSettings.transparentBackground) {
          setBackgroundColor(Color.TRANSPARENT);
        } else {
          setBackgroundColor(Color.parseColor("#FFFFFF"));
        }
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        if (newSettingsMap.get("mixedContentMode") != null && (customSettings.mixedContentMode == null || !customSettings.mixedContentMode.equals(newCustomSettings.mixedContentMode)))
          settings.setMixedContentMode(newCustomSettings.mixedContentMode);

      if (newSettingsMap.get("supportMultipleWindows") != null && customSettings.supportMultipleWindows != newCustomSettings.supportMultipleWindows)
        settings.setSupportMultipleWindows(newCustomSettings.supportMultipleWindows);

      if (newSettingsMap.get("useOnDownloadStart") != null && customSettings.useOnDownloadStart != newCustomSettings.useOnDownloadStart) {
        if (newCustomSettings.useOnDownloadStart) {
          setDownloadListener(new DownloadStartListener());
        } else {
          setDownloadListener(null);
        }
      }

      if (newSettingsMap.get("allowContentAccess") != null && customSettings.allowContentAccess != newCustomSettings.allowContentAccess)
        settings.setAllowContentAccess(newCustomSettings.allowContentAccess);

      if (newSettingsMap.get("allowFileAccess") != null && customSettings.allowFileAccess != newCustomSettings.allowFileAccess)
        settings.setAllowFileAccess(newCustomSettings.allowFileAccess);

      if (newSettingsMap.get("allowFileAccessFromFileURLs") != null && customSettings.allowFileAccessFromFileURLs != newCustomSettings.allowFileAccessFromFileURLs)
        settings.setAllowFileAccessFromFileURLs(newCustomSettings.allowFileAccessFromFileURLs);

      if (newSettingsMap.get("allowUniversalAccessFromFileURLs") != null && customSettings.allowUniversalAccessFromFileURLs != newCustomSettings.allowUniversalAccessFromFileURLs)
        settings.setAllowUniversalAccessFromFileURLs(newCustomSettings.allowUniversalAccessFromFileURLs);

      if (newSettingsMap.get("cacheEnabled") != null && customSettings.cacheEnabled != newCustomSettings.cacheEnabled)
        setCacheEnabled(newCustomSettings.cacheEnabled);

      if (newSettingsMap.get("appCachePath") != null && (customSettings.appCachePath == null || !customSettings.appCachePath.equals(newCustomSettings.appCachePath))) {
        // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
        // settings.setAppCachePath(newCustomSettings.appCachePath);
        Util.invokeMethodIfExists(settings, "setAppCachePath", newCustomSettings.appCachePath);
      }

      if (newSettingsMap.get("blockNetworkImage") != null && customSettings.blockNetworkImage != newCustomSettings.blockNetworkImage)
        settings.setBlockNetworkImage(newCustomSettings.blockNetworkImage);

      if (newSettingsMap.get("blockNetworkLoads") != null && customSettings.blockNetworkLoads != newCustomSettings.blockNetworkLoads)
        settings.setBlockNetworkLoads(newCustomSettings.blockNetworkLoads);

      if (newSettingsMap.get("cacheMode") != null && !customSettings.cacheMode.equals(newCustomSettings.cacheMode))
        settings.setCacheMode(newCustomSettings.cacheMode);

      if (newSettingsMap.get("cursiveFontFamily") != null && !customSettings.cursiveFontFamily.equals(newCustomSettings.cursiveFontFamily))
        settings.setCursiveFontFamily(newCustomSettings.cursiveFontFamily);

      if (newSettingsMap.get("defaultFixedFontSize") != null && !customSettings.defaultFixedFontSize.equals(newCustomSettings.defaultFixedFontSize))
        settings.setDefaultFixedFontSize(newCustomSettings.defaultFixedFontSize);

      if (newSettingsMap.get("defaultFontSize") != null && !customSettings.defaultFontSize.equals(newCustomSettings.defaultFontSize))
        settings.setDefaultFontSize(newCustomSettings.defaultFontSize);

      if (newSettingsMap.get("defaultTextEncodingName") != null && !customSettings.defaultTextEncodingName.equals(newCustomSettings.defaultTextEncodingName))
        settings.setDefaultTextEncodingName(newCustomSettings.defaultTextEncodingName);

      if (newSettingsMap.get("disabledActionModeMenuItems") != null &&
              (customSettings.disabledActionModeMenuItems == null ||
              !customSettings.disabledActionModeMenuItems.equals(newCustomSettings.disabledActionModeMenuItems))) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DISABLED_ACTION_MODE_MENU_ITEMS))
          WebSettingsCompat.setDisabledActionModeMenuItems(settings, newCustomSettings.disabledActionModeMenuItems);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
          settings.setDisabledActionModeMenuItems(newCustomSettings.disabledActionModeMenuItems);
      }

      if (newSettingsMap.get("fantasyFontFamily") != null && !customSettings.fantasyFontFamily.equals(newCustomSettings.fantasyFontFamily))
        settings.setFantasyFontFamily(newCustomSettings.fantasyFontFamily);

      if (newSettingsMap.get("fixedFontFamily") != null && !customSettings.fixedFontFamily.equals(newCustomSettings.fixedFontFamily))
        settings.setFixedFontFamily(newCustomSettings.fixedFontFamily);

      if (newSettingsMap.get("forceDark") != null && !customSettings.forceDark.equals(newCustomSettings.forceDark)) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK))
          WebSettingsCompat.setForceDark(settings, newCustomSettings.forceDark);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
          settings.setForceDark(newCustomSettings.forceDark);
      }

      if (newSettingsMap.get("forceDarkStrategy") != null &&
              !customSettings.forceDarkStrategy.equals(newCustomSettings.forceDarkStrategy) &&
              WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
        WebSettingsCompat.setForceDarkStrategy(settings, newCustomSettings.forceDarkStrategy);
      }

      settings.setGeolocationEnabled(customSettings.geolocationEnabled);
      if (customSettings.layoutAlgorithm != null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && customSettings.layoutAlgorithm.equals(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING)) {
          settings.setLayoutAlgorithm(customSettings.layoutAlgorithm);
        } else {
          settings.setLayoutAlgorithm(customSettings.layoutAlgorithm);
        }
      }
      settings.setLoadsImagesAutomatically(customSettings.loadsImagesAutomatically);
      settings.setMinimumFontSize(customSettings.minimumFontSize);
      settings.setMinimumLogicalFontSize(customSettings.minimumLogicalFontSize);
      setInitialScale(customSettings.initialScale);
      settings.setNeedInitialFocus(customSettings.needInitialFocus);
      if (WebViewFeature.isFeatureSupported(WebViewFeature.OFF_SCREEN_PRERASTER))
        WebSettingsCompat.setOffscreenPreRaster(settings, customSettings.offscreenPreRaster);
      else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        settings.setOffscreenPreRaster(customSettings.offscreenPreRaster);
      settings.setSansSerifFontFamily(customSettings.sansSerifFontFamily);
      settings.setSerifFontFamily(customSettings.serifFontFamily);
      settings.setStandardFontFamily(customSettings.standardFontFamily);
      if (customSettings.preferredContentMode != null &&
              customSettings.preferredContentMode == PreferredContentModeOptionType.DESKTOP.toValue()) {
        setDesktopMode(true);
      }
      settings.setSaveFormData(customSettings.saveFormData);
      if (customSettings.incognito)
        setIncognito(true);
      if (customSettings.useHybridComposition) {
        if (customSettings.hardwareAcceleration)
          setLayerType(View.LAYER_TYPE_HARDWARE, null);
        else
          setLayerType(View.LAYER_TYPE_NONE, null);
      }
      if (customSettings.regexToCancelSubFramesLoading != null) {
        regexToCancelSubFramesLoadingCompiled = Pattern.compile(customSettings.regexToCancelSubFramesLoading);
      }
      setScrollBarStyle(customSettings.scrollBarStyle);
      if (customSettings.scrollBarDefaultDelayBeforeFade != null) {
        setScrollBarDefaultDelayBeforeFade(customSettings.scrollBarDefaultDelayBeforeFade);
      } else {
        customSettings.scrollBarDefaultDelayBeforeFade = getScrollBarDefaultDelayBeforeFade();
      }
      setScrollbarFadingEnabled(customSettings.scrollbarFadingEnabled);
      if (customSettings.scrollBarFadeDuration != null) {
        setScrollBarFadeDuration(customSettings.scrollBarFadeDuration);
      } else {
        customSettings.scrollBarFadeDuration = getScrollBarFadeDuration();
      }
      setVerticalScrollbarPosition(customSettings.verticalScrollbarPosition);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        if (customSettings.verticalScrollbarThumbColor != null)
          setVerticalScrollbarThumbDrawable(new ColorDrawable(Color.parseColor(customSettings.verticalScrollbarThumbColor)));
        if (customSettings.verticalScrollbarTrackColor != null)
          setVerticalScrollbarTrackDrawable(new ColorDrawable(Color.parseColor(customSettings.verticalScrollbarTrackColor)));
        if (customSettings.horizontalScrollbarThumbColor != null)
          setHorizontalScrollbarThumbDrawable(new ColorDrawable(Color.parseColor(customSettings.horizontalScrollbarThumbColor)));
        if (customSettings.horizontalScrollbarTrackColor != null)
          setHorizontalScrollbarTrackDrawable(new ColorDrawable(Color.parseColor(customSettings.horizontalScrollbarTrackColor)));
      }

      setOverScrollMode(customSettings.overScrollMode);
      if (customSettings.networkAvailable != null) {
        setNetworkAvailable(customSettings.networkAvailable);
      }
      if (customSettings.rendererPriorityPolicy != null && (customSettings.rendererPriorityPolicy.get("rendererRequestedPriority") != null || customSettings.rendererPriorityPolicy.get("waivedWhenNotVisible") != null) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        setRendererPriorityPolicy(
                (int) customSettings.rendererPriorityPolicy.get("rendererRequestedPriority"),
                (boolean) customSettings.rendererPriorityPolicy.get("waivedWhenNotVisible"));
      }

      if (WebViewFeature.isFeatureSupported(WebViewFeature.SUPPRESS_ERROR_PAGE)) {
        WebSettingsCompat.setWillSuppressErrorPage(settings, customSettings.disableDefaultErrorPage);
      }
      if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, customSettings.algorithmicDarkeningAllowed);
      }
      if (WebViewFeature.isFeatureSupported(WebViewFeature.ENTERPRISE_AUTHENTICATION_APP_LINK_POLICY)) {
        WebSettingsCompat.setEnterpriseAuthenticationAppLinkPolicyEnabled(settings, customSettings.enterpriseAuthenticationAppLinkPolicyEnabled);
      }
      if (customSettings.requestedWithHeaderOriginAllowList != null &&
              WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
        WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, customSettings.requestedWithHeaderOriginAllowList);
      }

      contentBlockerHandler.getRuleList().clear();
      for (Map<String, Map<String, Object>> contentBlocker : customSettings.contentBlockers) {
        // compile ContentBlockerTrigger urlFilter
        ContentBlockerTrigger trigger = ContentBlockerTrigger.fromMap(contentBlocker.get("trigger"));
        ContentBlockerAction action = ContentBlockerAction.fromMap(contentBlocker.get("action"));
        contentBlockerHandler.getRuleList().add(new ContentBlocker(trigger, action));
      }

      setFindListener(new FindListener() {
        @Override
        public void onFindResultReceived(int activeMatchOrdinal, int numberOfMatches, boolean isDoneCounting) {
          if (findInteractionController != null && findInteractionController.channelDelegate != null)
            findInteractionController.channelDelegate.onFindResultReceived(activeMatchOrdinal, numberOfMatches, isDoneCounting);
          if (channelDelegate != null)
            channelDelegate.onFindResultReceived(activeMatchOrdinal, numberOfMatches, isDoneCounting);
        }
      });

      gestureDetector = new GestureDetector(this.getContext(), new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent ev) {
          if (floatingContextMenu != null) {
            hideContextMenu();
          }
          return super.onSingleTapUp(ev);
        }
      });

      checkScrollStoppedTask = new Runnable() {
        @Override
        public void run() {
          int newPosition = getScrollY();
          if (initialPositionScrollStoppedTask - newPosition == 0) {
            // has stopped
            onScrollStopped();
          } else {
            initialPositionScrollStoppedTask = getScrollY();
            mainLooperHandler.postDelayed(checkScrollStoppedTask, newCheckScrollStoppedTask);
          }
        }
      };

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && !customSettings.useHybridComposition) {
        checkContextMenuShouldBeClosedTask = new Runnable() {
          @Override
          public void run() {
            if (floatingContextMenu != null) {
              evaluateJavascript(PluginScriptsUtil.CHECK_CONTEXT_MENU_SHOULD_BE_HIDDEN_JS_SOURCE, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                  if (value == null || value.equals("true")) {
                    if (floatingContextMenu != null) {
                      hideContextMenu();
                    }
                  } else {
                    mainLooperHandler.postDelayed(checkContextMenuShouldBeClosedTask, newCheckContextMenuShouldBeClosedTaskTask);
                  }
                }
              });
            }
          }
        };
      }

      setOnTouchListener(new OnTouchListener() {
        float m_downX;
        float m_downY;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
          gestureDetector.onTouchEvent(event);

          if (event.getAction() == MotionEvent.ACTION_UP) {
            checkScrollStoppedTask.run();
          }

          if (customSettings.disableHorizontalScroll && customSettings.disableVerticalScroll) {
            return (event.getAction() == MotionEvent.ACTION_MOVE);
          } else if (customSettings.disableHorizontalScroll || customSettings.disableVerticalScroll) {
            switch (event.getAction()) {
              case MotionEvent.ACTION_DOWN: {
                // save the x
                m_downX = event.getX();
                // save the y
                m_downY = event.getY();
                break;
              }
              case MotionEvent.ACTION_MOVE:
              case MotionEvent.ACTION_CANCEL:
              case MotionEvent.ACTION_UP: {
                if (customSettings.disableHorizontalScroll) {
                  // set x so that it doesn't move
                  event.setLocation(m_downX, event.getY());
                } else {
                  // set y so that it doesn't move
                  event.setLocation(event.getX(), m_downY);
                }
                break;
              }
            }
          }
          return false;
        }
      });

      setOnLongClickListener(new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
          wtf.zikzak.zikzak_inappwebview_android.types.HitTestResult hitTestResult =
                  wtf.zikzak.zikzak_inappwebview_android.types.HitTestResult.fromWebViewHitTestResult(getHitTestResult());
          if (channelDelegate != null) channelDelegate.onLongPressHitTestResult(hitTestResult);
          return false;
        }
      });
    }

    public void prepareAndAddUserScripts() {
      userContentController.addPluginScript(PromisePolyfillJS.PROMISE_POLYFILL_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(JavaScriptBridgeJS.JAVASCRIPT_BRIDGE_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(ConsoleLogJS.CONSOLE_LOG_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(PrintJS.PRINT_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(OnWindowBlurEventJS.ON_WINDOW_BLUR_EVENT_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(OnWindowFocusEventJS.ON_WINDOW_FOCUS_EVENT_JS_PLUGIN_SCRIPT);
      interceptOnlyAsyncAjaxRequestsPluginScript = InterceptAjaxRequestJS.createInterceptOnlyAsyncAjaxRequestsPluginScript(customSettings.interceptOnlyAsyncAjaxRequests);
      if (customSettings.useShouldInterceptAjaxRequest) {
        userContentController.addPluginScript(interceptOnlyAsyncAjaxRequestsPluginScript);
        userContentController.addPluginScript(InterceptAjaxRequestJS.INTERCEPT_AJAX_REQUEST_JS_PLUGIN_SCRIPT);
      }
      if (customSettings.useShouldInterceptFetchRequest) {
        userContentController.addPluginScript(InterceptFetchRequestJS.INTERCEPT_FETCH_REQUEST_JS_PLUGIN_SCRIPT);
      }
      if (customSettings.useOnLoadResource) {
        userContentController.addPluginScript(OnLoadResourceJS.ON_LOAD_RESOURCE_JS_PLUGIN_SCRIPT);
      }
      if (!customSettings.useHybridComposition) {
        userContentController.addPluginScript(PluginScriptsUtil.CHECK_GLOBAL_KEY_DOWN_EVENT_TO_HIDE_CONTEXT_MENU_JS_PLUGIN_SCRIPT);
      }
      this.userContentController.addUserOnlyScripts(this.initialUserOnlyScripts);
    }

    public void setIncognito(boolean enabled) {
      WebSettings settings = getSettings();
      if (enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          CookieManager.getInstance().removeAllCookies(null);
        } else {
          CookieManager.getInstance().removeAllCookie();
        }

        // Disable caching
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
        // settings.setAppCacheEnabled(false);
        Util.invokeMethodIfExists(settings, "setAppCacheEnabled", false);

        clearHistory();
        clearCache(true);

        // No form data or autofill enabled
        clearFormData();
        settings.setSavePassword(false);
        settings.setSaveFormData(false);
      } else {
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
        // settings.setAppCacheEnabled(true);
        Util.invokeMethodIfExists(settings, "setAppCacheEnabled", true);

        settings.setSavePassword(true);
        settings.setSaveFormData(true);
      }
    }

    public void setCacheEnabled(boolean enabled) {
      WebSettings settings = getSettings();
      if (enabled) {
        Context ctx = getContext();
        if (ctx != null) {
          // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
          // settings.setAppCachePath(ctx.getCacheDir().getAbsolutePath());
          Util.invokeMethodIfExists(settings, "setAppCachePath", ctx.getCacheDir().getAbsolutePath());

          settings.setCacheMode(WebSettings.LOAD_DEFAULT);

          // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
          // settings.setAppCacheEnabled(true);
          Util.invokeMethodIfExists(settings, "setAppCacheEnabled", true);
        }
      } else {
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
        // settings.setAppCacheEnabled(false);
        Util.invokeMethodIfExists(settings, "setAppCacheEnabled", false);
      }
    }

    public void loadUrl(URLRequest urlRequest) {
      String url = urlRequest.getUrl();
      String method = urlRequest.getMethod();
      if (method != null && method.equals("POST")) {
        byte[] postData = urlRequest.getBody();
        postUrl(url, postData);
        return;
      }
      Map<String, String> headers = urlRequest.getHeaders();
      if (headers != null) {
        loadUrl(url, headers);
        return;
      }
      loadUrl(url);
    }

    public void loadFile(String assetFilePath) throws IOException {
      if (plugin == null) {
        return;
      }

      loadUrl(Util.getUrlAsset(plugin, assetFilePath));
    }

    public boolean isLoading() {
      return isLoading;
    }

    /**
     * @deprecated
     */
    @Deprecated
    private void clearCookies() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        CookieManager.getInstance().removeAllCookies(new ValueCallback<Boolean>() {
          @Override
          public void onReceiveValue(Boolean aBoolean) {

          }
        });
      } else {
        CookieManager.getInstance().removeAllCookie();
      }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void clearAllCache() {
      clearCache(true);
      clearCookies();
      clearFormData();
      WebStorage.getInstance().deleteAllData();
    }

    public void takeScreenshot(final @Nullable Map<String, Object> screenshotConfiguration, final MethodChannel.Result result) {
      final float pixelDensity = Util.getPixelDensity(getContext());

      mainLooperHandler.post(new Runnable() {
        @Override
        public void run() {
          try {
            Bitmap screenshotBitmap = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(screenshotBitmap);
            c.translate(-getScrollX(), -getScrollY());
            draw(c);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.PNG;
            int quality = 100;

            if (screenshotConfiguration != null) {
              Map<String, Double> rect = (Map<String, Double>) screenshotConfiguration.get("rect");
              if (rect != null) {
                int rectX = (int) Math.floor(rect.get("x") * pixelDensity + 0.5);
                int rectY = (int) Math.floor(rect.get("y") * pixelDensity + 0.5);
                int rectWidth = Math.min(screenshotBitmap.getWidth(), (int) Math.floor(rect.get("width") * pixelDensity + 0.5));
                int rectHeight = Math.min(screenshotBitmap.getHeight(), (int) Math.floor(rect.get("height") * pixelDensity + 0.5));
                screenshotBitmap = Bitmap.createBitmap(
                        screenshotBitmap,
                        rectX,
                        rectY,
                        rectWidth,
                        rectHeight);
              }

              Double snapshotWidth = (Double) screenshotConfiguration.get("snapshotWidth");
              if (snapshotWidth != null) {
                int dstWidth = (int) Math.floor(snapshotWidth * pixelDensity + 0.5);
                float ratioBitmap = (float) screenshotBitmap.getWidth() / (float) screenshotBitmap.getHeight();
                int dstHeight = (int) ((float) dstWidth / ratioBitmap);
                screenshotBitmap = Bitmap.createScaledBitmap(screenshotBitmap, dstWidth, dstHeight, true);
              }

              try {
                compressFormat = Bitmap.CompressFormat.valueOf((String) screenshotConfiguration.get("compressFormat"));
              } catch (IllegalArgumentException e) {
                Log.e(LOG_TAG, "", e);
              }

              quality = (Integer) screenshotConfiguration.get("quality");
            }

            screenshotBitmap.compress(
                    compressFormat,
                    quality,
                    byteArrayOutputStream);

            try {
              byteArrayOutputStream.close();
            } catch (IOException e) {
              Log.e(LOG_TAG, "", e);
            }
            screenshotBitmap.recycle();
            result.success(byteArrayOutputStream.toByteArray());

          } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "", e);
            result.success(null);
          }
        }
      });
    }

    @SuppressLint("RestrictedApi")
    public void setSettings(InAppWebViewSettings newCustomSettings, HashMap<String, Object> newSettingsMap) {

      WebSettings settings = getSettings();

      if (newSettingsMap.get("javaScriptEnabled") != null && customSettings.javaScriptEnabled != newCustomSettings.javaScriptEnabled)
        settings.setJavaScriptEnabled(newCustomSettings.javaScriptEnabled);

      if (newSettingsMap.get("useShouldInterceptAjaxRequest") != null && customSettings.useShouldInterceptAjaxRequest != newCustomSettings.useShouldInterceptAjaxRequest) {
        enablePluginScriptAtRuntime(
                InterceptAjaxRequestJS.FLAG_VARIABLE_FOR_SHOULD_INTERCEPT_AJAX_REQUEST_JS_SOURCE,
                newCustomSettings.useShouldInterceptAjaxRequest,
                InterceptAjaxRequestJS.INTERCEPT_AJAX_REQUEST_JS_PLUGIN_SCRIPT
        );
      }

      if (newSettingsMap.get("interceptOnlyAsyncAjaxRequests") != null && customSettings.interceptOnlyAsyncAjaxRequests != newCustomSettings.interceptOnlyAsyncAjaxRequests) {
        enablePluginScriptAtRuntime(
                InterceptAjaxRequestJS.FLAG_VARIABLE_FOR_INTERCEPT_ONLY_ASYNC_AJAX_REQUESTS_JS_SOURCE,
                newCustomSettings.interceptOnlyAsyncAjaxRequests,
                interceptOnlyAsyncAjaxRequestsPluginScript
        );
      }

      if (newSettingsMap.get("useShouldInterceptFetchRequest") != null && customSettings.useShouldInterceptFetchRequest != newCustomSettings.useShouldInterceptFetchRequest) {
        enablePluginScriptAtRuntime(
                InterceptFetchRequestJS.FLAG_VARIABLE_FOR_SHOULD_INTERCEPT_FETCH_REQUEST_JS_SOURCE,
                newCustomSettings.useShouldInterceptFetchRequest,
                InterceptFetchRequestJS.INTERCEPT_FETCH_REQUEST_JS_PLUGIN_SCRIPT
        );
      }

      if (newSettingsMap.get("useOnLoadResource") != null && customSettings.useOnLoadResource != newCustomSettings.useOnLoadResource) {
        enablePluginScriptAtRuntime(
                OnLoadResourceJS.FLAG_VARIABLE_FOR_ON_LOAD_RESOURCE_JS_SOURCE,
                newCustomSettings.useOnLoadResource,
                OnLoadResourceJS.ON_LOAD_RESOURCE_JS_PLUGIN_SCRIPT
        );
      }

      if (newSettingsMap.get("javaScriptCanOpenWindowsAutomatically") != null && customSettings.javaScriptCanOpenWindowsAutomatically != newCustomSettings.javaScriptCanOpenWindowsAutomatically)
        settings.setJavaScriptCanOpenWindowsAutomatically(newCustomSettings.javaScriptCanOpenWindowsAutomatically);

      if (newSettingsMap.get("builtInZoomControls") != null && customSettings.builtInZoomControls != newCustomSettings.builtInZoomControls)
        settings.setBuiltInZoomControls(newCustomSettings.builtInZoomControls);

      if (newSettingsMap.get("displayZoomControls") != null && customSettings.displayZoomControls != newCustomSettings.displayZoomControls)
        settings.setDisplayZoomControls(newCustomSettings.displayZoomControls);

      if (newSettingsMap.get("safeBrowsingEnabled") != null && customSettings.safeBrowsingEnabled != newCustomSettings.safeBrowsingEnabled) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE))
          WebSettingsCompat.setSafeBrowsingEnabled(settings, newCustomSettings.safeBrowsingEnabled);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
          settings.setSafeBrowsingEnabled(newCustomSettings.safeBrowsingEnabled);
      }

      if (newSettingsMap.get("mediaPlaybackRequiresUserGesture") != null && customSettings.mediaPlaybackRequiresUserGesture != newCustomSettings.mediaPlaybackRequiresUserGesture)
        settings.setMediaPlaybackRequiresUserGesture(newCustomSettings.mediaPlaybackRequiresUserGesture);

      if (newSettingsMap.get("databaseEnabled") != null && customSettings.databaseEnabled != newCustomSettings.databaseEnabled)
        settings.setDatabaseEnabled(newCustomSettings.databaseEnabled);

      if (newSettingsMap.get("domStorageEnabled") != null && customSettings.domStorageEnabled != newCustomSettings.domStorageEnabled)
        settings.setDomStorageEnabled(newCustomSettings.domStorageEnabled);

      if (newSettingsMap.get("userAgent") != null && !customSettings.userAgent.equals(newCustomSettings.userAgent) && !newCustomSettings.userAgent.isEmpty())
        settings.setUserAgentString(newCustomSettings.userAgent);

      if (newSettingsMap.get("applicationNameForUserAgent") != null && !customSettings.applicationNameForUserAgent.equals(newCustomSettings.applicationNameForUserAgent) && !newCustomSettings.applicationNameForUserAgent.isEmpty()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
          String userAgent = (newCustomSettings.userAgent != null && !newCustomSettings.userAgent.isEmpty()) ? newCustomSettings.userAgent : WebSettings.getDefaultUserAgent(getContext());
          String userAgentWithApplicationName = userAgent + " " + customSettings.applicationNameForUserAgent;
          settings.setUserAgentString(userAgentWithApplicationName);
        }
      }

      if (newSettingsMap.get("clearCache") != null && newCustomSettings.clearCache)
        clearAllCache();
      else if (newSettingsMap.get("clearSessionCache") != null && newCustomSettings.clearSessionCache)
        CookieManager.getInstance().removeSessionCookie();

      if (newSettingsMap.get("thirdPartyCookiesEnabled") != null && customSettings.thirdPartyCookiesEnabled != newCustomSettings.thirdPartyCookiesEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, newCustomSettings.thirdPartyCookiesEnabled);

      if (newSettingsMap.get("useWideViewPort") != null && customSettings.useWideViewPort != newCustomSettings.useWideViewPort)
        settings.setUseWideViewPort(newCustomSettings.useWideViewPort);

      if (newSettingsMap.get("supportZoom") != null && customSettings.supportZoom != newCustomSettings.supportZoom)
        settings.setSupportZoom(newCustomSettings.supportZoom);

      if (newSettingsMap.get("textZoom") != null && !customSettings.textZoom.equals(newCustomSettings.textZoom))
        settings.setTextZoom(newCustomSettings.textZoom);

      if (newSettingsMap.get("verticalScrollBarEnabled") != null && customSettings.verticalScrollBarEnabled != newCustomSettings.verticalScrollBarEnabled)
        setVerticalScrollBarEnabled(newCustomSettings.verticalScrollBarEnabled);

      if (newSettingsMap.get("horizontalScrollBarEnabled") != null && customSettings.horizontalScrollBarEnabled != newCustomSettings.horizontalScrollBarEnabled)
        setHorizontalScrollBarEnabled(newCustomSettings.horizontalScrollBarEnabled);

      if (newSettingsMap.get("transparentBackground") != null && customSettings.transparentBackground != newCustomSettings.transparentBackground) {
        if (newCustomSettings.transparentBackground) {
          setBackgroundColor(Color.TRANSPARENT);
        } else {
          setBackgroundColor(Color.parseColor("#FFFFFF"));
        }
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        if (newSettingsMap.get("mixedContentMode") != null && (customSettings.mixedContentMode == null || !customSettings.mixedContentMode.equals(newCustomSettings.mixedContentMode)))
          settings.setMixedContentMode(newCustomSettings.mixedContentMode);

      if (newSettingsMap.get("supportMultipleWindows") != null && customSettings.supportMultipleWindows != newCustomSettings.supportMultipleWindows)
        settings.setSupportMultipleWindows(newCustomSettings.supportMultipleWindows);

      if (newSettingsMap.get("useOnDownloadStart") != null && customSettings.useOnDownloadStart != newCustomSettings.useOnDownloadStart) {
        if (newCustomSettings.useOnDownloadStart) {
          setDownloadListener(new DownloadStartListener());
        } else {
          setDownloadListener(null);
        }
      }

      if (newSettingsMap.get("allowContentAccess") != null && customSettings.allowContentAccess != newCustomSettings.allowContentAccess)
        settings.setAllowContentAccess(newCustomSettings.allowContentAccess);

      if (newSettingsMap.get("allowFileAccess") != null && customSettings.allowFileAccess != newCustomSettings.allowFileAccess)
        settings.setAllowFileAccess(newCustomSettings.allowFileAccess);

      if (newSettingsMap.get("allowFileAccessFromFileURLs") != null && customSettings.allowFileAccessFromFileURLs != newCustomSettings.allowFileAccessFromFileURLs)
        settings.setAllowFileAccessFromFileURLs(newCustomSettings.allowFileAccessFromFileURLs);

      if (newSettingsMap.get("allowUniversalAccessFromFileURLs") != null && customSettings.allowUniversalAccessFromFileURLs != newCustomSettings.allowUniversalAccessFromFileURLs)
        settings.setAllowUniversalAccessFromFileURLs(newCustomSettings.allowUniversalAccessFromFileURLs);

      if (newSettingsMap.get("cacheEnabled") != null && customSettings.cacheEnabled != newCustomSettings.cacheEnabled)
        setCacheEnabled(newCustomSettings.cacheEnabled);

      if (newSettingsMap.get("appCachePath") != null && (customSettings.appCachePath == null || !customSettings.appCachePath.equals(newCustomSettings.appCachePath))) {
        // removed from Android API 33+ (https://developer.android.com/sdk/api_diff/33/changes)
        // settings.setAppCachePath(newCustomSettings.appCachePath);
        Util.invokeMethodIfExists(settings, "setAppCachePath", newCustomSettings.appCachePath);
      }

      if (newSettingsMap.get("blockNetworkImage") != null && customSettings.blockNetworkImage != newCustomSettings.blockNetworkImage)
        settings.setBlockNetworkImage(newCustomSettings.blockNetworkImage);

      if (newSettingsMap.get("blockNetworkLoads") != null && customSettings.blockNetworkLoads != newCustomSettings.blockNetworkLoads)
        settings.setBlockNetworkLoads(newCustomSettings.blockNetworkLoads);

      if (newSettingsMap.get("cacheMode") != null && !customSettings.cacheMode.equals(newCustomSettings.cacheMode))
        settings.setCacheMode(newCustomSettings.cacheMode);

      if (newSettingsMap.get("cursiveFontFamily") != null && !customSettings.cursiveFontFamily.equals(newCustomSettings.cursiveFontFamily))
        settings.setCursiveFontFamily(newCustomSettings.cursiveFontFamily);

      if (newSettingsMap.get("defaultFixedFontSize") != null && !customSettings.defaultFixedFontSize.equals(newCustomSettings.defaultFixedFontSize))
        settings.setDefaultFixedFontSize(newCustomSettings.defaultFixedFontSize);

      if (newSettingsMap.get("defaultFontSize") != null && !customSettings.defaultFontSize.equals(newCustomSettings.defaultFontSize))
        settings.setDefaultFontSize(newCustomSettings.defaultFontSize);

      if (newSettingsMap.get("defaultTextEncodingName") != null && !customSettings.defaultTextEncodingName.equals(newCustomSettings.defaultTextEncodingName))
        settings.setDefaultTextEncodingName(newCustomSettings.defaultTextEncodingName);

      if (newSettingsMap.get("disabledActionModeMenuItems") != null &&
              (customSettings.disabledActionModeMenuItems == null ||
              !customSettings.disabledActionModeMenuItems.equals(newCustomSettings.disabledActionModeMenuItems))) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DISABLED_ACTION_MODE_MENU_ITEMS))
          WebSettingsCompat.setDisabledActionModeMenuItems(settings, newCustomSettings.disabledActionModeMenuItems);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
          settings.setDisabledActionModeMenuItems(newCustomSettings.disabledActionModeMenuItems);
      }

      if (newSettingsMap.get("fantasyFontFamily") != null && !customSettings.fantasyFontFamily.equals(newCustomSettings.fantasyFontFamily))
        settings.setFantasyFontFamily(newCustomSettings.fantasyFontFamily);

      if (newSettingsMap.get("fixedFontFamily") != null && !customSettings.fixedFontFamily.equals(newCustomSettings.fixedFontFamily))
        settings.setFixedFontFamily(newCustomSettings.fixedFontFamily);

      if (newSettingsMap.get("forceDark") != null && !customSettings.forceDark.equals(newCustomSettings.forceDark)) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK))
          WebSettingsCompat.setForceDark(settings, newCustomSettings.forceDark);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
          settings.setForceDark(newCustomSettings.forceDark);
      }

      if (newSettingsMap.get("forceDarkStrategy") != null &&
              !customSettings.forceDarkStrategy.equals(newCustomSettings.forceDarkStrategy) &&
              WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
        WebSettingsCompat.setForceDarkStrategy(settings, newCustomSettings.forceDarkStrategy);
      }

      settings.setGeolocationEnabled(customSettings.geolocationEnabled);
      if (customSettings.layoutAlgorithm != null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && customSettings.layoutAlgorithm.equals(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING)) {
          settings.setLayoutAlgorithm(customSettings.layoutAlgorithm);
        } else {
          settings.setLayoutAlgorithm(customSettings.layoutAlgorithm);
        }
      }
      settings.setLoadsImagesAutomatically(customSettings.loadsImagesAutomatically);
      settings.setMinimumFontSize(customSettings.minimumFontSize);
      settings.setMinimumLogicalFontSize(customSettings.minimumLogicalFontSize);
      setInitialScale(customSettings.initialScale);
      settings.setNeedInitialFocus(customSettings.needInitialFocus);
      if (WebViewFeature.isFeatureSupported(WebViewFeature.OFF_SCREEN_PRERASTER))
        WebSettingsCompat.setOffscreenPreRaster(settings, customSettings.offscreenPreRaster);
      else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        settings.setOffscreenPreRaster(customSettings.offscreenPreRaster);
      settings.setSansSerifFontFamily(customSettings.sansSerifFontFamily);
      settings.setSerifFontFamily(customSettings.serifFontFamily);
      settings.setStandardFontFamily(customSettings.standardFontFamily);
      if (customSettings.preferredContentMode != null &&
              customSettings.preferredContentMode == PreferredContentModeOptionType.DESKTOP.toValue()) {
        setDesktopMode(true);
      }
      settings.setSaveFormData(customSettings.saveFormData);
      if (customSettings.incognito)
        setIncognito(true);
      if (customSettings.useHybridComposition) {
        if (customSettings.hardwareAcceleration)
          setLayerType(View.LAYER_TYPE_HARDWARE, null);
        else
          setLayerType(View.LAYER_TYPE_NONE, null);
      }
      if (customSettings.regexToCancelSubFramesLoading != null) {
        regexToCancelSubFramesLoadingCompiled = Pattern.compile(customSettings.regexToCancelSubFramesLoading);
      }
      setScrollBarStyle(customSettings.scrollBarStyle);
      if (customSettings.scrollBarDefaultDelayBeforeFade != null) {
        setScrollBarDefaultDelayBeforeFade(customSettings.scrollBarDefaultDelayBeforeFade);
      } else {
        customSettings.scrollBarDefaultDelayBeforeFade = getScrollBarDefaultDelayBeforeFade();
      }
      setScrollbarFadingEnabled(customSettings.scrollbarFadingEnabled);
      if (customSettings.scrollBarFadeDuration != null) {
        setScrollBarFadeDuration(customSettings.scrollBarFadeDuration);
      } else {
        customSettings.scrollBarFadeDuration = getScrollBarFadeDuration();
      }
      setVerticalScrollbarPosition(customSettings.verticalScrollbarPosition);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        if (customSettings.verticalScrollbarThumbColor != null)
          setVerticalScrollbarThumbDrawable(new ColorDrawable(Color.parseColor(customSettings.verticalScrollbarThumbColor)));
        if (customSettings.verticalScrollbarTrackColor != null)
          setVerticalScrollbarTrackDrawable(new ColorDrawable(Color.parseColor(customSettings.verticalScrollbarTrackColor)));
        if (customSettings.horizontalScrollbarThumbColor != null)
          setHorizontalScrollbarThumbDrawable(new ColorDrawable(Color.parseColor(customSettings.horizontalScrollbarThumbColor)));
        if (customSettings.horizontalScrollbarTrackColor != null)
          setHorizontalScrollbarTrackDrawable(new ColorDrawable(Color.parseColor(customSettings.horizontalScrollbarTrackColor)));
      }

      setOverScrollMode(customSettings.overScrollMode);
      if (customSettings.networkAvailable != null) {
        setNetworkAvailable(customSettings.networkAvailable);
      }
      if (customSettings.rendererPriorityPolicy != null && (customSettings.rendererPriorityPolicy.get("rendererRequestedPriority") != null || customSettings.rendererPriorityPolicy.get("waivedWhenNotVisible") != null) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        setRendererPriorityPolicy(
                (int) customSettings.rendererPriorityPolicy.get("rendererRequestedPriority"),
                (boolean) customSettings.rendererPriorityPolicy.get("waivedWhenNotVisible"));
      }

      if (WebViewFeature.isFeatureSupported(WebViewFeature.SUPPRESS_ERROR_PAGE)) {
        WebSettingsCompat.setWillSuppressErrorPage(settings, customSettings.disableDefaultErrorPage);
      }
      if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, customSettings.algorithmicDarkeningAllowed);
      }
      if (WebViewFeature.isFeatureSupported(WebViewFeature.ENTERPRISE_AUTHENTICATION_APP_LINK_POLICY)) {
        WebSettingsCompat.setEnterpriseAuthenticationAppLinkPolicyEnabled(settings, customSettings.enterpriseAuthenticationAppLinkPolicyEnabled);
      }
      if (customSettings.requestedWithHeaderOriginAllowList != null &&
              WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
        WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, customSettings.requestedWithHeaderOriginAllowList);
      }

      contentBlockerHandler.getRuleList().clear();
      for (Map<String, Map<String, Object>> contentBlocker : customSettings.contentBlockers) {
        // compile ContentBlockerTrigger urlFilter
        ContentBlockerTrigger trigger = ContentBlockerTrigger.fromMap(contentBlocker.get("trigger"));
        ContentBlockerAction action = ContentBlockerAction.fromMap(contentBlocker.get("action"));
        contentBlockerHandler.getRuleList().add(new ContentBlocker(trigger, action));
      }

      setFindListener(new FindListener() {
        @Override
        public void onFindResultReceived(int activeMatchOrdinal, int numberOfMatches, boolean isDoneCounting) {
          if (findInteractionController != null && findInteractionController.channelDelegate != null)
            findInteractionController.channelDelegate.onFindResultReceived(activeMatchOrdinal, numberOfMatches, isDoneCounting);
          if (channelDelegate != null)
            channelDelegate.onFindResultReceived(activeMatchOrdinal, numberOfMatches, isDoneCounting);
        }
      });

      gestureDetector = new GestureDetector(this.getContext(), new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent ev) {
          if (floatingContextMenu != null) {
            hideContextMenu();
          }
          return super.onSingleTapUp(ev);
        }
      });

      checkScrollStoppedTask = new Runnable() {
        @Override
        public void run() {
          int newPosition = getScrollY();
          if (initialPositionScrollStoppedTask - newPosition == 0) {
            // has stopped
            onScrollStopped();
          } else {
            initialPositionScrollStoppedTask = getScrollY();
            mainLooperHandler.postDelayed(checkScrollStoppedTask, newCheckScrollStoppedTask);
          }
        }
      };

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && !customSettings.useHybridComposition) {
        checkContextMenuShouldBeClosedTask = new Runnable() {
          @Override
          public void run() {
            if (floatingContextMenu != null) {
              evaluateJavascript(PluginScriptsUtil.CHECK_CONTEXT_MENU_SHOULD_BE_HIDDEN_JS_SOURCE, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                  if (value == null || value.equals("true")) {
                    if (floatingContextMenu != null) {
                      hideContextMenu();
                    }
                  } else {
                    mainLooperHandler.postDelayed(checkContextMenuShouldBeClosedTask, newCheckContextMenuShouldBeClosedTaskTask);
                  }
                }
              });
            }
          }
        };
      }

      setOnTouchListener(new OnTouchListener() {
        float m_downX;
        float m_downY;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
          gestureDetector.onTouchEvent(event);

          if (event.getAction() == MotionEvent.ACTION_UP) {
            checkScrollStoppedTask.run();
          }

          if (customSettings.disableHorizontalScroll && customSettings.disableVerticalScroll) {
            return (event.getAction() == MotionEvent.ACTION_MOVE);
          } else if (customSettings.disableHorizontalScroll || customSettings.disableVerticalScroll) {
            switch (event.getAction()) {
              case MotionEvent.ACTION_DOWN: {
                // save the x
                m_downX = event.getX();
                // save the y
                m_downY = event.getY();
                break;
              }
              case MotionEvent.ACTION_MOVE:
              case MotionEvent.ACTION_CANCEL:
              case MotionEvent.ACTION_UP: {
                if (customSettings.disableHorizontalScroll) {
                  // set x so that it doesn't move
                  event.setLocation(m_downX, event.getY());
                } else {
                  // set y so that it doesn't move
                  event.setLocation(event.getX(), m_downY);
                }
                break;
              }
            }
          }
          return false;
        }
      });

      setOnLongClickListener(new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
          wtf.zikzak.zikzak_inappwebview_android.types.HitTestResult hitTestResult =
                  wtf.zikzak.zikzak_inappwebview_android.types.HitTestResult.fromWebViewHitTestResult(getHitTestResult());
          if (channelDelegate != null) channelDelegate.onLongPressHitTestResult(hitTestResult);
          return false;
        }
      });
    }

    public void prepareAndAddUserScripts() {
      userContentController.addPluginScript(PromisePolyfillJS.PROMISE_POLYFILL_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(JavaScriptBridgeJS.JAVASCRIPT_BRIDGE_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(ConsoleLogJS.CONSOLE_LOG_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(PrintJS.PRINT_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(OnWindowBlurEventJS.ON_WINDOW_BLUR_EVENT_JS_PLUGIN_SCRIPT);
      userContentController.addPluginScript(OnWindowFocusEventJS.ON_WINDOW_FOCUS_EVENT_JS_PLUGIN_SCRIPT);
      interceptOnlyAsyncAjaxRequestsPluginScript = InterceptAjaxRequestJS.createInterceptOnlyAsyncAjaxRequestsPluginScript(customSettings.interceptOnlyAsyncAjaxRequests);
      if (customSettings.useShouldInterceptAjaxRequest) {
        userContentController.addPluginScript(interceptOnlyAsyncAjaxRequestsPluginScript);
        userContentController.addPluginScript(InterceptAjaxRequestJS.INTERCEPT_AJAX