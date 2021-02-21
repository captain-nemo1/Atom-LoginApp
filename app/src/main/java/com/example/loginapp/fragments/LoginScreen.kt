package com.example.loginapp.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import com.example.loginapp.MainActivity
import com.example.loginapp.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.royrodriguez.transitionbutton.TransitionButton

class LoginScreen : Fragment() {

    companion object {
        fun newInstance() = LoginScreen()
    }

    private lateinit var logInButton: TransitionButton
    private lateinit var signUpButton: Button
    private lateinit var googleSignIn: FloatingActionButton
    private lateinit var emailId: TextInputEditText
    private lateinit var password: TextInputEditText
    private lateinit var emailIdLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout

    private lateinit var errorText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.login_screen_fragment, container, false)
        signUpButton = root.findViewById(R.id.signupButton)
        logInButton = root.findViewById(R.id.logInButton)
        googleSignIn = root.findViewById(R.id.googleSignInButton)
        emailId = root.findViewById(R.id.emailIdEditText)
        password = root.findViewById(R.id.passwordEditText)
        emailIdLayout = root.findViewById(R.id.emailIdInputLayout)
        passwordLayout = root.findViewById(R.id.passwordInputLayout)
        errorText = root.findViewById(R.id.errorText)
        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        errorCheckAndRemove()

        logInButton.setOnClickListener {
            checkForErrors()
            hideKeyboard()
        }

        googleSignIn.setOnClickListener {
            (activity as MainActivity?)?.signIn(logInButton)
            logInButton.startAnimation()
        }

        signUpButton.setOnClickListener {
            (activity as MainActivity?)?.changeFragment(
                activity!!.supportFragmentManager,
                SignIn.newInstance()
            )
        }
    }

    /**
     *  Hides soft keyboard if it's visible
     */
    private fun hideKeyboard() {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view!!.windowToken, 0)
    }

    /**
     * Removes error from the respective edit text as soon as user starts typing in them
     */
    private fun errorCheckAndRemove() {
        emailIdLayout.editText?.doOnTextChanged { _, _, _, _ ->
            emailIdLayout.error = null
        }
        passwordLayout.editText?.doOnTextChanged { _, _, _, _ ->
            passwordLayout.error = null
        }
    }

    /**
     * Check for errors in the data entered by the user
     * If no error found then starts the log in user process
     */
    private fun checkForErrors() {
        emailIdLayout.error = (activity as MainActivity?)?.validateEmail(emailId)
        passwordLayout.error = (activity as MainActivity?)?.validatePassword(password)
        if (emailIdLayout.error == null && passwordLayout.error == null) {
            logInButton.startAnimation()
            (activity as MainActivity?)?.themeButton?.isEnabled =
                MainActivity.Companion.ThemeButtonState.Disabled.state
            logInUser(
                emailId.text.toString().trim(),
                password.text.toString().trim(),
                logInButton
            )
        }
    }

    /**
     * Logs in the user if he has an account already
     * @param email email of the current User
     * @param password password of the current User
     * @param logInButton Transition button
     */
    private fun logInUser(
        email: String,
        password: String,
        logInButton: TransitionButton
    ) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { signIn ->
                if (signIn.isSuccessful) {
                    (activity as MainActivity?)?.changeFragment(
                        activity!!.supportFragmentManager,
                        Welcome.newInstance()
                    )
                } else {
                    logInButton.stopAnimation(TransitionButton.StopAnimationStyle.SHAKE, null)
                    if (signIn.exception!!.message!!.contains("Unable to resolve host"))
                        errorText.text = getString(R.string.no_internet)
                    else
                        errorText.text = signIn.exception!!.message
                    errorText.visibility = View.VISIBLE
                    (activity as MainActivity?)?.themeButton?.isEnabled =
                        MainActivity.Companion.ThemeButtonState.Enabled.state
                }
            }
    }

}