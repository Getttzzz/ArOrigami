package com.example.thesis.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.example.thesis.R
import com.example.thesis.model.Plane
import com.viro.core.*

class ArPlaneActivity : Activity() {

    private var current3DModel: Object3D? = null
    private var viroViewRoot: ViroViewARCore? = null
    private var scene: ARScene? = null
    private var instructionsGroupView: View? = null
    private var instructions: TextView? = null
    private var nextButton: TextView? = null
    private var shakeView: View? = null

    private var mStatus = TRACK_STATUS.SURFACE_NOT_FOUND
    private var selectedPlane: Plane? = null
    private var currentNode: Node? = null
    private var pointer: Node? = null
    private var lastObjectRotation = Vector()
    private var savedRotateToRotation = Vector()
    private var pointerHitTestListener: ARHitTestPointerListener? = null
    private var arNode: ARNode? = null

    private enum class TRACK_STATUS {
        FINDING_SURFACE,
        SURFACE_NOT_FOUND,
        SURFACE_FOUND,
        SELECTED_SURFACE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viroViewRoot = ViroViewARCore(this, object : ViroViewARCore.StartupListener {
            override fun onSuccess() {
                displayScene()
            }

            override fun onFailure(error: ViroViewARCore.StartupError, errorMessage: String) {
                println("ArPlaneActivity.onFailure ---> errorMessage=$errorMessage")
            }
        })
        setContentView(viroViewRoot)

        selectedPlane = intent.getParcelableExtra(EXTRA_PLANE)

        View.inflate(this, R.layout.activity_ar_plane, viroViewRoot as ViewGroup?)
        instructionsGroupView = findViewById(R.id.main_hud_layout) as View
        instructionsGroupView!!.visibility = View.GONE
    }

    override fun onStart() {
        super.onStart()
        viroViewRoot!!.onActivityStarted(this)
    }

    override fun onResume() {
        super.onResume()
        viroViewRoot!!.onActivityResumed(this)
    }

    override fun onPause() {
        super.onPause()
        viroViewRoot!!.onActivityPaused(this)
    }

    override fun onStop() {
        super.onStop()
        viroViewRoot!!.onActivityStopped(this)
    }

    override fun onDestroy() {
        (viroViewRoot as ViroViewARCore).setCameraARHitTestListener(null)
        viroViewRoot!!.onActivityDestroyed(this)
        super.onDestroy()
    }

    private fun displayScene() {
        scene = ARScene()
        val ambientLight = AmbientLight(Color.parseColor("#606060").toLong(), 400f)
        ambientLight.influenceBitMask = 3
        scene!!.rootNode.addLight(ambientLight)

        initPointer()
        init3DModel()
        initInstruction()

        scene!!.setListener(ARSceneListener())
        viroViewRoot!!.scene = scene
    }

    private fun initInstruction() {
        instructions = viroViewRoot!!.findViewById(R.id.ar_hud_instructions) as TextView
        viroViewRoot!!.findViewById(R.id.bottom_frame_controls).visibility = View.VISIBLE

        val view = findViewById(R.id.ar_back_button) as ImageView
        view.setOnClickListener { this@ArPlaneActivity.finish() }

        nextButton = viroViewRoot!!.findViewById(R.id.next_button) as TextView
        nextButton!!.setOnClickListener {
            if (current3DModel != null) {
                val animationKeys = current3DModel!!.animationKeys
                println("onClick --->  animationKeys=$animationKeys")

                for (animationName in animationKeys) {
                    val animation = current3DModel!!.getAnimation(animationName)
                    animation?.play()
                }
            }
        }

        shakeView = viroViewRoot!!.findViewById(R.id.icon_shake_phone)
    }

    private fun initPointer() {
        if (pointer != null) {
            return
        }

        val am = AmbientLight()
        am.influenceBitMask = 2
        am.intensity = 1000f
        scene!!.rootNode.addLight(am)

        val crosshairModel = Object3D()
        scene!!.rootNode.addChildNode(crosshairModel)
        crosshairModel.loadModel(viroViewRoot!!.viroContext, Uri.parse("file:///android_asset/tracking_1.vrx"), Object3D.Type.FBX, object : AsyncObject3DListener {
            override fun onObject3DLoaded(object3D: Object3D, type: Object3D.Type) {
                pointer = object3D
                pointer!!.opacity = 0f
                object3D.lightReceivingBitMask = 2
                pointer!!.setScale(Vector(0.175, 0.175, 0.175))
                pointer!!.clickListener = object : ClickListener {
                    override fun onClick(i: Int, node: Node, vector: Vector) {
                        setTrackingStatus(TRACK_STATUS.SELECTED_SURFACE)
                    }

                    override fun onClickState(i: Int, node: Node, clickState: ClickState, vector: Vector) {}
                }
            }

            override fun onObject3DFailed(error: String) {
                println("ArPlaneActivity.onObject3DFailed --->  Model load failed $error")
            }
        })
    }

    private fun init3DModel() {
        currentNode = Node()

        val light = DirectionalLight()
        val color = ContextCompat.getColor(this@ArPlaneActivity, R.color.colorBlueWithOpacity)
        light.color = color.toLong()
        light.direction = Vector(0f, -1f, 0f)
        light.shadowOrthographicPosition = Vector(0f, 4f, 0f)
        light.shadowOrthographicSize = 10f
        light.shadowNearZ = 1f
        light.shadowFarZ = 4f
        light.castsShadow = true
        currentNode!!.addLight(light)

        current3DModel = Object3D()
        current3DModel!!.loadModel(viroViewRoot!!.viroContext, Uri.parse(selectedPlane!!.uri), Object3D.Type.FBX, object : AsyncObject3DListener {
            override fun onObject3DLoaded(object3D: Object3D, type: Object3D.Type) {
                object3D.lightReceivingBitMask = 1
                currentNode!!.opacity = 0f
                currentNode!!.setScale(Vector(0.00100, 0.00100, 0.00100))
                lastObjectRotation = object3D.rotationEulerRealtime
            }

            override fun onObject3DFailed(error: String) {
                println("ArPlaneActivity.onObject3DFailed ---> error=$error")
            }
        })

        currentNode!!.dragType = Node.DragType.FIXED_TO_WORLD
        currentNode!!.dragListener = DragListener { i, node, vector, vector1 -> }
        current3DModel!!.gestureRotateListener = GestureRotateListener { source, node, radians, rotateState ->
            if (rotateState == RotateState.ROTATE_END) {
                lastObjectRotation = savedRotateToRotation
            } else {
                val rotateTo = Vector(lastObjectRotation.x, lastObjectRotation.y + radians, lastObjectRotation.z)
                currentNode!!.setRotation(rotateTo)
                savedRotateToRotation = rotateTo
            }
        }

        currentNode!!.opacity = 0f
        currentNode!!.addChildNode(current3DModel!!)
    }

    private fun setTrackingStatus(status: TRACK_STATUS) {
        if (mStatus == TRACK_STATUS.SELECTED_SURFACE || mStatus == status) {
            return
        }

        if (status == TRACK_STATUS.SELECTED_SURFACE) {
            (viroViewRoot as ViroViewARCore).setCameraARHitTestListener(null)
        }

        mStatus = status
        updateUIHud()
        updatePointer()
        update3DModel()
    }

    private fun updateUIHud() {
        when (mStatus) {
            TRACK_STATUS.FINDING_SURFACE -> instructions!!.text = "Point the camera at the flat surface."
            TRACK_STATUS.SURFACE_NOT_FOUND -> instructions!!.text = "We can’t find a surface. Try to move your phone more in any direction."
            TRACK_STATUS.SURFACE_FOUND -> instructions!!.text = "Great! Now tap where you want to see the product."
            TRACK_STATUS.SELECTED_SURFACE -> instructions!!.text = "Great! Use one finger to move and two fingers to rotate."
            else -> instructions!!.text = "Initializing AR...."
        }

        if (mStatus == TRACK_STATUS.SELECTED_SURFACE) {
            nextButton!!.visibility = View.VISIBLE
        } else {
            nextButton!!.visibility = View.GONE
        }

        if (mStatus == TRACK_STATUS.SURFACE_NOT_FOUND) {
            shakeView!!.visibility = View.VISIBLE
        } else {
            shakeView!!.visibility = View.GONE
        }
    }

    private fun updatePointer() {
        when (mStatus) {
            TRACK_STATUS.FINDING_SURFACE, TRACK_STATUS.SURFACE_NOT_FOUND, TRACK_STATUS.SELECTED_SURFACE -> pointer!!.opacity = 0f
            TRACK_STATUS.SURFACE_FOUND -> pointer!!.opacity = 1f
        }

        if (mStatus == TRACK_STATUS.SELECTED_SURFACE && pointerHitTestListener != null) {
            pointerHitTestListener = null
            (viroViewRoot as ViroViewARCore).setCameraARHitTestListener(null)
        } else if (pointerHitTestListener == null) {
            pointerHitTestListener = ARHitTestPointerListener()
            (viroViewRoot as ViroViewARCore).setCameraARHitTestListener(pointerHitTestListener)
        }
    }

    private fun update3DModel() {
        if (mStatus != TRACK_STATUS.SELECTED_SURFACE) {
            currentNode!!.opacity = 0f
            return
        }

        if (arNode != null) {
            return
        }

        arNode = scene!!.createAnchoredNode(pointer!!.positionRealtime)
        arNode!!.addChildNode(currentNode!!)
        currentNode!!.opacity = 1f
    }

    private inner class ARHitTestPointerListener : ARHitTestListener {
        override fun onHitTestFinished(arHitTestResults: Array<ARHitTestResult>?) {
            if (arHitTestResults == null || arHitTestResults.size <= 0) {
                return
            }

            val cameraPos = viroViewRoot!!.lastCameraPositionRealtime

            var closestDistance = java.lang.Float.MAX_VALUE
            var result: ARHitTestResult? = null
            for (i in arHitTestResults.indices) {
                val currentResult = arHitTestResults[i]
                val distance = currentResult.position.distance(cameraPos)
                if (distance < closestDistance && distance > .3 && distance < 5) {
                    result = currentResult
                    closestDistance = distance
                }
            }

            if (result != null) {
                pointer!!.setPosition(result.position)
                pointer!!.setRotation(result.rotation)
            }

            if (result != null) {
                setTrackingStatus(TRACK_STATUS.SURFACE_FOUND)
            } else {
                setTrackingStatus(TRACK_STATUS.FINDING_SURFACE)
            }
        }
    }

    protected inner class ARSceneListener : ARScene.Listener {
        private var mInitialized: Boolean = false

        init {
            mInitialized = false
        }

        override fun onTrackingInitialized() {}

        override fun onTrackingUpdated(trackingState: ARScene.TrackingState, trackingStateReason: ARScene.TrackingStateReason) {
            if (trackingState == ARScene.TrackingState.NORMAL && !mInitialized) {
                instructionsGroupView!!.visibility = View.VISIBLE
                setTrackingStatus(TRACK_STATUS.FINDING_SURFACE)
                mInitialized = true
            }
        }

        override fun onAmbientLightUpdate(lightIntensity: Float, v: Vector) {}

        override fun onAnchorFound(anchor: ARAnchor, arNode: ARNode) {}

        override fun onAnchorUpdated(anchor: ARAnchor, arNode: ARNode) {}

        override fun onAnchorRemoved(anchor: ARAnchor, arNode: ARNode) {}
    }

    companion object {
        const val EXTRA_PLANE = "extra_plane"
        fun getIntent(plane: Plane, context: Context): Intent {
            val intent = Intent(context, ArPlaneActivity::class.java)
            intent.putExtra(EXTRA_PLANE, plane)
            return intent
        }
    }

}
