package com.example.to_doapp.fragments

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.VideoView
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.example.to_doapp.R
import com.google.firebase.auth.FirebaseAuth


class SplashFragment : Fragment() {
    private lateinit var mAuth: FirebaseAuth
    private lateinit var navController: NavController
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init(view)
        val videoView: VideoView = view.findViewById(R.id.videoView)
        val uri = Uri.parse("android.resource://" + requireContext().packageName + "/" + R.raw.splash_cat)
        videoView.setVideoURI(uri)
        videoView.setOnPreparedListener {
            it.isLooping = true
            videoView.start()
        }
        val isLogin: Boolean = mAuth.currentUser != null
        val handler = Handler(Looper.myLooper()!!)
        handler.postDelayed({
            if (isLogin)
                navController.navigate(R.id.action_splashFragment_to_homeFragment)
            else
                navController.navigate(R.id.action_splashFragment_to_signInFragment)
        }, 0)
    }
    private fun init(view: View) {
        mAuth = FirebaseAuth.getInstance()
        navController = Navigation.findNavController(view)
    }
}