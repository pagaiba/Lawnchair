package ch.deletescape.lawnchair.pixelify;

import android.animation.ObjectAnimator;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;

import ch.deletescape.lawnchair.Launcher;
import ch.deletescape.lawnchair.R;
import ch.deletescape.lawnchair.Utilities;
import ch.deletescape.lawnchair.compat.LauncherAppsCompat;
import ch.deletescape.lawnchair.config.FeatureFlags;
import ch.deletescape.lawnchair.util.PackageManagerHelper;

public abstract class BaseQsbView extends FrameLayout implements OnClickListener, OnSharedPreferenceChangeListener {
    private static final String TEXT_ASSIST = "com.google.android.googlequicksearchbox.TEXT_ASSIST";
    private static final String VOICE_ASSIST = "android.intent.action.VOICE_ASSIST";
    protected View mQsbView;
    protected final Launcher mLauncher;
    protected boolean showMic;
    protected QsbConnector qsbConnector;
    private ObjectAnimator elevationAnimator;
    private final BroadcastReceiver packageChangedReciever = new PackageChangedReciever(this);
    private boolean qsbHidden;
    private int mQsbViewId = 0;
    private boolean bM;

    protected abstract int getQsbView(boolean withMic);

    public BaseQsbView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        mLauncher = Launcher.getLauncher(context);
    }

    public void applyVoiceSearchPreference(SharedPreferences prefs) {
        showMic = FeatureFlags.showVoiceSearchButton(getContext());
        int qsbView = getQsbView(showMic);
        if (qsbView != mQsbViewId) {
            mQsbViewId = qsbView;
            if (mQsbView != null) {
                removeView(mQsbView);
            }
            mQsbView = LayoutInflater.from(getContext()).inflate(mQsbViewId, this, false);
            float qsbButtonElevation = (float) getResources().getDimensionPixelSize(R.dimen.qsb_button_elevation);
            addView(mQsbView);
            elevationAnimator = ObjectAnimator.ofFloat(mQsbView, "elevation", 0.0f, qsbButtonElevation);
            elevationAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            if (qsbHidden) {
                hideQsb();
            }
            mQsbView.setOnClickListener(this);
            mQsbView.setAccessibilityDelegate(new AccessibilityHelper());
            if (showMic) {
                mQsbView.findViewById(R.id.mic_icon).setOnClickListener(this);
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!FeatureFlags.showPixelBar(getContext())) {
            return;
        }
        SharedPreferences sharedPreferences = Utilities.getPrefs(getContext());
        applyVoiceSearchPreference(sharedPreferences);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        getContext().registerReceiver(packageChangedReciever, Util.createIntentFilter("android.intent.action.PACKAGE_CHANGED"));
        initializeQsbConnector();
        applyVisibility();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Utilities.getPrefs(getContext()).unregisterOnSharedPreferenceChangeListener(this);
        getContext().unregisterReceiver(packageChangedReciever);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String str) {
        if (FeatureFlags.KEY_SHOW_VOICE_SEARCH_BUTTON.equals(str)) {
            applyVoiceSearchPreference(sharedPreferences);
            applyVisibility();
        }
    }

    private void initializeQsbConnector() {
        if (qsbConnector == null && !FeatureFlags.useFullWidthSearchbar(getContext())) {
            qsbConnector = (QsbConnector) mLauncher.getLayoutInflater().inflate(R.layout.qsb_connector, this, false);
            addView(qsbConnector, 0);
        } else if (FeatureFlags.useFullWidthSearchbar(getContext())) {
            removeView(qsbConnector);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.mic_icon) {
            startQsbActivity(VOICE_ASSIST);
        } else {
            getContext().sendOrderedBroadcast(bm("com.google.nexuslauncher.FAST_TEXT_SEARCH"), null, new C0287l(this), null, 0, null, null);
        }
    }

    private Intent bm(String str) {
        int[] iArr = new int[2];
        mQsbView.getLocationOnScreen(iArr);
        Rect rect = new Rect(iArr[0], iArr[1], iArr[0] + mQsbView.getWidth(), iArr[1] + mQsbView.getHeight());
        Intent intent = new Intent(str);
        aL(rect, intent);
        intent.setSourceBounds(rect);
        View micIcon = findViewById(R.id.mic_icon);
        if (micIcon != null) {
            intent.putExtra("source_mic_offset", bn(micIcon, rect));
        }
        return intent.putExtra("source_round_left", true).putExtra("source_round_right", true).putExtra("source_logo_offset", bn(findViewById(R.id.g_icon), rect)).setPackage("com.google.android.googlequicksearchbox");//.addFlags(1342177280);
    }

    private Point bn(View view, Rect rect) {
        int[] iArr = new int[2];
        view.getLocationOnScreen(iArr);
        Point point = new Point();
        point.x = (iArr[0] - rect.left) + (view.getWidth() / 2);
        point.y = (iArr[1] - rect.top) + (view.getHeight() / 2);
        return point;
    }

    protected void aL(Rect rect, Intent intent) {
    }

    private void bq() {
        if (hasWindowFocus()) {
            bM = true;
        } else {
            hideQsb();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (!hasWindowFocus && bM) {
            hideQsb();
        } else if (hasWindowFocus) {
            showQsb(true);
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        showQsb(false);
    }

    private void hideQsb() {
        bM = false;
        qsbHidden = true;
        if (mQsbView != null) {
            mQsbView.setAlpha(0.0f);
            if (elevationAnimator != null && elevationAnimator.isRunning()) {
                elevationAnimator.end();
            }
        }
        if (qsbConnector != null) {
            qsbConnector.setAlpha(0.0f);
        }
    }

    private void showQsb(boolean animated) {
        bM = false;
        if (qsbHidden) {
            qsbHidden = false;
            if (mQsbView != null) {
                mQsbView.setAlpha(1.0f);
                if (elevationAnimator != null) {
                    elevationAnimator.start();
                    if (!animated) {
                        elevationAnimator.end();
                    }
                }
            }
            if (qsbConnector != null) {
                qsbConnector.setAlpha(1.0f);
                qsbConnector.bc(animated);
            }
        }
    }

    private void startQsbActivity(String str) {
        try {
            getContext().startActivity(new Intent(str).addFlags(268468224).setPackage("com.google.android.googlequicksearchbox"));
        } catch (ActivityNotFoundException e) {
            LauncherAppsCompat.getInstance(getContext()).showAppDetailsForProfile(new ComponentName("com.google.android.googlequicksearchbox", ".SearchActivity"), Utilities.myUserHandle());
        }
    }

    private void applyVisibility() {
        boolean isQsbAppEnabled = PackageManagerHelper.isAppEnabled(getContext().getPackageManager(), "com.google.android.googlequicksearchbox", 0);
        int visibility = isQsbAppEnabled ? View.VISIBLE : View.GONE;
        if (mQsbView != null) {
            mQsbView.setVisibility(visibility);
        }
        if (qsbConnector != null) {
            qsbConnector.setVisibility(visibility);
        }
    }

    final class C0287l extends BroadcastReceiver {
        final /* synthetic */ BaseQsbView cq;

        C0287l(BaseQsbView qsbView) {
            cq = qsbView;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (getResultCode() == 0) {
                cq.startQsbActivity(BaseQsbView.TEXT_ASSIST);
            } else {
                cq.bq();
            }
        }
    }

    final class PackageChangedReciever extends BroadcastReceiver {
        final /* synthetic */ BaseQsbView cp;

        PackageChangedReciever(BaseQsbView qsbView) {
            cp = qsbView;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            cp.applyVisibility();
        }
    }
}