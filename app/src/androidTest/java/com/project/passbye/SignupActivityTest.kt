package com.project.passbye

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.project.passbye.activities.SignupActivity
import com.project.passbye.database.DatabaseHelper
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignupActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(SignupActivity::class.java)

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
    fun testSuccessfulSignup() {
        onView(withId(R.id.nameField)).perform(typeText("New User"), closeSoftKeyboard())
        onView(withId(R.id.emailField)).perform(typeText("newuser@example.com"), closeSoftKeyboard())
        onView(withId(R.id.usernameField)).perform(typeText("user"), closeSoftKeyboard())
        onView(withId(R.id.passwordField)).perform(typeText("password123"), closeSoftKeyboard())
        onView(withId(R.id.signupButton)).perform(click())

        onView(withId(R.id.loginButton)).check(matches(isDisplayed()))
    }

    @Test
    fun testPasswordLength() {
        onView(withId(R.id.nameField)).perform(typeText("Mismatch Test"), closeSoftKeyboard())
        onView(withId(R.id.emailField)).perform(typeText("mismatch@example.com"), closeSoftKeyboard())
        onView(withId(R.id.usernameField)).perform(typeText("usertest"), closeSoftKeyboard())
        onView(withId(R.id.passwordField)).perform(typeText("short"), closeSoftKeyboard()) // fails regex
        onView(withId(R.id.signupButton)).perform(click())

        onView(withId(R.id.signupStatusText))
            .check(matches(withText("Password must be at least 8 characters, include letters and numbers.")))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSignupWithExistingEmail() {
        onView(withId(R.id.nameField)).perform(typeText("Adam Marik"), closeSoftKeyboard())
        onView(withId(R.id.emailField)).perform(typeText("adam@gmail.com"), closeSoftKeyboard())
        onView(withId(R.id.usernameField)).perform(typeText("adam"), closeSoftKeyboard())
        onView(withId(R.id.passwordField)).perform(typeText("adamtest1234"), closeSoftKeyboard())
        onView(withId(R.id.signupButton)).perform(click())

        onView(withId(R.id.signupStatusText))
            .check(matches(withText("Email already exists")))
            .check(matches(isDisplayed()))
    }
}

