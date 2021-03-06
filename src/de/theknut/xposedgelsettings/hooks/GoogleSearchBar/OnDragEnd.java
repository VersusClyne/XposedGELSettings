package de.theknut.xposedgelsettings.hooks.googlesearchbar;

import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.theknut.xposedgelsettings.hooks.Common;
import de.theknut.xposedgelsettings.hooks.ObfuscationHelper.Fields;

import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setBooleanField;

public final class OnDragEnd extends XC_MethodHook {
	
	// http://androidxref.com/4.4.2_r1/xref/packages/apps/Launcher3/src/com/android/launcher3/SearchDropTargetBar.java#202
	// public void onDragEnd()
	
	@Override
	protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
		// set the search bar hidden so that the animation wouldn't be shown (fade out)
		setBooleanField(param.thisObject, Fields.sdtbIsSearchBarHidden, true);
	}
	
	@Override
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		View qsb = (View) getObjectField(param.thisObject, Fields.sdtbQsbBar);
		qsb.setAlpha(1f);
		
		// hide the search bar
		Common.IS_DRAGGING = false;
		GoogleSearchBarHooks.hideSearchbar();
	}
}