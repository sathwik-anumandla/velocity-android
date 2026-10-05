package com.velocity.app.ui.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.HapticFeedbackConstants

object VelocityHaptics {

    private tailrec fun findActivity(context: Context): Activity? = when (context) {
        is Activity -> context
        is ContextWrapper -> if (context.baseContext !== context) findActivity(context.baseContext) else null
        else -> null
    }

    private fun perform(context: Context, feedback: Int) {
        findActivity(context)?.window?.decorView?.performHapticFeedback(feedback)
    }

    fun lightClick(context: Context) {
        perform(context, HapticFeedbackConstants.VIRTUAL_KEY)
    }

    fun subtleTick(context: Context) {
        perform(context, HapticFeedbackConstants.CLOCK_TICK)
    }

    fun success(context: Context) {
        perform(context, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.VIRTUAL_KEY
        })
    }

    fun error(context: Context) {
        perform(context, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.REJECT
        } else {
            HapticFeedbackConstants.LONG_PRESS
        })
    }
}
