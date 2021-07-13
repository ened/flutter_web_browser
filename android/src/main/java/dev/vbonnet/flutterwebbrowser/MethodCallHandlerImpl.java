package dev.vbonnet.flutterwebbrowser;

import static androidx.browser.customtabs.CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MethodCallHandlerImpl implements MethodCallHandler {

	private static final String TAG = MethodCallHandlerImpl.class.getSimpleName();

  private Activity activity;

  public void setActivity(Activity activity) {
    this.activity = activity;
  }

  @Override
  public void onMethodCall(MethodCall call, @NonNull Result result) {
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
			public void onCustomTabsServiceConnected(@NonNull ComponentName componentName, CustomTabsClient client) {
				final CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
				client.warmup(0L); // This prevents backgrounding after redirection

				String url = call.argument("url");
				HashMap<String, Object> options = call.argument("android_options");

				builder.setColorScheme((Integer) options.get("colorScheme"));

				builder.setInstantAppsEnabled(false);

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
				customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);

				customTabsIntent.launchUrl(activity, Uri.parse(url));

				result.success(null);
			}
			@Override
			public void onServiceDisconnected(ComponentName name) {}
		};

		ArrayList<ResolveInfo> packages = getCustomTabsPackages(activity);
		for(ResolveInfo resolveInfo : packages) {
			Log.d(TAG, "discovered browser package: " + resolveInfo);
		}

		CustomTabsClient.bindCustomTabsService(activity, "com.android.chrome", connection);
  }

	/**
	 * Returns a list of packages that support Custom Tabs.
	 */
	public static ArrayList<ResolveInfo> getCustomTabsPackages(Context context) {
		PackageManager pm = context.getPackageManager();
		// Get default VIEW intent handler.
		Intent activityIntent = new Intent()
				.setAction(Intent.ACTION_VIEW)
				.addCategory(Intent.CATEGORY_BROWSABLE)
				.setData(Uri.fromParts("http", "", null));

		// Get all apps that can handle VIEW intents.
		List<ResolveInfo> resolvedActivityList = pm.queryIntentActivities(activityIntent, 0);
		ArrayList<ResolveInfo> packagesSupportingCustomTabs = new ArrayList<>();
		for (ResolveInfo info : resolvedActivityList) {
			Intent serviceIntent = new Intent();
			serviceIntent.setAction(ACTION_CUSTOM_TABS_CONNECTION);
			serviceIntent.setPackage(info.activityInfo.packageName);
			// Check if this package also resolves the Custom Tabs service.
			if (pm.resolveService(serviceIntent, 0) != null) {
				packagesSupportingCustomTabs.add(info);
			}
		}
		return packagesSupportingCustomTabs;
	}

  private void warmup(Result result) {
    boolean success = CustomTabsClient.connectAndInitialize(activity, getPackageName());
    result.success(success);
  }

  private String getPackageName() {
    return CustomTabsClient.getPackageName(activity,
				Collections.singletonList("com.android.chrome"));
  }
}
