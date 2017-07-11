package com.mozz.htmlnative.css;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;

import com.mozz.htmlnative.HNLog;
import com.mozz.htmlnative.HNSandBoxContext;
import com.mozz.htmlnative.HNativeEngine;
import com.mozz.htmlnative.InheritStyleStack;
import com.mozz.htmlnative.common.PixelValue;
import com.mozz.htmlnative.css.stylehandler.LayoutStyleHandler;
import com.mozz.htmlnative.css.stylehandler.StyleHandler;
import com.mozz.htmlnative.css.stylehandler.StyleHelper;
import com.mozz.htmlnative.dom.DomElement;
import com.mozz.htmlnative.exception.AttrApplyException;
import com.mozz.htmlnative.utils.ParametersUtils;
import com.mozz.htmlnative.utils.ResourceUtils;
import com.mozz.htmlnative.view.BackgroundViewDelegate;
import com.mozz.htmlnative.view.HNDivLayout;
import com.mozz.htmlnative.view.IBackgroundView;
import com.mozz.htmlnative.view.LayoutParamsCreator;

import java.util.Iterator;

/**
 * @author Yang Tao, 17/3/30.
 */
public final class Styles {

    private Styles() {
    }

    public static final String ATTR_STYLE = "style";
    public static final String ATTR_WIDTH = "width";
    public static final String ATTR_HEIGHT = "height";
    public static final String ATTR_BACKGROUND = "background";
    public static final String ATTR_PADDING = "padding";
    public static final String ATTR_PADDING_LEFT = "padding-left";
    public static final String ATTR_PADDING_RIGHT = "padding-right";
    public static final String ATTR_PADDING_TOP = "padding-top";
    public static final String ATTR_PADDING_BOTTOM = "padding-bottom";
    public static final String ATTR_MARGIN = "margin";
    public static final String ATTR_MARGIN_LEFT = "margin-left";
    public static final String ATTR_MARGIN_RIGHT = "margin-right";
    public static final String ATTR_MARGIN_TOP = "margin-top";
    public static final String ATTR_MARGIN_BOTTOM = "margin-bottom";
    public static final String ATTR_LEFT = "left";
    public static final String ATTR_TOP = "top";
    public static final String ATTR_RIGHT = "right";
    public static final String ATTR_BOTTOM = "bottom";
    public static final String ATTR_FLOAT = "float";
    public static final String ATTR_ALPHA = "alpha";
    public static final String ATTR_ONCLICK = "onclick";
    public static final String ATTR_VISIBLE = "visibility";
    public static final String ATTR_DISPLAY = "display";
    public static final String ATTR_DIRECTION = "direction";
    public static final String ATTR_HREF = "href";

    // Hn specified styles:

    public static final String ATTR_HN_BACKGROUND = "-hn-background";

    public static final String VAL_FILL_PARENT = "100%";
    public static final String VAL_WRAP_CONTENT = "auto";

    public static final String VAL_DISPLAY_FLEX = "flex";
    public static final String VAL_DISPLAY_BOX = "box";
    public static final String VAL_DISPLAY_ABSOLUTE = "absolute";

    static {
        InheritStylesRegistry.register(ATTR_VISIBLE);
        InheritStylesRegistry.register(ATTR_DIRECTION);

        // to protect the build-in styles
        InheritStylesRegistry.preserve(ATTR_STYLE);
        InheritStylesRegistry.preserve(ATTR_WIDTH);
        InheritStylesRegistry.preserve(ATTR_HEIGHT);
        InheritStylesRegistry.preserve(ATTR_BACKGROUND);
        InheritStylesRegistry.preserve(ATTR_PADDING);
        InheritStylesRegistry.preserve(ATTR_PADDING_LEFT);
        InheritStylesRegistry.preserve(ATTR_PADDING_RIGHT);
        InheritStylesRegistry.preserve(ATTR_PADDING_TOP);
        InheritStylesRegistry.preserve(ATTR_PADDING_BOTTOM);
        InheritStylesRegistry.preserve(ATTR_MARGIN);
        InheritStylesRegistry.preserve(ATTR_MARGIN_LEFT);
        InheritStylesRegistry.preserve(ATTR_MARGIN_RIGHT);
        InheritStylesRegistry.preserve(ATTR_MARGIN_TOP);
        InheritStylesRegistry.preserve(ATTR_MARGIN_BOTTOM);
        InheritStylesRegistry.preserve(ATTR_LEFT);
        InheritStylesRegistry.preserve(ATTR_TOP);
        InheritStylesRegistry.preserve(ATTR_RIGHT);
        InheritStylesRegistry.preserve(ATTR_BOTTOM);
        InheritStylesRegistry.preserve(ATTR_FLOAT);
        InheritStylesRegistry.preserve(ATTR_ALPHA);
        InheritStylesRegistry.preserve(ATTR_ONCLICK);
        InheritStylesRegistry.preserve(ATTR_DISPLAY);
        InheritStylesRegistry.preserve(ATTR_HREF);

    }


    public static void applySingleStyle(Context context, final HNSandBoxContext sandBoxContext,
                                        View v, DomElement domElement, @NonNull LayoutParamsCreator layoutCreator, @NonNull
                                                ViewGroup parent, StyleHandler viewStyleHandler,
                                        StyleHandler extraStyleHandler, LayoutStyleHandler
                                                parentAttr, StyleEntry entry, InheritStyleStack
                                                outStack) throws AttrApplyException {
        applySingleStyle(context, sandBoxContext, v, domElement, layoutCreator, parent,
                viewStyleHandler, extraStyleHandler, parentAttr, entry.getStyleName(), entry
                        .getStyle(), outStack);
    }

    /**
     * Apply a params with value to a view
     *
     * @param context        {@link Context}
     * @param sandBoxContext {@link HNSandBoxContext}
     * @param v              {@link View} view to be processed
     * @param domElement     {@link DomElement} dom element
     * @param layoutCreator  {@link ViewGroup.LayoutParams}, layoutParams for parent
     *                       when add this view to parent
     * @param parent         {@link ViewGroup}, parent of the view
     * @param styleName      parameter name
     * @param style          parameter value     @throws AttrApplyException
     */
    public static void applySingleStyle(@NonNull Context context, @NonNull final HNSandBoxContext
            sandBoxContext, @NonNull View v, @Nullable DomElement domElement, @NonNull LayoutParamsCreator layoutCreator, @NonNull ViewGroup parent, StyleHandler
            viewStyleHandler, StyleHandler extraStyleHandler, LayoutStyleHandler parentAttr,
                                        String styleName, final Object style, InheritStyleStack
                                                outStack) throws AttrApplyException {

        if (styleName == null || style == null) {
            return;
        }

        if (domElement != null) {
            HNLog.d(HNLog.STYLE, "set style \"" + styleName + ": " + style + "\"  to " +
                    domElement.getType());
        }

        switch (styleName) {
            case ATTR_WIDTH: {
                if (style.toString().equalsIgnoreCase(VAL_FILL_PARENT)) {
                    layoutCreator.width = ViewGroup.LayoutParams.MATCH_PARENT;
                } else if (style.toString().equalsIgnoreCase(VAL_WRAP_CONTENT)) {
                    layoutCreator.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                } else {
                    try {
                        PixelValue pixel = ParametersUtils.toPixel(style);
                        layoutCreator.width = (int) pixel.getPxValue();
                    } catch (ParametersUtils.ParametersParseException e) {
                        e.printStackTrace();
                    }
                }
            }
            break;

            case ATTR_HEIGHT: {
                if (style.toString().equalsIgnoreCase(VAL_FILL_PARENT)) {
                    layoutCreator.height = ViewGroup.LayoutParams.MATCH_PARENT;
                } else if (style.toString().equalsIgnoreCase(VAL_WRAP_CONTENT)) {
                    layoutCreator.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                } else {
                    try {
                        PixelValue pixel = ParametersUtils.toPixel(style);
                        layoutCreator.height = (int) pixel.getPxValue();
                    } catch (ParametersUtils.ParametersParseException e) {
                        e.printStackTrace();
                    }

                }
            }
            break;

            case ATTR_BACKGROUND:
                if (style instanceof Background) {
                    Background background = (Background) style;

                    if (background.isAndroidResource() && v instanceof IBackgroundView) {
                        Drawable drawable = ResourceUtils.getDrawable(((Background) style).getUrl
                                ().substring(1), context);
                        if (drawable != null) {
                            ((IBackgroundView) v).setHtmlBackground(drawable, background);
                        }
                    } else if (!TextUtils.isEmpty(background.getUrl()) && v instanceof
                            IBackgroundView) {
                        Matrix matrix = Background.createBitmapMatrix(background);
                        HNativeEngine.getImageViewAdapter().setImage(background.getUrl(), new
                                BackgroundViewDelegate(v, matrix, background));
                    } else if (background.isColorSet()) {
                        if (v instanceof IBackgroundView) {
                            ((IBackgroundView) v).setHtmlBackground((Bitmap) null, background);
                        } else {
                            v.setBackgroundColor(background.getColor());
                        }
                    }
                } else {
                    throw new AttrApplyException("Background style is wrong when parsing.");
                }

                break;

            case ATTR_MARGIN: {
                try {
                    PixelValue[] pixelValues = ParametersUtils.toPixels(style.toString());
                    int top = -1;
                    int bottom = -1;
                    int left = -1;
                    int right = -1;
                    if (pixelValues.length == 1) {
                        top = bottom = left = right = (int) pixelValues[0].getPxValue();
                    } else if (pixelValues.length == 2) {
                        top = bottom = (int) pixelValues[0].getPxValue();
                        left = right = (int) pixelValues[1].getPxValue();
                    } else if (pixelValues.length == 4) {
                        top = (int) pixelValues[0].getPxValue();
                        bottom = (int) pixelValues[2].getPxValue();
                        left = (int) pixelValues[3].getPxValue();
                        right = (int) pixelValues[1].getPxValue();
                    }
                    if (top != -1 && bottom != -1 && left != -1 && right != -1) {
                        layoutCreator.setMargins(left, top, right, bottom);
                    }
                } catch (ParametersUtils.ParametersParseException e) {
                    e.printStackTrace();
                }
            }
            break;

            case ATTR_MARGIN_RIGHT:
                try {
                    layoutCreator.marginRight = (int) ParametersUtils.toPixel(style).getPxValue();
                } catch (ParametersUtils.ParametersParseException e) {
                    e.printStackTrace();
                }
                break;

            case ATTR_MARGIN_LEFT:
                try {
                    layoutCreator.marginLeft = (int) ParametersUtils.toPixel(style).getPxValue();
                } catch (ParametersUtils.ParametersParseException e) {
                    e.printStackTrace();
                }
                break;

            case ATTR_MARGIN_TOP:
                try {
                    layoutCreator.marginTop = (int) ParametersUtils.toPixel(style).getPxValue();
                } catch (ParametersUtils.ParametersParseException e) {
                    e.printStackTrace();
                }
                break;

            case ATTR_MARGIN_BOTTOM:
                try {
                    layoutCreator.marginBottom = (int) ParametersUtils.toPixel(style).getPxValue();
                } catch (ParametersUtils.ParametersParseException e) {
                    e.printStackTrace();
                }
                break;

            case ATTR_PADDING: {
                try {
                    PixelValue[] pixelValues = ParametersUtils.toPixels(style.toString());
                    int top = -1;
                    int bottom = -1;
                    int left = -1;
                    int right = -1;
                    if (pixelValues.length == 1) {
                        top = bottom = left = right = (int) pixelValues[0].getPxValue();
                    } else if (pixelValues.length == 2) {
                        top = bottom = (int) pixelValues[0].getPxValue();
                        left = right = (int) pixelValues[1].getPxValue();
                    } else if (pixelValues.length == 4) {
                        top = (int) pixelValues[0].getPxValue();
                        bottom = (int) pixelValues[2].getPxValue();
                        left = (int) pixelValues[3].getPxValue();
                        right = (int) pixelValues[1].getPxValue();
                    }
                    if (top != -1 && bottom != -1 && left != -1 && right != -1) {
                        v.setPadding(left, top, right, bottom);
                    }
                } catch (ParametersUtils.ParametersParseException e) {
                    e.printStackTrace();
                }
            }
            break;
            case ATTR_HREF:
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (HNativeEngine.getHrefLinkHandler() != null) {
                            HNativeEngine.getHrefLinkHandler().onHref(style.toString(), v);
                        }
                    }
                });
                break;
            case ATTR_PADDING_LEFT:
                try {
                    int paddingLeft = (int) ParametersUtils.toPixel(style).getPxValue();
                    StyleHelper.setLeftPadding(v, paddingLeft);
                } catch (ParametersUtils.ParametersParseException e) {
                    e.printStackTrace();
                }

                break;

            case ATTR_PADDING_RIGHT:
                try {
                    int paddingRight = (int) ParametersUtils.toPixel(style).getPxValue();
                    StyleHelper.setRightPadding(v, paddingRight);
                } catch (ParametersUtils.ParametersParseException e) {
                    e.printStackTrace();
                }
                break;

            case ATTR_PADDING_TOP:
                try {
                    int paddingTop = (int) ParametersUtils.toPixel(style).getPxValue();
                    StyleHelper.setTopPadding(v, paddingTop);
                } catch (ParametersUtils.ParametersParseException e) {
                    e.printStackTrace();
                }
                break;

            case ATTR_PADDING_BOTTOM:
                try {
                    int paddingBottom = (int) ParametersUtils.toPixel(style).getPxValue();
                    StyleHelper.setBottomPadding(v, paddingBottom);
                } catch (ParametersUtils.ParametersParseException e) {
                    e.printStackTrace();
                }
                break;

            case ATTR_LEFT:
                try {
                    layoutCreator.left = (int) ParametersUtils.toPixel(style).getPxValue();
                } catch (ParametersUtils.ParametersParseException e) {
                    e.printStackTrace();
                }
                break;

            case ATTR_TOP:
                try {
                    layoutCreator.top = (int) ParametersUtils.toPixel(style).getPxValue();
                } catch (ParametersUtils.ParametersParseException e) {
                    e.printStackTrace();
                }
                break;

            case ATTR_RIGHT:
                try {
                    layoutCreator.right = (int) ParametersUtils.toPixel(style).getPxValue();
                } catch (ParametersUtils.ParametersParseException e) {
                    e.printStackTrace();
                }
                break;

            case ATTR_BOTTOM:
                try {
                    layoutCreator.bottom = (int) ParametersUtils.toPixel(style).getPxValue();
                } catch (ParametersUtils.ParametersParseException e) {
                    e.printStackTrace();
                }
                break;

            case ATTR_FLOAT: {
                switch (style.toString()) {
                    case "left":
                        layoutCreator.positionMode = HNDivLayout.HNDivLayoutParams.POSITION_FLOAT_LEFT;
                        break;
                    case "right":
                        layoutCreator.positionMode = HNDivLayout.HNDivLayoutParams.POSITION_FLOAT_RIGHT;
                        break;
                }
            }
            break;

            case ATTR_ALPHA:
                try {
                    float alpha = ParametersUtils.toFloat(style);
                    v.setAlpha(alpha);
                } catch (ParametersUtils.ParametersParseException ignored) {

                }
                break;

            case ATTR_VISIBLE:
                String visible = style.toString();

                if (visible.equals("visible")) {
                    v.setVisibility(View.VISIBLE);
                } else if (visible.equals("invisible")) {
                    v.setVisibility(View.INVISIBLE);
                }
                break;
            case ATTR_DIRECTION:
                String direction = style.toString();
                if (direction.equals("ltr")) {
                    v.setTextDirection(View.TEXT_DIRECTION_LTR);
                } else if (direction.equals("rtl")) {
                    v.setTextDirection(View.TEXT_DIRECTION_RTL);
                }
                break;

            case ATTR_ONCLICK:
                if (style instanceof String) {
                    final String functionName = (String) style;
                    v.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            // to make sure the ui function is executed as fast as possible
                            sandBoxContext.executeUIFun(functionName);
                        }
                    });

                }
                break;

            default:

                // If not common attrs, then
                // 1. apply the corresponding view attr first;
                // 2. apply the extra attr
                // 3. use parent view attr to this

                if (viewStyleHandler != null) {
                    viewStyleHandler.apply(context, v, domElement, parent, layoutCreator,
                            styleName, style);
                }

                // If there extra attr is set, then should be applied also.
                if (extraStyleHandler != null) {
                    extraStyleHandler.apply(context, v, domElement, parent, layoutCreator,
                            styleName, style);
                }

                // finally apply corresponding parent attr to child
                if (parentAttr != null) {
                    parentAttr.applyToChild(context, v, domElement, parent, layoutCreator,
                            styleName, style);
                }
                break;
        }

        // Put inherit style into stack
        if (outStack != null && InheritStylesRegistry.isInherit(styleName)) {
            outStack.newStyle(styleName, style);
        }
    }

    /**
     * Apply a default style to view
     */
    public static void setDefaultStyle(Context context, final HNSandBoxContext sandBoxContext,
                                       View v, DomElement domElement, @NonNull ViewGroup parent,
                                       StyleHandler viewStyleHandler, StyleHandler
                                               extraStyleHandler, LayoutStyleHandler parentAttr,
                                       @NonNull LayoutParamsCreator paramsLazyCreator) throws
            AttrApplyException {
        if (viewStyleHandler != null) {
            viewStyleHandler.setDefault(context, v, domElement, paramsLazyCreator, parent);
        }

        if (extraStyleHandler != null) {
            extraStyleHandler.setDefault(context, v, domElement, paramsLazyCreator, parent);
        }

        if (parentAttr != null) {
            parentAttr.setDefaultToChild(context, v, domElement, parent, paramsLazyCreator);
        }
    }

    /**
     * apply the attr to the view
     *
     * @param context           {@link Context}
     * @param sandBoxContext    {@link HNSandBoxContext}
     * @param v                 {@link View}
     * @param tree              {@link AttrsSet.AttrsOwner}
     * @param parent            {@link ViewGroup}, parent of the view
     * @param paramsLazyCreator {@link ViewGroup.LayoutParams}, layoutParams for parent
     *                          when add this view to parent
     * @param viewStyleHandler
     * @param extraStyleHandler
     * @param parentAttrHandler @throws AttrApplyException
     */
    public static void applyStyles(Context context, @NonNull final HNSandBoxContext
            sandBoxContext, AttrsSet source, View v, @NonNull AttrsSet.AttrsOwner tree,
                                   DomElement domElement, @NonNull ViewGroup parent, @NonNull LayoutParamsCreator paramsLazyCreator,
                                   StyleHandler viewStyleHandler, StyleHandler extraStyleHandler,
                                   LayoutStyleHandler parentAttrHandler, InheritStyleStack stack)
            throws AttrApplyException {
        // Apply the default attr to view first;
        // Then process each parameter.
        Iterator<StyleEntry> itr = source.iterator(tree);
        while (itr.hasNext()) {
            StyleEntry styleEntry = itr.next();

            applySingleStyle(context, sandBoxContext, v, domElement, paramsLazyCreator, parent,
                    viewStyleHandler, extraStyleHandler, parentAttrHandler, styleEntry
                            .getStyleName(), styleEntry.getStyle(), stack);

        }
    }

    public static Object getStyle(View v, String styleName, StyleHandler styleHandler,
                                  StyleHandler extraStyleHandler, LayoutStyleHandler
                                          parentHandler) {
        switch (styleName) {
            case ATTR_WIDTH:
                int width = v.getLayoutParams().width;
                if (width == ViewGroup.LayoutParams.MATCH_PARENT) {
                    return VAL_FILL_PARENT;
                } else {
                    return v.getLayoutParams().width + "px";
                }

            case ATTR_HEIGHT:
                int height = v.getLayoutParams().height;
                if (height == ViewGroup.LayoutParams.MATCH_PARENT) {
                    return VAL_FILL_PARENT;
                } else {
                    return v.getLayoutParams().height + "px";
                }

            case ATTR_BACKGROUND:
                if (v instanceof IBackgroundView) {
                    return ((IBackgroundView) v).getHtmlBackground();
                }
                return null;

            case ATTR_MARGIN_RIGHT:
                if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    return ((ViewGroup.MarginLayoutParams) v.getLayoutParams()).rightMargin + "px";
                }
                return null;

            case ATTR_MARGIN_LEFT:
                if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    return ((ViewGroup.MarginLayoutParams) v.getLayoutParams()).leftMargin + "px";
                }
                return null;

            case ATTR_MARGIN_TOP:
                if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    return ((ViewGroup.MarginLayoutParams) v.getLayoutParams()).topMargin + "px";
                }
                return null;

            case ATTR_MARGIN_BOTTOM:
                if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    return ((ViewGroup.MarginLayoutParams) v.getLayoutParams()).bottomMargin + "px";
                }
                return null;
            case ATTR_PADDING_TOP:
                return v.getPaddingTop() + "px";
            case ATTR_PADDING_LEFT:
                return v.getPaddingLeft() + "px";
            case ATTR_PADDING_BOTTOM:
                return v.getPaddingBottom() + "px";
            case ATTR_PADDING_RIGHT:
                return v.getPaddingRight() + "px";

            case ATTR_LEFT:
                if (v.getLayoutParams() instanceof AbsoluteLayout.LayoutParams) {
                    return ((AbsoluteLayout.LayoutParams) v.getLayoutParams()).x + "px";
                } else {
                    return null;
                }

            case ATTR_TOP:
                if (v.getLayoutParams() instanceof AbsoluteLayout.LayoutParams) {
                    return ((AbsoluteLayout.LayoutParams) v.getLayoutParams()).y + "px";
                } else {
                    return null;
                }

            case ATTR_ALPHA:
                return v.getAlpha();

            case ATTR_VISIBLE:
                int visibility = v.getVisibility();
                if (visibility == View.VISIBLE) {
                    return "visible";
                } else if (visibility == View.INVISIBLE) {
                    return "invisible";
                }

            case ATTR_DIRECTION:
                int textDirection = v.getTextDirection();

                if (textDirection == View.TEXT_DIRECTION_LTR) {
                    return "ltr";
                } else if (textDirection == View.TEXT_DIRECTION_RTL) {
                    return "rtl";
                }
                return null;

            default:
                if (styleHandler != null) {
                    Object val = styleHandler.getStyle(v, styleName);
                    if (val != null) {
                        return val;
                    }
                }

                if (extraStyleHandler != null) {
                    Object val = extraStyleHandler.getStyle(v, styleName);
                    if (val != null) {
                        return val;
                    }
                }
        }

        return null;
    }

    /**
     * @author Yang Tao, 17/4/28.
     */
    public static class StyleEntry {

        private String mStyleName;
        private Object mStyleValue;

        public StyleEntry(String param, Object value) {
            this.mStyleName = param;
            this.mStyleValue = value;
        }

        public String getStyleName() {
            return mStyleName;
        }

        public Object getStyle() {
            return mStyleValue;
        }

        public Object setValue(Object value) {
            Object oldVal = mStyleValue;
            mStyleValue = value;
            return oldVal;
        }

        @Override
        public String toString() {
            return mStyleName + "=" + mStyleValue;
        }
    }
}


