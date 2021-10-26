package se.adanware.visaparser.fragment

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import se.adanware.visaparser.R

import androidx.fragment.app.FragmentActivity




class StatusDialog : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.status_dialog, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.window?.addFlags(
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        )

        dialog?.window?.decorView?.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val dialogBounds = Rect()
                v.getHitRect(dialogBounds)
                if (!dialogBounds.contains(event.x.toInt(), event.y.toInt())) {
                    // You have clicked the grey area
                    false // stop activity closing
                }
            }
            true
        }

    }


    fun updateProgress(text: String) {
        val progressTextView : TextView? = view?.findViewById(R.id.progress_textview)
        progressTextView?.text = text
    }
}