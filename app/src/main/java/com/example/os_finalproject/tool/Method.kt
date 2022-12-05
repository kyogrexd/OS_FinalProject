package com.example.os_finalproject.tool

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

object Method  {

    fun showKeyBoard(activity: AppCompatActivity, ed: EditText){
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(ed, 0)
    }

    fun hideKeyBoard(activity: AppCompatActivity) {
        activity.currentFocus?.let {
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken,0)
        }
    }

    fun hideKeyBoard(context: Context, view: View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken,0)
    }
}