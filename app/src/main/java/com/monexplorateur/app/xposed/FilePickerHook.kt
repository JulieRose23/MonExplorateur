package com.monexplorateur.app.xposed

import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

class FilePickerHook : IXposedHookLoadPackage {

    companion object {
        private const val TARGET_PACKAGE = "com.monexplorateur.app"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == TARGET_PACKAGE) return

        XposedBridge.hookAllMethods(
            Instrumentation::class.java,
            "execStartActivity",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val args = param.args ?: return
                    val context = args.firstOrNull { it is Context } as? Context ?: return
                    val intent = args.firstOrNull { it is Intent } as? Intent ?: return

                    val action = intent.action ?: return
                    if (action != Intent.ACTION_OPEN_DOCUMENT && action != Intent.ACTION_GET_CONTENT) {
                        return
                    }

                    // Verifie que MonExplorateur est bien installe avant de rediriger
                    val estInstalle = try {
                        context.packageManager.getPackageInfo(TARGET_PACKAGE, 0)
                        true
                    } catch (_: Exception) {
                        false
                    }
                    if (!estInstalle) return

                    intent.action = Intent.ACTION_GET_CONTENT
                    if (intent.type.isNullOrEmpty()) {
                        intent.type = "*/*"
                    }
                    intent.setPackage(TARGET_PACKAGE)
                }
            }
        )
    }
}