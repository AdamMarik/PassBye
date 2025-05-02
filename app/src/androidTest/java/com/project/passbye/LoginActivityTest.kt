package com.project.passbye

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.project.passbye.activities.LoginActivity
import com.project.passbye.activities.SignupActivity
import com.project.passbye.database.DatabaseHelper
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(LoginActivity::class.java)

    private lateinit var dbHelper: DatabaseHelper

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        dbHelper = DatabaseHelper(context)

        val email = "adam@gmail.com"
        val username = "adam"
        val password = "adam12345"

        // Insert user manually into database before test
        dbHelper.insertUser(email, username, password)
    }

    @Test
    fun testSuccessfulLogin() {
        onView(withId(R.id.usernameField)).perform(typeText("adam"), closeSoftKeyboard())
        onView(withId(R.id.passwordField)).perform(typeText("adam12345"), closeSoftKeyboard())
        onView(withId(R.id.loginButton)).perform(click())

        onView(withId(R.id.tool_spinner)).check(matches(isDisplayed()))
    }

    @Test
    fun testLoginWithInvalidPassword() {
        onView(withId(R.id.usernameField)).perform(typeText("wronguser"), closeSoftKeyboard())
        onView(withId(R.id.passwordField)).perform(typeText("wrongpass"), closeSoftKeyboard())
        onView(withId(R.id.loginButton)).perform(click())

        onView(withId(R.id.loginStatusText))
            .check(matches(withText("Invalid credentials. Please try again.")))
            .check(matches(isDisplayed()))
    }
}

