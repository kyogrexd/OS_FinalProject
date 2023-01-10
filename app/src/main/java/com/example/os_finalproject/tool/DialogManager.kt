package com.example.os_finalproject.tool

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import com.example.os_finalproject.R

class DialogManager private constructor(){
    companion object {
        val instance : DialogManager by lazy { DialogManager() }
    }

    private var loadingDialog: Dialog? = null
    private var dialog: Dialog? = null
    private var successDialog: Dialog? = null

    fun dismissAll() {
        loadingDialog?.dismiss()
        dialog?.cancel()
        successDialog?.dismiss()
    }

    fun showCustom(activity: Activity, layout: View, keyboard: Boolean = false, gravityPosition: Int = -1): View? {
        if (!activity.isDestroyed) {
            try {
                dialog?.dismiss()

                dialog = AlertDialog.Builder(activity, R.style.Theme_Dialog).create()
                dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

                if (gravityPosition != -1) {
                    val wlp = dialog?.window?.attributes
                    wlp?.gravity = gravityPosition
                    wlp?.flags = wlp?.flags?.and(WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv())
                    dialog?.window?.attributes = wlp
                }

                dialog?.show()

                if (keyboard) {
                    dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)

                    dialog?.setOnDismissListener {
                        dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)

                        var virtualKeyboardHeight = 0
                        val res = activity.resources
                        val resourceId = activity.resources.getIdentifier("navigation_bar_height", "dimen", "android")
                        if (resourceId > 0) virtualKeyboardHeight = res.getDimensionPixelSize(resourceId)

                        val rect = Rect()
                        activity.window.decorView.getWindowVisibleDisplayFrame(rect)
                        val screenHeight = activity.window.decorView.rootView.height
                        val heiDifference = screenHeight - (rect.bottom + virtualKeyboardHeight)

                        if (heiDifference > 0) {
                            val inputMgr = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            inputMgr.toggleSoftInput(InputMethodManager.HIDE_NOT_ALWAYS, 0)
                        }
                    }
                }

                dialog?.setContentView(layout)
                return layout
            } catch (e: Exception) {
                Log.e("DialogManager", "DialogManager_showCustom $e")
                return null
            }
        }
        return null
    }



    fun cancelDialog() = dialog?.cancel()

    fun getDialog() = dialog
}