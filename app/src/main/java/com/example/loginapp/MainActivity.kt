package com.example.loginapp

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.loginapp.fragments.LoginScreen
import com.example.loginapp.fragments.Welcome
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.royrodriguez.transitionbutton.TransitionButton

class MainActivity : AppCompatActivity() {

    companion object {
        private const val RC_SIGN_IN = 9001

        enum class ThemeMode(val value: Int) {
            Light(1),
            Dark(2)
        }

        enum class ThemeButtonState(val state: Boolean) {
            Enabled(true),
            Disabled(false)
        }
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private var transitionButton: TransitionButton? = null
    lateinit var themeButton: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPref = getPreferences(Context.MODE_PRIVATE) ?: return
        val currentThemeMode =
            sharedPref.getInt(getString(R.string.theme_mode), ThemeMode.Light.value)
        AppCompatDelegate.setDefaultNightMode(currentThemeMode)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)


        themeButton = findViewById(R.id.themeMode)
        changeCurrentThemeIcon(currentThemeMode)  //set correct Icon for the theme floating button

        window.statusBarColor = ContextCompat.getColor(applicationContext, R.color.colorPrimary)
        auth = Firebase.auth

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        if (savedInstanceState == null) {
            if (auth.currentUser == null)
                changeFragment(supportFragmentManager, LoginScreen.newInstance())
            else {
                changeFragment(supportFragmentManager, Welcome.newInstance())
            }
        }

        themeButton.setOnClickListener {
            changeThemeCheck()
        }
    }

    /**
     * Handles the icon of the Floating Theme Change Button
     * @param currentThemeMode stores the current theme value
     */
    private fun changeCurrentThemeIcon(currentThemeMode: Int) {
        if (currentThemeMode == ThemeMode.Light.value)
            themeButton.setImageResource(R.drawable.ic_moon)
        else
            themeButton.setImageResource(R.drawable.ic_sun)
    }

    /**
     * Finds the current theme of the UI and changes it to the opposite one
     */
    private fun changeThemeCheck() {
        var k = ThemeMode.Light.value
        when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> {
                k = ThemeMode.Light.value
                recreate()
            }
            Configuration.UI_MODE_NIGHT_NO -> {
                k = ThemeMode.Dark.value
                recreate()
            }
        }
        storeCurrentTheme(k)
    }

    /**
     * Stores the value of the current theme in SharedPreference
     * @param currentThemeMode stores the current theme value
     */
    private fun storeCurrentTheme(currentThemeMode: Int) {
        val sharedPref = getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putInt(getString(R.string.theme_mode), currentThemeMode)
            apply()
        }
    }

    /**
     * Replace fragment to the one passed as argument
     * @param supportFragmentManager is the Fragment manager of the Fragment
     * @param newInstance the new fragment that is to be showm
     */
    fun changeFragment(supportFragmentManager: FragmentManager, newInstance: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.container, newInstance)
            .commitNow()
        themeButton.isEnabled = ThemeButtonState.Enabled.state
    }

    /**
     *  Check if the given email is valid or not
     *  @returns null if no errors
     *  otherwise returns the error found
     */
    fun validateEmail(emailId: TextInputEditText): String? {
        if (isEmpty(emailId.text.toString()))
            return "Enter Email Id"
        else if (!Patterns.EMAIL_ADDRESS.matcher(emailId.text!!).matches())
            return "Enter valid email address"
        return null
    }

    /**
     *  Check if the given password is valid or not
     *  @returns null if no errors
     *  otherwise returns the error found
     */
    fun validatePassword(password: TextInputEditText): String? {
        if (isEmpty(password.text.toString()))
            return "Enter Password"
        else if (password.text!!.length < 6)
            return "Password length should be more than 6 digits"
        return null
    }

    /**
     * Checks if the given string has length 0 or not
     * @param text the string to be checked
     * @return true if empty else returns false
     */
    fun isEmpty(text: String): Boolean {
        return TextUtils.isEmpty(text)
    }

    /**
     *  Google Sign in function
     *  Used in LoginScreen Fragment and SignIn Fragment
     *  @param transitionButton the signUp or logIn button from respective fragments
     */
    fun signIn(transitionButton: TransitionButton) {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
        this.transitionButton = transitionButton
        themeButton.isEnabled = ThemeButtonState.Disabled.state
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            if (task.isSuccessful) {
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    Toast.makeText(applicationContext, "Some Error Occurred", Toast.LENGTH_LONG)
                        .show()
                    stopButtonTransition()
                }
            } else
                stopButtonTransition()
        }
    }

    /**
     * To stop button loading animation and release it.
     */
    private fun stopButtonTransition() {
        transitionButton?.stopAnimation(TransitionButton.StopAnimationStyle.SHAKE, null)
        transitionButton = null
        themeButton.isEnabled = ThemeButtonState.Enabled.state
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    changeFragment(supportFragmentManager, Welcome.newInstance())
                } else {
                    Toast.makeText(applicationContext, "Some Error Occurred", Toast.LENGTH_LONG)
                        .show()
                    stopButtonTransition()
                }
            }
    }
}