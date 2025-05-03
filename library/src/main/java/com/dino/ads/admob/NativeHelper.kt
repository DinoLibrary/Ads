package com.dino.ads.admob

import android.os.Build
import android.transition.TransitionManager
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import com.dino.ads.R
import com.dino.ads.utils.AdNativeSize
import com.dino.ads.utils.dpToPx
import com.dino.ads.utils.gone
import com.dino.ads.utils.visible
import com.google.android.gms.ads.VideoController
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

class NativeHelper {

    companion object {
        fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView, size: AdNativeSize) {
            if (nativeAd == null || adView == null || size == null) {
                return
            }

            adView.findViewById<MediaView>(R.id.ad_media)?.let {
                adView.mediaView = it
            }
            adView.findViewById<TextView>(R.id.ad_headline)?.let {
                adView.headlineView = it
            }
            adView.findViewById<TextView>(R.id.ad_body)?.let {
                adView.bodyView = it
            }
            adView.findViewById<Button>(R.id.ad_call_to_action)?.let {
                adView.callToActionView = it
            }
            adView.findViewById<ImageView>(R.id.ad_app_icon)?.let {
                adView.iconView = it
            }
            adView.findViewById<RatingBar>(R.id.ad_stars)?.let {
                adView.starRatingView = it
            }
            if (nativeAd.mediaContent != null) {
                if (size == AdNativeSize.MEDIUM) {
                    adView.mediaView!!.visible()
                    adView.mediaView!!.mediaContent = nativeAd.mediaContent!!
                } else {
                    adView.mediaView?.gone()
                }
            }

            if (nativeAd.headline != null) {
                (adView.headlineView as TextView).text = nativeAd.headline
            }
            if (nativeAd.body == null) {
                adView.bodyView!!.visibility = View.INVISIBLE
            } else {
                adView.bodyView!!.visibility = View.VISIBLE
                (adView.bodyView as TextView).text = nativeAd.body
            }
            if (nativeAd.callToAction == null) {
                adView.callToActionView!!.visibility = View.INVISIBLE

            } else {
                adView.callToActionView!!.visibility = View.VISIBLE
                (adView.callToActionView as Button).text = nativeAd.callToAction
            }

            if (adView.iconView != null) {
                if (nativeAd.icon == null) {
                    adView.iconView!!.visibility = View.GONE
                } else {
                    (adView.iconView as ImageView).setImageDrawable(
                        nativeAd.icon!!.drawable
                    )
                    adView.iconView!!.visibility = View.VISIBLE
                }
            }

            if (nativeAd.starRating != null) {
                (adView.starRatingView as RatingBar).rating = 5f
            }
            adView.setNativeAd(nativeAd)

//            val vc = nativeAd.mediaContent!!.videoController
//            if (vc.hasVideoContent()) {
//                vc.videoLifecycleCallbacks = object : VideoController.VideoLifecycleCallbacks() {
//                    override fun onVideoEnd() {
//                        super.onVideoEnd()
//                    }
//                }
//            }
        }

        fun populateNativeAdViewFull(nativeAd: NativeAd, adView: NativeAdView) {
            val mediaView = adView.findViewById<MediaView>(R.id.ad_media)
            // Set the media view.
            adView.mediaView = mediaView

            // Set other ad assets.
            adView.headlineView = adView.findViewById(R.id.ad_headline)
            adView.bodyView = adView.findViewById(R.id.ad_body)
            adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
            val imageView = adView.findViewById<ImageView>(R.id.ad_app_icon)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                imageView.clipToOutline = true
            }
            adView.iconView = imageView

            // The headline and mediaContent are guaranteed to be in every NativeAd.
            (adView.headlineView as TextView?)!!.text = nativeAd.headline
            mediaView.mediaContent = nativeAd.mediaContent

            // These assets aren't guaranteed to be in every NativeAd, so it's important to
            // check before trying to display them.
            if (nativeAd.body == null) {
                adView.bodyView!!.visibility = View.INVISIBLE
            } else {
                adView.bodyView!!.visibility = View.VISIBLE
                (adView.bodyView as TextView?)!!.text = nativeAd.body
            }
            if (nativeAd.callToAction == null) {
                adView.callToActionView!!.visibility = View.INVISIBLE
            } else {
                adView.callToActionView!!.visibility = View.VISIBLE
                (adView.callToActionView as Button?)!!.text = nativeAd.callToAction
            }
            if (nativeAd.icon == null) {
                adView.iconView!!.visibility = View.GONE
            } else {
                (adView.iconView as ImageView?)!!.setImageDrawable(
                    nativeAd.icon!!.drawable
                )
                adView.iconView!!.visibility = View.VISIBLE
            }

            // This method tells the Google Mobile Ads SDK that you have finished populating your
            // native ad view with this native ad.
            adView.setNativeAd(nativeAd)

            // Get the video controller for the ad. One will always be provided,
            // even if the ad doesn't have a video asset.
            val videoController = nativeAd.mediaContent!!.videoController

            // Updates the UI to say whether or not this ad has a video asset.
            if (videoController.hasVideoContent()) {
                // Create a new VideoLifecycleCallbacks object and pass it to the VideoController.
                // The VideoController will call methods on this object when events occur in the
                // video lifecycle.
                videoController.videoLifecycleCallbacks =
                    object : VideoController.VideoLifecycleCallbacks() {
                    }
            }
        }

        fun populateNativeAdViewNoBtn(nativeAd: NativeAd, adView: NativeAdView, size: AdNativeSize) {
            if (nativeAd == null || adView == null || size == null) {
                return
            }

            adView.findViewById<MediaView>(R.id.ad_media)?.let {
                adView.mediaView = it
            }
            adView.findViewById<TextView>(R.id.ad_headline)?.let {
                adView.headlineView = it
            }
            adView.findViewById<TextView>(R.id.ad_body)?.let {
                adView.bodyView = it
            }
            adView.findViewById<Button>(R.id.ad_call_to_action)?.let {
                adView.callToActionView = it
            }
            adView.findViewById<ImageView>(R.id.ad_app_icon)?.let {
                adView.iconView = it
            }
            adView.findViewById<RatingBar>(R.id.ad_stars)?.let {
                adView.starRatingView = it
            }
            if (nativeAd.mediaContent != null) {
                if (size == AdNativeSize.MEDIUM) {
                    adView.mediaView?.let {
                        it.setImageScaleType(ImageView.ScaleType.CENTER_INSIDE)
                        val mediaContent = nativeAd.mediaContent
                        if (mediaContent != null && mediaContent.hasVideoContent()) {
                            // Create a MediaView and set its media content.
                            val mediaView = MediaView(it.context)
                            mediaView.mediaContent = mediaContent
                            it.addView(mediaView)
                        }
                    }
                }
            }

            if (nativeAd.headline != null) {
                (adView.headlineView as TextView).text = nativeAd.headline
            }
            (adView.bodyView as TextView).text = nativeAd.body
            (adView.callToActionView as Button).text = nativeAd.callToAction

            if (adView.iconView != null) {
                if (nativeAd.icon == null) {
                    adView.iconView!!.visibility = View.GONE
                } else {
                    (adView.iconView as ImageView).setImageDrawable(
                        nativeAd.icon!!.drawable
                    )
                    adView.iconView!!.visibility = View.VISIBLE
                }
            }

            if (nativeAd.starRating != null) {
                (adView.starRatingView as RatingBar).rating = 5f
            }

            adView.setNativeAd(nativeAd)

            val vc = nativeAd.mediaContent!!.videoController
            if (vc.hasVideoContent()) {
                vc.videoLifecycleCallbacks = object : VideoController.VideoLifecycleCallbacks() {
                }
            }
        }

        fun populateNativeAdViewCollap(nativeAd: NativeAd, adView: NativeAdView, size: AdNativeSize, anchor: String, onCollapsed: () -> Unit) {
            if (nativeAd == null || adView == null || size == null) {
                return
            }

            adView.findViewById<MediaView>(R.id.ad_media)?.let {
                adView.mediaView = it
            }
            adView.findViewById<TextView>(R.id.ad_headline)?.let {
                adView.headlineView = it
            }
            adView.findViewById<TextView>(R.id.ad_body)?.let {
                adView.bodyView = it
            }
            adView.findViewById<Button>(R.id.ad_call_to_action)?.let {
                adView.callToActionView = it
            }
            adView.findViewById<ImageView>(R.id.ad_app_icon)?.let {
                adView.iconView = it
            }
            adView.findViewById<RatingBar>(R.id.ad_stars)?.let {
                adView.starRatingView = it
            }
            if (nativeAd.mediaContent != null) {
                if (size == AdNativeSize.MEDIUM) {
                    adView.mediaView!!.mediaContent = nativeAd.mediaContent!!
                }
            }

            if (nativeAd.headline != null) {
                (adView.headlineView as TextView).text = nativeAd.headline
            }
            if (nativeAd.body == null) {
                adView.bodyView!!.visibility = View.INVISIBLE
            } else {
                adView.bodyView!!.visibility = View.VISIBLE
                (adView.bodyView as TextView).text = nativeAd.body
            }
            if (nativeAd.callToAction == null) {
                adView.callToActionView!!.visibility = View.INVISIBLE
            } else {
                adView.callToActionView!!.visibility = View.VISIBLE
                (adView.callToActionView as Button).text = nativeAd.callToAction
            }

            if (adView.iconView != null) {
                if (nativeAd.icon == null) {
                    adView.iconView!!.visibility = View.GONE
                } else {
                    (adView.iconView as ImageView).setImageDrawable(
                        nativeAd.icon!!.drawable
                    )
                    adView.iconView!!.visibility = View.VISIBLE
                }
            }
            adView.findViewById<ImageView>(R.id.ad_collap)?.let { ivCollap ->
                ivCollap.isVisible = true
                if (anchor == "top") ivCollap.rotation = 180f
                ivCollap.setOnClickListener {
                    adView.mediaView?.mediaContent = null
                    onCollapsed()
                }
            }
            if (nativeAd.starRating != null) {
                (adView.starRatingView as RatingBar).rating = 5f
            }

            adView.setNativeAd(nativeAd)

//            val vc = nativeAd.mediaContent!!.videoController
//            if (vc.hasVideoContent()) {
//                vc.videoLifecycleCallbacks = object : VideoController.VideoLifecycleCallbacks() {
//                    override fun onVideoEnd() {
//                        super.onVideoEnd()
//                    }
//                }
//            }
        }

        fun reConstraintNativeCollapView(adView: NativeAdView) {
            try {
                //* Convert NativeCollap => NativeSmall
                adView.findViewById<ImageView>(R.id.ad_collap)?.gone()
                adView.findViewById<MediaView>(R.id.ad_media)?.gone()
                val constraintLayout = adView.findViewById<ConstraintLayout>(R.id.ad_container)
                val middle = adView.findViewById<ViewGroup>(R.id.middle)
                val button = adView.callToActionView ?: return
                (button as? Button)?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                button.layoutParams = button.layoutParams.apply {
                    height = 40.dpToPx(adView.context)
                    width = 0
                }
                middle.layoutParams = middle.layoutParams.apply { width = 0 }

                val constraintSet = ConstraintSet()
                constraintSet.clone(constraintLayout)

                constraintSet.clear(button.id, ConstraintSet.START)
                constraintSet.connect(button.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                constraintSet.connect(button.id, ConstraintSet.TOP, middle.id, ConstraintSet.TOP)
                constraintSet.connect(button.id, ConstraintSet.BOTTOM, middle.id, ConstraintSet.BOTTOM)

                constraintSet.clear(middle.id, ConstraintSet.END)
                constraintSet.connect(middle.id, ConstraintSet.END, button.id, ConstraintSet.START)
                constraintSet.connect(middle.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
                constraintSet.setMargin(middle.id, ConstraintSet.END, 4.dpToPx(adView.context))
                constraintSet.setMargin(middle.id, ConstraintSet.START, 0)

                constraintSet.setMargin(button.id, ConstraintSet.TOP, 0)
                constraintSet.setMargin(button.id, ConstraintSet.BOTTOM, 0)
                constraintSet.setMargin(button.id, ConstraintSet.END, 0)

                TransitionManager.beginDelayedTransition(constraintLayout)
                constraintSet.applyTo(constraintLayout)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }
}