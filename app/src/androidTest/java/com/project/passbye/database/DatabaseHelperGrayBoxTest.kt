package com.project.passbye.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DatabaseHelperGrayBoxTest {

    private lateinit var dbHelper: DatabaseHelper

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        dbHelper = DatabaseHelper(context)
    }

    @After
    fun tearDown() {
        dbHelper.getAllUsers().forEach { username ->
            dbHelper.deleteUser(username)
        }
    }

    @Test
    fun testInsertAndGetUser() {
        val email = "testuser@example.com"
        val username = "testuser"
        val password = "TestPass123"

        val inserted = dbHelper.insertUser(email, username, password)
        assertTrue(inserted)

        val userDetails = dbHelper.getUserDetails(username)
        assertNotNull(userDetails)
        assertEquals(email, userDetails?.email)
        assertEquals(username, userDetails?.username)
    }

    @Test
    fun testSuccessfulLoginVerification() {
        val email = "login@example.com"
        val username = "loginuser"
        val password = "LoginPass123"

        dbHelper.insertUser(email, username, password)

        val verified = dbHelper.verifyUser(username, password)
        assertTrue(verified)
    }

    @Test
    fun testFailedLoginAndLockout() {
        val email = "fail@example.com"
        val username = "failuser"
        val password = "CorrectPassword123"

        dbHelper.insertUser(email, username, password)

        repeat(5) {
            val result = dbHelper.verifyUser(username, "WrongPassword")
            assertFalse(result)
        }

        val lockedOutResult = dbHelper.verifyUser(username, password)
        assertFalse(lockedOutResult)
    }

    @Test
    fun testUnlockUser() {
        val email = "locked@example.com"
        val username = "lockeduser"
        val password = "LockedPass123"

        dbHelper.insertUser(email, username, password)

        repeat(3) {
            dbHelper.verifyUser(username, "WrongPassword")
        }

        val unlocked = dbHelper.unlockUser(username)
        assertTrue(unlocked)

        val loginResult = dbHelper.verifyUser(username, password)
        assertTrue(loginResult)
    }

    @Test
    fun testDeleteUser() {
        val email = "delete@example.com"
        val username = "deleteuser"
        val password = "DeletePass123"

        dbHelper.insertUser(email, username, password)

        val deleted = dbHelper.deleteUser(username)
        assertTrue(deleted)

        val userDetails = dbHelper.getUserDetails(username)
        assertNull(userDetails)
    }

    @Test
    fun testAdminLogin() {
        val correctAdmin = dbHelper.isAdmin("Admin", "Admin_4002!")
        assertTrue(correctAdmin)

        val wrongAdmin = dbHelper.isAdmin("Admin", "WrongPassword")
        assertFalse(wrongAdmin)
    }

    @Test
    fun testInsertDuplicateEmailFails() {
        val email = "duplicate@example.com"
        val username1 = "user1"
        val username2 = "user2"
        val password = "SomePassword123"

        val firstInsert = dbHelper.insertUser(email, username1, password)
        assertTrue(firstInsert)

        val secondInsert = dbHelper.insertUser(email, username2, password)
        assertFalse(secondInsert)
    }

    @Test
    fun testPasswordUpdate() {
        val email = "update@example.com"
        val username = "updateuser"
        val oldPassword = "OldPassword123"
        val newPassword = "NewPassword456"

        dbHelper.insertUser(email, username, oldPassword)

        val updated = dbHelper.updatePassword(username, newPassword)
        assertTrue(updated)

        assertFalse(dbHelper.verifyUser(username, oldPassword))
        assertTrue(dbHelper.verifyUser(username, newPassword))
    }
}
