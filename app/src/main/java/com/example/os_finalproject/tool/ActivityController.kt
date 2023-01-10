package com.example.os_finalproject.tool

import android.app.Activity
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.text.TextUtils

object ActivityController {
    var activities = mutableListOf<Activity>()

    fun addActivity(activity: Activity) {
        activities.add(activity)
    }

    fun removeActivity(activity: Activity) {
        activities.remove(activity)
    }

    fun checkOnActivityIsExist(activityName: String) : Boolean {
        activities.find {
            it.javaClass.name == activityName
        }?.let {
            return true
        }
        return false
    }

    //關閉指定activity
    fun finishOneActivity(activityName: String, isRemoveTask: Boolean = false) {
        for (activity in activities) {
            val name = activity.javaClass.name
            if (name == activityName) {
                if (activity.isFinishing) activities.remove(activity)
                else {
                    if (isRemoveTask) activity.finishAndRemoveTask()
                    else activity.finish()
                }
            }
        }
    }

    //關閉除了指定activity外的所有activity
    fun finishOtherActivity(activityName: String) {
        for (activity in activities) {
            val name = activity.javaClass.name
            if (name != activityName) {
                if (activity.isFinishing) activities.remove(activity)
                else activity.finish()
            }
        }
    }
}