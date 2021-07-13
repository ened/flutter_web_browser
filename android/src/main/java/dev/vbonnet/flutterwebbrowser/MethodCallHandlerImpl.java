package dev.vbonnet.flutterwebbrowser;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.app.Activity;
import android.content.ComponentName;
import android.graphics.Color;
import android.net.Uri;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import java.util.Arrays;
import java.util.HashMap;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

public class MethodCallHandlerImpl implements MethodCallHandler {

  private Activity activity;

  public void setActivity(Activity activity) {
    this.activity = activity;
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    switch (call.method) {
      case "openWebPage":
        openUrl(call, result);
        break;
      case "warmup":
        warmup(result);
        break;
      default:
        result.notImplemented();
        break;
    }
  }

  private void openUrl(final MethodCall call, final Result result) {
    if (activity == null) {
      result.error("no_activity", "Plugin is only available within a activity context", null);
      return;
    }

		final CustomTabsServiceConnection connection = new CustomTabsServiceConnection() {
			@Override
			public void onCustomTabsServiceConnected(ComponentName componentName, CustomTabsClient client) {
				final CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
				final CustomTabsIntent intent = builder.build();
				client.warmup(0L); // This prevents backgrounding after redirection

				String url = call.argument("url");
				HashMap<String, Object> options = call.<HashMap<String, Object>>argument("android_options");

				builder.setColorScheme((Integer) options.get("colorScheme"));

				String navigationBarColor = (String)options.get("navigationBarColor");
				if (navigationBarColor != null) {
					builder.setNavigationBarColor(Color.parseColor(navigationBarColor));
				}

				String toolbarColor = (String)options.get("toolbarColor");
				if (toolbarColor != null) {
					builder.setToolbarColor(Color.parseColor(toolbarColor));
				}

				String secondaryToolbarColor = (String)options.get("secondaryToolbarColor");
				if (secondaryToolbarColor != null) {
					builder.setSecondaryToolbarColor(Color.parseColor(secondaryToolbarColor));
				}

				builder.setInstantAppsEnabled((Boolean) options.get("instantAppsEnabled"));

				if ((Boolean) options.get("addDefaultShareMenuItem")) {
					builder.addDefaultShareMenuItem();
				}

				builder.setShowTitle((Boolean) options.get("showTitle"));

				if ((Boolean) options.get("urlBarHidingEnabled")) {
					builder.enableUrlBarHiding();
				}

				CustomTabsIntent customTabsIntent = builder.build();
				customTabsIntent.intent.setPackage(getPackageName());
//				customTabsIntent.intent.setFlags(FLAG_ACTIVITY_NEW_TASK);

				customTabsIntent.launchUrl(activity, Uri.parse(url));

				result.success(null);
			}
			@Override
			public void onServiceDisconnected(ComponentName name) {}
		};

		CustomTabsClient.bindCustomTabsService(activity, "com.android.chrome", connection);
  }

  private void warmup(Result result) {
    boolean success = CustomTabsClient.connectAndInitialize(activity, getPackageName());
    result.success(success);
  }

  private String getPackageName() {
    return CustomTabsClient.getPackageName(activity, Arrays.asList("com.android.chrome"));
  }
}
