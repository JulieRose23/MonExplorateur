package com.monexplorateur.app.xposed

import android.app.Activity
import android.app.Instrumentation
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

class FilePickerHook : IXposedHookLoadPackage {

    companion object {
        private const val TARGET_PACKAGE = "com.monexplorateur.app"
        private const val ACTION_PICK_IMAGES = "android.provider.action.PICK_IMAGES"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == TARGET_PACKAGE) return
        XposedBridge.log("MonExplorateur Hook: module actif dans ${lpparam.packageName}")

        val hookCallback = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val args = param.args ?: return
                val intentIndex = args.indexOfFirst { it is Intent }
                if (intentIndex == -1) return
                val intent = args[intentIndex] as Intent

                val cible = if (intent.action == Intent.ACTION_CHOOSER) {
                    @Suppress("DEPRECATION")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                    } else {
                        intent.getParcelableExtra(Intent.EXTRA_INTENT)
                    }
                } else {
                    intent
                } ?: return

                if (cible.`package` == TARGET_PACKAGE || cible.component?.packageName == TARGET_PACKAGE) return

                val action = cible.action
                val pkgComposant = cible.component?.packageName.orEmpty()
                val categories = cible.categories.orEmpty()

                XposedBridge.log("MonExplorateur Hook [${lpparam.packageName}] INTENT: action=$action type=${cible.type} data=${cible.data} comp=$pkgComposant cats=$categories")

                if (action == Intent.ACTION_OPEN_DOCUMENT_TREE || action == Intent.ACTION_CREATE_DOCUMENT) return

                val estActionFichier = action == Intent.ACTION_OPEN_DOCUMENT ||
                    action == Intent.ACTION_GET_CONTENT ||
                    action == Intent.ACTION_PICK ||
                    action == ACTION_PICK_IMAGES ||
                    Intent.CATEGORY_OPENABLE in categories ||
                    pkgComposant.contains("documentsui", ignoreCase = true)

                if (!estActionFichier) return

                val mimeType = cible.type?.takeIf { it.isNotEmpty() } ?: "*/*"
                val multiple = cible.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
                val mimeTypesExtra = cible.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)

                val nouvelIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = mimeType
                    addCategory(Intent.CATEGORY_OPENABLE)
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple)
                    if (mimeTypesExtra != null) {
                        putExtra(Intent.EXTRA_MIME_TYPES, mimeTypesExtra)
                    }
                    component = null
                    selector = null
                    setPackage(TARGET_PACKAGE)
                }

                cible.action = Intent.ACTION_GET_CONTENT
                cible.component = null
                cible.selector = null
                cible.setDataAndType(null, mimeType)
                cible.addCategory(Intent.CATEGORY_OPENABLE)
                cible.setPackage(TARGET_PACKAGE)

                args[intentIndex] = nouvelIntent
                XposedBridge.log("MonExplorateur Hook [${lpparam.packageName}]: REDIRIGE -> $TARGET_PACKAGE")
            }
        }

        XposedBridge.hookAllMethods(Activity::class.java, "startActivity", hookCallback)
        XposedBridge.hookAllMethods(Activity::class.java, "startActivityForResult", hookCallback)
        XposedBridge.hookAllMethods(ContextWrapper::class.java, "startActivity", hookCallback)
        XposedBridge.hookAllMethods(Instrumentation::class.java, "execStartActivity", hookCallback)
    }
}