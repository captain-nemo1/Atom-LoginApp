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
import com.google.firebase.auth.UserProfileChangeRequest
import com.royrodriguez.transitionbutton.TransitionButton

/**
 * Fragment for Sign Up page
 */
class SignIn : Fragment() {

    companion object {
        fun newInstance() = SignIn()
    }

    private lateinit var signUpButton: TransitionButton
    private lateinit var loginPageButton: Button
    private lateinit var googleSignIn: FloatingActionButton
    private lateinit var name: TextInputEditText
    private lateinit var emailId: TextInputEditText
    private lateinit var password: TextInputEditText
    private lateinit var confirmPassword: TextInputEditText
    private lateinit var nameLayout: TextInputLayout
    private lateinit var emailIdLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var confirmPasswordLayout: TextInputLayout
    private lateinit var errorText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.sign_in_fragment, container, false)
        loginPageButton = root.findViewById(R.id.loginPageButton)
        signUpButton = root.findViewById(R.id.signUpButton)
        googleSignIn = root.findViewById(R.id.googleSignIn)
        name = root.findViewById(R.id.namEditText)
        emailId = root.findViewById(R.id.emailIdEditText)
        password = root.findViewById(R.id.passwordEditText)
        confirmPassword = root.findViewById(R.id.confirmpasswordEditText)
        nameLayout = root.findViewById(R.id.nameInputLayout)
        emailIdLayout = root.findViewById(R.id.emailInputLayout)
        passwordLayout = root.findViewById(R.id.passwordInputLayout)
        confirmPasswordLayout = root.findViewById(R.id.confirmpasswordInputLayout)
        errorText = root.findViewById(R.id.errorText)
        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        removeErrorsOnTextChange()

        signUpButton.setOnClickListener {
            checkForErrors()
            hideKeyboard()
        }

        loginPageButton.setOnClickListener {
            (activity as MainActivity?)?.changeFragment(
                activity!!.supportFragmentManager,
                LoginScreen.newInstance()
            )
        }

        googleSignIn.setOnClickListener {
            (activity as MainActivity?)?.signIn(signUpButton)
            signUpButton.startAnimation()
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
    private fun removeErrorsOnTextChange() {

        nameLayout.editText?.doOnTextChanged { _, _, _, _ ->
            nameLayout.error = null
        }

        emailIdLayout.editText?.doOnTextChanged { _, _, _, _ ->
            emailIdLayout.error = null
        }

        passwordLayout.editText?.doOnTextChanged { _, _, _, _ ->
            passwordLayout.error = null
        }
        confirmPasswordLayout.editText?.doOnTextChanged { _, _, _, _ ->
            confirmPasswordLayout.error = null
        }
    }

    /**
     * Checks for error in the data entered by the user
     * If no errors found then creates a new user
     */
    private fun checkForErrors() {
        nameLayout.error =
            if ((activity as MainActivity?)?.isEmpty(name.text.toString()) == true)
                "Enter Name"
            else
                null
        emailIdLayout.error = (activity as MainActivity?)?.validateEmail(emailId)
        passwordLayout.error = (activity as MainActivity?)?.validatePassword(password)
        confirmPasswordLayout.error = samePasswordEntered(password, confirmPassword)
        if (nameLayout.error == null && emailIdLayout.error == null && passwordLayout.error == null
            && confirmPasswordLayout.error == null
        ) {
            signUpButton.startAnimation()
            (activity as MainActivity?)?.themeButton?.isEnabled =
                MainActivity.Companion.ThemeButtonState.Disabled.state
            createUser(
                name.text.toString().trim(),
                emailId.text.toString().trim(),
                password.text.toString().trim(),
                signUpButton
            )
        }
    }

    /**
     *  Creates a new user and adds it to Firebase
     *  @param name name of the current User
     *  @param email email of the current User
     *  @param password password of the current User
     *  @param signUp Transition Button
     */
    private fun createUser(
        name: String,
        email: String,
        password: String,
        signUpButton: TransitionButton
    ) {
        (activity as MainActivity?)?.fixOrientationWhenWorking()
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { signIn ->
                if (signIn.isSuccessful) {
                    addNameToData(FirebaseAuth.getInstance(), name)
                } else {
                    signUpButton.stopAnimation(TransitionButton.StopAnimationStyle.SHAKE, null)
                    if (signIn.exception!!.message!!.contains("Unable to resolve host"))
                        errorText.text = getString(R.string.no_internet)
                    else
                        errorText.text = signIn.exception!!.message
                    errorText.visibility = View.VISIBLE
                    (activity as MainActivity?)?.themeButton?.isEnabled =
                        MainActivity.Companion.ThemeButtonState.Enabled.state
                    (activity as MainActivity?)?.releaseOrientationAfterWorkDone()
                }
            }
    }

    /**
     * Adds the name of the currentUser to the Firebase Authentication data
     * and takes to Welcome Fragment
     * @param auth current Firebase Authentication instance
     * @param name name of the current user
     */
    private fun addNameToData(auth: FirebaseAuth, name: String) {
        val user = auth.currentUser
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()
        user!!.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    (activity as MainActivity?)?.changeFragment(
                        activity!!.supportFragmentManager,
                        Welcome.newInstance()
                    )
                }
            }
    }

    /**
     *  Checks if the password and confirmPassword are same or not
     *  @returns null if same
     *  otherwise returns the error
     *  @param password EditText of the password field
     *  @param confirmPassword EditText of the confirm password field
     */
    private fun samePasswordEntered(
        password: TextInputEditText,
        confirmPassword: TextInputEditText
    ): String? {
        return if (password.text.toString() == confirmPassword.text.toString())
            null
        else
            "Password do not match"
    }
}