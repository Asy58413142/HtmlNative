package com.mozz.htmlnative;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.mozz.htmlnative.common.ContextProvider;
import com.mozz.htmlnative.css.stylehandler.StyleHandlerFactory;
import com.mozz.htmlnative.dom.HNHead;
import com.mozz.htmlnative.http.HNHttpClient;
import com.mozz.htmlnative.script.ScriptLib;
import com.mozz.htmlnative.script.ScriptRunner;
import com.mozz.htmlnative.script.lua.EmptyHttpClient;
import com.mozz.htmlnative.utils.ParametersUtils;
import com.mozz.htmlnative.view.BackgroundViewDelegate;

import java.io.InputStream;
import java.lang.ref.WeakReference;

/**
 * @author Yang Tao, 17/2/21.
 */

public final class HNativeEngine {


    static {
        HNRenderer.registerViewFactory(WebView.class.getName(), DefaultWebViewFactory.sInstance);
    }

    private static ImageViewAdapter sImageViewAdapter = DefaultImageAdapter.sInstance;
    private static HrefLinkHandler sHrefLinkHandler = DefaultHrefLinkHandler.sInstance;
    private static HNHttpClient sHttpClient = EmptyHttpClient.instance;

    private HNativeEngine() {
        HNInternalThread.init();
        HNScriptRunnerThread.init();
    }

    @Nullable
    private static HNativeEngine sInstance = null;

    public static void init(Application application) {
        ContextProvider.install(application);
        initScreenMetrics(application);
    }

    @Nullable
    public static HNativeEngine getInstance() {
        if (sInstance == null) {
            synchronized (HNativeEngine.class) {
                if (sInstance == null) {
                    sInstance = new HNativeEngine();
                }
            }
        }

        return sInstance;
    }

    public void debugParseProcess() {
        HNLog.setDebugLevel(HNLog.LEXER);
        HNLog.setDebugLevel(HNLog.PARSER);
        HNLog.setDebugLevel(HNLog.CSS_PARSER);
    }

    public void debugRenderProcess() {
        HNLog.setDebugLevel(HNLog.RENDER);
        HNLog.setDebugLevel(HNLog.STYLE);
        HNLog.setDebugLevel(HNLog.PROCESS_THREAD);
    }

    private static void initScreenMetrics(@NonNull Context context) {
        ParametersUtils.init(context);
    }

    public final void loadView(final Context context, final InputStream inputStream, final
    OnHNViewLoaded onHNViewLoaded) {
        HNProcessThread.runRenderTask(new HNProcessThread.RenderTask(context, inputStream,
                onHNViewLoaded));
    }

    public void loadView(Context context, InputStream inputStream, final Activity activity) {
        loadView(context, inputStream, new OnHNViewLoadedWeak<Activity>(activity) {
            @Override
            public void onViewLoaded(@Nullable View v) {
                Activity act = mWeakRef.get();
                if (act != null && !act.isDestroyed() && v != null) {
                    act.setContentView(v);
                }
            }

            @Override
            public void onError(Exception e) {

            }

            @Override
            public void onHead(HNHead head) {
            }
        });
    }

    public void loadView(Context context, InputStream inputStream, final ViewGroup viewGroup) {
        loadView(context, inputStream, new OnHNViewLoadedWeak<ViewGroup>(viewGroup) {
            @Override
            public void onViewLoaded(View v) {
                ViewGroup vv = mWeakRef.get();
                vv.addView(v);
            }

            @Override
            public void onError(Exception e) {

            }

            @Override
            public void onHead(HNHead head) {

            }
        });
    }

    public static String version() {
        return HNEnvironment.v;
    }

    public static int versionCode() {
        return HNEnvironment.versionCode;
    }

    /**
     * @param tag
     * @param HNViewItem
     */
    public static void registerHNiew(String tag, @NonNull HNViewItem HNViewItem) {
        ViewTypeRelations.registerExtraView(tag, HNViewItem);
    }

    public void destroy() {
        HNSegment.clearCache();
        HNInternalThread.quit();
        HNScriptRunnerThread.quit();
        StyleHandlerFactory.clear();
    }

    public void setImageViewAdapter(@NonNull ImageViewAdapter adapter) {
        sImageViewAdapter = adapter;
    }

    public static ImageViewAdapter getImageViewAdapter() {
        return sImageViewAdapter;
    }

    public static void registerViewFactory(String androidViewClassName, ViewFactory viewFactory) {
        HNRenderer.registerViewFactory(androidViewClassName, viewFactory);
    }

    public void setHrefLinkHandler(@NonNull HrefLinkHandler handler) {
        sHrefLinkHandler = handler;
    }

    public static HrefLinkHandler getHrefLinkHandler() {
        return sHrefLinkHandler;
    }

    public static void registerScriptCallback(OnScriptCallback callback) {
        HNScriptRunnerThread.setErrorCallback(callback);
    }

    public static void registerHttpClient(HNHttpClient client) {
        sHttpClient = client;
    }

    public static HNHttpClient getHttpClient() {
        return sHttpClient;
    }

    public static final void registerScriptLib(ScriptLib lib) {
        ScriptRunner.registerLib(lib);
    }


    public interface OnHNViewLoaded {
        void onViewLoaded(View v);

        void onError(Exception e);

        void onHead(HNHead head);
    }

    private abstract class OnHNViewLoadedWeak<T> implements OnHNViewLoaded {
        protected WeakReference<T> mWeakRef;

        public OnHNViewLoadedWeak(T tt) {
            this.mWeakRef = new WeakReference<>(tt);
        }
    }

    /**
     * @author Yang Tao, 17/3/11.
     */

    private static final class DefaultHrefLinkHandler implements HrefLinkHandler {

        @NonNull
        static final DefaultHrefLinkHandler sInstance;

        static {
            sInstance = new DefaultHrefLinkHandler();
        }

        @Override
        public void onHref(String url, @NonNull View view) {
            Intent intent = new Intent();
            intent.setAction("Android.intent.action.VIEW");
            Uri content_url = Uri.parse(url);
            intent.setData(content_url);
            view.getContext().startActivity(intent);
        }
    }

    /**
     * @author Yang Tao, 17/3/10.
     */

    private static final class DefaultImageAdapter implements ImageViewAdapter {

        static DefaultImageAdapter sInstance;

        static {
            sInstance = new DefaultImageAdapter();
        }

        private DefaultImageAdapter() {
        }

        @Override
        public void setImage(String src, BackgroundViewDelegate imageView) {
            //do nothing
        }
    }

    /**
     * @author Yang Tao, 17/3/8.
     */

    private static final class DefaultWebViewFactory implements WebViewFactory {

        static DefaultWebViewFactory sInstance;

        static {
            sInstance = new DefaultWebViewFactory();
        }


        @NonNull
        @Override
        public WebView create(Context context) {
            return new WebView(context);
        }
    }


}
