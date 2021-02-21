package com.example.loginapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.loginapp.MainActivity
import com.example.loginapp.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.royrodriguez.transitionbutton.TransitionButton

/**
 * Fragment for Welcome page
 */
class Welcome : Fragment() {
    private lateinit var logOutButton: TransitionButton
    private lateinit var userName: TextView

    companion object {
        fun newInstance() = Welcome()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.welcome_fragment, container, false)
        userName = root.findViewById(R.id.userName)
        logOutButton = root.findViewById(R.id.logOutButton)
        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        if (FirebaseAuth.getInstance().currentUser != null)
            (getString(R.string.hi) + " " + FirebaseAuth.getInstance().currentUser!!.displayName?.substringBefore(
                " "
            ) + "!").also { userName.text = it }

        logOutButton.setOnClickListener {
            logOutButton.startAnimation()
            Firebase.auth.signOut()  //Logout from Firebase
            context?.let { it1 ->    //Remove default Google sign in
                GoogleSignIn.getClient(
                    it1, GoogleSignInOptions.Builder(
                        GoogleSignInOptions.DEFAULT_SIGN_IN
                    ).build()
                ).signOut()
            }

            (activity as MainActivity?)?.changeFragment(
                (activity as MainActivity?)!!.supportFragmentManager,
                LoginScreen.newInstance()
            )
        }
        super.onActivityCreated(savedInstanceState)
    }
}